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
import org.cangnova.cangjie.project.event.CjProjectEvent
import org.cangnova.cangjie.project.event.CjProjectListener

/**
 * 宏展开项目同步监听器
 *
 * 监听项目同步完成事件，在同步完成后触发全项目宏展开缓存预热。
 * 这确保了宏编译完成后（由 [org.cangnova.cangjie.macro.autocompile.MacroAutoCompilationListener] 处理），
 * 宏展开结果能及时被解析并集成到分析管线中。
 *
 * 通过 `cangjie.macro.expansion.analysis.enabled` 控制是否启用。
 */
class MacroExpansionProjectSyncListener(
    private val project: Project
) : CjProjectListener {

    companion object {
        private val LOG = Logger.getInstance(MacroExpansionProjectSyncListener::class.java)
    }

    override fun projectSynced(event: CjProjectEvent) {
        if (!isEnabled()) return

        LOG.info("项目同步完成，开始宏展开缓存预热: ${event.project.name}")

        // 清除旧缓存
        MacroExpandedPsiCache.getInstance(project).clearAll()
        MacroExpandedDescriptorProvider.getInstance(project).clearAll()

        // 触发全项目宏展开
        MacroExpansionBackgroundTask.runForProject(project)
    }

    private fun isEnabled(): Boolean {
        return try {
            Registry.`is`("cangjie.macro.expansion.analysis.enabled", false)
        } catch (e: Exception) {
            false
        }
    }
}
