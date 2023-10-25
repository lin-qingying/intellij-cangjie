package com.huawei.cangjie

import com.huawei.cangjie.lexer.CangJieLexer
import com.huawei.cangjie.lexer.CjSingleValueToken
import com.huawei.cangjie.lexer.CjTokens
import com.huawei.cangjie.parsing.CangJieExpressionParsing

import com.intellij.lang.LighterASTNode
import com.intellij.psi.tree.IElementType
import com.intellij.psi.tree.IErrorCounterReparseableElementType


import com.huawei.cangjie.CjNodeTypes.*
import com.huawei.cangjie.psi.stubs.elements.CjStubElementTypes.DOT_QUALIFIED_EXPRESSION
import com.huawei.cangjie.psi.stubs.elements.CjStubElementTypes.REFERENCE_EXPRESSION

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

        FUNC
    )

    fun LighterASTNode.isExpression(): Boolean {
        return when (this.tokenType) {
            is CjNodeType,

            in expressionSet -> true
            else -> false
        }
    }
}
