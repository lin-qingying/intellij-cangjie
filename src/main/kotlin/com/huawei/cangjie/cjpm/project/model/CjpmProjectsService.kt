package com.huawei.cangjie.cjpm.project.model

import com.huawei.cangjie.cjpm.CjpmConstants
import com.huawei.cangjie.cjpm.project.model.impl.ContentEntryWrapper
import com.huawei.cangjie.cjpm.project.pathAsPath
import com.huawei.cangjie.cjpm.project.settings.cangjieSettings
import com.huawei.cangjie.cjpm.toolchain.CjToolchainBase
import com.huawei.cangjie.cjpm.toolchain.impl.CjcVersion
import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.roots.ContentEntry
import com.intellij.openapi.util.NlsContexts
import com.intellij.openapi.util.UserDataHolderEx
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.messages.Topic
import java.nio.file.Path
import java.util.concurrent.CompletableFuture


/**。
 *存储与当前IntelliJ[Project]关联的[CjpmProject]\的列表
 *使用[Project.cjpmProjects]获取服务的实例
 */
interface CjpmProjectsService {
    val project: Project

    val hasAtLeastOneValidProject: Boolean
    val initialized: Boolean
    val allProjects: Collection<CjpmProject>
    fun findProjectForFile(file: VirtualFile): CjpmProject?

    fun discoverAndRefresh(): CompletableFuture<out List<CjpmProject>>
    fun refreshAllProjects(): CompletableFuture<out List<CjpmProject>>
    fun suggestManifests(): Sequence<VirtualFile>
    fun attachCjpmProject(manifest: Path): Boolean
    interface CjpmProjectsRefreshListener {
        fun onRefreshStarted()
        fun onRefreshFinished(status: CjpmRefreshStatus)
    }

    fun interface CjpmProjectsListener {
        fun cjpmProjectsUpdated(service: CjpmProjectsService, projects: Collection<CjpmProject>)
    }

    enum class CjpmRefreshStatus {
        SUCCESS,
        FAILURE,
        CANCEL
    }

    companion object {
        val CJPM_PROJECTS_TOPIC: Topic<CjpmProjectsListener> = Topic(
            "cjpm projects changes",
            CjpmProjectsListener::class.java
        )
        val CJPM_PROJECTS_REFRESH_TOPIC: Topic<CjpmProjectsRefreshListener> = Topic(
            "Cjpm refresh",
            CjpmProjectsRefreshListener::class.java
        )
    }
}


/**。
 *参见[CjpmProjectsService]的文档
 *此类的实例是不变的，并且将在每次项目刷新时重新创建
 *此类实现[UserDataHolderEx]接口，因此可以附加任何数据
 *致此。请注意，由于此类的实例在每次项目刷新时重新创建
 *用户数据也将在项目刷新时刷新
 */
interface CjpmProject : UserDataHolderEx {

    val rootDir: VirtualFile?

    val project: Project

    val workspaceRootDir: VirtualFile?
    val cjcInfo: CjcInfo?
    val presentableName: String

    val manifest: Path

    //    val workspace: CjpmWorkspace?
//    val userDisabledFeatures: UserDisabledFeatures
    val workspaceStatus: UpdateStatus
    val stdlibStatus: UpdateStatus

    val cjcInfoStatus: UpdateStatus


    val mergedStatus: UpdateStatus
        get() = workspaceStatus
            .merge(stdlibStatus)
            .merge(cjcInfoStatus)

    sealed class UpdateStatus(private val priority: Int) {
        object UpToDate : UpdateStatus(0)
        object NeedsUpdate : UpdateStatus(1)
        class UpdateFailed(@Suppress("UnstableApiUsage") @NlsContexts.Tooltip val reason: String) : UpdateStatus(2) {
            override fun toString(): String = reason
        }

        fun merge(status: UpdateStatus): UpdateStatus = if (priority >= status.priority) this else status
    }
}

val Project.cjpmProjects: CjpmProjectsService get() = service()


data class CjcInfo(

    val version: CjcVersion?,

    val targets: List<String>? = null,

    /**
     * In production environments it is always equal to [version].
     * In unit tests it is real, non-mocked toolchain version
     */
    val realVersion: CjcVersion? = version,
)


fun guessAndSetupCangJieProject(project: Project, explicitRequest: Boolean = false): Boolean {
    if (!explicitRequest) {
        val alreadyTried = run {
            val key = "com.huawei.cangjie.cjpm.project.model.PROJECT_DISCOVERY"
            val properties = PropertiesComponent.getInstance(project)
            val alreadyTried = properties.getBoolean(key)
            properties.setValue(key, true)
            alreadyTried
        }
        if (alreadyTried) return false
    }

    val toolchain = project.cangjieSettings.toolchain
    if (toolchain == null || !toolchain.looksLikeValidToolchain()) {
        discoverToolchain(project)
        return true
    }
    if (!project.cjpmProjects.hasAtLeastOneValidProject) {
        project.cjpmProjects.discoverAndRefresh()
        return true
    }
    return false
}

private fun discoverToolchain(project: Project) {
    val projectPath = project.guessProjectDir()?.pathAsPath
    val toolchain = CjToolchainBase.suggest(projectPath) ?: return
    invokeLater {
        if (project.isDisposed) return@invokeLater

        val oldToolchain = project.cangjieSettings.toolchain
        if (oldToolchain != null && oldToolchain.looksLikeValidToolchain()) {
            return@invokeLater
        }

        runWriteAction {
            project.cangjieSettings.modify { it.toolchain = toolchain }
        }



        project.cjpmProjects.discoverAndRefresh()
    }
}

fun ContentEntryWrapper.setup(contentRoot: VirtualFile) {
    val makeVfsUrl = { dirName: String -> contentRoot.findChild(dirName)?.url }
    CjpmConstants.ProjectLayout.sources.mapNotNull(makeVfsUrl).forEach {
        addSourceFolder(it, isTestSource = false)
    }
    CjpmConstants.ProjectLayout.tests.mapNotNull(makeVfsUrl).forEach {
        addSourceFolder(it, isTestSource = true)
    }
    makeVfsUrl(CjpmConstants.ProjectLayout.target)?.let(::addExcludeFolder)
}
fun ContentEntry.setup(contentRoot: VirtualFile) = ContentEntryWrapper(this).setup(contentRoot)
