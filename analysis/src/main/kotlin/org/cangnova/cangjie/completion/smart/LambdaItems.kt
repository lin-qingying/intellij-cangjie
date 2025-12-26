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


import org.cangnova.cangjie.indices.ExpectedInfo
import org.cangnova.cangjie.completion.LambdaSignatureTemplates
import org.cangnova.cangjie.completion.suppressAutoInsertion
import org.cangnova.cangjie.indices.fuzzyType
import org.cangnova.cangjie.resolve.calls.util.getValueParametersCountFromFunctionType
import org.cangnova.cangjie.types.CangJieType
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.openapi.util.TextRange
import org.cangnova.cangjie.types.isFunctionType

object LambdaItems {
    fun collect(functionExpectedInfos: Collection<ExpectedInfo>): Collection<LookupElement> {
        val list = ArrayList<LookupElement>()
        addToCollection(list, functionExpectedInfos)
        return list
    }

    fun addToCollection(collection: MutableCollection<LookupElement>, expectedInfos: Collection<ExpectedInfo>) {
        val functionExpectedInfos = expectedInfos.filter { it.fuzzyType?.type?.isFunctionType == true }
        if (functionExpectedInfos.isEmpty()) return

        val functionTypes = functionExpectedInfos.mapNotNull { it.fuzzyType?.type }.toSet()

        val singleType = if (functionTypes.size == 1) functionTypes.single() else null
        val singleSignatureLength = singleType?.let(::getValueParametersCountFromFunctionType)
        val offerNoParametersLambda = singleSignatureLength == 0 || singleSignatureLength == 1
        if (offerNoParametersLambda) {
            val lookupElement = LookupElementBuilder.create(LambdaSignatureTemplates.DEFAULT_LAMBDA_PRESENTATION)
                .withInsertHandler(ArtificialElementInsertHandler("{ ", " }", false))
                .suppressAutoInsertion()
                .assignSmartCompletionPriority(SmartCompletionItemPriority.LAMBDA_NO_PARAMS)
                .addTailAndNameSimilarity(functionExpectedInfos)

            collection.add(lookupElement)
        }

        if (singleSignatureLength != 0) {
            for (functionType in functionTypes) {
                if (LambdaSignatureTemplates.explicitParameterTypesRequired(functionTypes, functionType)) {
                    collection.add(
                        createLookupElement(
                            functionType,
                            functionExpectedInfos,
                            LambdaSignatureTemplates.SignaturePresentation.NAMES_OR_TYPES,
                            explicitParameterTypes = true,
                        )
                    )

                } else {
                    collection.add(
                        createLookupElement(
                            functionType,
                            functionExpectedInfos,
                            LambdaSignatureTemplates.SignaturePresentation.NAMES_AND_TYPES,
                            explicitParameterTypes = true,
                        )
                    )

                    collection.add(
                        createLookupElement(
                            functionType,
                            functionExpectedInfos,
                            LambdaSignatureTemplates.SignaturePresentation.NAMES,
                            explicitParameterTypes = false,
                        )
                    )
                }
            }
        }
    }

    private fun createLookupElement(
        functionType: CangJieType,
        functionExpectedInfos: List<ExpectedInfo>,
        signaturePresentation: LambdaSignatureTemplates.SignaturePresentation,
        explicitParameterTypes: Boolean,
    ): LookupElement {
        val lookupString = LambdaSignatureTemplates.lambdaPresentation(functionType, signaturePresentation)
        return LookupElementBuilder.create(lookupString)
            .withInsertHandler { context, _ ->
                LambdaSignatureTemplates.insertTemplate(
                    context,
                    TextRange(context.startOffset, context.tailOffset),
                    functionType,
                    explicitParameterTypes,
                    signatureOnly = false,
                )
            }
            .suppressAutoInsertion()
            .assignSmartCompletionPriority(SmartCompletionItemPriority.LAMBDA)
            .addTailAndNameSimilarity(functionExpectedInfos.filter { it.fuzzyType?.type == functionType })
    }
}
