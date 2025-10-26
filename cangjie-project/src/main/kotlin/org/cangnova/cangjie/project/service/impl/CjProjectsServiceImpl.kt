/*
 * Copyright 2025 LinQingYing. and contributors.
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

package org.cangnova.cangjie.project.service.impl

import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import org.cangnova.cangjie.project.event.CjProjectEvent
import org.cangnova.cangjie.project.event.CjProjectEventType
import org.cangnova.cangjie.project.event.CjProjectListener
import org.cangnova.cangjie.project.extension.CjProjectProvider
import org.cangnova.cangjie.project.model.CjProject
import org.cangnova.cangjie.project.service.CjProjectsService
import org.cangnova.cangjie.project.service.GeneratedFilesHolder
import org.cangnova.cangjie.result.CjProcessResult
import java.util.concurrent.ConcurrentHashMap

/**
 * 仓颉项目管理服务实现
 */
@Service(Service.Level.PROJECT)
class CjProjectsServiceImpl(
    private val intellijProject: Project
) : CjProjectsService {

    private val log = logger<CjProjectsServiceImpl>()

    /**
     * 项目缓存: rootDir -> CjProject
     */
    private val projectCache = ConcurrentHashMap<VirtualFile, CjProject>()


    private val providerCache =
        CjProjectProvider.EP_NAME.extensionList.firstOrNull() ?: error("CJProjectProvider not found")


    override val allProjects: List<CjProject>
        get() = projectCache.values.toList()

    override fun findProject(rootDir: VirtualFile): CjProject? {
        return projectCache[rootDir]
    }

    override fun findProjectByName(name: String): CjProject? {
        return projectCache.values.firstOrNull { it.name == name }
    }

    override fun discoverProject(rootDir: VirtualFile): CjProject? {
        // 先检查缓存
        projectCache[rootDir]?.let { return it }

        // 获取所有项目提供者，按优先级排序
        val provider = providerCache

        // 尝试使用每个提供者创建项目

        if (provider.canHandle(rootDir)) {
            log.info("Found project provider: ${provider.providerName} for $rootDir")
            val project = provider.createProject(rootDir, intellijProject)
            if (project != null) {
                addProject(project)
                return project
            }
        }

        log.warn("No suitable project provider found for $rootDir")
        return null
    }

    override fun addProject(project: CjProject) {
        val existing = projectCache.putIfAbsent(project.rootDir, project)
        if (existing == null) {
            // 发布项目创建事件
            publishEvent(CjProjectEvent(project, CjProjectEventType.CREATED))
            log.info("Project added: ${project.name} at ${project.rootDir}")
        }
    }

    override fun removeProject(project: CjProject) {
        val removed = projectCache.remove(project.rootDir)
        if (removed != null) {
            // 发布项目删除事件
            publishEvent(CjProjectEvent(project, CjProjectEventType.REMOVED))
            log.info("Project removed: ${project.name}")
        }
    }

    override fun refreshAllProjects() {
        log.info("Refreshing all projects...")
        projectCache.values.forEach { refreshProject(it) }
    }

    override fun refreshProject(project: CjProject) {
        log.info("Refreshing project: ${project.name}")
        project.refresh()
        // 发布项目更新事件
        publishEvent(CjProjectEvent(project, CjProjectEventType.UPDATED))
    }

    override fun findProjectForFile(file: VirtualFile): CjProject? {
        // 遍历项目，检查文件是否在项目根目录下
        var current: VirtualFile? = file
        while (current != null) {
            projectCache[current]?.let { return it }
            current = current.parent
        }
        return null
    }

    override fun createProject(
        sdkId: String,
        owner: Disposable,
        directory: VirtualFile,
        projectType: String
    ): CjProcessResult<GeneratedFilesHolder> {
        return providerCache.createProjectFromPhysicalFile(sdkId, intellijProject, owner, directory, projectType)
    }

    /**
     * 发布项目事件
     */
    private fun publishEvent(event: CjProjectEvent) {
        val publisher = intellijProject.messageBus.syncPublisher(CjProjectListener.TOPIC)
        when (event.eventType) {
            CjProjectEventType.CREATED -> publisher.projectCreated(event)
            CjProjectEventType.UPDATED -> publisher.projectUpdated(event)
            CjProjectEventType.REMOVED -> publisher.projectRemoved(event)
            CjProjectEventType.CONFIG_CHANGED -> publisher.projectConfigChanged(event)
        }
    }
}