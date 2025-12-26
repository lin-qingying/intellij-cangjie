/*
 * Copyright 2025 LinQingYing. and contributors.
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

package org.cangnova.cangjie.codeinsight.intentions

import com.intellij.codeInsight.intention.LowPriorityAction
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiWhiteSpace
import org.cangnova.cangjie.codeinsight.ShortenReferences
import org.cangnova.cangjie.descriptors.ValueParameterDescriptor
import org.cangnova.cangjie.diagnostics.rendering.IdeDescriptorRenderers
import org.cangnova.cangjie.messages.CangJieCodeInsightBundle
import org.cangnova.cangjie.psi.CjFunctionLiteral
import org.cangnova.cangjie.psi.CjLambdaExpression
import org.cangnova.cangjie.psi.CjParameterList
import org.cangnova.cangjie.psi.CjPsiFactory
import org.cangnova.cangjie.resolve.binding.BindingContext
import org.cangnova.cangjie.resolve.caches.analyze
import org.cangnova.cangjie.resolve.caches.safeAnalyzeNonSourceRootCode
import org.cangnova.cangjie.resolve.lazy.BodyResolveMode
import org.cangnova.cangjie.types.isError
import org.cangnova.cangjie.utils.runWriteActionIfPhysical
import kotlin.text.get

open class SpecifyExplicitLambdaSignatureIntention : SelfTargetingOffsetIndependentIntention<CjLambdaExpression>(
    CjLambdaExpression::class.java, CangJieCodeInsightBundle.lazyMessage("specify.explicit.lambda.signature")
), LowPriorityAction {
    override fun isApplicableTo(element: CjLambdaExpression): Boolean {
        if (element.functionLiteral.arrow != null && element.valueParameters.all { it.typeReference != null }) return false
        val functionDescriptor = element.safeAnalyzeNonSourceRootCode(BodyResolveMode.PARTIAL)[BindingContext.FUNCTION, element.functionLiteral] ?: return false
        return functionDescriptor.valueParameters.none { it.type.isError }
    }

    override fun applyTo(element: CjLambdaExpression, editor: Editor?) {
        Holder.applyTo(element)
    }

    object Holder {
        fun applyTo(element: CjLambdaExpression) {
            val functionLiteral = element.functionLiteral
            val functionDescriptor = element.analyze(BodyResolveMode.PARTIAL)[BindingContext.FUNCTION, functionLiteral]!!

            applyWithParameters(element, functionDescriptor.valueParameters
                .asSequence()
                .mapIndexed { index, parameterDescriptor ->
                    parameterDescriptor.render(psiName = functionLiteral.valueParameters.getOrNull(index)?.let {
                        it.name
                    })
                }
                .joinToString())
        }

        fun CjFunctionLiteral.setParameterListIfAny(psiFactory: CjPsiFactory, newParameterList: CjParameterList?) {
            val oldParameterList = valueParameterList
            if (oldParameterList != null && newParameterList != null) {
                oldParameterList.replace(newParameterList)
            } else {
                val openBraceElement = lBrace
                val nextSibling = openBraceElement.nextSibling
                val addNewline = nextSibling is PsiWhiteSpace && nextSibling.text?.contains("\n") ?: false
                val (whitespace, arrow) = psiFactory.createWhitespaceAndArrow()
                addRangeAfter(whitespace, arrow, openBraceElement)
                if (newParameterList != null) {
                    addAfter(newParameterList, openBraceElement)
                }

                if (addNewline) {
                    addAfter(psiFactory.createNewLine(), openBraceElement)
                }
            }
        }

        fun applyWithParameters(element: CjLambdaExpression, parameterString: String) {
            val psiFactory = CjPsiFactory(element.project)
            val functionLiteral = element.functionLiteral
            val newParameterList =
                (psiFactory.createExpression("{ $parameterString -> }") as CjLambdaExpression).functionLiteral.valueParameterList
            runWriteActionIfPhysical(element) {
                functionLiteral.setParameterListIfAny(psiFactory, newParameterList)
                ShortenReferences.DEFAULT.process(element.valueParameters)
            }
        }
    }
}

private fun ValueParameterDescriptor.render(psiName: String?): String = IdeDescriptorRenderers.SOURCE_CODE.let {
    "${psiName ?: it.renderName(name, true)}: ${it.renderType(type)}"
}
