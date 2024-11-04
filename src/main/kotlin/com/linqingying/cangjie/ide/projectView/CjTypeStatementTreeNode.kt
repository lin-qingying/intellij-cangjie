package com.linqingying.cangjie.ide.projectView

import com.linqingying.cangjie.psi.CjPsiUtil
import com.linqingying.cangjie.psi.CjTypeStatement
import com.intellij.ide.projectView.PresentationData
import com.intellij.ide.projectView.ViewSettings
import com.intellij.ide.projectView.impl.nodes.AbstractPsiBasedNode
import com.intellij.ide.projectView.impl.nodes.FileNodeWithNestedFileNodes
import com.intellij.ide.util.treeView.AbstractTreeNode
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement



class CjTypeStatementTreeNode(
    project: Project?,
    cjClassOrObject: CjTypeStatement,
    viewSettings: ViewSettings,
    private val nestedFileNodes: Collection<AbstractTreeNode<*>>
) : AbstractPsiBasedNode<CjTypeStatement>(project, cjClassOrObject, viewSettings), FileNodeWithNestedFileNodes {

    // this constructor is kept for plugin API compatibility
    constructor(
        project: Project?,
        cjClassOrObject: CjTypeStatement,
        viewSettings: ViewSettings
    ) : this(project, cjClassOrObject, viewSettings, emptyList())

    override fun extractPsiFromValue(): PsiElement? = value

    override fun getNestedFileNodes(): Collection<AbstractTreeNode<*>> = nestedFileNodes

    override fun getChildrenImpl(): Collection<AbstractTreeNode<*>> =
        if (value != null && settings.isShowMembers) {
            nestedFileNodes + value.getStructureDeclarations().toNodes(settings)
        } else {
            nestedFileNodes
        }

    override fun updateImpl(data: PresentationData) {
        value?.let {
            data.presentableText = CjDeclarationTreeNode.tryGetRepresentableText(it)
        }
    }

    override fun isDeprecated() = CjPsiUtil.isDeprecated(value)

    override fun canRepresent(element: Any?): Boolean {
        if (!isValid) {
            return false
        }

        return super.canRepresent(element) || canRepresentPsiElement(element)
    }

    private fun canRepresentPsiElement(element: Any?): Boolean {
        if (value == null || !value.isValid) {
            return false
        } else if (value === element) {
            return true
        }

        val file = value.containingFile
        return when (element) {
            file -> true
            is VirtualFile -> element == file.virtualFile
            is PsiElement -> !settings.isShowMembers && file == element.containingFile
            else -> false
        }
    }

    override fun expandOnDoubleClick(): Boolean = false

    override fun getWeight() = 20
}
fun CjTypeStatement.getStructureDeclarations() =
    buildList {
        primaryConstructor?.let { add(it) }
        primaryConstructorParameters.filterTo(this) { it.hasLetOrVar() }
        addAll(declarations)
    }
