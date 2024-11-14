package com.linqingying.cangjie.psi

import com.intellij.psi.PsiElement

/**
 * CjCallableDeclaration接口定义了一个可调用声明的结构，如函数或属性。
 * 它继承自CjNamedDeclaration和CjTypeParameterListOwner，集成了命名声明和类型参数列表的所有权特性。
 */
interface CjCallableDeclaration : CjNamedDeclaration, CjTypeParameterListOwner {
    /**
     * 获取值参数列表。
     */
    val valueParameterList: CjParameterList?

    /**
     * 获取值参数的列表。
     */
    val valueParameters: List<CjParameter>

    /**
     * 获取接收者类型引用。
     */
    val receiverTypeReference: CjTypeReference?

    /**
     * 获取上下文接收者列表，默认为空列表。
     */
    val contextReceivers: List<CjContextReceiver>
        get() = emptyList()

    /**
     * 获取类型引用。
     */
    val typeReference: CjTypeReference?

    /**
     * 设置类型引用。
     * @param typeRef 要设置的类型引用。
     * @return 设置后的类型引用。
     */
    fun setTypeReference(typeRef: CjTypeReference?): CjTypeReference?

    /**
     * 获取冒号元素，用于标识声明中的类型分隔符。
     */
    val colon: PsiElement?
}

