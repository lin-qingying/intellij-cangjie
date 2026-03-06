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
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit
import java.util.concurrent.ConcurrentHashMap

/**
 * 宏展开文件变更监听器
 *
 * 监听 `.cj` 文件的变更事件，当文件保存时延迟触发该文件的宏重新展开。
 * 使用防抖（debounce）机制避免频繁触发。
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

    /**
     * 待处理的文件防抖任务
     */
    private val pendingTasks = ConcurrentHashMap<String, ScheduledFuture<*>>()

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

        for (file in changedCjFiles) {
            scheduleExpansion(file)
        }
    }

    /**
     * 调度延迟展开任务（带防抖）
     */
    private fun scheduleExpansion(file: VirtualFile) {
        val filePath = file.path

        // 取消之前的任务
        pendingTasks.remove(filePath)?.cancel(false)

        // 立即失效展开文件缓存
        MacroExpandedFileManager.getInstance(project).invalidate(filePath)

        // 调度新任务
        val future = scheduler.schedule({
            pendingTasks.remove(filePath)
            try {
                MacroExpansionBackgroundTask.runForFiles(project, listOf(file))
            } catch (e: Exception) {
                LOG.debug("文件 ${file.name} 宏展开调度失败", e)
            }
        }, DEBOUNCE_DELAY_MS, TimeUnit.MILLISECONDS)

        pendingTasks[filePath] = future
    }

    private fun isEnabled(): Boolean {
        return try {
            Registry.`is`("cangjie.macro.expansion.analysis.enabled", false)
        } catch (e: Exception) {
            false
        }
    }
}
