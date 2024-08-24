package com.huawei.cangjie.ide.completion

import com.huawei.cangjie.ide.completion.back.or
import com.huawei.cangjie.ide.completion.back.singleCharPattern
import com.huawei.cangjie.lexer.CjTokens
import com.huawei.cangjie.psi.CjFunctionLiteral
import com.intellij.patterns.ElementPattern
import com.intellij.patterns.StandardPatterns
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil

fun cangjieIdentifierStartPattern(): ElementPattern<Char> =
    StandardPatterns.character().javaIdentifierStart().andNot(singleCharPattern('$'))
fun cangjieIdentifierPartPattern(): ElementPattern<Char> =
    StandardPatterns.character().javaIdentifierPart().andNot(singleCharPattern('$')) or singleCharPattern('@')

fun isAtFunctionLiteralStart(position: PsiElement): Boolean {
    val lBrace = PsiTreeUtil.prevCodeLeaf(position)
        ?.let { if (it.node.elementType == CjTokens.LPAR) PsiTreeUtil.prevCodeLeaf(it) else it }
        ?.takeIf { it.node.elementType == CjTokens.LBRACE }

    val functionLiteral = lBrace?.parent as? CjFunctionLiteral ?: return false
    return functionLiteral.lBrace == lBrace
}
