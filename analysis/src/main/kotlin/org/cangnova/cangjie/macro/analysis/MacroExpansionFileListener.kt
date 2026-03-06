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

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.registry.Registry
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.newvfs.BulkFileListener
import com.intellij.openapi.vfs.newvfs.events.VFileContentChangeEvent
import com.intellij.openapi.vfs.newvfs.events.VFileEvent
import com.intellij.util.concurrency.AppExecutorUtil
import org.cangnova.cangjie.macro.expanded.MacroExpandedFileManager
import org.cangnova.cangjie.macro.pipeline.MacroPipelineCoordinator
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit
import java.util.concurrent.ConcurrentHashMap

/**
 * 宏展开文件变更监听器
 *
 * 监听 `.cj` 文件的变更事件，使用批量防抖机制收集所有变更文件，
 * 然后通过 [MacroPipelineCoordinator] 触发增量管线。
 *
 * 协调器会区分宏源文件和普通文件，按需触发重编译或仅展开。
 *
 * 通过 `cangjie.macro.expansion.analysis.enabled` 控制是否启用。
 */
class MacroExpansionFileListener(
    private val project: Project
) : BulkFileListener {

    companion object {
        private val LOG = Logger.getInstance(MacroExpansionFileListener::class.java)
        private const val DEBOUNCE_DELAY_MS = 2000L
        private const val CJ_EXTENSION = "cj"
    }

    /** 批量收集的待处理文件 */
    private val pendingFiles = ConcurrentHashMap.newKeySet<VirtualFile>()

    /** 批量防抖任务 */
    @Volatile
    private var batchFuture: ScheduledFuture<*>? = null

    private val scheduler = AppExecutorUtil.getAppScheduledExecutorService()

    override fun after(events: List<VFileEvent>) {
        if (!isEnabled()) return

        val changedCjFiles = mutableSetOf<VirtualFile>()

        for (event in events) {
            if (event !is VFileContentChangeEvent) continue
            val file = event.file
            if (file.extension != CJ_EXTENSION) continue
            changedCjFiles.add(file)
        }

        if (changedCjFiles.isEmpty()) return

        // 立即失效展开文件缓存
        val fileManager = MacroExpandedFileManager.getInstance(project)
        for (file in changedCjFiles) {
            fileManager.invalidate(file.path)
        }

        // 收集到批量待处理集合
        pendingFiles.addAll(changedCjFiles)

        // 重置防抖定时器
        scheduleBatchPipeline()
    }

    /**
     * 调度批量管线（带防抖）
     *
     * 每次有新文件变更时重置 2s 定时器，
     * 定时器到期后一次性提交所有收集到的文件给协调器。
     */
    private fun scheduleBatchPipeline() {
        batchFuture?.cancel(false)

        batchFuture = scheduler.schedule({
            batchFuture = null
            val files = pendingFiles.toList()
            pendingFiles.clear()

            if (files.isEmpty()) return@schedule

            try {
                LOG.info("批量防抖完成，提交 ${files.size} 个文件到增量管线")
                MacroPipelineCoordinator.getInstance(project).scheduleIncrementalPipeline(files)
            } catch (e: Exception) {
                LOG.debug("增量管线调度失败", e)
            }
        }, DEBOUNCE_DELAY_MS, TimeUnit.MILLISECONDS)
    }

    private fun isEnabled(): Boolean {
        return try {
            Registry.`is`("cangjie.macro.expansion.analysis.enabled", false)
        } catch (e: Exception) {
            false
        }
    }
}
