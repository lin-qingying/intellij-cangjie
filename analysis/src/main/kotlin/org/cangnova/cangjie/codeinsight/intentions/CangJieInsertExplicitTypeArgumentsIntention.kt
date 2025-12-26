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

import org.cangnova.cangjie.diagnostics.Diagnostic
import org.cangnova.cangjie.psi.CjCallElement
import org.cangnova.cangjie.psi.CjCallExpression
import org.cangnova.cangjie.psi.CjPsiFactory
import org.cangnova.cangjie.psi.CjTypeArgumentList
import org.cangnova.cangjie.resolve.caches.analyze
import org.cangnova.cangjie.resolve.caches.safeAnalyzeNonSourceRootCode
import org.cangnova.cangjie.resolve.calls.tower.NewResolvedCallImpl
import org.cangnova.cangjie.resolve.calls.util.getResolvedCall
import org.cangnova.cangjie.resolve.lazy.BodyResolveMode
import org.cangnova.cangjie.types.ErrorUtils
import org.cangnova.cangjie.types.checker.NewCapturedType
import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.codeInsight.intention.LowPriorityAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.util.TextRange
import org.cangnova.cangjie.codeinsight.ShortenReferences
import org.cangnova.cangjie.diagnostics.rendering.IdeDescriptorRenderers
import org.cangnova.cangjie.messages.CangJieCodeInsightBundle
import org.cangnova.cangjie.quickfix.CangJieSingleIntentionActionFactory
import org.cangnova.cangjie.resolve.binding.BindingContext
import org.cangnova.cangjie.resolve.call.inference.CapturedType


class CangJieInsertExplicitTypeArgumentsIntention : SelfTargetingRangeIntention<CjCallExpression>(
    CjCallExpression::class.java,
    CangJieCodeInsightBundle.lazyMessage("add.explicit.type.arguments")
), LowPriorityAction {
    override fun applicabilityRange(element: CjCallExpression): TextRange? =
        if (isApplicableTo(element)) element.calleeExpression?.textRange else null

    override fun applyTo(element: CjCallExpression, editor: Editor?) = applyTo(element)

    companion object : CangJieSingleIntentionActionFactory() {
        override fun createAction(diagnostic: Diagnostic): IntentionAction = CangJieInsertExplicitTypeArgumentsIntention()

        fun isApplicableTo(element: CjCallElement, bindingContext: BindingContext = element.safeAnalyzeNonSourceRootCode(
            BodyResolveMode.PARTIAL)): Boolean {
            if (element.typeArguments.isNotEmpty()) return false
            if (element.calleeExpression == null) return false

            val resolvedCall = element.getResolvedCall(bindingContext) ?: return false
            val typeArgs = resolvedCall.typeArguments
            val valueParameters = resolvedCall.resultingDescriptor.valueParameters
            if (resolvedCall is NewResolvedCallImpl<*> && valueParameters.any { ErrorUtils.containsErrorType(it.type) }) return false


            return typeArgs.isNotEmpty() && typeArgs.values.none { ErrorUtils.containsErrorType(it) || it is CapturedType || it is NewCapturedType }
        }

        fun applyTo(element: CjCallElement, argumentList: CjTypeArgumentList, shortenReferences: Boolean = true) {
            val callee = element.calleeExpression ?: return
            val newArgumentList = element.addAfter(argumentList, callee) as CjTypeArgumentList
            if (shortenReferences) {
                ShortenReferences.DEFAULT.process(newArgumentList)
            }
        }

        fun applyTo(element: CjCallElement, shortenReferences: Boolean = true) {
            val argumentList = createTypeArguments(element, element.analyze()) ?: return
            applyTo(element, argumentList, shortenReferences)
        }

        fun createTypeArguments(element: CjCallElement, bindingContext: BindingContext): CjTypeArgumentList? {
            val resolvedCall = element.getResolvedCall(bindingContext) ?: return null

            val args = resolvedCall.typeArguments
            val types = resolvedCall.candidateDescriptor.typeParameters

            val text = types.joinToString(", ", "<", ">") {
                IdeDescriptorRenderers.SOURCE_CODE.renderType(args.getValue(it))
            }

            return CjPsiFactory(element.project).createTypeArguments(text)
        }
    }
}
