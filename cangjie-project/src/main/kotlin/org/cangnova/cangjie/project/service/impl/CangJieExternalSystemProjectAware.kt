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
import com.intellij.openapi.externalSystem.autoimport.ExternalSystemProjectAware
import com.intellij.openapi.externalSystem.autoimport.ExternalSystemProjectId
import com.intellij.openapi.externalSystem.autoimport.ExternalSystemProjectListener
import com.intellij.openapi.externalSystem.autoimport.ExternalSystemProjectReloadContext
import com.intellij.openapi.externalSystem.autoimport.ExternalSystemRefreshStatus
import com.intellij.openapi.externalSystem.model.ProjectSystemId
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import org.cangnova.cangjie.project.CangJieSettingsFilesService
import org.cangnova.cangjie.project.service.CjProjectsService
import org.cangnova.cangjie.project.service.CjProjectsService.Companion.CANGJIE_PROJECTS_REFRESH_TOPIC
import org.cangnova.cangjie.project.service.cangjieProjectService
import org.cangnova.cangjie.task.taskQueue

/**
 * 仓颉外部系统项目感知器
 *
 * 实现 IntelliJ 的 [ExternalSystemProjectAware] 接口，用于集成仓颉项目管理(CJPM)到 IDE 的外部系统框架中。
 * 该类负责：
 * - 监听项目配置文件的变更
 * - 触发项目的重新加载和同步
 * - 管理项目自动导入功能
 *
 * 通过 [ExternalSystemProjectTracker] 注册后，IDE 会自动监听配置文件变更并触发项目刷新，
 * 确保 IDE 的项目模型与实际的 CangJie 项目配置保持同步。
 *
 * @property project IntelliJ 项目实例
 * @see ExternalSystemProjectAware
 * @see ExternalSystemProjectTracker
 */
@Suppress("UnstableApiUsage")
class CangJieExternalSystemProjectAware(
    private val project: Project
) : ExternalSystemProjectAware {
    /**
     * 外部系统项目标识符
     *
     * 返回仓颉项目在 IntelliJ 外部系统框架中的唯一标识，由系统 ID 和项目名称组成。
     */
    override val projectId: ExternalSystemProjectId
        get() = ExternalSystemProjectId(CANGJIE_SYSTEM_ID, project.name)

    /**
     * 需要监听的配置文件路径集合
     *
     * 返回所有影响项目结构的配置文件路径（如 cjpm.toml）。
     * 当这些文件发生变更时，IDE 会自动触发项目重新加载。
     *
     * 只有在存在有效的仓颉项目时才返回配置文件，避免在没有项目时显示刷新按钮。
     *
     * @return 配置文件路径的集合，如果没有有效项目则返回空集合
     */
    override val settingsFiles: Set<String>
        get() {
            // 检查是否存在有效的仓颉项目
            val projectService = project.cangjieProjectService
            if (projectService.allProjects.isEmpty()) {
                return emptySet()
            }

            val settingsFilesService = CangJieSettingsFilesService.getInstance(project)
            // Always collect fresh settings files
            return settingsFilesService.collectSettingsFiles(useCache = false).keys
        }

    /**
     * 重新加载项目
     *
     * 当检测到配置文件变更或用户手动触发刷新时调用此方法，
     * 负责重新解析项目配置、更新依赖关系和刷新项目结构。
     * 使用新的 CangJieSyncTask 进行异步刷新，提供更好的用户体验。
     *
     * @param context 项目重新加载的上下文信息，包含刷新状态和相关设置
     */
    override fun reloadProject(context: ExternalSystemProjectReloadContext) {
        // 保存所有文档，确保文件系统与内存中的内容同步
        FileDocumentManager.getInstance().saveAllDocuments()
        project.cangjieProjectService.refreshAllProjects()


    }

    /**
     * 订阅项目事件
     *
     * 注册项目事件监听器，用于接收项目状态变更通知（如项目加载完成、刷新失败等）。
     *
     * @param listener 外部系统项目事件监听器
     * @param parentDisposable 父级 Disposable，用于管理监听器的生命周期
     */
    override fun subscribe(listener: ExternalSystemProjectListener, parentDisposable: Disposable) {
        project.messageBus.connect(parentDisposable).subscribe(
            CANGJIE_PROJECTS_REFRESH_TOPIC,
            object : CjProjectsService.CangJieProjectsRefreshListener {
                override fun onRefreshStarted() {
                    listener.onProjectReloadStart()
                }

                override fun onRefreshFinished(status: CjProjectsService.RefreshStatus) {
                    val externalStatus = when (status) {
                        CjProjectsService.RefreshStatus.SUCCESS -> ExternalSystemRefreshStatus.SUCCESS
                        CjProjectsService.RefreshStatus.FAILURE -> ExternalSystemRefreshStatus.FAILURE
                        CjProjectsService.RefreshStatus.CANCEL -> ExternalSystemRefreshStatus.CANCEL
                    }
                    listener.onProjectReloadFinish(externalStatus)
                }
            })
    }

    companion object {
        /**
         * 仓颉项目系统标识符
         *
         * 在 IntelliJ 的外部系统框架中标识仓颉项目管理系统。
         */
        val CANGJIE_SYSTEM_ID: ProjectSystemId = ProjectSystemId("CangJie")
    }
}
