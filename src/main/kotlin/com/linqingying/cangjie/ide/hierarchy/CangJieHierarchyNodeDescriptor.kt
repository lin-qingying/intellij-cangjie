package com.linqingying.cangjie.ide.hierarchy

import com.linqingying.cangjie.psi.CjTypeStatement
import com.intellij.ide.hierarchy.HierarchyNodeDescriptor
import com.intellij.ide.util.treeView.NodeDescriptor
import com.intellij.openapi.roots.ui.util.CompositeAppearance
import com.intellij.pom.Navigatable
import com.intellij.psi.PsiElement


class aba

class CangJieHierarchyNodeDescriptor(
    parentDescriptor: NodeDescriptor<*>?,
     psiElement: PsiElement,
    val usages: List<PsiElement>,
    isBase: Boolean
) : HierarchyNodeDescriptor(
    psiElement.project, parentDescriptor, psiElement, isBase
), Navigatable {

    constructor(parentDescriptor: NodeDescriptor<*>?, element: PsiElement, isBase: Boolean) : this(
        parentDescriptor,
        element,
        emptyList <PsiElement>(),
        isBase
    )

    private fun getNavigationTarget(): Navigatable?  {

        return psiElement as? Navigatable
    }
    override fun navigate(requestFocus: Boolean) {
        val element  = getNavigationTarget()
        if (element != null && element.canNavigate()) {
            element.navigate(requestFocus)
        }
    }

    override fun canNavigate(): Boolean {
        val element = getNavigationTarget()
        return element != null && element.canNavigate()
    }
    override fun update(): Boolean {
        val changes = super.update()
        myHighlightedText = CompositeAppearance()

        if (psiElement is CjTypeStatement) {
            myHighlightedText.ending.addText(
                (psiElement as CjTypeStatement).name
            )

        }
        return changes
    }

}
