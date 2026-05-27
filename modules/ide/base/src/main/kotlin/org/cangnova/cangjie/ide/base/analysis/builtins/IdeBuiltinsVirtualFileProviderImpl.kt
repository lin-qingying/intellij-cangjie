package org.cangnova.cangjie.ide.base.analysis.builtins

import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.search.GlobalSearchScope
import org.cangnova.cangjie.analysis.decompiled.psi.BuiltinsVirtualFileProviderBaseImpl
import org.cangnova.cangjie.toolchain.api.CjProjectSdkConfig
import kotlin.io.path.pathString

/**
 * IDE 插件侧的 builtins root 提供者。
 *
 * Kotlin IDEA 插件在插件层提供 `IdeBuiltInsVirtualFileProviderImpl`，
 * 仓颉这里同样由 IDE 插件层负责把 toolchain 的标准库根暴露给 Analysis API /
 * decompiled / low-level CFIR，而不是继续沿用 CLI 环境变量语义。
 */
class IdeBuiltinsVirtualFileProviderImpl : BuiltinsVirtualFileProviderBaseImpl() {
    override fun getBuiltinRootVirtualFiles(): Set<VirtualFile> {
        return ProjectManager.getInstance().openProjects
            .asSequence()
            .filter { project -> !project.isDisposed }
            .mapNotNull(::resolveStdlibRoot)
            .toCollection(linkedSetOf())
    }

    override fun getBuiltinRootVirtualFiles(project: Project): Set<VirtualFile> {
        if (project.isDisposed) return emptySet()
        return listOfNotNull(resolveStdlibRoot(project)).toCollection(linkedSetOf())
    }

    override fun createBuiltinsScope(project: Project): GlobalSearchScope {
        val projectRoots = getBuiltinVirtualFiles(project)
        return GlobalSearchScope.filesScope(project, projectRoots)
    }

    private fun resolveStdlibRoot(project: Project): VirtualFile? {
        val sdk = CjProjectSdkConfig.getInstance(project).getProjectSdk() ?: return null
        val path = sdk.stdlibPath.pathString
        return resolveLocalRootVirtualFile(path)
            ?: run {
                logger<IdeBuiltinsVirtualFileProviderImpl>().warn(
                    "Cannot resolve stdlib path from toolchain `${sdk.name}`: $path",
                )
                null
            }
    }
}
