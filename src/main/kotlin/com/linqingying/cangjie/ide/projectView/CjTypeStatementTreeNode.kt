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
