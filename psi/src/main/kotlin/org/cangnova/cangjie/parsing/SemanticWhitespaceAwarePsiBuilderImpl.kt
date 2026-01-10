/*
 * Copyright 2026 LinQingYing. and contributors.
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

package org.cangnova.cangjie.parsing

import org.cangnova.cangjie.lexer.CjTokens
import com.intellij.lang.PsiBuilder
import com.intellij.lang.impl.PsiBuilderAdapter
import com.intellij.lang.impl.PsiBuilderImpl
import com.intellij.psi.TokenType
import com.intellij.psi.tree.IElementType
import com.intellij.util.containers.Stack

class SemanticWhitespaceAwarePsiBuilderImpl(delegate: PsiBuilder) :
    PsiBuilderAdapter(
        delegate,
    ),
    SemanticWhitespaceAwarePsiBuilder {

    private val joinComplexTokens = Stack<Boolean>()
    private val newlinesEnabled = Stack<Boolean>()
    private val delegateImpl: PsiBuilderImpl?

    init {
        newlinesEnabled.push(true)
        joinComplexTokens.push(true)
        delegateImpl =
            findPsiBuilderImpl(delegate)
    }

    override fun isWhitespaceOrComment(elementType: IElementType): Boolean {
        assert(delegateImpl != null) { "PsiBuilderImpl not found" }
        return delegateImpl!!.whitespaceOrComment(elementType)
    }

    override fun newlineBeforeCurrentToken(): Boolean {
        if (!newlinesEnabled.peek()) return false

        if (eof()) return true

        for (i in 1..currentOffset) {
            val previousToken = rawLookup(-i)
            if (previousToken === CjTokens.BLOCK_COMMENT || previousToken === CjTokens.DOC_COMMENT || previousToken === CjTokens.EOL_COMMENT || previousToken === CjTokens.SHEBANG_COMMENT) {
                continue
            }
            if (previousToken !== TokenType.WHITE_SPACE) {
                break
            }
            val previousTokenStart = rawTokenTypeStart(-i)
            val previousTokenEnd = rawTokenTypeStart(-i + 1)
            assert(previousTokenStart >= 0)
            assert(previousTokenEnd < originalText.length)
            for (j in previousTokenStart until previousTokenEnd) {
                if (originalText[j] == '\n') {
                    return true
                }
            }
        }
        return false
    }

    override fun disableNewlines() {
        newlinesEnabled.push(false)
    }

    override fun enableNewlines() {
        newlinesEnabled.push(true)
    }

    override fun restoreNewlinesState() {
        assert(newlinesEnabled.size > 1)
        newlinesEnabled.pop()
    }

    private fun joinComplexTokens(): Boolean {
        return joinComplexTokens.peek()
    }

    override fun restoreJoiningComplexTokensState() {
        joinComplexTokens.pop()
    }

    override fun enableJoiningComplexTokens() {
        joinComplexTokens.push(true)
    }

    override fun disableJoiningComplexTokens() {
        joinComplexTokens.push(false)
    }

    override fun getTokenType(): IElementType? {
        return if (!joinComplexTokens()) super.getTokenType() else getJoinedTokenType(super.getTokenType(), 1)
    }

    private fun getJoinedTokenType(rawTokenType: IElementType?, rawLookupSteps: Int): IElementType? {
//        if (rawTokenType === CjTokens.QUEST) {
//            val nextRawToken = rawLookup(rawLookupSteps)
//
//        } else if (rawTokenType === CjTokens.EXCL) {
//            val nextRawToken = rawLookup(rawLookupSteps)
//
//        }
        return rawTokenType
    }

    override fun advanceLexer() {
        if (!joinComplexTokens()) {
            super.advanceLexer()
            return
        }
        val tokenType = tokenType

        super.advanceLexer()
    }

    override fun getTokenText(): String? {
        if (!joinComplexTokens()) return super.getTokenText()

        return super.getTokenText()
    }

    override fun lookAhead(steps: Int): IElementType? {
        if (!joinComplexTokens()) return super.lookAhead(steps)
        return getJoinedTokenType(super.lookAhead(steps), 2)
    }

    companion object {
        private fun findPsiBuilderImpl(builder: PsiBuilder): PsiBuilderImpl? {
            var builder: PsiBuilder? = builder
            while (true) {
                if (builder is PsiBuilderImpl) {
                    return builder
                }
                if (builder !is PsiBuilderAdapter) {
                    return null
                }
                builder = builder.delegate
            }
        }
    }
}
