/*
 * Copyright 2025 LinQingYing. and contributors.
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



package org.cangnova.cangjie.formatter.util

import org.cangnova.cangjie.formatter.*
import org.cangnova.cangjie.lexer.CjTokens
import org.cangnova.cangjie.psi.*
import org.cangnova.cangjie.psi.psiUtil.getPrevSiblingIgnoringWhitespaceAndComments
import org.cangnova.cangjie.psi.psiUtil.nextLeaf
import org.cangnova.cangjie.psi.psiUtil.prevLeaf
import org.cangnova.cangjie.psi.psiUtil.siblings
import org.cangnova.cangjie.utils.cast
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

        else -> commaOwner.firstChild?.takeIfIsNotError()
    }

    fun elementAfterLastElement(commaOwner: CjElement): PsiElement? = when (commaOwner) {
        is CjParameterList -> {
            val parent = commaOwner.parent
            if (parent is CjFunctionLiteral) parent.arrow else commaOwner.rightParenthesis
        }
        is CjMatchEntry -> commaOwner.arrow

        else -> commaOwner.lastChild?.takeIfIsNotError()
    }

    private fun PsiElement.takeIfIsNotError(): PsiElement? = takeIf { !PsiTreeUtil.hasErrorElements(it) }

    private val RIGHT_BARRIERS = TokenSet.create(CjTokens.RBRACKET, CjTokens.RPAR, CjTokens.RBRACE, CjTokens.GT, CjTokens.ARROW)
    private val LEFT_BARRIERS = TokenSet.create(CjTokens.LBRACKET, CjTokens.LPAR, CjTokens.LBRACE, CjTokens.LT)
    private val PsiElement.lastSignificantChild: PsiElement?
        get() = when (this) {
            is CjMatchEntry -> arrow

            else -> lastChild
        }
}
