package com.huawei.cangjie.lang.core.psi


import com.huawei.cangjie.lang.CjLanguage
import com.huawei.cangjie.lang.core.parser.CangJieParserDefinition.Companion.BLOCK_COMMENT
import com.huawei.cangjie.lang.core.parser.CangJieParserDefinition.Companion.EOL_COMMENT
import com.huawei.cangjie.lang.core.parser.CangJieParserDefinition.Companion.INNER_BLOCK_DOC_COMMENT
import com.huawei.cangjie.lang.core.parser.CangJieParserDefinition.Companion.INNER_EOL_DOC_COMMENT
import com.huawei.cangjie.lang.core.parser.CangJieParserDefinition.Companion.OUTER_BLOCK_DOC_COMMENT
import com.huawei.cangjie.lang.core.parser.CangJieParserDefinition.Companion.OUTER_EOL_DOC_COMMENT
import com.huawei.cangjie.lang.core.psi.CjElementTypes.*
import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.impl.source.tree.LeafPsiElement

import com.intellij.psi.tree.IElementType
import com.intellij.psi.tree.IReparseableLeafElementType
import com.intellij.psi.tree.TokenSet


open class CjTokenType(debugName: String) : IElementType(debugName, CjLanguage)

fun tokenSetOf(vararg tokens: IElementType) = TokenSet.create(*tokens)

val CJ_REGULAR_COMMENTS = tokenSetOf(BLOCK_COMMENT, EOL_COMMENT)


val CJ_INNER_DOC_COMMENTS = tokenSetOf(INNER_BLOCK_DOC_COMMENT, INNER_EOL_DOC_COMMENT)


val CJ_OUTER_DOC_COMMENTS = tokenSetOf(OUTER_BLOCK_DOC_COMMENT, OUTER_EOL_DOC_COMMENT)


val CJ_DOC_COMMENTS = TokenSet.orSet(CJ_INNER_DOC_COMMENTS, CJ_OUTER_DOC_COMMENTS)



val CJ_COMMENTS = TokenSet.orSet(CJ_REGULAR_COMMENTS, CJ_DOC_COMMENTS)


//语法项
val CJ_ITEMS = tokenSetOf(

    FUNCTION,


    )

//关键字
val CJ_KEYWORDS = tokenSetOf(
    AS, FUNC

)

//保留关键字
val CJSERVED_KEYWORDS: Set<String> = setOf(

)


//val CJ_OPERATOCJ = tokenSetOf(
//    AND
//)

//interface PsiEolWs : PsiElement
//class EolWsTokenType : IElementType("EOL_WS",CjLanguage) , IReparseableLeafElementType<ASTNode>
//{
//    override fun reparseLeaf(leaf: ASTNode, newText: CharSequence): ASTNode? {
//
//        return  null
//    }
//
//}
//
//val EOL_WS = EolWsTokenType()
//class  PsiEolWsImpl (text :CharSequence) : LeafPsiElement(EOL_WS ,text)  , PsiEolWs {
//    override fun toString(): String = "PsiEolWs"
//}
//
