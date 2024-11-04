package com.linqingying.cangjie.ide.projectView

import com.linqingying.cangjie.psi.CjDeclaration
import com.linqingying.cangjie.psi.CjFile
import com.linqingying.cangjie.psi.CjTypeStatement
import com.intellij.ide.projectView.ViewSettings
import com.intellij.ide.projectView.impl.nodes.AbstractPsiBasedNode
import com.intellij.ide.projectView.impl.nodes.FileNodeWithNestedFileNodes
import com.intellij.ide.projectView.impl.nodes.PsiFileNode
import com.intellij.ide.util.treeView.AbstractTreeNode
import com.intellij.openapi.project.Project

class CjFileTreeNode(
    project: Project?,
    val cjFile: CjFile,
    viewSettings: ViewSettings,
    private val nestedFileNodes: Collection<AbstractTreeNode<*>>
) : PsiFileNode(project, cjFile, viewSettings), FileNodeWithNestedFileNodes {

    override fun getNestedFileNodes(): Collection<AbstractTreeNode<*>> = nestedFileNodes

    override fun getChildrenImpl(): Collection<AbstractTreeNode<*>> =
        if (settings.isShowMembers) {
            nestedFileNodes + cjFile.toDeclarationsNodes(settings)
        } else {
            nestedFileNodes
        }
}

internal fun CjFile.toDeclarationsNodes(settings: ViewSettings): Collection<AbstractPsiBasedNode<out CjDeclaration?>> =
    this.declarations.toNodes(settings)

internal fun Collection<CjDeclaration>.toNodes(settings: ViewSettings): Collection<AbstractPsiBasedNode<out CjDeclaration?>> =
    mapNotNull {
        val project = it.project
        if (it is CjTypeStatement) {
            CjTypeStatementTreeNode(project, it, settings)
        } else {
           CjDeclarationTreeNode.create(project, it, settings)
        }
    }
