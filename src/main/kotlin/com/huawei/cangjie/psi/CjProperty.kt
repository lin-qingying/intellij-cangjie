package com.huawei.cangjie.psi

import com.huawei.cangjie.psi.stubs.CangJiePropertyStub
import com.huawei.cangjie.psi.stubs.elements.CjStubElementTypes
import com.intellij.lang.ASTNode
import com.intellij.openapi.diagnostic.Logger
import com.intellij.psi.PsiElement

open class CjProperty : CjTypeParameterListOwnerStub<CangJiePropertyStub>, CjVariableDeclaration {
    companion object {

        val LOG: Logger = Logger.getInstance(
            CjProperty::class.java
        )

    }

    override fun getTypeReference(): CjTypeReference? {
        val stub: CangJiePropertyStub? = stub
        if (stub != null) {
            if (!stub.hasReturnTypeRef()) {
                return null
            } else {
                val typeReferences: List<CjTypeReference> =
                    getStubOrPsiChildrenAsList(CjStubElementTypes.TYPE_REFERENCE)
                val returnTypeRefPositionInPsi = /*if (stub.isExtension()) 1 else*/ 0
                if (typeReferences.size <= returnTypeRefPositionInPsi) {
                    LOG.error(
                        """
                        Invalid stub structure built for property:
                        $text
                        """.trimIndent()
                    )
                    return null
                }
                return typeReferences[returnTypeRefPositionInPsi]
            }
        }
        return getTypeReference(this)
    }

    override fun setTypeReference(typeRef: CjTypeReference?): CjTypeReference? {
        TODO("Not yet implemented")
    }

    override fun getColon(): PsiElement? {
        TODO("Not yet implemented")
    }

    override val initializer: CjExpression?
        get() = TODO("Not yet implemented")

    override fun hasInitializer(): Boolean {
        TODO("Not yet implemented")
    }

    override val letOrVarKeyword: PsiElement?
        get() = TODO("Not yet implemented")


    constructor(stub: CangJiePropertyStub) : super(stub, CjStubElementTypes.PROPERTY)
    constructor(node: ASTNode) : super(node)

    override val isVar: Boolean
        get() = TODO("Not yet implemented")


    override fun toString(): String = super.toString() + ": " + name
    override fun getValueParameterList(): CjParameterList? {
        TODO("Not yet implemented")
    }

    override fun getValueParameters(): MutableList<CjParameter> {
        TODO("Not yet implemented")
    }

    override fun getReceiverTypeReference(): CjTypeReference? {
        TODO("Not yet implemented")
    }
}
