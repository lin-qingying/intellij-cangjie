package org.cangnova.cangjie.projectStructure.scope

import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.vfs.VirtualFileWithId
import com.intellij.psi.search.impl.VirtualFileEnumeration
import it.unimi.dsi.fastutil.ints.IntList
import it.unimi.dsi.fastutil.ints.IntOpenHashSet
import it.unimi.dsi.fastutil.ints.IntSet
import it.unimi.dsi.fastutil.objects.Object2IntMap
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap

/**
 * 把有序 roots 集合转换成 roots 到 classpath 序号的映射。
 */
internal fun <T : Any> LinkedHashSet<T>.toObject2IndexMap(): Object2IntMap<T> {
    var index = 1
    val map = Object2IntOpenHashMap<T>(size)
    for (element in this) {
        map.put(element, index++)
    }
    return map
}

internal fun computeFileEnumerationUnderRoots(roots: Collection<VirtualFile>): VirtualFileEnumeration {
    val ids = IntOpenHashSet()

    for (file in roots) {
        if (file is VirtualFileWithId) {
            val children = VirtualFileManager.getInstance().listAllChildIds(file.id)
            ids.addAll(IntList.of(*children))
        }
    }

    return IntSetVirtualFileEnumeration(ids)
}

private class IntSetVirtualFileEnumeration(private val ids: IntSet) : VirtualFileEnumeration {
    override fun contains(fileId: Int): Boolean = ids.contains(fileId)

    override fun asArray(): IntArray = ids.toIntArray()

    override fun equals(other: Any?): Boolean = this === other || other is IntSetVirtualFileEnumeration && ids == other.ids

    override fun hashCode(): Int = ids.hashCode()

    override fun toString(): String = asArray().contentToString()
}
