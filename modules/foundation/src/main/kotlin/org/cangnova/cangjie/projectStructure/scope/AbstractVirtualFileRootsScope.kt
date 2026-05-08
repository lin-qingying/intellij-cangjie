package org.cangnova.cangjie.projectStructure.scope

import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.util.Comparing
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.impl.VirtualFileEnumeration
import com.intellij.psi.search.impl.VirtualFileEnumerationAware
import it.unimi.dsi.fastutil.objects.Object2IntMap

/**
 * 基于根目录集合的作用域基类。
 *
 * 这里直接对齐 Kotlin `AbstractVirtualFileRootsScope`：
 * - 用 roots map 保存 classpath 顺序；
 * - 用统一的根定位逻辑实现 `contains` / `compare`；
 * - 复用 IntelliJ 的 `VirtualFileEnumerationAware` 优化索引查找。
 */
abstract class AbstractVirtualFileRootsScope(project: Project) : GlobalSearchScope(project), VirtualFileEnumerationAware {
    @Volatile
    private var virtualFileEnumeration: VirtualFileEnumeration? = null

    @Volatile
    private var vfsModificationCount: Long = 0

    protected val myProjectFileIndex: ProjectFileIndex = ProjectRootManager.getInstance(project).fileIndex

    protected abstract val roots: Object2IntMap<VirtualFile>

    protected abstract fun getFileRoot(file: VirtualFile): VirtualFile?

    override fun contains(file: VirtualFile): Boolean {
        val root = getFileRoot(file) ?: return false
        return roots.containsKey(root)
    }

    override fun compare(file1: VirtualFile, file2: VirtualFile): Int {
        val root1 = getFileRoot(file1)
        val root2 = getFileRoot(file2)
        if (Comparing.equal(root1, root2)) return 0

        if (root1 == null) return -1
        if (root2 == null) return 1

        val index1 = roots.getInt(root1)
        val index2 = roots.getInt(root2)
        if (index1 == 0 && index2 == 0) return 0
        if (index1 > 0 && index2 > 0) return index2 - index1
        return if (index1 > 0) 1 else -1
    }

    override fun extractFileEnumeration(): VirtualFileEnumeration? {
        val currentVfsStamp = VirtualFileManager.VFS_STRUCTURE_MODIFICATIONS.modificationCount
        if (currentVfsStamp != vfsModificationCount) {
            virtualFileEnumeration = computeFileEnumeration()
            vfsModificationCount = currentVfsStamp
        }
        return virtualFileEnumeration.takeIf { it != VirtualFileEnumeration.EMPTY }
    }

    protected open fun computeFileEnumeration(): VirtualFileEnumeration? = computeFileEnumerationUnderRoots(roots.keys)
}
