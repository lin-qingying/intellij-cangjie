package com.huawei.cangjie.psi

import com.intellij.psi.PsiElement

interface CjCallableDeclaration : CjNamedDeclaration, CjTypeParameterListOwner {
    val valueParameterList: CjParameterList?

    val valueParameters: List<CjParameter >

    val receiverTypeReference: CjTypeReference?

    val contextReceivers: List<CjContextReceiver>
        get() = emptyList()


    val typeReference: CjTypeReference?

    fun setTypeReference(typeRef: CjTypeReference?): CjTypeReference?

    val colon: PsiElement?
}
