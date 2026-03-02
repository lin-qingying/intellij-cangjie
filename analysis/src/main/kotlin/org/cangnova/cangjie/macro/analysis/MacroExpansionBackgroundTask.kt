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

package org.cangnova.cangjie.macro.analysis

import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.registry.Registry
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.search.FileTypeIndex
import com.intellij.psi.search.GlobalSearchScope
import kotlinx.coroutines.runBlocking
import org.cangnova.cangjie.lang.CangJieFileType
import org.cangnova.cangjie.macro.service.MacroExpansionOptions
import org.cangnova.cangjie.macro.service.MacroExpansionService
import org.cangnova.cangjie.result.CjResult

/**
 * 宏展开后台任务
 *
 * 遍历项目中的所有 `.cj` 文件，调用 [MacroExpansionService.expandAllMacrosInFile]
 * 展开宏并将结果存入 [MacroExpandedPsiCache]，用于预热缓存。
 *
 * 仅在 `cangjie.macro.expansion.analysis.enabled` 启用时执行。
 */
class MacroExpansionBackgroundTask(
    private val project: Project,
    private val files: Collection<VirtualFile>? = null
) {

    companion object {
        private val LOG = Logger.getInstance(MacroExpansionBackgroundTask::class.java)

        /**
         * 触发全项目宏展开后台任务
         */
        fun runForProject(project: Project) {
            if (!isAnalysisEnabled()) return

            val expansionService = MacroExpansionService.getInstance(project)
            if (!expansionService.isAvailable()) {
                LOG.info("宏展开服务不可用，跳过后台预热")
                return
            }

            MacroExpansionBackgroundTask(project).run()
        }

        /**
         * 触发单文件宏展开后台任务
         */
        fun runForFiles(project: Project, files: Collection<VirtualFile>) {
            if (!isAnalysisEnabled()) return
            if (files.isEmpty()) return

            val expansionService = MacroExpansionService.getInstance(project)
            if (!expansionService.isAvailable()) return

            MacroExpansionBackgroundTask(project, files).run()
        }

        private fun isAnalysisEnabled(): Boolean {
            return try {
                Registry.`is`("cangjie.macro.expansion.analysis.enabled", false)
            } catch (e: Exception) {
                false
            }
        }
    }

    fun run() {
        ProgressManager.getInstance().run(object : Task.Backgroundable(
            project,
            "展开项目宏表达式...",
            true
        ) {
            override fun run(indicator: ProgressIndicator) {
                val targetFiles = files ?: collectCangJieFiles()
                if (targetFiles.isEmpty()) return

                val expansionService = MacroExpansionService.getInstance(project)
                val psiCache = MacroExpandedPsiCache.getInstance(project)

                val total = targetFiles.size
                var processed = 0
                var expanded = 0

                for (file in targetFiles) {
                    indicator.checkCanceled()
                    indicator.fraction = processed.toDouble() / total
                    indicator.text2 = file.name

                    try {
                        val result = runBlocking {
                            expansionService.expandAllMacrosInFile(
                                file,
                                MacroExpansionOptions.NO_AUTO_COMPILE
                            )
                        }

                        when (result) {
                            is CjResult.Ok -> {
                                val results = result.ok
                                if (results.isNotEmpty()) {
                                    // 预热 PSI 缓存
                                    psiCache.getOrCreateAllPsi(results, file)
                                    expanded += results.size
                                }
                            }
                            is CjResult.Err -> {
                                LOG.debug("文件 ${file.name} 宏展开失败: ${result.err}")
                            }
                        }
                    } catch (e: Exception) {
                        LOG.debug("文件 ${file.name} 宏展开异常", e)
                    }

                    processed++
                }

                LOG.info("宏展开预热完成: 处理 $processed 个文件，展开 $expanded 个宏")
            }
        })
    }

    /**
     * 收集项目中所有 .cj 文件
     */
    private fun collectCangJieFiles(): Collection<VirtualFile> {
        return try {
            ReadAction.compute<Collection<VirtualFile>, Exception> {
                FileTypeIndex.getFiles(CangJieFileType.INSTANCE, GlobalSearchScope.projectScope(project))
            }
        } catch (e: Exception) {
            LOG.warn("收集项目文件失败", e)
            emptyList()
        }
    }
}
