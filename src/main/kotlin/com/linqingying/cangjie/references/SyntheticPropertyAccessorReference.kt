package com.linqingying.cangjie.references

import com.linqingying.cangjie.name.Name
import com.linqingying.cangjie.psi.CjNameReferenceExpression
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement

abstract class SyntheticPropertyAccessorReference(
    expression: CjNameReferenceExpression,
    val getter: Boolean
) : CjSimpleReference<CjNameReferenceExpression>(expression)
{

    protected fun isAccessorName(name: String): Boolean {
        if (getter) {
            return name.startsWith("get") || name.startsWith("is")
        }
        return name.startsWith("set")
    }

    override fun canBeReferenceTo(candidateTarget: PsiElement): Boolean {
//        if (candidateTarget !is PsiMethod || !isAccessorName(candidateTarget.name)) return false
//        if (getter && !candidateTarget.canHaveSyntheticGetter || !getter && !candidateTarget.canHaveSyntheticSetter) return false
//        if (!getter && expression.readWriteAccess(true) == ReferenceAccess.READ) return false
        return true
    }

    override fun getRangeInElement() = TextRange(0, expression.textLength)

    override fun canRename() = true

    override val resolvesByNames: Collection<Name>
        get() = listOf(element.getReferencedNameAsName())
}
