package com.linqingying.cangjie.psi

import com.linqingying.cangjie.psi.psiUtil.createExpressionByPattern
import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.tree.IElementType
import com.intellij.util.ArrayFactory


//表达式
interface CjExpression : CjElement {
    override fun <R, D> accept(visitor: CjVisitor<R, D>, data: D?): R

    companion object {
        @JvmStatic

        val EMPTY_ARRAY = arrayOf<CjExpression>()

        @JvmStatic
        val ARRAY_FACTORY =
            ArrayFactory { count: Int ->
                if (count == 0) EMPTY_ARRAY else arrayOfNulls<CjExpression>(count)

            }
    }
}


abstract class CjExpressionImpl(node: ASTNode) : CjElementImpl(node), CjExpression {

    override fun <R, D> accept(visitor: CjVisitor<R, D>, data: D?): R = visitor.visitExpression(this, data)

    protected fun findExpressionUnder(type: IElementType): CjExpression? {
        val containerNode = findChildByType<CjContainerNode>(type) ?: return null
        return containerNode.findChildByClass(CjExpression::class.java)
    }

    override fun replace(newElement: PsiElement): PsiElement {
        return replaceExpression(this, newElement) { super.replace(it) }
    }

    companion object {
        fun replaceExpression(
            expression: CjExpression,
            newElement: PsiElement,
            reformat: Boolean = true,
            rawReplaceHandler: (PsiElement) -> PsiElement
        ): PsiElement {
            val parent = expression.parent

            if (newElement is CjExpression) {
                when (parent) {
                    is CjExpression, is CjValueArgument -> {
                        if (CjPsiUtil.areParenthesesNecessary(newElement, expression, parent as CjElement)) {
                            val factory = CjPsiFactory(expression.project)
                            return rawReplaceHandler(
                                factory.createExpressionByPattern(
                                    "($0)",
                                    newElement,
                                    reformat = reformat
                                )
                            )
                        }
                    }
//                    is CjSimpleNameStringTemplateEntry -> {
//                        if (newElement !is CjSimpleNameExpression && !newElement.isThisWithoutLabel()) {
//                            val factory = CjPsiFactory(expression.project)
//                            val newEntry = parent.replace(factory.createBlockStringTemplateEntry(newElement)) as CjBlockStringTemplateEntry
//                            return newEntry.expression!!
//                        }
//                    }
                }
            }

            return rawReplaceHandler(newElement)
        }
    }
}

