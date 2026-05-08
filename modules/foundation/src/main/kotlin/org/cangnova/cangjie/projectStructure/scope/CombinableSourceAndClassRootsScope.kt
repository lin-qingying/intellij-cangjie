package org.cangnova.cangjie.projectStructure.scope

import com.intellij.openapi.module.Module
import com.intellij.openapi.vfs.VirtualFile
import it.unimi.dsi.fastutil.objects.Object2IntMap

/**
 * 可组合的源码和类文件根作用域接口。
 *
 * 这里直接对齐 Kotlin `CombinableSourceAndClassRootsScope`：
 * - 暴露 roots 到 classpath 序号的映射；
 * - 暴露模块覆盖范围；
 * - 暴露是否包含 library classes / library sources。
 */
interface CombinableSourceAndClassRootsScope {
    /**
     * 根目录映射。
     *
     * key 是根 VirtualFile，value 是该根在 classpath 中的顺序。
     */
    val roots: Object2IntMap<VirtualFile>

    /**
     * 当前 scope 覆盖到的源码模块集合。
     */
    val modules: Set<Module>

    /**
     * 当前 scope 是否包含库二进制根。
     */
    val includesLibraryClassRoots: Boolean

    /**
     * 当前 scope 是否包含库源码根。
     */
    val includesLibrarySourceRoots: Boolean
}

/**
 * 按 classpath 顺序返回 roots。
 */
fun CombinableSourceAndClassRootsScope.getOrderedRoots(): List<VirtualFile> = roots.keys.sortedBy(roots::getInt)
