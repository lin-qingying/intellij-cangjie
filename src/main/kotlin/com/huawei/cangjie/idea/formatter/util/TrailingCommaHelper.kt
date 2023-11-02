

package com.huawei.cangjie.idea.formatter.util

import com.huawei.cangjie.idea.formatter.*
import com.huawei.cangjie.lexer.CjTokens
import com.huawei.cangjie.psi.*
import com.huawei.cangjie.psi.psiUtil.getPrevSiblingIgnoringWhitespaceAndComments
import com.huawei.cangjie.psi.psiUtil.nextLeaf
import com.huawei.cangjie.psi.psiUtil.prevLeaf
import com.huawei.cangjie.psi.psiUtil.siblings
import com.huawei.cangjie.utils.cast
import com.intellij.psi.PsiElement
import com.intellij.psi.codeStyle.CodeStyleSettings
import com.intellij.psi.tree.TokenSet
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.util.PsiUtilCore


object TrailingCommaHelper {
    fun findInvalidCommas(commaOwner: CjElement): List<PsiElement> = commaOwner.firstChild
        ?.siblings(withItself = false)
        ?.filter { it.isComma }
        ?.filter {
            it.prevLeaf(true)?.isLineBreak() == true || it.leafIgnoringWhitespace(false) != it.leafIgnoringWhitespaceAndComments(false)
        }?.toList().orEmpty()

    fun trailingCommaExistsOrCanExist(psiElement: PsiElement, settings: CodeStyleSettings): Boolean =
        TrailingCommaContext.create(psiElement).commaExistsOrMayExist(settings.cangjieCustomSettings)

    fun trailingCommaExists(commaOwner: CjElement): Boolean = when (commaOwner) {
        is CjFunctionLiteral -> commaOwner.valueParameterList?.trailingComma != null
        is CjMatchEntry -> commaOwner.trailingComma != null
        is CjDestructuringDeclaration -> commaOwner.trailingComma != null
        else -> trailingCommaOrLastElement(commaOwner)?.isComma == true
    }

    fun trailingCommaOrLastElement(commaOwner: CjElement): PsiElement? {
        val lastChild = commaOwner.lastSignificantChild ?: return null
        val withSelf = when (PsiUtilCore.getElementType(lastChild)) {
            CjTokens.COMMA -> return lastChild
            in RIGHT_BARRIERS -> false
            else -> true
        }

        return lastChild.getPrevSiblingIgnoringWhitespaceAndComments(withSelf)?.takeIf {
            PsiUtilCore.getElementType(it) !in LEFT_BARRIERS
        }?.takeIfIsNotError()
    }


    fun lineBreakIsMissing(commaOwner: CjElement): Boolean {
        if (!trailingCommaExists(commaOwner)) return false

        val first = elementBeforeFirstElement(commaOwner)
        if (first?.nextLeaf(true)?.isLineBreak() == false) return true

        val last = elementAfterLastElement(commaOwner)
        return last?.prevLeaf(true)?.isLineBreak() == false
    }

    fun elementBeforeFirstElement(commaOwner: CjElement): PsiElement? = when (commaOwner) {
        is CjParameterList -> {
            val parent = commaOwner.parent
            if (parent is CjFunctionLiteral) parent.lBrace else commaOwner.leftParenthesis
        }
        is CjMatchEntry -> commaOwner.parent.cast<CjMatchExpression>().openBrace
        is CjDestructuringDeclaration -> commaOwner.lPar
        else -> commaOwner.firstChild?.takeIfIsNotError()
    }

    fun elementAfterLastElement(commaOwner: CjElement): PsiElement? = when (commaOwner) {
        is CjParameterList -> {
            val parent = commaOwner.parent
            if (parent is CjFunctionLiteral) parent.arrow else commaOwner.rightParenthesis
        }
        is CjMatchEntry -> commaOwner.arrow
        is CjDestructuringDeclaration -> commaOwner.rPar
        else -> commaOwner.lastChild?.takeIfIsNotError()
    }

    private fun PsiElement.takeIfIsNotError(): PsiElement? = takeIf { !PsiTreeUtil.hasErrorElements(it) }

    private val RIGHT_BARRIERS = TokenSet.create(CjTokens.RBRACKET, CjTokens.RPAR, CjTokens.RBRACE, CjTokens.GT, CjTokens.ARROW)
    private val LEFT_BARRIERS = TokenSet.create(CjTokens.LBRACKET, CjTokens.LPAR, CjTokens.LBRACE, CjTokens.LT)
    private val PsiElement.lastSignificantChild: PsiElement?
        get() = when (this) {
            is CjMatchEntry -> arrow
            is CjDestructuringDeclaration -> rPar
            else -> lastChild
        }
}
