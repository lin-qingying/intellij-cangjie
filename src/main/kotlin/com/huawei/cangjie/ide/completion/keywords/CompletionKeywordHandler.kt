package com.huawei.cangjie.ide.completion.keywords

import com.huawei.cangjie.lexer.CjKeywordToken
import com.huawei.cangjie.psi.CjExpression
import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.openapi.project.Project

abstract class CompletionKeywordHandler<CONTEXT>(
    val keyword: CjKeywordToken
) {
    object NO_CONTEXT

    context(CONTEXT)
    abstract fun createLookups(
        parameters: CompletionParameters,
        expression: CjExpression?,
        lookup: LookupElement,
        project: Project
    ): Collection<LookupElement>
}
inline fun <CONTEXT> completionKeywordHandler(
    keyword: CjKeywordToken,
    crossinline create: context(CONTEXT)(
        parameters: CompletionParameters,
        expression: CjExpression?,
        lookup: LookupElement,
        project: Project
    ) -> Collection<LookupElement>
) = object : CompletionKeywordHandler<CONTEXT>(keyword) {
    context(CONTEXT)
    override fun createLookups(
        parameters: CompletionParameters,
        expression: CjExpression?,
        lookup: LookupElement,
        project: Project
    ): Collection<LookupElement> = create(this@CONTEXT, parameters, expression, lookup, project)
}
/**
 * Create a list of [LookupElement] for [CompletionKeywordHandler] which has no context
 *
 * This function is needed to avoid writing `with(CompletionKeywordHandler.NO_CONTEXT) { ... }` to create such lookups
 */
fun CompletionKeywordHandler<CompletionKeywordHandler.NO_CONTEXT>.createLookups(
    parameters: CompletionParameters,
    expression: CjExpression?,
    lookup: LookupElement,
    project: Project
): Collection<LookupElement> = with(CompletionKeywordHandler.NO_CONTEXT) { createLookups(parameters, expression, lookup, project) }
