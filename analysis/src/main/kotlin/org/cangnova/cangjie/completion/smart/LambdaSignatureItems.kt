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

package org.cangnova.cangjie.completion.smart

import org.cangnova.cangjie.indices.ExpectedInfos
import org.cangnova.cangjie.completion.LambdaSignatureTemplates
import org.cangnova.cangjie.completion.suppressAutoInsertion
import org.cangnova.cangjie.indices.fuzzyType
import org.cangnova.cangjie.psi.CjBlockExpression
import org.cangnova.cangjie.psi.CjExpression
import org.cangnova.cangjie.psi.CjFunctionLiteral
import org.cangnova.cangjie.psi.CjLambdaExpression
import org.cangnova.cangjie.resolve.ResolutionFacade
import org.cangnova.cangjie.types.CangJieType
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.openapi.util.TextRange
import org.cangnova.cangjie.resolve.binding.BindingContext
import org.cangnova.cangjie.types.getValueParameterTypesFromFunctionType
import org.cangnova.cangjie.types.isFunctionType

object LambdaSignatureItems {
    fun addToCollection(
        collection: MutableCollection<LookupElement>,
        position: CjExpression,
        bindingContext: BindingContext,
        resolutionFacade: ResolutionFacade,
    ) {
        val block = position.parent as? CjBlockExpression ?: return
        if (position != block.statements.first()) return
        val functionLiteral = block.parent as? CjFunctionLiteral ?: return
        if (functionLiteral.arrow != null) return
        val literalExpression = functionLiteral.parent as CjLambdaExpression

        val expectedFunctionTypes = ExpectedInfos(bindingContext, resolutionFacade, null).calculate(literalExpression)
            .mapNotNull { it.fuzzyType?.type }
            .filter { it.isFunctionType }
            .toSet()

        for (functionType in expectedFunctionTypes) {
            if (functionType.getValueParameterTypesFromFunctionType().isEmpty()) continue

            if (LambdaSignatureTemplates.explicitParameterTypesRequired(expectedFunctionTypes, functionType)) {
                collection.add(
                    createLookupElement(
                        functionType,
                        LambdaSignatureTemplates.SignaturePresentation.NAMES_OR_TYPES,
                        explicitParameterTypes = true,
                    )
                )
            } else {
                collection.add(
                    createLookupElement(
                        functionType,
                        LambdaSignatureTemplates.SignaturePresentation.NAMES,
                        explicitParameterTypes = false,
                    )
                )

                collection.add(
                    createLookupElement(
                        functionType,
                        LambdaSignatureTemplates.SignaturePresentation.NAMES_AND_TYPES,
                        explicitParameterTypes = true,
                    )
                )
            }
        }
    }

    private fun createLookupElement(
        functionType: CangJieType,
        signaturePresentation: LambdaSignatureTemplates.SignaturePresentation,
        explicitParameterTypes: Boolean,
    ): LookupElement {
        val lookupString = LambdaSignatureTemplates.signaturePresentation(functionType, signaturePresentation)
        val priority = if (explicitParameterTypes)
            SmartCompletionItemPriority.LAMBDA_SIGNATURE_EXPLICIT_PARAMETER_TYPES
        else
            SmartCompletionItemPriority.LAMBDA_SIGNATURE

        return LookupElementBuilder.create(lookupString)
            .withInsertHandler { context, _ ->
                LambdaSignatureTemplates.insertTemplate(
                    context,
                    TextRange(context.startOffset, context.tailOffset),
                    functionType,
                    explicitParameterTypes,
                    signatureOnly = true,
                )
            }
            .suppressAutoInsertion()
            .assignSmartCompletionPriority(priority)
    }
}
