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

package cn.cangnova.cangjie.ide.structureView

import cn.cangnova.cangjie.ide.AbstractCangJieIconProvider
import cn.cangnova.cangjie.ide.CangJieIconProvider
import cn.cangnova.cangjie.psi.CjFile
import com.intellij.ide.structureView.StructureViewBuilder
import com.intellij.ide.structureView.StructureViewModel
import com.intellij.ide.structureView.TreeBasedStructureViewBuilder
import com.intellij.ide.util.treeView.smartTree.NodeProvider
import com.intellij.lang.PsiStructureViewFactory
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiFile


class CangJieStructureViewFactory : PsiStructureViewFactory {
    override fun getStructureViewBuilder(psiFile: PsiFile): StructureViewBuilder? {
        if (psiFile !is CjFile) {
            return null
        }

        val isSingleClassFile: Boolean = AbstractCangJieIconProvider. isSingleClassFile(psiFile)

        return object : TreeBasedStructureViewBuilder() {
            override fun createStructureViewModel(editor: Editor?): StructureViewModel {
                return object : CangJieStructureViewModel(psiFile, editor, CangJieStructureViewElement(psiFile, false)) {

                    override fun getNodeProviders(): List<NodeProvider<*>> {
                        return NODE_PROVIDERS
                    }
                }
            }

            override fun isRootNodeShown(): Boolean {
                return !isSingleClassFile
            }
        }
    }

    companion object {
        private val NODE_PROVIDERS = listOf<NodeProvider<*>>(CangJieInheritedMembersNodeProvider())
    }
}
