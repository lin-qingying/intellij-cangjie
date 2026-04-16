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
 *
 * The use of this source code is governed by the Apache License 2.0,
 * which allows users to freely use, modify, and distribute the code,
 * provided they adhere to the terms of the license.
 *
 * The software is provided "as-is", and the authors are not responsible for
 * any damages or issues arising from its use.
 *
 */

package org.cangnova.cangjie.project.listener

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile

import org.cangnova.cangjie.project.event.CjProjectEvent
import org.cangnova.cangjie.project.event.CjProjectEventType
import org.cangnova.cangjie.project.event.CjProjectListener
import org.cangnova.cangjie.project.model.CjProject
import org.cangnova.cangjie.project.service.CjProjectsService

/**
 * Cangjie项目配置监听器
 *
 * 监听项目事件，在项目被发现或更新时自动配置源代码目录
 */
class CjProjectConfigurationListener : CjProjectListener {

    override fun projectCreated(event: CjProjectEvent) {
        when (event.eventType) {
            CjProjectEventType.CREATED -> {
                configureProjectSources(event.project)
            }
            else -> {}
        }
    }

    override fun projectUpdated(event: CjProjectEvent) {
        when (event.eventType) {
            CjProjectEventType.UPDATED,
            CjProjectEventType.CONFIG_CHANGED -> {
                configureProjectSources(event.project)
            }
            else -> {}
        }
    }

    /**
     * 配置项目的源代码目录
     * 注意：目录配置现在通过Workspace Model在其他地方处理
     */
    private fun configureProjectSources(cjProject: CjProject) {
        // 项目发现后，ModuleEntity已经通过CjWorkspaceModelUtils创建
        // 源代码目录配置通过Workspace Model在其他地方处理
        // 这里可以添加额外的项目配置逻辑


    }
}