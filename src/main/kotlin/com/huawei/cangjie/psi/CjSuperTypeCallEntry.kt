package com.huawei.cangjie.psi

import com.huawei.cangjie.CjNodeTypes
import com.huawei.cangjie.psi.stubs.CangJiePlaceHolderStub
import com.huawei.cangjie.psi.stubs.elements.CjStubElementTypes
import com.intellij.lang.ASTNode

class CjSuperTypeCallEntry : CjSuperTypeListEntry, CjCallElement {
    constructor(node: ASTNode) : super(node)

    constructor(stub: CangJiePlaceHolderStub<out CjSuperTypeListEntry >) : super(
        stub,
        CjStubElementTypes.SUPER_TYPE_CALL_ENTRY
    )

    override fun <R, D> accept(visitor: CjVisitor<R, D>, data: D?): R {
        return visitor.visitSuperTypeCallEntry(this, data)
    }

    override val calleeExpression : CjConstructorCalleeExpression get()   {
        return getRequiredStubOrPsiChild(CjStubElementTypes.CONSTRUCTOR_CALLEE)
    }

    override val lambdaArguments : List<CjLambdaArgument> get(){
        return emptyList()
    }

    override val typeArguments : List<CjTypeProjection> get(){
        val typeArgumentList = typeArgumentList ?: return emptyList()
        return typeArgumentList.arguments
    }

    override val typeArgumentList : CjTypeArgumentList?   = null

    override val valueArgumentList : CjValueArgumentList? get(){
        return findChildByType(CjNodeTypes.VALUE_ARGUMENT_LIST)
    }

    override val valueArguments : List<ValueArgument > get(){
        val list = valueArgumentList
        return list?.arguments ?: emptyList<CjValueArgument>()
    }


    override val typeReference : CjTypeReference? get(){
        return calleeExpression.typeReference
    }
}
