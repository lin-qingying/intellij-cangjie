package com.huawei.cangjie.ide.projectView

import com.intellij.ide.projectView.TreeStructureProvider
import com.intellij.ide.projectView.ViewSettings
import com.intellij.ide.util.treeView.AbstractTreeNode
import com.intellij.openapi.project.DumbAware

class CangJieExpandNodeProjectViewProvider: TreeStructureProvider, DumbAware {
    override fun modify(
        parent: AbstractTreeNode<*>,
        children: Collection<AbstractTreeNode<*>>,
        settings: ViewSettings
    ): MutableCollection<AbstractTreeNode<*>> {
        TODO("Not yet implemented")
    }
}
