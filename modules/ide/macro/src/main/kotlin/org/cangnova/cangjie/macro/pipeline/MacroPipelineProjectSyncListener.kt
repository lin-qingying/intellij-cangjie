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
import com.intellij.openapi.util.registry.Registry
import org.cangnova.cangjie.project.event.CjProjectEvent
import org.cangnova.cangjie.project.event.CjProjectListener

/**
 * 宏管线项目同步监听器
 *
 * 监听项目同步完成事件，通过 [MacroPipelineCoordinator] 触发全量管线（编译 → 展开）。
 * 替代原来分离的宏自动编译监听器和宏展开项目同步监听器，
 * 确保编译和展开的时序正确。
 *
 * 通过 `cangjie.macro.expansion.analysis.enabled` 控制是否启用展开阶段。
 */
class MacroPipelineProjectSyncListener(
    private val project: Project
) : CjProjectListener {

    companion object {
        private val LOG = Logger.getInstance(MacroPipelineProjectSyncListener::class.java)
    }

    override fun projectSynced(event: CjProjectEvent) {
        LOG.info("项目同步完成，触发宏管线: ${event.project.name}")
        MacroPipelineCoordinator.getInstance(project).scheduleFullPipeline()
    }
}
