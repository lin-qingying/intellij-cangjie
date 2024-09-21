package com.huawei.cangjie.references

import com.huawei.cangjie.name.Name
import com.huawei.cangjie.psi.CjConstructorDelegationReferenceExpression
import com.huawei.cangjie.psi.CjImportAlias
import com.huawei.cangjie.resolve.BindingContext
import com.huawei.cangjie.resolve.getReferenceTargets
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
