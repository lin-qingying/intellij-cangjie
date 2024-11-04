package com.linqingying.cangjie.references

import com.linqingying.cangjie.name.Name
import com.linqingying.cangjie.psi.CjConstructorDelegationReferenceExpression
import com.linqingying.cangjie.psi.CjImportAlias
import com.linqingying.cangjie.resolve.BindingContext
import com.linqingying.cangjie.resolve.getReferenceTargets
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement


class CjConstructorDelegationReference(
    expression: CjConstructorDelegationReferenceExpression
) : CjSimpleReference<CjConstructorDelegationReferenceExpression>(expression), CjReference {
    override fun getTargetDescriptors(context: BindingContext) = expression.getReferenceTargets(context)


    override fun getRangeInElement(): TextRange {
        return TextRange(0, element.textLength)
    }

    override val resolvesByNames: Collection<Name>
        get() = emptyList()

    override fun handleElementRename(newElementName: String): PsiElement? {
        // Class rename never affects this reference, so there is no need to fail with exception
        return expression
    }
}
