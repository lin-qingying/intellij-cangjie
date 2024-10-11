package com.huawei.cangjie.ide.hierarchy

import com.huawei.cangjie.psi.CjTypeStatement
import com.intellij.ide.hierarchy.HierarchyBrowser
import com.intellij.ide.hierarchy.HierarchyProvider
import com.intellij.ide.hierarchy.TypeHierarchyBrowserBase
import com.intellij.psi.PsiElement

abstract class AbstractCangJieTypeHierarchyProvider: HierarchyProvider {

    override fun browserActivated(hierarchyBrowser: HierarchyBrowser) {
        val browser = hierarchyBrowser as CangJieTypeHierarchyBrowser
        val typeName =
            if (browser.isInterface) TypeHierarchyBrowserBase.getSubtypesHierarchyType() else TypeHierarchyBrowserBase.getTypeHierarchyType()
        browser.changeView(typeName)
    }
    override fun createHierarchyBrowser(target: PsiElement): HierarchyBrowser {
        return CangJieTypeHierarchyBrowser(target.project, target as CjTypeStatement)

    }
}
