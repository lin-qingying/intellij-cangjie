package com.huawei.cangjie.doc.psi.impl

import com.huawei.cangjie.doc.psi.CDoc
import com.huawei.cangjie.lexer.CjTokens
import com.huawei.cangjie.name.FqName
import com.huawei.cangjie.psi.CjElementImpl
import com.huawei.cangjie.psi.psiUtil.getChildOfType
import com.huawei.cangjie.psi.psiUtil.getStrictParentOfType
import com.intellij.lang.ASTNode
import com.intellij.openapi.util.TextRange


/**
 * 标签主题或链接中限定名称的单个部分。
 */
class CDocName(node: ASTNode) : CjElementImpl(node) {
    fun getContainingDoc(): CDoc {
        val Cdoc = getStrictParentOfType<CDoc>()
        return Cdoc ?: throw IllegalStateException("CDOCName must be inside a CDOC")
    }

    fun getContainingSection(): CDocSection {
        val CDOC = getStrictParentOfType<CDocSection>()
        return CDOC ?: throw IllegalStateException("CDOCName must be inside a CDOCSection")
    }

    fun getQualifier(): CDocName? = getChildOfType()

    /**
     *返回包含名称的元素内的范围(换句话说， 不包括限定符和点的元素的范围(如果存在)。
     */
    fun getNameTextRange(): TextRange {
        val dot = node.findChildByType(CjTokens.DOT)
        val textRange = textRange
        val nameStart = if (dot != null) dot.textRange.endOffset - textRange.startOffset else 0
        return TextRange(nameStart, textRange.length)
    }

    fun getNameText(): String = getNameTextRange().substring(text)

    fun getQualifiedName(): List<String> {
        val qualifier = getQualifier()
        val nameAsList = listOf(getNameText())
        return if (qualifier != null) qualifier.getQualifiedName() + nameAsList else nameAsList
    }

    fun getQualifiedNameAsFqName(): FqName {
        return FqName.fromSegments(getQualifiedName())
    }
}
