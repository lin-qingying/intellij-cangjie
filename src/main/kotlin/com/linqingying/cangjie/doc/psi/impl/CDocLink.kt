package com.linqingying.cangjie.doc.psi.impl

import com.linqingying.cangjie.psi.CjElementImpl
import com.linqingying.cangjie.psi.psiUtil.getStrictParentOfType
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

    /**
     * If this link is the subject of a tag, returns the tag. Otherwise, returns null.
     */
    fun getTagIfSubject(): CDocTag? {
        val tag = getStrictParentOfType<CDocTag>()
        return if (tag != null && tag.getSubjectLink() == this) tag else null
    }
}
