package com.huawei.cangjie.ide.completion.keywords

import com.huawei.cangjie.lexer.CjKeywordToken
import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.openapi.project.Project
//
//class BreakContinueKeywordHandler(keyword: CjKeywordToken) : CompletionKeywordHandler<CjAnalysisSession>(keyword) {
//    init {
//        check(keyword == CjTokens.BREAK_KEYWORD || keyword == CjTokens.CONTINUE_KEYWORD) {
//            "Keyword should be either `break` or `continue`. But was: $keyword"
//        }
//    }
//
//    context(CjAnalysisSession)
//    fun createLookups(expression: CjExpression?): Collection<LookupElement> {
//        if (expression == null) return emptyList()
//        val supportsNonLocalBreakContinue =
//            expression.languageVersionSettings.supportsFeature(LanguageFeature.BreakContinueInInlineLambdas)
//        return expression.parentsWithSelf
//            .takeWhile { it !is CjDeclarationWithBody || canDoNonLocalJump(it, supportsNonLocalBreakContinue) }
//            .filterIsInstance<CjLoopExpression>()
//            .flatMapIndexed { index: Int, loop: CjLoopExpression ->
//                listOfNotNull(
//                    if (index == 0) createKeywordElement(keyword.value) else null,
//                    (loop.parent as? CjLabeledExpression)?.getLabelNameAsName()?.let { label ->
//                        createKeywordElement(keyword.value, tail = label.labelNameToTail())
//                    }
//                )
//            }
//            .toList()
//    }
//    context(CjAnalysisSession)
//    private fun canDoNonLocalJump(
//        body: CjDeclarationWithBody,
//        supportsNonLocalBreakContinue: Boolean,
//    ) = supportsNonLocalBreakContinue &&
//            body is CjFunctionLiteral &&
//            isInlineFunctionCall(body.findLabelAndCall().second)
//
//    context(CjAnalysisSession)
//    override fun createLookups(
//        parameters: CompletionParameters,
//        expression: CjExpression?,
//        lookup: LookupElement,
//        project: Project
//    ): Collection<LookupElement> = createLookups(expression)
//}
//
//context(CjAnalysisSession)
//fun isInlineFunctionCall(call: CjCallExpression?): Boolean =
//    (call?.calleeExpression as? CjReferenceExpression)?.mainReference
//        ?.resolveToSymbol()
//        ?.let { it as? CjFunctionSymbol }
//        ?.isInline == true
