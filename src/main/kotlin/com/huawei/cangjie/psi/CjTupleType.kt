package com.huawei.cangjie.psi

import com.google.common.collect.Lists
import com.huawei.cangjie.psi.stubs.CangJieTupleTypeStub
import com.huawei.cangjie.psi.stubs.elements.CjStubElementTypes
import com.intellij.lang.ASTNode

class CjTupleType: CjElementImplStub<CangJieTupleTypeStub>,CjTypeElement {

    constructor(node:ASTNode):super(node)

    constructor(stub:CangJieTupleTypeStub):super(stub, CjStubElementTypes.TUPLE_TYPE)

    fun getTypeArgumentList(): CjTypeArgumentList? {
        return getStubOrPsiChild(CjStubElementTypes.TYPE_ARGUMENT_LIST)
    }
    override fun <R, D> accept(visitor: CjVisitor<R, D>, data: D?): R {
        return visitor.visitTupleType(this, data)
    }
    fun getTypeArguments(): List<CjTypeProjection> {
        val typeArgumentList: CjTypeArgumentList? = getTypeArgumentList()
        return typeArgumentList?.arguments ?: emptyList()
    }
    override fun getTypeArgumentsAsTypes(): MutableList<CjTypeReference?> {

        val result: MutableList<CjTypeReference?> = Lists.newArrayList()
        for (projection in getTypeArguments()) {
            result.add(projection.typeReference)
        }
        return result
    }
}
