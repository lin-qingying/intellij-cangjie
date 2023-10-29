package com.huawei.cangjie.psi

import com.huawei.cangjie.lexer.CjTokens
import com.huawei.cangjie.name.Name
import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.tree.IElementType


interface CjSimpleNameExpression : CjReferenceExpression {

    fun getReferencedName(): String

    fun getReferencedNameAsName(): Name

    fun getReferencedNameElement(): PsiElement

    fun getIdentifier(): PsiElement?

    fun getReferencedNameElementType(): IElementType
}

abstract class CjSimpleNameExpressionImpl(node: ASTNode) : CjExpressionImpl(node), CjSimpleNameExpression {
    override fun getIdentifier(): PsiElement? = findChildByType(CjTokens.IDENTIFIER)

    override fun getReferencedNameElementType() = getReferencedNameElementTypeImpl(this)

    override fun <R, D> accept(visitor: CjVisitor<R, D>, data: D?): R {
        return visitor.visitSimpleNameExpression(this, data)
    }

    override fun getReferencedNameAsName() = getReferencedNameAsNameImpl(this)

    override fun getReferencedName() = getReferencedNameImpl(this)

    //NOTE: an unfortunate way to share an implementation between stubbed and not stubbed tree
    companion object {
        fun getReferencedNameElementTypeImpl(expression: CjSimpleNameExpression): IElementType {
            return expression.getReferencedNameElement().node!!.elementType
        }

        fun getReferencedNameAsNameImpl(expresssion: CjSimpleNameExpression): Name {
            val name = expresssion.getReferencedName()
            return Name.identifier(name)
        }

        fun getReferencedNameImpl(expression: CjSimpleNameExpression): String {
            val text = expression.getReferencedNameElement().node!!.text
            return CjPsiUtil.unquoteIdentifierOrFieldReference(text)
        }
    }
}
