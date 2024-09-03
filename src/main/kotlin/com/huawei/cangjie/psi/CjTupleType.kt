package com.huawei.cangjie.psi

import com.huawei.cangjie.psi.stubs.CangJiePlaceHolderStub
import com.huawei.cangjie.psi.stubs.elements.CjStubElementTypes
import com.intellij.lang.ASTNode


class CjTupleType : CjElementImplStub<CangJiePlaceHolderStub<CjTupleType>>, CjTypeElement {

    constructor(node: ASTNode) : super(node)

    constructor(stub: CangJiePlaceHolderStub<CjTupleType>) : super(stub, CjStubElementTypes.TUPLE_TYPE)
//    constructor(stub:CangJieTupleTypeStub):super(stub, CjStubElementTypes.TUPLE_TYPE)


    override fun <R, D> accept(visitor: CjVisitor<R, D>, data: D?): R {
        return visitor.visitTupleType(this, data)
    }


//
//    fun getParameters(): List<CjParameter> {
//        val list: CjParameterList = getParameterList()
//        return list?.parameters ?: emptyList()
//    }

    override fun getTypeArgumentsAsTypes(): List<CjTypeReference> {

        return getStubOrPsiChildrenAsList(CjStubElementTypes.TYPE_REFERENCE)


    }
}
