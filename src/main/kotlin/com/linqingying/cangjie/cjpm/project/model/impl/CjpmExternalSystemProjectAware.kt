package com.linqingying.cangjie.cjpm.project.model.impl

import com.linqingying.cangjie.cjpm.project.model.CjpmProjectsService
import com.linqingying.cangjie.cjpm.project.model.cjpmProjects
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
