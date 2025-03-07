/*
 * Copyright 2024 LinQingYing. and contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * The use of this source code is governed by the Apache License 2.0,
 * which allows users to freely use, modify, and distribute the code,
 * provided they adhere to the terms of the license.
 *
 * The software is provided "as-is", and the authors are not responsible for
 * any damages or issues arising from its use.
 *
 */

package cn.cangnova.cangjie.ide.hierarchy

import cn.cangnova.cangjie.psi.CjInterface
import cn.cangnova.cangjie.psi.CjTypeStatement
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
