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
 *
 * The use of this source code is governed by the Apache License 2.0,
 * which allows users to freely use, modify, and distribute the code,
 * provided they adhere to the terms of the license.
 *
 * The software is provided "as-is", and the authors are not responsible for
 * any damages or issues arising from its use.
 *
 */

package org.cangnova.cangjie.ide.codeInsight.template.template

import org.cangnova.cangjie.refactoring.introduce.CangJieIntroduceVariableHandler
import org.cangnova.cangjie.lang.CangJieLanguage
import org.cangnova.cangjie.psi.CjExpression
import com.intellij.codeInsight.template.postfix.templates.PostfixTemplateProvider
import com.intellij.lang.LanguageRefactoringSupport
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiElement

internal enum class VaribaleKind(val kind: String) {
    VAR("var"),
    LET("let"),
    CONST("const");

    override fun toString(): String {
        return kind
    }
}

internal abstract class AbstractCangJieVariablePostfixTemplate(
    kind: String,
    provider: PostfixTemplateProvider
) : AbstractCangJiePostfixTemplateWithExpression(
    kind, provider
) {
    override fun expandForChooseExpression(expression: PsiElement, editor: Editor) {
        val isVar = kind == "var"
        val provider = LanguageRefactoringSupport.INSTANCE.forLanguage(CangJieLanguage)
        val introduceVariableHandler =
            provider.introduceVariableHandler as? CangJieIntroduceVariableHandler ?: return


        introduceVariableHandler.selectTargetContainerAndPerformIntroduce(
            expression.project, editor, expression as? CjExpression, isVar,
        )

    }
}

internal class CangJieVarPostfixTemplate(
    provider: CangJiePostfixTemplateProvider,
) : AbstractCangJieVariablePostfixTemplate("var", provider)

internal class CangJieLetPostfixTemplate(
    provider: CangJiePostfixTemplateProvider,

    ) : AbstractCangJieVariablePostfixTemplate("let", provider) {

}
