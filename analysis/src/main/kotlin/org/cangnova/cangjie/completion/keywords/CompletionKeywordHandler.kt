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

package org.cangnova.cangjie.completion.keywords

import org.cangnova.cangjie.lexer.CjKeywordToken
import org.cangnova.cangjie.psi.CjExpression
import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.openapi.project.Project

abstract class CompletionKeywordHandler<CONTEXT>(
    val keyword: CjKeywordToken
) {
    object NO_CONTEXT

    context(c:CONTEXT)
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
    context(c:CONTEXT)
    override fun createLookups(
        parameters: CompletionParameters,
        expression: CjExpression?,
        lookup: LookupElement,
        project: Project
    ): Collection<LookupElement> = create(c, parameters, expression, lookup, project)
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
