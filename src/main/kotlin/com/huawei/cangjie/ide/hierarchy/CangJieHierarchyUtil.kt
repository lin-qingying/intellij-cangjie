package com.huawei.cangjie.ide.hierarchy

import com.intellij.ide.hierarchy.HierarchyBrowserManager
import com.intellij.ide.util.treeView.AlphaComparator
import com.intellij.ide.util.treeView.NodeDescriptor
import com.intellij.openapi.project.Project

object CangJieHierarchyUtil {

    val NODE_DESCRIPTOR_COMPARATOR: java.util.Comparator<NodeDescriptor<*>> =
        java.util.Comparator.comparingInt { obj: NodeDescriptor<*> -> obj.index }

    fun getComparator(project: Project): Comparator<NodeDescriptor<*>> {
        return if (HierarchyBrowserManager.getInstance(project).state!!.SORT_ALPHABETICALLY) {
            AlphaComparator.INSTANCE
        } else {
           NODE_DESCRIPTOR_COMPARATOR
        }
    }

}
