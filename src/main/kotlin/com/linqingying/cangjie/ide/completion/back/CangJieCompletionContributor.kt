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

package com.linqingying.cangjie.ide.completion.back

import com.linqingying.cangjie.analyzer.CangJieAnalysisSession
import com.linqingying.cangjie.analyzer.analyze
import com.linqingying.cangjie.ide.completion.back.CangJieCompletionParameters.*
import com.linqingying.cangjie.ide.completion.back.CangJieCompletionParameters.Corrected
import com.linqingying.cangjie.ide.completion.back.CangJieCompletionParameters.Original
import com.linqingying.cangjie.ide.completion.back.CangJieCompletionParametersProvider.provide
import com.linqingying.cangjie.ide.completion.back.context.CangJieBasicCompletionContext
import com.linqingying.cangjie.ide.completion.back.positionContext.CangJieClassifierNamePositionContext
import com.linqingying.cangjie.ide.completion.back.positionContext.CangJiePositionContextDetector
import com.linqingying.cangjie.ide.completion.back.positionContext.CangJieRawPositionContext
import com.linqingying.cangjie.ide.completion.back.positionContext.CangJieSimpleParameterPositionContext
import com.linqingying.cangjie.ide.completion.back.weighers.Weighers
import com.linqingying.cangjie.psi.CjDeclaration
import com.intellij.codeInsight.completion.*
import com.intellij.codeInsight.completion.addingPolicy.PolicyController
import com.intellij.patterns.PlatformPatterns.psiElement
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.ProcessingContext

class CangJieCompletionContributor : CompletionContributor() {

    init {
        extend(CompletionType.BASIC, psiElement(), CangJieCompletionProvider)

    }
}

private object CangJieCompletionProvider : CompletionProvider<CompletionParameters>() {
    //    private val AFTER_NUMBER_LITERAL = CangJiePsiPatterns.psiElement().afterLeafSkipping(
//        CangJiePsiPatterns.psiElement().withText(""),
//        CangJiePsiPatterns.psiElement().withElementType(CangJiePsiPatterns.elementType().oneOf(CjTokens.FLOAT_LITERAL, CjTokens.INTEGER_LITERAL))
//    )
    private fun shouldSuppressCompletion(parameters: CompletionParameters, prefixMatcher: PrefixMatcher): Boolean {
//        val position = parameters.position
//        val invocationCount = parameters.invocationCount

        // no completion inside number literals
//        if (AFTER_NUMBER_LITERAL.accepts(position)) return true

        // no completion auto-popup after integer and dot
//        if (invocationCount == 0 && prefixMatcher.prefix.isEmpty() && AFTER_INTEGER_LITERAL_AND_DOT.accepts(position)) return true

        return false
    }
    private fun createSorter(
        parameters: CompletionParameters,
        positionContext: CangJieRawPositionContext,
        result: CompletionResultSet
    ): CompletionSorter = CompletionSorter.defaultSorter(parameters, result.prefixMatcher)
        .let { Weighers.addWeighersToCompletionSorter(it, positionContext) }

    private fun createResultSet(
        parameters: CangJieCompletionParameters,
        positionContext: CangJieRawPositionContext,
        result: CompletionResultSet
    ): Pair<PolicyController, CompletionResultSet> {
        val prefix = CompletionUtil.findIdentifierPrefix(
            parameters.ijParameters.position.containingFile,
            parameters.ijParameters.offset,
            cangjieIdentifierPartPattern(),
            cangjieIdentifierPartPattern()
        )
        val resultWithSorter =
            result.withRelevanceSorter(
                createSorter(
                    parameters.ijParameters,
                    positionContext,
                    result
                )
            )
                .withPrefixMatcher(prefix)
        val controller = PolicyController(resultWithSorter)
        val obeyingResultSet = PolicyObeyingResultSet(resultWithSorter, controller)
        return controller to obeyingResultSet
    }

    private inline fun analyzeInContext(
        basicContext: CangJieBasicCompletionContext,
        positionContext: CangJieRawPositionContext,
        action: CangJieAnalysisSession.() -> Unit
    ) {
        analyze(basicContext.fakeCjFile) {
            when (positionContext) {
                is CangJieSimpleParameterPositionContext -> recordOriginalDeclaration(
                    basicContext,
                    positionContext.cjParameter
                )

                is CangJieClassifierNamePositionContext -> recordOriginalDeclaration(
                    basicContext,
                    positionContext.classLikeDeclaration
                )

                else -> {}
            }

            action()
        }
    }

    override fun addCompletions(
        parameters: CompletionParameters,
        context: ProcessingContext,
        result: CompletionResultSet
    ) {
        @Suppress("NAME_SHADOWING") val parameters =
            provide(
                parameters
            )


        if (shouldSuppressCompletion(
                parameters.ijParameters,
                result.prefixMatcher
            )
        ) return
        val positionContext = CangJiePositionContextDetector.detect(parameters.ijParameters.position)
        val (resultController, resultSet) = createResultSet(
            parameters,
            positionContext,
            result
        )
//
        val basicContext = CangJieBasicCompletionContext.createFromParameters(parameters, resultSet) ?: return

        analyzeInContext(
            basicContext,
            positionContext
        ) {
            recordOriginalFile(
                basicContext
            )
//            complete(basicContext, positionContext, resultController)
        }
    }
//    context(CangJieAnalysisSession)
//    private fun complete(
//        basicContext: CangJieBasicCompletionContext,
//        positionContext:CangJieRawPositionContext,
//        resultController: PolicyController,
//    ) {
//        val factory = CangJieCompletionContributorFactory(basicContext, resultController)
//        with(Completions) {
//            val weighingContext = createWeighingContext(basicContext, positionContext)
//            val sessionParameters = CangJieCompletionSessionParameters(basicContext, positionContext)
//            complete(factory, positionContext, weighingContext, sessionParameters)
//        }
//    }
    context(CangJieAnalysisSession)
    private fun recordOriginalFile(basicCompletionContext: CangJieBasicCompletionContext) {
        val originalFile = basicCompletionContext.originalCjFile
        val fakeFile = basicCompletionContext.fakeCjFile
        fakeFile.recordOriginalCjFile(originalFile)
    }

    context(CangJieAnalysisSession)

    private fun recordOriginalDeclaration(basicContext: CangJieBasicCompletionContext, declaration: CjDeclaration) {
        try {
            declaration.recordOriginalDeclaration(
                PsiTreeUtil.findSameElementInCopy(
                    declaration,
                    basicContext.originalCjFile
                )
            )
        } catch (ignore: IllegalStateException) {
            //declaration is written at empty space
        }
    }
}

internal object CangJieCompletionParametersProvider {
    fun provide(parameters: CompletionParameters): CangJieCompletionParameters {
        val (corrected, type) = correctParameters(
            parameters
        ) ?: return Original(
            parameters
        )
        return Corrected(
            corrected,
            parameters,
            type
        )
    }

    private fun correctParameters(parameters: CompletionParameters): Pair<CompletionParameters, CorrectionType>? {
//        val correctParametersForInStringTemplateCompletion =
//            StringTemplateCompletion.correctParametersForInStringTemplateCompletion(parameters)
//                ?: return null
//        return correctParametersForInStringTemplateCompletion to CangJieCompletionParameters.CorrectionType.BRACES_FOR_STRING_TEMPLATE
        return null
    }
}

sealed class CangJieCompletionParameters {
    abstract val ijParameters: CompletionParameters
    abstract val type: CorrectionType?

    internal class Original(
        override val ijParameters: CompletionParameters,
    ) : CangJieCompletionParameters() {
        override val type: CorrectionType? get() = null
    }

    internal class Corrected(
        override val ijParameters: CompletionParameters,
        val original: CompletionParameters,
        override val type: CorrectionType,
    ) : CangJieCompletionParameters()

    enum class CorrectionType {
        BRACES_FOR_STRING_TEMPLATE
    }
}

//
//object CangJiePsiPatterns : StandardPatterns()
//{
//    fun psiElement(): PsiJavaElementPattern.Capture<PsiElement> {
//        return Capture(PsiElement::class.java)
//    }
//}
