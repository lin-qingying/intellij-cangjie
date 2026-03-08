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

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.newvfs.BulkFileListener
import com.intellij.openapi.vfs.newvfs.events.VFileContentChangeEvent
import com.intellij.openapi.vfs.newvfs.events.VFileCreateEvent
import com.intellij.openapi.vfs.newvfs.events.VFileEvent
import org.cangnova.cangjie.lang.CangJieFileType

/**
 * 宏管线文件变更监听器
 *
 * 监听 `.cj` 文件的内容变更和创建事件，通过 [MacroPipelineCoordinator]
 * 触发增量管线（按需编译 → 展开变更文件）。
 *
 * 替代被移除的 `MacroExpansionFileListener`，确保文件编辑后宏展开自动更新。
 *
 * 注册方式：在 `cangjie-macro.xml` 中作为 `projectListeners` 注册，
 * 订阅 `VirtualFileManager.VFS_CHANGES` 主题。
 */
class MacroFileChangeListener(private val project: Project) : BulkFileListener {

    companion object {
        private val LOG = Logger.getInstance(MacroFileChangeListener::class.java)
    }

    override fun after(events: List<VFileEvent>) {
        if (project.isDisposed) return

        val changedCjFiles = events.mapNotNull { event ->
            when (event) {
                is VFileContentChangeEvent -> event.file
                is VFileCreateEvent -> event.file
                else -> null
            }
        }.filter { file ->
            file.extension == CangJieFileType.EXTENSION
        }

        if (changedCjFiles.isEmpty()) return

        LOG.debug("检测到 ${changedCjFiles.size} 个 .cj 文件变更，触发增量管线")
        MacroPipelineCoordinator.getInstance(project).scheduleIncrementalPipeline(changedCjFiles)
    }
}
