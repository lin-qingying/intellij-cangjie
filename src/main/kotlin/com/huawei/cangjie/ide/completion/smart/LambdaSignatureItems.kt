package com.huawei.cangjie.ide.completion.smart

import com.huawei.cangjie.builtins.getValueParameterTypesFromFunctionType
import com.huawei.cangjie.builtins.isFunctionType
import com.huawei.cangjie.ide.ExpectedInfos
import com.huawei.cangjie.ide.completion.LambdaSignatureTemplates
import com.huawei.cangjie.ide.completion.suppressAutoInsertion
import com.huawei.cangjie.ide.fuzzyType
import com.huawei.cangjie.psi.CjBlockExpression
import com.huawei.cangjie.psi.CjExpression
import com.huawei.cangjie.psi.CjFunctionLiteral
import com.huawei.cangjie.psi.CjLambdaExpression
import com.huawei.cangjie.resolve.BindingContext
import com.huawei.cangjie.resolve.ResolutionFacade
import com.huawei.cangjie.types.CangJieType
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.openapi.util.TextRange

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
