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
import com.intellij.openapi.util.registry.Registry
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.cangnova.cangjie.macro.analysis.MacroExpansionBackgroundTask
import org.cangnova.cangjie.macro.compiler.MacroDeclarationLocator
import org.cangnova.cangjie.macro.messages.CangJieMacroBundle
import org.cangnova.cangjie.macro.psi.MacroPsiExpansionService
import org.cangnova.cangjie.macro.service.*
import org.cangnova.cangjie.result.CjResult

/**
 * 宏编译/展开管线协调器实现
 *
 * 通过 [compilationMutex] 保证同一时刻只有一个编译任务执行，
 * 通过 [compilationGate] 让展开任务等待当前编译完成。
 *
 * 由两个 Registry Key 分别控制：
 * - `cangjie.macro.expansion.auto.compile`：是否自动编译宏包
 * - `cangjie.macro.expansion.analysis.enabled`：是否自动展开宏并注入分析结果
 *
 * 展开依赖编译：如果启用了展开但未启用编译，展开仍会执行（LspMacroServer 可独立工作），
 * 但 cjc 引擎需要的动态库可能缺失。
 */
@Service(Service.Level.PROJECT)
internal class MacroPipelineCoordinatorImpl(
    private val project: Project
) : MacroPipelineCoordinator {

    companion object {
        private val LOG = Logger.getInstance(MacroPipelineCoordinatorImpl::class.java)

        /** 是否自动编译宏包 */
        private const val KEY_AUTO_COMPILE = "cangjie.macro.expansion.auto.compile"

        /** 是否自动展开宏并注入分析结果 */
        private const val KEY_EXPANSION_ANALYSIS = "cangjie.macro.expansion.analysis.enabled"
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
        val autoCompile = isAutoCompileEnabled()
        val expansionAnalysis = isExpansionAnalysisEnabled()
        if (!autoCompile && !expansionAnalysis) return

        // 取消旧的全量管线
        fullPipelineJob?.cancel()

        fullPipelineJob = scope.launch {
            LOG.info("全量管线启动: 编译=${autoCompile}, 展开=${expansionAnalysis}")

            // 等待索引完成，避免 Stub 访问时出现 Outdated stub 异常
            DumbService.getInstance(project).waitForSmartMode()

            // 编译阶段
            if (autoCompile) {
                val compilationService = MacroCompilationService.getInstance(project)
                if (compilationService.isAvailable()) {
                    val compiled = executeCompilation(forceRecompile = false)
                    if (!compiled) {
                        LOG.warn("宏编译失败，LspMacroServer 引擎仍可继续展开")
                    }
                } else {
                    LOG.info("宏编译服务不可用，跳过编译阶段")
                }
            }

            // 展开阶段
            if (expansionAnalysis) {
                MacroExpansionService.getInstance(project).clearCache()
                MacroPsiExpansionService.getInstance(project).clearCache()
                MacroExpansionBackgroundTask.runForProject(project)
            }

            LOG.info("全量管线完成")
        }
    }

    override fun scheduleIncrementalPipeline(changedFiles: Collection<VirtualFile>) {
        if (changedFiles.isEmpty()) return

        val autoCompile = isAutoCompileEnabled()
        val expansionAnalysis = isExpansionAnalysisEnabled()
        if (!autoCompile && !expansionAnalysis) return

        scope.launch {
            LOG.info("增量管线启动: ${changedFiles.size} 个文件变更, 编译=${autoCompile}, 展开=${expansionAnalysis}")

            // 等待索引完成，避免 Stub 访问时出现 Outdated stub 异常
            DumbService.getInstance(project).waitForSmartMode()

            // 区分宏源文件和普通文件
            val macroPackageDirs = getMacroPackageDirs()
            val (macroSourceFiles, normalFiles) = partitionMacroSourceFiles(changedFiles, macroPackageDirs)

            if (macroSourceFiles.isNotEmpty()) {
                // 宏源文件变更：尝试重编译 → 全项目展开
                LOG.info("检测到 ${macroSourceFiles.size} 个宏源文件变更")

                if (autoCompile) {
                    val compilationService = MacroCompilationService.getInstance(project)
                    if (compilationService.isAvailable()) {
                        val compiled = executeCompilation(forceRecompile = true)
                        if (!compiled) {
                            LOG.warn("宏重编译失败，LspMacroServer 引擎仍可继续展开")
                        }
                    } else {
                        LOG.info("宏编译服务不可用，跳过编译阶段")
                    }
                }

                if (expansionAnalysis) {
                    MacroExpansionService.getInstance(project).clearCache()
                    MacroPsiExpansionService.getInstance(project).clearCache()
                    MacroExpansionBackgroundTask.runForProject(project)
                }
            } else if (normalFiles.isNotEmpty() && expansionAnalysis) {
                // 普通文件变更：确保宏动态库就绪后展开变更文件
                if (autoCompile) {
                    ensureMacroLibsReady()
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
                        notifyCompilationFailed(result.err)
                        false
                    }
                }

                gate.complete(success)
                success
            } catch (e: Exception) {
                LOG.warn("宏编译过程中发生异常", e)
                notifyCompilationFailed(e)
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

    private fun isAutoCompileEnabled(): Boolean {
        return try {
            Registry.`is`(KEY_AUTO_COMPILE, true)
        } catch (e: Exception) {
            true
        }
    }

    private fun isExpansionAnalysisEnabled(): Boolean {
        return try {
            Registry.`is`(KEY_EXPANSION_ANALYSIS, false)
        } catch (e: Exception) {
            false
        }
    }

    /**
     * 编译失败时向用户发送友好通知
     */
    private fun notifyCompilationFailed(error: Throwable) {
        val content = when (error) {
            is MacroCompilationError.SdkNotConfigured ->
                CangJieMacroBundle.message("macro.notification.compilation.failed.sdk")
            is MacroCompilationError.CompilerUnavailable ->
                CangJieMacroBundle.message("macro.notification.compilation.failed.compiler", error.reason)
            is MacroCompilationError.CompilationFailed ->
                CangJieMacroBundle.message("macro.notification.compilation.failed.compile.error", error.exitCode, error.stderr)
            is MacroCompilationError.Timeout ->
                CangJieMacroBundle.message("macro.notification.compilation.failed.timeout", error.timeoutMs)
            is MacroCompilationError.NoMacrosFound ->
                CangJieMacroBundle.message("macro.notification.compilation.failed.no.macros", error.filePath)
            is MacroCompilationError.InternalError ->
                CangJieMacroBundle.message("macro.notification.compilation.failed.internal", error.details)
            else ->
                CangJieMacroBundle.message("macro.notification.compilation.failed.unknown", error.message ?: "")
        }

        val title = CangJieMacroBundle.message("macro.notification.compilation.failed.title")

        try {
            NotificationGroupManager.getInstance()
                .getNotificationGroup("CangJie Macro")
                .createNotification(title, content, NotificationType.WARNING)
                .notify(project)
        } catch (e: Exception) {
            LOG.warn("无法发送宏编译失败通知", e)
        }
    }
}
