package com.huawei.cangjie.cjpm.project.model.impl

import com.huawei.cangjie.cjpm.project.model.CjcInfo
import com.huawei.cangjie.cjpm.project.model.CjpmProject
import com.huawei.cangjie.cjpm.project.workspace.CjpmWorkspace
import com.huawei.cangjie.cjpm.project.workspace.StandardLibrary
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.UserDataHolderBase
import com.intellij.openapi.util.registry.Registry
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import java.nio.file.Path
import java.util.concurrent.atomic.AtomicReference
import kotlin.reflect.KProperty

val isNewProjectModelImportEnabled: Boolean
    get() = Registry.`is`("com.huawei.cangjie.cjpm.new.auto.import", false)


data class CjpmProjectImpl(
    override val manifest: Path,
    private val projectService: CjpmProjectsServiceImpl,
    override val userDisabledFeatures: UserDisabledFeatures = UserDisabledFeatures.EMPTY,

    val rawWorkspace: CjpmWorkspace? = null,
    private val stdlib: StandardLibrary? = null,

    override val cjcInfo: CjcInfo? = null,
    override val workspaceStatus: CjpmProject.UpdateStatus = CjpmProject.UpdateStatus.NeedsUpdate,
    override val stdlibStatus: CjpmProject.UpdateStatus = CjpmProject.UpdateStatus.NeedsUpdate,
    override val cjcInfoStatus: CjpmProject.UpdateStatus = CjpmProject.UpdateStatus.NeedsUpdate
) : UserDataHolderBase(), CjpmProject {
    //    override val workspaceRootDir: VirtualFile? = project.baseDir
    override val workspaceRootDir: VirtualFile?
        get() = rawWorkspace?.workspaceRoot

    private val rootDirCache = AtomicReference<VirtualFile>()


    override val isWorkspace: Boolean
        get() {
            return rawWorkspace != null
        }

    override val rootDir: VirtualFile?
        get() {
            val cached = rootDirCache.get()
            if (cached != null && cached.isValid) return cached
            val file = LocalFileSystem.getInstance().findFileByIoFile(workingDirectory.toFile())
            rootDirCache.set(file)
            return file
        }
    override val project: Project
        get() = projectService.project
    override val workspace: CjpmWorkspace? by lazy(LazyThreadSafetyMode.PUBLICATION) {

//        val rawWorkspace = rawWorkspace ?: return@lazy null

//        val stdlib = stdlib ?: return@lazy rawWorkspace
//        val stdlib = stdlib ?: return@lazy if (!userDisabledFeatures.isEmpty() && isUnitTestMode) {
//            rawWorkspace.withDisabledFeatures(userDisabledFeatures)
//        } else {
//            rawWorkspace
//        }
//        rawWorkspace.withStdlib(stdlib, cjcInfo)
        rawWorkspace
    }

    fun withStdlib(result: TaskResult<StandardLibrary>): CjpmProjectImpl = when (result) {
        is TaskResult.Ok -> copy(stdlib = result.value, stdlibStatus = CjpmProject.UpdateStatus.UpToDate)
        is TaskResult.Err -> copy(stdlibStatus = CjpmProject.UpdateStatus.UpdateFailed(result.reason))
    }

//    override val presentableName: String by lazy {
//        workspace?.packages?.singleOrNull {
//            it.origin == PackageOrigin.WORKSPACE && it.rootDirectory == workingDirectory
//        }?.name ?: workingDirectory.fileName.toString()
//    }
    override val presentableName: String
        get() {
            return workingDirectory.fileName.toString()
        }

    fun withWorkspace(result: TaskResult<CjpmWorkspace>): CjpmProjectImpl = when (result) {
        is TaskResult.Ok -> copy(
            rawWorkspace = result.value,
            workspaceStatus = CjpmProject.UpdateStatus.UpToDate,
//            userDisabledFeatures = userDisabledFeatures.retain(result.value.packages)
        )

        is TaskResult.Err -> copy(workspaceStatus = CjpmProject.UpdateStatus.UpdateFailed(result.reason))
    }

    fun withCjcInfo(result: TaskResult<CjcInfo>): CjpmProjectImpl = when (result) {
        is TaskResult.Ok -> copy(cjcInfo = result.value, cjcInfoStatus = CjpmProject.UpdateStatus.UpToDate)
        is TaskResult.Err -> copy(cjcInfoStatus = CjpmProject.UpdateStatus.UpdateFailed(result.reason))
    }
}

val CjpmProject.workingDirectory: Path get() = manifest.parent


class CachedVirtualFile(private val url: String?) {
    private val cache = AtomicReference<VirtualFile>()

    operator fun getValue(thisRef: Any?, property: KProperty<*>): VirtualFile? {
        if (url == null) return null
        val cached = cache.get()
        if (cached != null && cached.isValid) return cached
        val file = VirtualFileManager.getInstance().findFileByUrl(url)
        cache.set(file)
        return file
    }
}
