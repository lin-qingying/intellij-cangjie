package com.huawei.cangjie.cjpm.project.model

import com.huawei.cangjie.cjpm.project.workspace.CjpmWorkspace
import com.huawei.cangjie.cjpm.project.workspace.FeatureState
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.NlsContexts
import com.intellij.openapi.util.UserDataHolderEx
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.messages.Topic
import java.nio.file.Path
import java.util.concurrent.CompletableFuture


interface CjpmProject : UserDataHolderEx {
    val project: Project
    val manifest: Path
    val rootDir: VirtualFile?
    val workspaceRootDir: VirtualFile?

    val presentableName: String



    val procMacroExpanderPath: Path?

    val workspaceStatus: UpdateStatus
    val stdlibStatus: UpdateStatus
    val cangjiecInfoStatus: UpdateStatus

    val mergedStatus: UpdateStatus
        get() = workspaceStatus
            .merge(stdlibStatus)
            .merge(cangjiecInfoStatus)


    sealed class UpdateStatus(private val priority: Int) {
        object UpToDate : UpdateStatus(0)
        object NeedsUpdate : UpdateStatus(1)
        class UpdateFailed(@Suppress("UnstableApiUsage") @NlsContexts.Tooltip val reason: String) : UpdateStatus(2) {
            override fun toString(): String = reason
        }

        fun merge(status: UpdateStatus): UpdateStatus = if (priority >= status.priority) this else status
    }
}

interface CjpmProjectsService {
    val project: Project
    val allProjects: Collection<CjpmProject>
    val hasAtLeastOneValidProject: Boolean
    val initialized: Boolean

    fun findProjectForFile(file: VirtualFile): CjpmProject?

    fun findPackageForFile(file: VirtualFile): CjpmWorkspace.Package?

    fun attachCargoProject(manifest: Path): Boolean
    fun attachCargoProjects(vararg manifests: Path)
    fun detachCargoProject(cargoProject: CjpmProject)
    fun refreshAllProjects(): CompletableFuture<out List<CjpmProject>>
    fun discoverAndRefresh(): CompletableFuture<out List<CjpmProject>>
    fun suggestManifests(): Sequence<VirtualFile>


    companion object {
        val CARGO_PROJECTS_TOPIC: Topic<CargoProjectsListener> = Topic(
            "cargo projects changes",
            CargoProjectsListener::class.java
        )

        val CARGO_PROJECTS_REFRESH_TOPIC: Topic<CargoProjectsRefreshListener> = Topic(
            "Cargo refresh",
            CargoProjectsRefreshListener::class.java
        )
    }

    fun interface CargoProjectsListener {
        fun cargoProjectsUpdated(service: CjpmProjectsService, projects: Collection<CjpmProject>)
    }

    interface CargoProjectsRefreshListener {
        fun onRefreshStarted()
        fun onRefreshFinished(status: CargoRefreshStatus)
    }

    enum class CargoRefreshStatus {
        SUCCESS,
        FAILURE,
        CANCEL
    }
}


val Project.cjpmProjects: CjpmProjectsService get() = service()
