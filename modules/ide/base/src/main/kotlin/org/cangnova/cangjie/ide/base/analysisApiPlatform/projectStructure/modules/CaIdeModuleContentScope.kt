package org.cangnova.cangjie.ide.base.analysisApiPlatform.projectStructure.modules

import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.search.GlobalSearchScope

/**
 * IDE module 的根目录作用域。
 *
 * `GlobalSearchScope.files*Scope` 只把传入的根 VirtualFile 当成精确文件集合；
 * source root / library root 是目录时，必须按祖先关系递归包含其子文件，否则
 * `canBeAnalysed` 会拒绝同一源码模块里的普通 `.cj` 文件。
 */
internal class CaIdeModuleContentScope(
    private val owningProject: Project,
    private val roots: List<VirtualFile>,
    private val includeLibrariesInScope: Boolean,
) : GlobalSearchScope(owningProject) {
    override fun contains(file: VirtualFile): Boolean =
        roots.any { root -> root.isValid && file.isValid && root.containsFile(file) }

    /**
     * IntelliJ 在高亮、注入和文件视图切换路径上可能给出同一路径的不同 VirtualFile 包装。
     * 模块内容范围表达的是 root 到文件的路径包含关系，因此这里同时按对象祖先关系和 URL
     * 祖先关系判断，避免 Analysis API 把同一源码根下的声明误判为越界 PSI。
     */
    private fun VirtualFile.containsFile(file: VirtualFile): Boolean =
        this == file ||
            VfsUtilCore.isAncestor(this, file, false) ||
            VfsUtilCore.isEqualOrAncestor(url, file.url)

    override fun isSearchInModuleContent(aModule: Module): Boolean =
        !includeLibrariesInScope

    override fun isSearchInLibraries(): Boolean =
        includeLibrariesInScope

    override fun getProject(): Project? = owningProject

    override fun toString(): String =
        "CangJie IDE module content scope (${roots.joinToString { it.presentableUrl }})"
}
