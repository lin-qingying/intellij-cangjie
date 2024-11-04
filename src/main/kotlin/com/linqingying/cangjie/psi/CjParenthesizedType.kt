package com.linqingying.cangjie.psi

import com.google.common.collect.Lists
import com.linqingying.cangjie.psi.stubs.CangJiePlaceHolderStub
import com.linqingying.cangjie.psi.stubs.elements.CjStubElementTypes
import com.intellij.lang.ASTNode

class CjParenthesizedType : CjElementImplStub<CangJiePlaceHolderStub<CjParenthesizedType>>, CjTypeElement {


    constructor(node: ASTNode) : super(node)

    constructor(stub: CangJiePlaceHolderStub<CjParenthesizedType>) : super(stub, CjStubElementTypes.PARENTHESIZED_TYPE)

    fun getTypeArgumentList(): CjTypeArgumentList? {
        return getStubOrPsiChild(CjStubElementTypes.TYPE_ARGUMENT_LIST)
    }

    override fun <R, D> accept(visitor: CjVisitor<R, D>, data: D?): R {
        return visitor.visitParenthesizedType(this, data)
    }

    fun getType(): CjTypeElement? {
        return getStubOrPsiChild(CjStubElementTypes.TYPE_REFERENCE)?.typeElement
    }

    fun getTypeArguments(): List<CjTypeProjection> {
        val typeArgumentList: CjTypeArgumentList? = getTypeArgumentList()
        return typeArgumentList?.arguments ?: emptyList()
    }

    override val typeArgumentsAsTypes: List<CjTypeReference>
        get() {

            val result: MutableList<CjTypeReference> = Lists.newArrayList()
            for (projection in getTypeArguments()) {
                projection.typeReference?.let { result.add(it) }
            }
            return result
        }

}
