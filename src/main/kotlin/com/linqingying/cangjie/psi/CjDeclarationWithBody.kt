package com.linqingying.cangjie.psi

import com.intellij.psi.PsiElement

interface CjDeclarationWithBody : CjDeclaration {

    val bodyExpression: CjExpression?

    val equalsToken: PsiElement?

    override fun getName(): String?


    fun hasBlockBody(): Boolean

    fun hasBody(): Boolean

    fun hasDeclaredReturnType(): Boolean


    val valueParameters: List<CjParameter >

    val bodyBlockExpression: CjBlockExpression?
        get() {
            val bodyExpression = bodyExpression
            if (bodyExpression is CjBlockExpression) {
                return bodyExpression
            }

            return null
        }
}

