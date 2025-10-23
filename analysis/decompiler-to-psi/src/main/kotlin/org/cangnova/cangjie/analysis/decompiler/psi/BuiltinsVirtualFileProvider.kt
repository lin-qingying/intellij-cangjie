package org.cangnova.cangjie.analysis.decompiler.psi

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.search.GlobalSearchScope
import org.cangnova.cangjie.builtins.StandardNames
import org.cangnova.cangjie.builtins.StandardNames.ALL_NAMES
import org.cangnova.cangjie.serialization.deserialization.BuiltInSerializerFlatbuffers
import org.cangnova.cangjie.toolchain.api.CjProjectSdkConfig
import java.nio.file.Path
import kotlin.io.path.Path

abstract class BuiltinsVirtualFileProvider {
    abstract fun getBuiltinVirtualFiles(project: Project): Set<VirtualFile>

    abstract fun createBuiltinsScope(project: Project): GlobalSearchScope

    companion object {
        fun getInstance(project: Project): BuiltinsVirtualFileProvider =
            IdeBuiltInsVirtualFileProviderImpl()
    }
}

abstract class BuiltinsVirtualFileProviderBaseImpl : BuiltinsVirtualFileProvider() {

    private fun getBuiltInUrls(project: Project): Set<Path> {

        return ALL_NAMES.filter { it != StandardNames.BASIC_PACKAGE_FQ_NAME }
            .mapNotNull { builtInPackageFqName ->
                val resourcePath = BuiltInSerializerFlatbuffers.getBuiltInsFilePath(
                    builtInPackageFqName,
                    CjProjectSdkConfig.getInstance(project).getProjectSdk()
                )
                resourcePath?.let { Path(it) }
            }.toSet()
    }

    override fun createBuiltinsScope(project: Project): GlobalSearchScope {
        val builtInFiles = getBuiltinVirtualFiles(project)
        return GlobalSearchScope.filesScope(project, builtInFiles)
    }

    protected abstract fun findVirtualFile(url: Path): VirtualFile?

    override fun getBuiltinVirtualFiles(project: Project): Set<VirtualFile> {
        val builtInUrls = getBuiltInUrls(project)
        return builtInUrls.mapNotNull { url ->
            findVirtualFile(url)
        }.toSet()
    }
}

internal class IdeBuiltInsVirtualFileProviderImpl : BuiltinsVirtualFileProviderBaseImpl() {
    override fun findVirtualFile(url: Path): VirtualFile? {
        return VfsUtil.findFile(url, true)
    }
}