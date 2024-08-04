package com.linqingying.cangjie.doc.psi.impl

import com.linqingying.cangjie.psi.CjElementImpl
import com.intellij.lang.ASTNode
import com.intellij.openapi.util.TextRange


class CDocLink(node: ASTNode) : CjElementImpl(node) {
    fun getLinkText(): String = getLinkTextRange().substring(text)

    fun getLinkTextRange(): TextRange {
        val text = text
        if (text.startsWith('[') && text.endsWith(']')) {
            return TextRange(1, text.length - 1)
        }
        return TextRange(0, text.length)
    }


}
