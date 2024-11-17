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

package com.linqingying.cangjie.ide.completion

import com.linqingying.cangjie.builtins.extractParameterNameFromFunctionTypeArgument
import com.linqingying.cangjie.builtins.getValueParameterTypesFromFunctionType
import com.linqingying.cangjie.builtins.isFunctionType
import com.linqingying.cangjie.ide.ExpectedInfos
import com.linqingying.cangjie.ide.IdeDescriptorRenderers
import com.linqingying.cangjie.ide.codeinsight.newDeclaration.CangJieNameSuggester
import com.linqingying.cangjie.ide.completion.handlers.isCharAt
import com.linqingying.cangjie.ide.fuzzyType
import com.linqingying.cangjie.psi.CjExpression
import com.linqingying.cangjie.psi.CjFile
import com.linqingying.cangjie.renderer.DescriptorRendererModifier
import com.linqingying.cangjie.renderer.render
import com.linqingying.cangjie.resolve.caches.getResolutionFacade
import com.linqingying.cangjie.resolve.calls.util.getValueParametersCountFromFunctionType
import com.linqingying.cangjie.resolve.lazy.BodyResolveMode
import com.linqingying.cangjie.types.CangJieType
import com.linqingying.cangjie.types.TypeProjection
import com.linqingying.cangjie.utils.executeWriteCommand
import com.intellij.codeInsight.completion.InsertionContext
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.codeInsight.template.*
import com.intellij.openapi.command.CommandProcessor
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.util.PsiTreeUtil

object LambdaSignatureTemplates {
    fun insertTemplate(
        context: InsertionContext,
        placeholderRange: TextRange,
        lambdaType: CangJieType,
        explicitParameterTypes: Boolean,
        signatureOnly: Boolean
    ) {
        // we start template later to not interfere with insertion of tail type
        val commandProcessor = CommandProcessor.getInstance()
        val commandName = commandProcessor.currentCommandName ?: CangJieCompletionBundle.message("insert.lambda.template")
        val commandGroupId = commandProcessor.currentCommandGroupId

        val rangeMarker = context.document.createRangeMarker(placeholderRange)

        context.setLaterRunnable {
            context.project.executeWriteCommand(commandName, groupId = commandGroupId) {
                try {
                    if (rangeMarker.isValid) {
                        val startOffset = rangeMarker.startOffset
                        context.document.deleteString(startOffset, rangeMarker.endOffset)

                        if (signatureOnly) {
                            val spaceAhead = context.document.charsSequence.isCharAt(startOffset, ' ')
                            if (!spaceAhead) {
                                context.document.insertString(startOffset, " ")
                            }
                        }

                        context.editor.caretModel.currentCaret.moveToOffset(startOffset)
                        val template = buildTemplate(lambdaType, signatureOnly, explicitParameterTypes, context.project)
                        TemplateManager.getInstance(context.project).startTemplate(
                            /* editor = */ context.editor,
                            /* template = */ template,
                            /* inSeparateCommand = */ false,
                            /* predefinedVarValues = */ null,
                            /* listener = */ null,
                        )
                    }
                } finally {
                    rangeMarker.dispose()
                }
            }
        }
    }

    val DEFAULT_LAMBDA_PRESENTATION = "{...}"

    enum class SignaturePresentation {
        NAMES,
        NAMES_OR_TYPES,
        NAMES_AND_TYPES
    }

    fun lambdaPresentation(lambdaType: CangJieType, presentationKind: SignaturePresentation): String {
        return "{ " + signaturePresentation(lambdaType, presentationKind) + " ... }"
    }

    fun signaturePresentation(lambdaType: CangJieType, presentationKind: SignaturePresentation): String {
        fun typePresentation(type: CangJieType) = IdeDescriptorRenderers.SOURCE_CODE_SHORT_NAMES_NO_ANNOTATIONS.renderType(type)

        fun parameterPresentation(parameterType: CangJieType): String {
            val name = parameterType.extractParameterNameFromFunctionTypeArgument()?.render()
            return when (presentationKind) {
                SignaturePresentation.NAMES -> name ?: nameSuggestion(parameterType)
                SignaturePresentation.NAMES_OR_TYPES -> name ?: typePresentation(parameterType)
                SignaturePresentation.NAMES_AND_TYPES -> "${name ?: nameSuggestion(parameterType)}: ${typePresentation(parameterType)}"
            }
        }

        return functionParameterTypes(lambdaType).joinToString(", ", transform = ::parameterPresentation) + " ->"
    }

    fun explicitParameterTypesRequired(context: InsertionContext, placeholderRange: TextRange, lambdaType: CangJieType): Boolean {
        val file = context.file as? CjFile ?: return false
        PsiDocumentManager.getInstance(file.project).commitDocument(context.document)
        val expression =
            PsiTreeUtil.findElementOfClassAtRange(file, placeholderRange.startOffset, placeholderRange.endOffset, CjExpression::class.java)
                ?: return false

        val resolutionFacade = file.getResolutionFacade()
        val bindingContext = resolutionFacade.analyze(expression, BodyResolveMode.PARTIAL)
        val expectedInfos =
            ExpectedInfos(bindingContext, resolutionFacade, indicesHelper = null, useHeuristicSignatures = false).calculate(expression)
        val functionTypes = expectedInfos
            .mapNotNull { it.fuzzyType?.type }
            .filter(CangJieType::isFunctionType)
            .toSet()
        return explicitParameterTypesRequired(functionTypes, lambdaType)
    }

    fun explicitParameterTypesRequired(expectedFunctionTypes: Set<CangJieType>, lambdaType: CangJieType): Boolean {
        if (expectedFunctionTypes.size <= 1) return false
        val lambdaParameterCount = getValueParametersCountFromFunctionType(lambdaType)
        return expectedFunctionTypes.filter { getValueParametersCountFromFunctionType(it) == lambdaParameterCount }.size > 1
    }

    private val TYPE_RENDERER = IdeDescriptorRenderers.SOURCE_CODE.withOptions {
        modifiers -= DescriptorRendererModifier.ANNOTATIONS
    }

    private fun buildTemplate(
        lambdaType: CangJieType,
        signatureOnly: Boolean,
        explicitParameterTypes: Boolean,
        project: Project
    ): Template {
        val parameterTypes = functionParameterTypes(lambdaType)

        val manager = TemplateManager.getInstance(project)

        val template = manager.createTemplate("", "")
        template.isToShortenLongNames = true
        //template.setToReformat(true) //TODO
        if (!signatureOnly) {
            template.addTextSegment("{ ")
        }

        val noNameParameterCount = mutableMapOf<CangJieType, Int>()
        for ((i, parameterType) in parameterTypes.withIndex()) {
            if (i > 0) {
                template.addTextSegment(", ")
            }
            //TODO: check for names in scope
            val parameterName = parameterType.extractParameterNameFromFunctionTypeArgument()?.render()
            val nameExpression = if (parameterName != null) {
                object : Expression() {
                    override fun calculateResult(context: ExpressionContext?) = TextResult(parameterName)
                    override fun calculateQuickResult(context: ExpressionContext?): Result = TextResult(parameterName)
                    override fun calculateLookupItems(context: ExpressionContext?) = emptyArray<LookupElement>()
                }
            } else {
                val count = (noNameParameterCount[parameterType] ?: 0) + 1
                noNameParameterCount[parameterType] = count
                val suffix = if (count == 1) null else "$count"
                val nameSuggestions = nameSuggestions(parameterType, suffix)
                object : Expression() {
                    override fun calculateResult(context: ExpressionContext?) = TextResult(nameSuggestions[0])
                    override fun calculateQuickResult(context: ExpressionContext?): Result? = null
                    override fun calculateLookupItems(context: ExpressionContext?) =
                        nameSuggestions.map { LookupElementBuilder.create(it) }.toTypedArray()
                }
            }
            template.addVariable(nameExpression, true)

            if (explicitParameterTypes) {
                template.addTextSegment(": " + TYPE_RENDERER.renderType(parameterType))
            }
        }

        template.addTextSegment(" -> ")
        template.addEndVariable()

        if (!signatureOnly) {
            template.addTextSegment(" }")
        }

        return template
    }

    private fun nameSuggestions(parameterType: CangJieType, suffix: String? = null): List<String> {
        val suggestions =  CangJieNameSuggester.suggestNamesByType(parameterType, { true }, "p")
        return if (suffix != null) suggestions.map { "$it$suffix" } else suggestions
    }

    private fun nameSuggestion(parameterType: CangJieType) = nameSuggestions(parameterType)[0]

    private fun functionParameterTypes(functionType: CangJieType): List<CangJieType> {
        return functionType.getValueParameterTypesFromFunctionType().map(TypeProjection::getType)
    }
}

