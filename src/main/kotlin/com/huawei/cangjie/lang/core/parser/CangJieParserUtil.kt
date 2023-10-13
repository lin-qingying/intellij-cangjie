package com.huawei.cangjie.lang.core.parser

import com.huawei.cangjie.lang.core.parser.CangJieParserDefinition.Companion.EOL_COMMENT
import com.huawei.cangjie.lang.core.psi.CjElementTypes.*
import com.huawei.cangjie.stdext.makeBitMask

import com.intellij.lang.LighterASTNode
import com.intellij.lang.PsiBuilder
import com.intellij.lang.PsiBuilderUtil
import com.intellij.lang.WhitespacesAndCommentsBinder
import com.intellij.lang.parser.GeneratedParserUtilBase
import com.intellij.lexer.Lexer
import com.intellij.openapi.util.Key
import com.intellij.psi.TokenType
import com.intellij.psi.tree.IElementType
import com.intellij.psi.tree.TokenSet
import com.intellij.util.BitUtil
import com.intellij.util.containers.Stack
import org.intellij.plugins.relaxNG.compact.RncTokenTypes.GTGT

import kotlin.math.max

@Suppress("UNUSED_PARAMETER")
object CangJieParserUtil : GeneratedParserUtilBase() {

    enum class StmtMode { ON, OFF }



    private val STMT_EXPR_MODE: Int = makeBitMask(3)
    private val STRUCT_ALLOWED: Int = makeBitMask(1)

    private val TYPE_QUAL_ALLOWED: Int = makeBitMask(2)

    @JvmStatic
    fun parseCodeBlockLazy(builder: PsiBuilder, level: Int): Boolean {
        return PsiBuilderUtil.parseBlockLazy(builder, LBRACE, RBRACE, BLOCK) != null
    }
    @JvmStatic
    fun setStmtMode(b: PsiBuilder, level: Int, mode: StmtMode): Boolean {
        b.pushFlag(STMT_EXPR_MODE, mode == StmtMode.ON)
        return true
    }


    @JvmStatic
    fun resetFlags(b: PsiBuilder, level: Int): Boolean {
        b.popFlag()
        return true
    }

    private fun PsiBuilder.popFlag() {
        flags = flagStack.pop()
    }

    private val FLAG_STACK: Key<Stack<Int>> = Key("CangJieParserUtil.FLAG_STACK")
    private var PsiBuilder.flagStack: Stack<Int>
        get() = getUserData(FLAG_STACK) ?: Stack<Int>(0)
        set(value) = putUserData(FLAG_STACK, value)
    private fun PsiBuilder.pushFlag(flag: Int, mode: Boolean) {
        val stack = flagStack
        stack.push(flags)
        flagStack = stack
        flags = BitUtil.set(flags, flag, mode)
    }

    private val DEFAULT_FLAGS: Int = STRUCT_ALLOWED or TYPE_QUAL_ALLOWED
    private val FLAGS: Key<Int> = Key("CangJieParserUtil.FLAGS")
    private var PsiBuilder.flags: Int
        get() = getUserData(FLAGS) ?: DEFAULT_FLAGS
        set(value) = putUserData(FLAGS, value)




    @JvmStatic
    private fun collapse(b: PsiBuilder, tokenType: IElementType, vararg parts: IElementType): Boolean {
//我们不希望分块之间有空格，所以首先对每个分块进行原始查找，
//当我们确保拥有所需的令牌时，我们消费并折叠它。
        parts.forEachIndexed { i, tt ->
            if (b.rawLookup(i) != tt) return false
        }
        val marker = b.mark()
        PsiBuilderUtil.advance(b, parts.size)
        marker.collapse(tokenType)
        return true
    }

    @JvmStatic
    fun gtgtImpl(b: PsiBuilder, level: Int): Boolean = collapse(b, GTGT, GT, GT)
    @JvmStatic
    fun gteqImpl(b: PsiBuilder, level: Int): Boolean = collapse(b, GTEQ, GT, EQ)



    @JvmStatic
    fun ltltImpl(b: PsiBuilder, level: Int): Boolean = collapse(b, LTLT, LT, LT)

    @JvmStatic
    fun lteqImpl(b: PsiBuilder, level: Int): Boolean = collapse(b, LTEQ, LT, EQ)

    @JvmStatic
    fun ororImpl(b: PsiBuilder, level: Int): Boolean = collapse(b, OROR, OR, OR)

    @JvmStatic
    fun andandImpl(b: PsiBuilder, level: Int): Boolean = collapse(b, ANDAND, AND, AND)

    @JvmField
    val ADJACENT_LINE_COMMENTS = WhitespacesAndCommentsBinder { tokens, _, getter ->
        var candidate = tokens.size
        for (i in 0 until tokens.size) {
            val token = tokens[i]
            if (EOL_COMMENT == token) {
                candidate = minOf(candidate, i)
            }
            if (TokenType.WHITE_SPACE == token && "\n\n" in getter[i]) {
                candidate = tokens.size
            }

            println(token)
        }
        candidate
    }


}
