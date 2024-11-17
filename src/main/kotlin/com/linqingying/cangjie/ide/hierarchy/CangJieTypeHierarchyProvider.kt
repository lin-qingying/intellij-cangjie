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

import com.linqingying.cangjie.ide.base.projectStructure.RootKindFilter
import com.linqingying.cangjie.ide.base.projectStructure.matches
import com.linqingying.cangjie.ide.stubindex.CangJieClassShortNameIndex
import com.linqingying.cangjie.psi.CjConstructor
import com.linqingying.cangjie.psi.CjNamedFunction
import com.linqingying.cangjie.psi.CjTypeStatement
import com.linqingying.cangjie.psi.psiUtil.getNonStrictParentOfType
import com.linqingying.cangjie.utils.module
import com.intellij.codeInsight.TargetElementUtil
import com.intellij.ide.hierarchy.HierarchyBrowser
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.actionSystem.LangDataKeys
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiNamedElement
import com.intellij.psi.search.GlobalSearchScope

class CangJieTypeHierarchyProvider : AbstractCangJieTypeHierarchyProvider() {
    private fun getTargetByReference(
        project: Project,
        editor: Editor,
        module: Module?
    ): CjTypeStatement? {

        return when (val target =
            TargetElementUtil.findTargetElement(editor, TargetElementUtil.getInstance().allAccepted)) {

            is CjConstructor<*> -> target.getContainingTypeStatement()
            is CjTypeStatement -> target
            is CjNamedFunction -> { // Factory methods
                val functionName = target.name ?: return null
                val returnTypeText = target.typeReference?.text
                if (returnTypeText?.substringAfter(".") != functionName) return null
                CangJieClassShortNameIndex.get(functionName, project, GlobalSearchScope.allScope(project))
                    .singleOrNull()
                    ?: return null

            }

            else -> null
        }
    }

    private fun getTargetByContainingElement(editor: Editor, file: PsiFile): CjTypeStatement? {
        val offset = editor.caretModel.offset
        val element = file.findElementAt(offset) ?: return null
        return element.getNonStrictParentOfType<CjTypeStatement>()

    }

    override fun getTarget(dataContext: DataContext): PsiElement? {
        val project = PlatformDataKeys.PROJECT.getData(dataContext) ?: return null

        val editor = PlatformDataKeys.EDITOR.getData(dataContext)
        if (editor != null) {
            val file = PsiDocumentManager.getInstance(project).getPsiFile(editor.document) ?: return null
            if (!RootKindFilter.projectAndLibrarySources.matches(file)) return null
            val psiElement =
                getTargetByReference(project, editor, file.module) ?: getTargetByContainingElement(editor, file)
            if (psiElement is PsiNamedElement && psiElement.name == null) {
                return null
            }
            return psiElement
        }

        val element = LangDataKeys.PSI_ELEMENT.getData(dataContext)
        if (element is CjTypeStatement) return element

        return null
    }



}
