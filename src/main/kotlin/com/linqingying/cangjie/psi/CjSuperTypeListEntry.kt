package com.linqingying.cangjie.psi

import com.linqingying.cangjie.psi.CjTypeStatement
import com.linqingying.cangjie.psi.stubs.CangJiePlaceHolderStub
import com.linqingying.cangjie.psi.stubs.elements.CjStubElementTypes
import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.IStubElementType
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.ArrayFactory

open class CjSuperTypeListEntry : CjElementImplStub<CangJiePlaceHolderStub<out CjSuperTypeListEntry > > {
    constructor(node: ASTNode) : super(node)

    constructor(
        stub: CangJiePlaceHolderStub<out CjSuperTypeListEntry >,
        nodeType: IStubElementType<*, *>
    ) : super(stub, nodeType)

    val parentDeclaration: PsiElement?
        get() = PsiTreeUtil.getParentOfType(
            this,
            CjTypeStatement::class.java
        )

    override fun <R, D> accept(visitor: CjVisitor<R, D>, data: D?): R {
        return visitor.visitSuperTypeListEntry(this, data)
    }

    override fun toString(): String {
        return node.elementType.toString()
    }

    open val typeReference: CjTypeReference?
        get() = getStubOrPsiChild(CjStubElementTypes.TYPE_REFERENCE)

    val typeAsUserType: CjUserType?
        get() {
            val reference = typeReference
            if (reference != null) {
                val element = reference.typeElement
                if (element is CjUserType) {
                    return element
                }
            }
            return null
        }

    companion object {
        private val EMPTY_ARRAY = arrayOfNulls<CjSuperTypeListEntry>(0)

        var ARRAY_FACTORY: ArrayFactory<CjSuperTypeListEntry > =
            ArrayFactory { count: Int -> if (count == 0) EMPTY_ARRAY else arrayOfNulls(count) }
    }
}
