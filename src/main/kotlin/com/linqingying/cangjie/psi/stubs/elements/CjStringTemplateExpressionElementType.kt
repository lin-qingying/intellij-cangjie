package com.linqingying.cangjie.psi.stubs.elements

import com.linqingying.cangjie.psi.CjStringTemplateExpression
import com.intellij.lang.ASTNode
import org.jetbrains.annotations.NonNls


class CjStringTemplateExpressionElementType(@NonNls debugName: String) :
    CjPlaceHolderStubElementType<CjStringTemplateExpression>(debugName, CjStringTemplateExpression::class.java) {

    override fun shouldCreateStub(node: ASTNode): Boolean {
        if (node.treeParent?.elementType != CjStubElementTypes.VALUE_ARGUMENT) return false
        return super.shouldCreateStub(node)
    }
}
