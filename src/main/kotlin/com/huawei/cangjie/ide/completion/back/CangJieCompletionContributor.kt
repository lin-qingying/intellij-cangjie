package com.huawei.cangjie.ide.completion.back

import com.huawei.cangjie.analyzer.CjAnalysisSession
import com.huawei.cangjie.analyzer.analyze
import com.huawei.cangjie.ide.completion.back.CangJieCompletionParameters.*
import com.huawei.cangjie.ide.completion.back.CangJieCompletionParameters.Corrected
import com.huawei.cangjie.ide.completion.back.CangJieCompletionParameters.Original
import com.huawei.cangjie.ide.completion.back.CangJieCompletionParametersProvider.provide
import com.huawei.cangjie.ide.completion.back.context.CangJieBasicCompletionContext
import com.huawei.cangjie.ide.completion.back.contributors.CangJieCompletionContributorFactory
import com.huawei.cangjie.ide.completion.back.positionContext.CangJieClassifierNamePositionContext
import com.huawei.cangjie.ide.completion.back.positionContext.CangJiePositionContextDetector
import com.huawei.cangjie.ide.completion.back.positionContext.CangJieRawPositionContext
import com.huawei.cangjie.ide.completion.back.positionContext.CangJieSimpleParameterPositionContext
import com.huawei.cangjie.ide.completion.back.weighers.Weighers
import com.huawei.cangjie.psi.CjDeclaration
import com.intellij.codeInsight.completion.*
import com.intellij.codeInsight.completion.addingPolicy.PolicyController
import com.intellij.patterns.PlatformPatterns.psiElement
import com.intellij.platform.ml.impl.turboComplete.KindCollector
import com.intellij.platform.ml.impl.turboComplete.KindVariety
import com.intellij.platform.ml.impl.turboComplete.SuggestionGeneratorExecutor
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
        action: CjAnalysisSession.() -> Unit
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
//    context(CjAnalysisSession)
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
    context(CjAnalysisSession)
    private fun recordOriginalFile(basicCompletionContext: CangJieBasicCompletionContext) {
        val originalFile = basicCompletionContext.originalCjFile
        val fakeFile = basicCompletionContext.fakeCjFile
        fakeFile.recordOriginalCjFile(originalFile)
    }

    context(CjAnalysisSession)

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
