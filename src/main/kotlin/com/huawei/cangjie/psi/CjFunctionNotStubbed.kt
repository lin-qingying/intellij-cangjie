package com.huawei.cangjie.psi

import com.huawei.cangjie.CjNodeTypes
import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement

@Suppress("deprecation")
abstract class CjFunctionNotStubbed(node: ASTNode) : CjTypeParameterListOwnerNotStubbed(node),
    CjFunction {
    override val valueParameterList: CjParameterList?
        get() = findChildByType(CjNodeTypes.VALUE_PARAMETER_LIST)

    override val valueParameters: List<CjParameter>
        get() {
            val list = valueParameterList
            return list?.parameters ?: emptyList()
        }

    override val bodyExpression: CjExpression?
        get() {
            return findChildByClass(CjExpression::class.java)
        }

    override fun hasDeclaredReturnType(): Boolean {
        return false
    }

    override val receiverTypeReference: CjTypeReference?
        get() = null

    override val contextReceivers: List<CjContextReceiver>
        get() = emptyList()

    override val typeReference: CjTypeReference?
        get() = null

    override fun setTypeReference(typeRef: CjTypeReference?): CjTypeReference? {
        if (typeRef == null) return null
        throw IllegalStateException("Lambda expressions can't have type reference")
    }

    override val colon: PsiElement?
        get() = null

    override val isLocal: Boolean
        get() {
            val parent = parent
            return !(parent is CjFile || parent is CjAbstractClassBody)
        }
}
