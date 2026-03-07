/*
 * Copyright 2026 LinQingYing. and contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.cangnova.cangjie.macro.pipeline

import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.cangnova.cangjie.macro.analysis.MacroExpansionBackgroundTask
import org.cangnova.cangjie.macro.compiler.MacroDeclarationLocator
import org.cangnova.cangjie.macro.expanded.MacroExpandedFileManager
import org.cangnova.cangjie.macro.service.MacroCompilationOptions
import org.cangnova.cangjie.macro.service.MacroCompilationService
import org.cangnova.cangjie.macro.service.MacroExpansionService
import org.cangnova.cangjie.result.CjResult

/**
 * 宏编译/展开管线协调器实现
 *
 * 通过 [compilationMutex] 保证同一时刻只有一个编译任务执行，
 * 通过 [compilationGate] 让展开任务等待当前编译完成。
 */
@Service(Service.Level.PROJECT)
internal class MacroPipelineCoordinatorImpl(
    private val project: Project
) : MacroPipelineCoordinator {

    companion object {
        private val LOG = Logger.getInstance(MacroPipelineCoordinatorImpl::class.java)
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /** 保证同一时刻只有一个编译任务执行 */
    private val compilationMutex = Mutex()

    /** 展开任务通过 await() 等待当前编译完成；null 表示无编译进行 */
    @Volatile
    private var compilationGate: CompletableDeferred<Boolean>? = null

    /** 当前全量管线 Job，可取消旧的（快速连续触发时） */
    @Volatile
    private var fullPipelineJob: Job? = null

    /** 缓存已知宏包目录路径，避免频繁扫描 */
    @Volatile
    private var macroPackageDirsCache: Set<String>? = null

    override fun scheduleFullPipeline() {
        // 取消旧的全量管线
        fullPipelineJob?.cancel()

        fullPipelineJob = scope.launch {
            LOG.info("全量管线启动: 编译 → 展开")

            // 等待索引完成，避免 Stub 访问时出现 Outdated stub 异常
            DumbService.getInstance(project).waitForSmartMode()

            val compilationService = MacroCompilationService.getInstance(project)
            if (!compilationService.isAvailable()) {
                LOG.info("宏编译服务不可用，跳过全量管线")
                return@launch
            }

            // 编译阶段
            val compiled = executeCompilation(forceRecompile = false)
            if (!compiled) {
                LOG.warn("宏编译失败，跳过展开阶段")
            }

            // 清除缓存并触发全项目展开
            MacroExpandedFileManager.getInstance(project).clearAll()
            MacroExpansionService.getInstance(project).clearCache()

            MacroExpansionBackgroundTask.runForProject(project)
            LOG.info("全量管线完成")
        }
    }

    override fun scheduleIncrementalPipeline(changedFiles: Collection<VirtualFile>) {
        if (changedFiles.isEmpty()) return

        scope.launch {
            LOG.info("增量管线启动: ${changedFiles.size} 个文件变更")

            // 等待索引完成，避免 Stub 访问时出现 Outdated stub 异常
            DumbService.getInstance(project).waitForSmartMode()

            val compilationService = MacroCompilationService.getInstance(project)
            if (!compilationService.isAvailable()) {
                LOG.info("宏编译服务不可用，跳过增量管线")
                return@launch
            }

            // 区分宏源文件和普通文件
            val macroPackageDirs = getMacroPackageDirs()
            val (macroSourceFiles, normalFiles) = partitionMacroSourceFiles(changedFiles, macroPackageDirs)

            if (macroSourceFiles.isNotEmpty()) {
                // 宏源文件变更：重编译 → 全项目展开
                LOG.info("检测到 ${macroSourceFiles.size} 个宏源文件变更，触发重编译")
                val compiled = executeCompilation(forceRecompile = true)
                if (!compiled) {
                    LOG.warn("宏重编译失败")
                }
                // 宏源文件变更影响面广，需要全项目展开
                MacroExpandedFileManager.getInstance(project).clearAll()
                MacroExpansionService.getInstance(project).clearCache()
                MacroExpansionBackgroundTask.runForProject(project)
            } else if (normalFiles.isNotEmpty()) {
                // 普通文件变更：检查产物 → 仅展开变更文件
                val libsReady = ensureMacroLibsReady()
                if (!libsReady) {
                    LOG.warn("宏动态库不可用，跳过文件展开")
                    return@launch
                }
                MacroExpansionBackgroundTask.runForFiles(project, normalFiles)
            }

            LOG.info("增量管线完成")
        }
    }

    override suspend fun ensureMacroLibsReady(): Boolean {
        // 如果有正在进行的编译，先等待完成
        compilationGate?.let { gate ->
            LOG.info("等待当前编译完成...")
            return gate.await()
        }

        val compilationService = MacroCompilationService.getInstance(project)
        if (!compilationService.isAvailable()) return false

        // 检查产物是否存在
        if (compilationService.hasCompiledMacroLibs()) return true

        // 产物缺失，触发编译
        LOG.info("宏动态库缺失，触发编译")
        return executeCompilation(forceRecompile = false)
    }

    /**
     * 执行编译（互斥保护）
     *
     * @return true 表示编译成功，false 表示编译失败
     */
    private suspend fun executeCompilation(forceRecompile: Boolean): Boolean {
        val gate = CompletableDeferred<Boolean>()

        return compilationMutex.withLock {
            compilationGate = gate
            try {
                val service = MacroCompilationService.getInstance(project)
                val options = MacroCompilationOptions(forceRecompile = forceRecompile)
                val result = service.compileAllMacrosInProject(options)

                val success = when (result) {
                    is CjResult.Ok -> {
                        val data = result.ok
                        // 编译成功后清除宏包目录缓存，下次重新扫描
                        macroPackageDirsCache = null
                        LOG.info(
                            "宏编译完成: 编译了 ${data.compiledFiles.size} 个包, " +
                                    "生成 ${data.outputFiles.size} 个输出文件, " +
                                    "耗时 ${data.compilationTimeMs}ms"
                        )
                        true
                    }
                    is CjResult.Err -> {
                        LOG.warn("宏编译失败: ${result.err.message}")
                        false
                    }
                }

                gate.complete(success)
                success
            } catch (e: Exception) {
                LOG.warn("宏编译过程中发生异常", e)
                gate.complete(false)
                false
            } finally {
                compilationGate = null
            }
        }
    }

    /**
     * 获取已知宏包目录（带缓存）
     */
    private fun getMacroPackageDirs(): Set<String> {
        macroPackageDirsCache?.let { return it }

        val dirs = MacroDeclarationLocator.EP_NAME.extensionList
            .sortedByDescending { it.priority }
            .firstNotNullOfOrNull { locator ->
                locator.findMacroPackageDirs(project, null)
            }
            ?.toSet()
            ?: emptySet()

        macroPackageDirsCache = dirs
        return dirs
    }

    /**
     * 将变更文件分为宏源文件和普通文件
     *
     * 如果变更文件的父目录在已知宏包目录中，则视为宏源文件。
     */
    private fun partitionMacroSourceFiles(
        files: Collection<VirtualFile>,
        macroPackageDirs: Set<String>
    ): Pair<List<VirtualFile>, List<VirtualFile>> {
        val macroFiles = mutableListOf<VirtualFile>()
        val normalFiles = mutableListOf<VirtualFile>()

        for (file in files) {
            val parentPath = file.parent?.path
            if (parentPath != null && parentPath in macroPackageDirs) {
                macroFiles.add(file)
            } else {
                normalFiles.add(file)
            }
        }

        return Pair(macroFiles, normalFiles)
    }
}
