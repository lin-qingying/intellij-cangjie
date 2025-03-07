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

package cn.cangnova.cangjie.ide.projectView

import cn.cangnova.cangjie.psi.CjDeclaration
import cn.cangnova.cangjie.psi.CjFile
import cn.cangnova.cangjie.psi.CjTypeStatement
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
