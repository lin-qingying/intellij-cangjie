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

package cn.cangnova.cangjie.ide.inspections.suppress

import cn.cangnova.cangjie.ide.codeinsight.CangJieCodeInsightBundle
import cn.cangnova.cangjie.psi.CjDestructuringDeclarationEntry
import cn.cangnova.cangjie.psi.CjElement
import cn.cangnova.cangjie.psi.CjParameter
import com.intellij.codeInsight.intention.FileModifier
import com.intellij.codeInspection.SuppressIntentionAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement


class CangJieSuppressIntentionAction(
    suppressAt: CjElement,
    private val suppressionKey: String,
    @FileModifier.SafeFieldForPreview private val kind: AnnotationHostKind
) : SuppressIntentionAction() {
    override fun getFamilyName(): String  = CangJieCodeInsightBundle.message("intention.suppress.family")
    private fun isLambdaParameter(element: PsiElement): Boolean {
        if (kind.kind != CangJieCodeInsightBundle.message("declaration.kind.parameter")) return false
        val parentParameter = element.parent as? CjParameter
            ?: (element.parent as? CjDestructuringDeclarationEntry)?.parent?.parent as? CjParameter
        return parentParameter?.isLambdaParameter == true
    }
    override fun isAvailable(project: Project, editor: Editor?, element: PsiElement): Boolean {
        if (isLambdaParameter(element)) {

            return false
        }

        return element.isValid
    }

    override fun invoke(project: Project, editor: Editor?, element: PsiElement) {
//        TODO("Not yet implemented")
    }
}
