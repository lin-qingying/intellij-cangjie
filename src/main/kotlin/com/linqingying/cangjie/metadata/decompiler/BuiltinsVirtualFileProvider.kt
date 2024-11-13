package com.linqingying.cangjie.metadata.decompiler

import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.search.GlobalSearchScope
import com.linqingying.cangjie.builtins.StandardNames.ALL_NAMES
import com.linqingying.cangjie.cjpm.toolchain.CjToolchainBase
import com.linqingying.cangjie.cjpm.toolchain.cjc
import com.linqingying.cangjie.ide.run.cjpm.toolchain
import com.linqingying.cangjie.name.StandardClassIds
import com.linqingying.cangjie.serialization.deserialization.BuiltInSerializerProtocol
import com.linqingying.cangjie.utils.exceptions.errorWithAttachment
import java.net.URL
import java.nio.file.Path


abstract class BuiltInsVirtualFileProvider(val project: Project) {
    abstract fun getBuiltinVirtualFiles(): Set<VirtualFile>

    abstract fun createBuiltinsScope(project: Project): GlobalSearchScope

    companion object {
        fun getInstance(project: Project): BuiltInsVirtualFileProvider = project.service<BuiltInsVirtualFileProvider>()
//            ApplicationManager.getApplication().getService(BuiltInsVirtualFileProvider::class.java)
    }
}

internal class IdeBuiltInsVirtualFileProviderImpl(project: Project) : BuiltInsVirtualFileProviderBaseImpl(project) {
    override fun findVirtualFile(url: URL): VirtualFile? {
        return VfsUtil.findFileByURL(url)
    }
}

fun getProject(): Project {

    return ProjectManager.getInstance().openProjects[0]
}

abstract class BuiltInsVirtualFileProviderBaseImpl(project: Project) : BuiltInsVirtualFileProvider(project) {
    private val stdlibPathByVersion: Path get() = CjToolchainBase.stdlibPath.resolve(toolchainVsrsion)
    private fun getURL(resourcePath: String): URL {
        return CjToolchainBase.stdlibPath.resolve(resourcePath).toUri().toURL()
    }

    private val toolchainVsrsion: String
        get() {
            return runReadAction {
                project.toolchain?.cjc()?.version?.semver?.rawVersion ?: ""
            }
        }

    private val builtInUrls: Set<URL> by lazy {
        val classLoader = this::class.java.classLoader
        ALL_NAMES.mapTo(mutableSetOf()) { builtInPackageFqName ->
            val resourcePath = BuiltInSerializerProtocol.getBuiltInsFilePath(builtInPackageFqName)

//            getURL(resourcePath)
            classLoader.getResource("stdlib/$resourcePath")
                ?: errorWithAttachment("Resource for builtin $builtInPackageFqName not found") {
                    withEntry("resourcePath", resourcePath)
                }
        }
    }

    override fun createBuiltinsScope(project: Project): GlobalSearchScope {
        val builtInFiles = getBuiltinVirtualFiles()
        return GlobalSearchScope.filesScope(project, builtInFiles)
    }

    protected abstract fun findVirtualFile(url: URL): VirtualFile?

    override fun getBuiltinVirtualFiles(): Set<VirtualFile> = builtInUrls.mapNotNull { url ->
        findVirtualFile(url)
//            ?: errorWithAttachment("Virtual file for builtin is not found") {
//                withEntry("resourceUrl", url) { it.toString() }
//            }
    }.toSet()
}
