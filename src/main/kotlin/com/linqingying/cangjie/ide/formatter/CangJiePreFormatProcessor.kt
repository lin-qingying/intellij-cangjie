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
