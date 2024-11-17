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
