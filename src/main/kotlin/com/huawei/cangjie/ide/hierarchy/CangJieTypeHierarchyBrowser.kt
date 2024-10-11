package com.huawei.cangjie.ide.hierarchy

import com.huawei.cangjie.psi.CjInterface
import com.huawei.cangjie.psi.CjTypeStatement
import com.intellij.ide.hierarchy.HierarchyNodeDescriptor
import com.intellij.ide.hierarchy.HierarchyTreeStructure
import com.intellij.ide.hierarchy.TypeHierarchyBrowserBase
import com.intellij.ide.util.treeView.NodeDescriptor
import com.intellij.openapi.actionSystem.IdeActions
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import javax.swing.JPanel
import javax.swing.JTree

class CangJieTypeHierarchyBrowser(project: Project, cjTypeStatement: CjTypeStatement) : TypeHierarchyBrowserBase(
    project, cjTypeStatement
) {
    companion object {

        val LOG: Logger = Logger.getInstance(
            CangJieTypeHierarchyBrowser::class.java
        )

    }

    override fun getElementFromDescriptor(descriptor: HierarchyNodeDescriptor): PsiElement? {
        if (descriptor !is CangJieHierarchyNodeDescriptor) {
            return null
        }
        return descriptor.psiElement
    }

    override fun createTrees(trees: MutableMap<in String, in JTree>) {
        createTreeAndSetupCommonActions(trees, IdeActions.GROUP_TYPE_HIERARCHY_POPUP)

    }

    override fun createLegendPanel(): JPanel? {
        return null
    }

    override fun isApplicableElement(element: PsiElement): Boolean {
        return element is CjTypeStatement
    }

    override fun createHierarchyTreeStructure(type: String, psiElement: PsiElement): HierarchyTreeStructure? {
        if (getSupertypesHierarchyType() == type) {
            return CangJieSuperTypesHierarchyTreeStructure(psiElement as CjTypeStatement)
        }
        else if (getSubtypesHierarchyType() == type) {
            return CangJieSubTypesHierarchyTreeStructure(psiElement as CjTypeStatement)
        }
        else if (getTypeHierarchyType() == type) {
            return CangJieTypeHierarchyTreeStructure(psiElement as CjTypeStatement)
        } else {
            LOG.error("unexpected type: $type")
            return null
        }

    }

    override fun getComparator(): Comparator<NodeDescriptor<*>> {
        return CangJieHierarchyUtil.getComparator(myProject)
    }

    override fun isInterface(psiElement: PsiElement): Boolean {
        return psiElement is CjInterface
    }

    override fun canBeDeleted(psiElement: PsiElement?): Boolean {
        return psiElement is CjTypeStatement
    }

    override fun getQualifiedName(psiElement: PsiElement?): String {
        return (psiElement as? CjTypeStatement)?.fqName?.asString() ?: ""
    }

}
