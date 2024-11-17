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

package com.linqingying.cangjie.ide.formatter

import com.linqingying.cangjie.lexer.CjTokens
import com.linqingying.cangjie.psi.*
import com.linqingying.cangjie.psi.psiUtil.allChildren
import com.linqingying.cangjie.psi.psiUtil.nextSiblingOfSameType
import com.linqingying.cangjie.utils.lastIsInstanceOrNull
import com.intellij.lang.ASTNode
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.impl.source.codeStyle.PreFormatProcessor
import com.intellij.psi.tree.IElementType


private class Visitor(var range: TextRange) : CjTreeVisitorVoid() {
    override fun visitNamedDeclaration(declaration: CjNamedDeclaration) {
        fun PsiElement.containsToken(type: IElementType) = allChildren.any { it.node.elementType == type }

        if (!range.contains(declaration.textRange)) return

        val classBody = declaration.parent as? CjAbstractClassBody ?: return
        val cjlass = classBody.parent as? CjClass ?: return


        var delta = 0

        val psiFactory = CjPsiFactory(cjlass.project)
        if (declaration is CjEnumEntry) {
            val comma = psiFactory.createComma()

            val nextEntry = declaration.nextSiblingOfSameType()
            if (nextEntry != null && !declaration.containsToken(CjTokens.COMMA)) {
                declaration.add(comma)
                delta += comma.textLength
            }
        } else {
            val lastEntry = cjlass.declarations.lastIsInstanceOrNull<CjEnumEntry>()
            if (lastEntry != null &&
                (lastEntry.containsToken(CjTokens.SEMICOLON) || lastEntry.nextSibling?.node?.elementType == CjTokens.SEMICOLON)
            ) return
            if (lastEntry == null && classBody.containsToken(CjTokens.SEMICOLON)) return

            val semicolon = psiFactory.createSemicolon()
            delta += if (lastEntry != null) {
                classBody.addAfter(semicolon, lastEntry)
                semicolon.textLength
            } else {
                val newLine = psiFactory.createNewLine()
                classBody.addAfter(semicolon, classBody.lBrace)
                classBody.addAfter(psiFactory.createNewLine(), classBody.lBrace)
                semicolon.textLength + newLine.textLength
            }
        }

        range = TextRange(range.startOffset, range.endOffset + delta)
    }
}

class CangJiePreFormatProcessor : PreFormatProcessor {
    override fun process(element: ASTNode, range: TextRange): TextRange {
        val psi = element.psi ?: return range
        if (!psi.isValid) return range
        if (psi.containingFile !is CjFile) return range
        return Visitor(range).apply { psi.accept(this) }.range
    }
}
