package com.huawei.cangjie.psi

import com.google.common.collect.Lists
import com.huawei.cangjie.lexer.CjToken
import com.huawei.cangjie.lexer.CjTokens
import com.huawei.cangjie.psi.stubs.CangJiePlaceHolderStub
import com.huawei.cangjie.psi.stubs.elements.CjStubElementTypes
import com.intellij.lang.ASTNode

class CjFunctionType : CjElementImplStub<CangJiePlaceHolderStub<CjFunctionType > >, CjTypeElement {
    constructor(node: ASTNode) : super(node)

    constructor(stub: CangJiePlaceHolderStub<CjFunctionType >) : super(stub, CjStubElementTypes.FUNCTION_TYPE)

    override val typeArgumentsAsTypes: List<CjTypeReference >
        get() {
            val result =
                Lists.newArrayList<CjTypeReference >()

            result.addAll(contextReceiversTypeReferences)
            val receiverTypeRef = receiverTypeReference
            if (receiverTypeRef != null) {
                result.add(receiverTypeRef)
            }
            for (cjParameter in parameters) {
                result.add(cjParameter.typeReference)
            }
            val returnTypeRef = returnTypeReference
            if (returnTypeRef != null) {
                result.add(returnTypeRef)
            }
            return result
        }

    override fun <R, D> accept(visitor: CjVisitor<R, D>, data: D?): R {
        return visitor.visitFunctionType(this, data)
    }

    val parameterList: CjParameterList?
        get() = getStubOrPsiChild(CjStubElementTypes.VALUE_PARAMETER_LIST)

    val parameters: List<CjParameter>
        get() {
            val list = parameterList
            return list?.parameters ?: emptyList()
        }

    val receiver: CjFunctionTypeReceiver?
        get() = getStubOrPsiChild(CjStubElementTypes.FUNCTION_TYPE_RECEIVER)

    val receiverTypeReference: CjTypeReference?
        get() {
            val receiverDeclaration = receiver ?: return null
            return receiverDeclaration.typeReference
        }

    val contextReceiverList: CjContextReceiverList?
        get() = getStubOrPsiChild(CjStubElementTypes.CONTEXT_RECEIVER_LIST)

    val contextReceiversTypeReferences: List<CjTypeReference >
        get() {
            val contextReceiverList = contextReceiverList
            return contextReceiverList?.typeReferences() ?: emptyList ()
        }

    val returnTypeReference: CjTypeReference?
        get() = getStubOrPsiChild(CjStubElementTypes.TYPE_REFERENCE)

    companion object {
        val RETURN_TYPE_SEPARATOR: CjToken = CjTokens.ARROW
    }
}
