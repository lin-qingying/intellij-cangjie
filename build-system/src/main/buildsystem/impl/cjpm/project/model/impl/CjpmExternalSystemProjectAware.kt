/*
 * Copyright 2024 LinQingYing. and contributors.
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

package cn.cangnova.cangjie.buildsystem.impl.cjpm.project.model.impl

import cn.cangnova.cangjie.cjpm.project.model.CjpmProjectsService
import cn.cangnova.cangjie.cjpm.project.model.cjpmProjects
import com.intellij.openapi.Disposable
import com.intellij.openapi.externalSystem.autoimport.*
import com.intellij.openapi.externalSystem.model.ProjectSystemId
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project

@Suppress("UnstableApiUsage")
class CjpmExternalSystemProjectAware(
    private val project: Project
) : ExternalSystemProjectAware {
    override val projectId: ExternalSystemProjectId
        get() = ExternalSystemProjectId(CJPM_SYSTEM_ID, project.name)
    override val settingsFiles: Set<String>
        get() {
            val settingsFilesService = CjpmSettingsFilesService.getInstance(project)
            // Always collect fresh settings files
            return settingsFilesService.collectSettingsFiles(useCache = false).keys
        }

    override fun reloadProject(context: ExternalSystemProjectReloadContext) {
        FileDocumentManager.getInstance().saveAllDocuments()
        project.cjpmProjects.refreshAllProjects()
    }

    override fun subscribe(listener: ExternalSystemProjectListener, parentDisposable: Disposable) {
        project.messageBus.connect(parentDisposable).subscribe(
            CjpmProjectsService.CJPM_PROJECTS_REFRESH_TOPIC,
            object : CjpmProjectsService.CjpmProjectsRefreshListener {
                override fun onRefreshStarted() {
                    listener.onProjectReloadStart()
                }

                override fun onRefreshFinished(status: CjpmProjectsService.CjpmRefreshStatus) {
                    val externalStatus = when (status) {
                        CjpmProjectsService.CjpmRefreshStatus.SUCCESS -> ExternalSystemRefreshStatus.SUCCESS
                        CjpmProjectsService.CjpmRefreshStatus.FAILURE -> ExternalSystemRefreshStatus.FAILURE
                        CjpmProjectsService.CjpmRefreshStatus.CANCEL -> ExternalSystemRefreshStatus.CANCEL
                    }
                    listener.onProjectReloadFinish(externalStatus)
                }
            })
    }

    companion object {
        val CJPM_SYSTEM_ID: ProjectSystemId = ProjectSystemId("Cjpm")
    }
}
