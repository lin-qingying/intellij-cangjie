//package com.huawei.cangjie.cjpm.project.workspace
//
//import com.huawei.cangjie.cjpm.project.model.CjcInfo
//import com.huawei.cangjie.cjpm.project.model.impl.UserDisabledFeatures
//import com.intellij.openapi.vfs.VirtualFile
//import com.intellij.openapi.vfs.VirtualFileManager
//import java.nio.file.Path
//import java.util.concurrent.atomic.AtomicReference
//import kotlin.reflect.KProperty
//
//class CachedVirtualFile(private val url: String?) {
//    private val cache = AtomicReference<VirtualFile>()
//
//    operator fun getValue(thisRef: Any?, property: KProperty<*>): VirtualFile? {
//        if (url == null) return null
//        val cached = cache.get()
//        if (cached != null && cached.isValid) return cached
//        val file = VirtualFileManager.getInstance().findFileByUrl(url)
//        cache.set(file)
//        return file
//    }
//}
//
//interface CjpmWorkspace {
//    val manifestPath: Path
//    val contentRoot: Path get() = manifestPath.parent
//
//    val workspaceRoot: VirtualFile?
//
//    fun withDisabledFeatures(userDisabledFeatures: UserDisabledFeatures): CjpmWorkspace
//    fun withStdlib(stdlib: StandardLibrary , cjcInfo: CjcInfo? = null): CjpmWorkspace
//
//}
//
//class WorkspaceImpl (
//    override val manifestPath: Path,
//    val workspaceRootUrl: String?,
//): CjpmWorkspace{
//    override val workspaceRoot: VirtualFile? by CachedVirtualFile(workspaceRootUrl)
//
//}