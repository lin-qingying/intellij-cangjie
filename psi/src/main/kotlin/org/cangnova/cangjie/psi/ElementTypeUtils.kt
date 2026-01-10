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

package org.cangnova.cangjie.psi

import org.cangnova.cangjie.psi.CjNodeTypes.DOT_QUALIFIED_EXPRESSION
import org.cangnova.cangjie.psi.CjNodeTypes.FUNC
import org.cangnova.cangjie.psi.CjNodeTypes.REFERENCE_EXPRESSION
import org.cangnova.cangjie.lexer.CangJieLexer
import org.cangnova.cangjie.lexer.CjSingleValueToken
import org.cangnova.cangjie.lexer.CjTokens
import org.cangnova.cangjie.parsing.CangJieExpressionParsing
import com.intellij.lang.LighterASTNode
import com.intellij.psi.tree.IElementType
import com.intellij.psi.tree.IErrorCounterReparseableElementType

object ElementTypeUtils {
    @JvmStatic
    fun getCangJieBlockImbalanceCount(seq: CharSequence): Int {
        val lexer = CangJieLexer()

        lexer.start(seq)
        if (lexer.tokenType !== CjTokens.LBRACE) return IErrorCounterReparseableElementType.FATAL_ERROR
        lexer.advance()
        var balance = 1
        while (lexer.tokenType != CjTokens.EOF) {
            val type = lexer.tokenType ?: break
            if (balance == 0) {
                return IErrorCounterReparseableElementType.FATAL_ERROR
            }
            if (type === CjTokens.LBRACE) {
                balance++
            } else if (type === CjTokens.RBRACE) {
                balance--
            }
            lexer.advance()
        }
        return balance
    }

    fun String.getOperationSymbol(): IElementType {
        CangJieExpressionParsing.ALL_OPERATIONS?.types?.forEach {
            if (it is CjSingleValueToken && it.value == this) return it
        }
//        if (this == "as?") return CjTokens.AS_SAFE
        return CjTokens.IDENTIFIER
    }

    private val expressionSet = listOf(
        REFERENCE_EXPRESSION,
        DOT_QUALIFIED_EXPRESSION,

        FUNC,
    )

    fun LighterASTNode.isExpression(): Boolean {
        return when (this.tokenType) {
            is CjNodeType,

            in expressionSet,
            -> true
            else -> false
        }
    }
}
