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

import org.cangnova.cangjie.completion.handlers.createKeywordConstructLookupElement
import org.cangnova.cangjie.completion.handlers.withLineIndentAdjuster
import org.cangnova.cangjie.lexer.CjTokens
import org.cangnova.cangjie.psi.psiUtil.prevLeaf
import com.intellij.codeInsight.completion.CompletionParameters

class CompletionKeywordHandlers<CONTEXT>(vararg handlers: CompletionKeywordHandler<CONTEXT>) {
    private val handlerByKeyword = handlers.associateBy { it.keyword.value }

    internal fun getHandlerForKeyword(keyword: String): CompletionKeywordHandler<CONTEXT>? =
        handlerByKeyword[keyword]
}
abstract class CompletionKeywordHandlerProvider<CONTEXT> {
    protected abstract val handlers: CompletionKeywordHandlers<CONTEXT>

    fun getHandlerForKeyword(keyword: String): CompletionKeywordHandler<CONTEXT>? =
        handlers.getHandlerForKeyword(keyword)
}

object DefaultCompletionKeywordHandlerProvider : CompletionKeywordHandlerProvider<CompletionKeywordHandler.NO_CONTEXT>() {
//    private val CONTRACT_HANDLER = completionKeywordHandler<CompletionKeywordHandler.NO_CONTEXT>(CjTokens.CONTRACT_KEYWORD) { _, _, _, _ ->
//        emptyList()
//    }

    private val GETTER_HANDLER =
        completionKeywordHandler<CompletionKeywordHandler.NO_CONTEXT>(CjTokens.GET_KEYWORD) { parameters, _, lookupElement, project ->
            buildList {
                add(lookupElement.withLineIndentAdjuster())
                if (!parameters.isUseSiteAnnotationTarget) {

                    add(
                        createKeywordConstructLookupElement(
                            project,
                            CjTokens.GET_KEYWORD.value,
                            "get(){caret}",
                            trimSpacesAroundCaret = true,
                            adjustLineIndent = true,
                        )
                    )
                }
            }
        }

    private val SETTER_HANDLER =
        completionKeywordHandler<CompletionKeywordHandler.NO_CONTEXT>(CjTokens.SET_KEYWORD) { parameters, _, lookupElement, project ->
            buildList {
                add(lookupElement.withLineIndentAdjuster())
                if (!parameters.isUseSiteAnnotationTarget) {

                    add(
                        createKeywordConstructLookupElement(
                            project,
                            CjTokens.SET_KEYWORD.value,
                            " set(value){caret} ",
                            trimSpacesAroundCaret = true,
                            adjustLineIndent = true,
                        )
                    )
                }
            }
        }

    override val handlers = CompletionKeywordHandlers(
        GETTER_HANDLER, SETTER_HANDLER,
//        CONTRACT_HANDLER,
    )
}

private val CompletionParameters.isUseSiteAnnotationTarget
    get() = position.prevLeaf()?.node?.elementType == CjTokens.AT
