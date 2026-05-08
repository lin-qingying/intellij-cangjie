package org.cangnova.cangjie.projectStructure.scope

import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.impl.VirtualFileEnumeration
import it.unimi.dsi.fastutil.objects.Object2IntMap

/**
 * 把多个根目录作用域合并成一个按根定位的统一作用域。
 *
 * 这里对齐 Kotlin `CombinedSourceAndClassRootsScope` 的职责：
 * 1. 把多个可组合 scope 的根目录折叠成一个 scope；
 * 2. 统一保存 classpath 顺序；
 * 3. 避免 union 后继续保留大量小 scope 对象。
 */
@Suppress("EqualsOrHashCode")
class CombinedSourceAndClassRootsScope private constructor(
    override val roots: Object2IntMap<VirtualFile>,
    override val modules: Set<Module>,
    override val includesLibraryClassRoots: Boolean,
    override val includesLibrarySourceRoots: Boolean,
    project: Project,
) : AbstractVirtualFileRootsScope(project), CombinableSourceAndClassRootsScope {
    override fun getFileRoot(file: VirtualFile): VirtualFile? {
        if (myProjectFileIndex.isInLibraryClasses(file)) {
            return myProjectFileIndex.getClassRootForFile(file)
        }

        val sourceRoot = myProjectFileIndex.getSourceRootForFile(file)
        if (sourceRoot != null && myProjectFileIndex.isInSource(file)) {
            return sourceRoot
        }

        if (includesLibrarySourceRoots && myProjectFileIndex.isInLibrarySource(file)) {
            return sourceRoot
        }
        return null
    }

    override fun isSearchInModuleContent(aModule: Module): Boolean = aModule in modules

    override fun isSearchInLibraries(): Boolean = includesLibraryClassRoots || includesLibrarySourceRoots

    override fun computeFileEnumeration(): VirtualFileEnumeration? = null

    override fun equals(other: Any?): Boolean =
        this === other || other is CombinedSourceAndClassRootsScope && roots == other.roots

    override fun calcHashCode(): Int = roots.hashCode()

    override fun toString(): String = "Combined source and class roots scope: $roots"

    companion object {
        fun create(scopes: List<CombinableSourceAndClassRootsScope>, project: Project): GlobalSearchScope {
            if (scopes.isEmpty()) return EMPTY_SCOPE

            val roots = scopes.flatMapTo(LinkedHashSet()) { it.getOrderedRoots() }.toObject2IndexMap()
            val modules = scopes.flatMapTo(linkedSetOf()) { it.modules }
            val includesLibraryClassRoots = scopes.any { it.includesLibraryClassRoots }
            val includesLibrarySourceRoots = scopes.any { it.includesLibrarySourceRoots }

            return CombinedSourceAndClassRootsScope(
                roots = roots,
                modules = modules,
                includesLibraryClassRoots = includesLibraryClassRoots,
                includesLibrarySourceRoots = includesLibrarySourceRoots,
                project = project,
            )
        }
    }
}
