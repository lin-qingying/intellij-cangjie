/*
 * Copyright 2026 LinQingYing. and contributors.
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
 */

package org.cangnova.cangjie.ide.project

import com.intellij.ide.projectView.TreeStructureProvider
import com.intellij.ide.projectView.ViewSettings
import com.intellij.ide.projectView.impl.nodes.PsiFileNode
import com.intellij.ide.util.treeView.AbstractTreeNode
import com.intellij.openapi.project.DumbAware
import org.cangnova.cangjie.macro.file.CjMacroCallFile

/**
 * 在项目视图中隐藏 `.macrocall` 文件
 *
 * 当 `cangjie.macro.expansion.file.visible` 为 `false`（默认）时，
 * `.macrocall` 文件将从项目视图树中过滤掉。
 */
internal class CjMacroCallFileTreeStructureProvider : TreeStructureProvider, DumbAware {

    override fun modify(
        parent: AbstractTreeNode<*>,
        children: MutableCollection<AbstractTreeNode<*>>,
        settings: ViewSettings
    ): Collection<AbstractTreeNode<*>> {
        if (CjMacroCallFile.isUserVisible()) return children

        return children.filter { node ->
            if (node is PsiFileNode) {
                val file = node.virtualFile
                file == null || !CjMacroCallFile.isMacroCallFile(file)
            } else {
                true
            }
        }
    }
}