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

package com.linqingying.cangjie.ide.search

import com.google.common.collect.ImmutableSet
import com.linqingying.cangjie.doc.lexer.CDocTokens
import com.linqingying.cangjie.lexer.CangJieLexer
import com.linqingying.cangjie.lexer.CjToken
import com.linqingying.cangjie.lexer.CjTokens
import com.intellij.lexer.Lexer
import com.intellij.psi.TokenType
import com.intellij.psi.impl.cache.impl.BaseFilterLexer
import com.intellij.psi.impl.cache.impl.IdAndToDoScannerBasedOnFilterLexer
import com.intellij.psi.impl.cache.impl.OccurrenceConsumer
import com.intellij.psi.impl.cache.impl.id.LexerBasedIdIndexer
import com.intellij.psi.impl.cache.impl.todo.LexerBasedTodoIndexer
import com.intellij.psi.search.UsageSearchContext
import com.intellij.psi.tree.IElementType
import com.intellij.psi.tree.TokenSet
import java.util.*

const val CANGJIE_NAMED_ARGUMENT_SEARCH_CONTEXT: Short = 0x20

private val ALL_SEARCHABLE_OPERATIONS: ImmutableSet<CjToken> = ImmutableSet
    .builder<CjToken>()

    .add(CjTokens.LBRACKET)

    .build()

class CangJieFilterLexer(private val occurrenceConsumer: OccurrenceConsumer) :
    BaseFilterLexer(CangJieLexer(), occurrenceConsumer) {
    private companion object {
        private val CODE_TOKENS = TokenSet.orSet(
            TokenSet.create(*ALL_SEARCHABLE_OPERATIONS.toTypedArray()),
            TokenSet.create(CjTokens.IDENTIFIER)
        )

        private val COMMENT_TOKENS = TokenSet.orSet(CjTokens.COMMENTS, TokenSet.create(CDocTokens.CDOC))
        private const val MAX_PREV_TOKENS = 2
    }

    private val prevTokens = ArrayDeque<IElementType>(MAX_PREV_TOKENS)

    private var prevTokenStart = -1
    private var prevTokenEnd = -1

    override fun advance() {
        val tokenType = myDelegate.tokenType

        when (tokenType) {
            CjTokens.EQ -> {
                if (prevTokens.peekFirst() == CjTokens.IDENTIFIER) {
                    val prevPrev = prevTokens.elementAtOrNull(1)
                    if (prevPrev == CjTokens.COMMA || prevPrev == CjTokens.LPAR) {
                        occurrenceConsumer.addOccurrence(
                            bufferSequence,
                            null,
                            prevTokenStart,
                            prevTokenEnd,
                            CANGJIE_NAMED_ARGUMENT_SEARCH_CONTEXT.toInt()
                        )
                    }
                }
            }

            CjTokens.LPAR -> {
                if (isMultiDeclarationPosition()) {
                    addOccurrenceInToken(UsageSearchContext.IN_CODE.toInt())
                }
            }

            CjTokens.IDENTIFIER -> {
                if (myDelegate.tokenText.startsWith("`")) {
                    scanWordsInToken(UsageSearchContext.IN_CODE.toInt(), false, false)
                } else {
                    addOccurrenceInToken(UsageSearchContext.IN_CODE.toInt())
                    if (myDelegate.tokenText == "TODO") {
                        // Heuristics to reduce mismatches between indexer and searcher. The searcher returns only occurrences of TO_DO
                        // as the callee of a call expression, but we can't tell calls and other usages apart based on limited lexer context,
                        // so we just exclude occurrences in declaration names (and even that doesn't work precisely because it doesn't handle
                        // declarations with type parameters)
                        val prevToken = prevTokens.peekFirst()
                        if (prevToken != CjTokens.FUNC_KEYWORD && prevToken != CjTokens.VAR_KEYWORD && prevToken != CjTokens.LET_KEYWORD && prevToken != CjTokens.CLASS_KEYWORD) {
                            advanceTodoItemCountsInToken()
                        }
                    }
                }
            }

            in CODE_TOKENS -> addOccurrenceInToken(UsageSearchContext.IN_CODE.toInt())

//            in CjTokens.STRINGS -> scanWordsInToken(
//                UsageSearchContext.IN_STRINGS + UsageSearchContext.IN_FOREIGN_LANGUAGES,
//                false,
//                true
//            )

            in COMMENT_TOKENS -> {
                scanWordsInToken(UsageSearchContext.IN_COMMENTS.toInt(), false, false)
                advanceTodoItemCountsInToken()
            }
        }

        if (tokenType != TokenType.WHITE_SPACE && tokenType !in COMMENT_TOKENS) {
            if (prevTokens.size == MAX_PREV_TOKENS) {
                prevTokens.removeLast()
            }
            prevTokens.addFirst(tokenType)
            prevTokenStart = tokenStart
            prevTokenEnd = tokenEnd
        }

        myDelegate.advance()
    }

    private fun isMultiDeclarationPosition(): Boolean {
        val first = prevTokens.peekFirst()
        if (first == CjTokens.LET_KEYWORD || first == CjTokens.VAR_KEYWORD) return true
        return first == CjTokens.LPAR && prevTokens.elementAtOrNull(1) == CjTokens.FOR_KEYWORD
    }
}

class CangJieTodoIndexer : LexerBasedTodoIndexer(), IdAndToDoScannerBasedOnFilterLexer {
    override fun getVersion() = 2

    override fun createLexer(consumer: OccurrenceConsumer) = CangJieFilterLexer(consumer)
}


class CangJieIdIndexer : LexerBasedIdIndexer() {
    override fun createLexer(consumer: OccurrenceConsumer): Lexer = CangJieFilterLexer(consumer)

    override fun getVersion() = 3
}
