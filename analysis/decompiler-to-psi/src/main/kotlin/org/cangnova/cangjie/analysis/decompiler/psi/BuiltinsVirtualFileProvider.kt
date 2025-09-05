package org.cangnova.cangjie.analysis.decompiler.psi

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.search.GlobalSearchScope
import io.ktor.http.Url
import org.cangnova.cangjie.builtins.StandardNames.ALL_NAMES
import org.cangnova.cangjie.name.StandardClassIds
import org.cangnova.cangjie.serialization.deserialization.BuiltInSerializerFlatbuffers
import java.net.URL
import java.nio.file.Path
import kotlin.io.path.Path

abstract class BuiltinsVirtualFileProvider {
    abstract fun getBuiltinVirtualFiles(): Set<VirtualFile>

    abstract fun createBuiltinsScope(project: Project): GlobalSearchScope

    companion object {
        fun getInstance(): BuiltinsVirtualFileProvider =
            ApplicationManager.getApplication().getService(BuiltinsVirtualFileProvider::class.java)
    }
}

abstract class BuiltinsVirtualFileProviderBaseImpl : BuiltinsVirtualFileProvider() {
    private val builtInUrls: Set<Path> by lazy {
        ALL_NAMES.mapTo(mutableSetOf()) { builtInPackageFqName ->
            val resourcePath = BuiltInSerializerFlatbuffers.getBuiltInsFilePath(builtInPackageFqName)
            Path(resourcePath)
        }

    }

    override fun createBuiltinsScope(project: Project): GlobalSearchScope {
        val builtInFiles = getBuiltinVirtualFiles()
        return GlobalSearchScope.filesScope(project, builtInFiles)
    }

    protected abstract fun findVirtualFile(url: Path): VirtualFile?

    override fun getBuiltinVirtualFiles(): Set<VirtualFile> = builtInUrls.mapNotNull { url ->
        findVirtualFile(url)
//            ?: errorWithAttachment("Virtual file for builtin is not found") {
//                withEntry("resourceUrl", url) { it.toString() }
//            }
    }.toSet()
}

internal class IdeBuiltInsVirtualFileProviderImpl : BuiltinsVirtualFileProviderBaseImpl() {
    override fun findVirtualFile(url: Path): VirtualFile? {
        return VfsUtil.findFile(url, true)
    }
}