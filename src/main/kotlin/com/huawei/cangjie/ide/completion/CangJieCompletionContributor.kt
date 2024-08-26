package com.huawei.cangjie.ide.completion

import com.huawei.cangjie.lexer.CjTokens
import com.huawei.cangjie.psi.CjFile
import com.huawei.cangjie.psi.CjNameReferenceExpression
import com.huawei.cangjie.psi.psiUtil.getNonStrictParentOfType
import com.intellij.codeInsight.completion.*
import com.intellij.codeInsight.completion.addingPolicy.PolicyController
import com.intellij.codeInsight.completion.impl.CamelHumpMatcher
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.openapi.util.ThrowableComputable
import com.intellij.openapi.util.registry.Registry
import com.intellij.patterns.PlatformPatterns.elementType
import com.intellij.patterns.PlatformPatterns.psiElement
import com.intellij.platform.ml.impl.turboComplete.KindCollector
import com.intellij.platform.ml.impl.turboComplete.KindVariety
import com.intellij.platform.ml.impl.turboComplete.SuggestionGeneratorExecutor
import com.intellij.psi.PsiComment
import com.intellij.util.indexing.DumbModeAccessType


abstract class CangJieKindExecutingCompletionContributor : CompletionContributor(), KindCollector

class CangJieCompletionContributor : CangJieKindExecutingCompletionContributor() {


//    init {
//        extend(CompletionType.BASIC, psiElement(),
//
//            object : CompletionProvider<CompletionParameters>() {
//                override fun addCompletions(
//                    parameters: CompletionParameters,
//                    context: ProcessingContext,
//                    result: CompletionResultSet
//                ) {
//
//                    CjTokens.KEYWORDALL.types.forEach {
//                        result.addElement(LookupElementBuilder.create(it.toString()))
//
//                    }
//                }
//
//            })
//    }

    override val kindVariety: KindVariety = CangJieKindVariety
    private val AFTER_NUMBER_LITERAL = psiElement().afterLeafSkipping(
        psiElement().withText(""),
        psiElement().withElementType(elementType().oneOf(CjTokens.FLOAT_LITERAL, CjTokens.INTEGER_LITERAL))
    )
    private val AFTER_INTEGER_LITERAL_AND_DOT = psiElement().afterLeafSkipping(
        psiElement().withText("."),
        psiElement().withElementType(elementType().oneOf(CjTokens.INTEGER_LITERAL))
    )

    override fun collectKinds(
        parameters: CompletionParameters,
        generatorExecutor: SuggestionGeneratorExecutor,
        result: CompletionResultSet
    ) {
//        StringTemplateCompletion.correctParametersForInStringTemplateCompletion(parameters)?.let { correctedParameters ->
////            generateCompletionKinds(correctedParameters, generatorExecutor, result, ::wrapLookupElementForStringTemplateAfterDotCompletion)
//            return
//        }

        DumbModeAccessType.RELIABLE_DATA_ONLY.ignoreDumbMode(ThrowableComputable {
            generateCompletionKinds(parameters, generatorExecutor, result, null)
        })
    }

    override fun shouldBeCalled(parameters: CompletionParameters): Boolean {
        val position = parameters.position
        val parametersOriginFile = parameters.originalFile
        return position.containingFile is CjFile && parametersOriginFile is CjFile
    }

    private fun shouldSuppressCompletion(parameters: CompletionParameters, prefixMatcher: PrefixMatcher): Boolean {
        val position = parameters.position
        val invocationCount = parameters.invocationCount

        if (prefixMatcher is CamelHumpMatcher && prefixMatcher.isTypoTolerant) return true

        // no completion inside number literals
        if (AFTER_NUMBER_LITERAL.accepts(position)) return true

        // no completion auto-popup after integer and dot
        if (invocationCount == 0 && prefixMatcher.prefix.isEmpty() && AFTER_INTEGER_LITERAL_AND_DOT.accepts(position)) return true

        if (invocationCount == 0 && Registry.`is`("cangjie.disable.auto.completion.inside.expression", false)) {
            val originalPosition = parameters.originalPosition
            val originalExpression = originalPosition?.getNonStrictParentOfType<CjNameReferenceExpression>()
            val expression = position.getNonStrictParentOfType<CjNameReferenceExpression>()

            if (expression != null && originalExpression != null &&
                !expression.getReferencedName().startsWith(originalExpression.getReferencedName())
            ) {
                return true
            }
        }

        return false
    }

    private fun generateCompletionKinds(
        parameters: CompletionParameters,
        suggestionGeneratorExecutor: SuggestionGeneratorExecutor,
        result: CompletionResultSet,
        lookupElementPostProcessor: ((LookupElement) -> LookupElement)?
    ) {
        val position = parameters.position
        if (position.getNonStrictParentOfType<PsiComment>() != null) {
            // don't stop here, allow other contributors to run
            return
        }

        if (shouldSuppressCompletion(parameters, result.prefixMatcher)) {
            result.stopHere()
            return
        }
//
//        if (PackageDirectiveCompletion.perform(parameters, result)) {
//            result.stopHere()
//            return
//        }

        fun addPostProcessor(session: CompletionSession) {
            if (lookupElementPostProcessor != null) {
                session.addLookupElementPostProcessor(lookupElementPostProcessor)
            }
        }

        result.restartCompletionWhenNothingMatches()

        val resultPolicyController = PolicyController(result)

        val configuration = CompletionSessionConfiguration(parameters)
        if (parameters.completionType == CompletionType.BASIC) {
            val session =
                BasicCompletionSession(configuration, parameters, resultPolicyController, suggestionGeneratorExecutor)
            addPostProcessor(session)

            if (parameters.isAutoPopup && session.shouldDisableAutoPopup()) {
                result.stopHere()
                return
            }

            session.complete()
            suggestionGeneratorExecutor.executeAll()

            if (session.isNothingAddedToResult && parameters.invocationCount < 2) {
                // Rerun completion if nothing was found
                val newConfiguration = CompletionSessionConfiguration(
//                    useBetterPrefixMatcherForNonImportedClasses = false,
//                    nonAccessibleDeclarations = false,
//                    javaGettersAndSetters = true,
//                    javaClassesNotToBeUsed = false,
                    staticMembers = parameters.invocationCount > 0,
//                    dataClassComponentFunctions = true,
//                    excludeEnumEntries = configuration.excludeEnumEntries,
                )

                val newSession = BasicCompletionSession(
                    newConfiguration, parameters, resultPolicyController, suggestionGeneratorExecutor
                )

                addPostProcessor(newSession)
                newSession.complete()
            }
        } else {
            val session = SmartCompletionSession(configuration, parameters, result)
            addPostProcessor(session)
            session.complete()
        }

        println()
    }

}
