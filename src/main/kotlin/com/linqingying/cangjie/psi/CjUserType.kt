package com.linqingying.cangjie.psi

import com.google.common.collect.Lists
import com.linqingying.cangjie.lexer.CjTokens
import com.linqingying.cangjie.psi.stubs.CangJieUserTypeStub
import com.linqingying.cangjie.psi.stubs.elements.CjStubElementTypes
import com.intellij.lang.ASTNode

class CjUserType : CjElementImplStub<CangJieUserTypeStub >, CjTypeElement {
    constructor(node: ASTNode) : super(node)

    constructor(stub: CangJieUserTypeStub) : super(stub, CjStubElementTypes.USER_TYPE)

    override fun <R, D> accept(visitor: CjVisitor<R, D>, data: D?): R {
        return visitor.visitUserType(this, data)
    }

    val typeArgumentList: CjTypeArgumentList?
        get() = getStubOrPsiChild(CjStubElementTypes.TYPE_ARGUMENT_LIST)

    val typeArguments: List<CjTypeProjection>
        get() {
            val typeArgumentList = typeArgumentList
            return typeArgumentList?.arguments ?: emptyList()
        }

    override fun toString(): String {
        return node.elementType.toString()
    }

    override val typeArgumentsAsTypes: List<CjTypeReference>
        get() {
            val result: MutableList<CjTypeReference > =
                Lists.newArrayList()
            for (projection in typeArguments) {
                projection.typeReference?.let { result.add(it) }
            }
            return result
        }

    @get:IfNotParsed
    val referenceExpression: CjSimpleNameExpression?
        get() = getStubOrPsiChild(CjStubElementTypes.REFERENCE_EXPRESSION)

    val qualifier: CjUserType?
        get() = getStubOrPsiChild(CjStubElementTypes.USER_TYPE)

    /**
     * 保留除该USER_TYPE以外指定数量的psi元素
     * 例如 USER_TYPE = a.b.c; size = 1
     * 保留 b.c
     *
     * @param size
     */
    fun deleteQualifier(size: Int) {
        if (size <= 0) {
            deleteQualifier()
        }
        val qualifier = qualifier

        qualifier?.deleteQualifier(size - 1)
    }

    fun deleteQualifier() {
        val qualifier = checkNotNull(qualifier)
        val dot = checkNotNull(findChildByType(CjTokens.DOT))
        qualifier.delete()
        dot.delete()
    }

    val referencedName: String?
        get() {
            val referenceExpression = referenceExpression
            return referenceExpression?.getReferencedName()
        }
}
