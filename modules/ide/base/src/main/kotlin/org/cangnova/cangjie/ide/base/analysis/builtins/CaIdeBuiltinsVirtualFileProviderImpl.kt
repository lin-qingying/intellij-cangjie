package org.cangnova.cangjie.ide.base.analysis.builtins

import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.vfs.StandardFileSystems
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.search.GlobalSearchScope
import org.cangnova.cangjie.analysis.api.decompiled.CaBuiltinsRootAware
import org.cangnova.cangjie.analysis.api.decompiled.CaBuiltinsVirtualFileProvider
import org.cangnova.cangjie.lang.declarations.CangJieBuiltInFileType
import org.cangnova.cangjie.toolchain.api.CjProjectSdkConfig
import kotlin.io.path.pathString

/**
 * IDE 插件侧的 builtins root 提供者。
 *
 * Kotlin IDEA 插件在插件层提供 `IdeBuiltInsVirtualFileProviderImpl`，
 * 仓颉这里同样由 IDE 插件层负责把 toolchain 的标准库根暴露给 Analysis API /
 * decompiled / low-level CFIR，而不是继续沿用 CLI 环境变量语义。
 */
class CaIdeBuiltinsVirtualFileProviderImpl : CaBuiltinsVirtualFileProvider(), CaBuiltinsRootAware {
    override fun getBuiltinVirtualFiles(): Set<VirtualFile> {
        return getBuiltinRootVirtualFiles()
            .flatMapTo(linkedSetOf(), ::collectBuiltinFiles)
    }

    override fun getBuiltinRootVirtualFiles(): Set<VirtualFile> {
        return ProjectManager.getInstance().openProjects
            .asSequence()
            .filter { project -> !project.isDisposed }
            .mapNotNull(::resolveStdlibRoot)
            .toCollection(linkedSetOf())
    }

    override fun createBuiltinsScope(project: Project): GlobalSearchScope {
        val projectRoots = resolveStdlibRoot(project)?.let(::collectBuiltinFiles).orEmpty()
        if (projectRoots.isNotEmpty()) {
            return GlobalSearchScope.filesScope(project, projectRoots)
        }

        return GlobalSearchScope.filesScope(project, getBuiltinVirtualFiles())
    }

    private fun resolveStdlibRoot(project: Project): VirtualFile? {
        val sdk = CjProjectSdkConfig.getInstance(project).getProjectSdk() ?: return null
        val path = sdk.stdlibPath.pathString.replace('\\', '/')
        return StandardFileSystems.local().findFileByPath(path)
            ?: run {
                logger<CaIdeBuiltinsVirtualFileProviderImpl>().warn(
                    "Cannot resolve stdlib path from toolchain `${sdk.name}`: $path",
                )
                null
            }
    }

    private fun collectBuiltinFiles(root: VirtualFile): List<VirtualFile> {
        if (!root.isDirectory) {
            return listOfNotNull(root.takeIf(::isBuiltinBinary))
        }

        val files = linkedSetOf<VirtualFile>()
        VfsUtilCore.iterateChildrenRecursively(root, null) { child ->
            if (!child.isDirectory && isBuiltinBinary(child)) {
                files += child
            }
            true
        }
        return files.toList()
    }

    private fun isBuiltinBinary(file: VirtualFile): Boolean {
        return file.fileType == CangJieBuiltInFileType ||
            file.extension.equals(CangJieBuiltInFileType.defaultExtension, ignoreCase = true)
    }
}
