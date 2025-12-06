/*
 * Copyright 2025 LinQingYing. and contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * The use of this source code is governed by the Apache License 2.0,
 * which allows users to freely use, modify, and distribute the code,
 * provided they adhere to the terms of the license.
 *
 * The software is provided "as-is", and the authors are not responsible for
 * any damages or issues arising from its use.
 *
 */

package org.cangnova.cangjie.psi

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
