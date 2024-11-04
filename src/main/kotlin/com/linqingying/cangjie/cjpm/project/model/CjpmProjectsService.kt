package com.linqingying.cangjie.cjpm.project.model

import com.linqingying.cangjie.CangJieBundle
import com.linqingying.cangjie.cjpm.CjpmConstants
import com.linqingying.cangjie.cjpm.project.pathAsPath
import com.linqingying.cangjie.cjpm.project.settings.cangjieSettings
import com.linqingying.cangjie.cjpm.project.workspace.CjpmWorkspace
import com.linqingying.cangjie.cjpm.resolve
import com.linqingying.cangjie.cjpm.toolchain.CjToolchainBase
import com.linqingying.cangjie.cjpm.toolchain.impl.CjcVersion
import com.intellij.ide.util.PropertiesComponent
import com.intellij.notification.NotificationType
import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.roots.ContentEntry
import com.intellij.openapi.util.UserDataHolderEx
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.messages.Topic
import java.nio.file.Path
import java.util.concurrent.CompletableFuture

val Project.currentCjpmProject: CjpmProject?
    get() {
        return cjpmProjects.currentCjpmProject
    }

/**。
 *存储与当前IntelliJ[Project]关联的[CjpmProject]\的列表
 *使用[Project.cjpmProjects]获取服务的实例
 */
interface CjpmProjectsService {
    val project: Project

    val hasAtLeastOneValidProject: Boolean
    val initialized: Boolean
    val allProjects: Collection<CjpmProject>


    //    拿到当前cjpm项目
    val currentCjpmProject: CjpmProject?
        get() = project.guessProjectDir()?.pathAsPath?.resolve(CjpmConstants.MANIFEST_FILE)?.toFile()
            ?.let { LocalFileSystem.getInstance().findFileByIoFile(it) }
            ?.let { project.cjpmProjects.findProjectForModuleFile(it) }


    fun findProjectForFile(file: VirtualFile): CjpmProject?

    fun findProjectForModuleFile(file: VirtualFile): CjpmProject?

    fun discoverAndRefresh(): CompletableFuture<out List<CjpmProject>>
    fun refreshAllProjects(): CompletableFuture<out List<CjpmProject>>
    fun suggestManifests(): Sequence<VirtualFile>
    fun attachCjpmProject(manifest: Path): Boolean
    fun findPackageForFile(file: VirtualFile): CjpmWorkspace.Package?

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
            val key = "com.linqingying.cangjie.cjpm.project.model.PROJECT_DISCOVERY"
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

//        val tool = if (toolchain.isRustupAvailable) CangJieBundle.message("notification.content.rustup") else RsBundle.message("notification.content.cargo.at", toolchain.presentableLocation)
//        project.showBalloon(RsBundle.message("notification.content.using", tool), NotificationType.INFORMATION)
//

        project.cjpmProjects.discoverAndRefresh()
    }
}


fun ContentEntry.setup(contentRoot: VirtualFile) = ContentEntryWrapper(this).setup(contentRoot)
