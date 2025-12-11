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
 * 仓颉语言中可调用声明的基础接口
 *
 * 该接口表示所有可以被调用的声明元素,包括:
 * - 函数 (CjFunction)
 * - 构造函数 (CjConstructor)
 * - 属性的 getter/setter (CjPropertyAccessor)
 *
 * ## 核心特性
 * 可调用声明具有以下共同特征:
 * - **参数列表**: 通过 [valueParameterList] 和 [valueParameters] 访问
 * - **返回类型**: 通过 [typeReference] 指定
 * - **类型参数**: 通过 [CjTypeParameterListOwner] 支持泛型
 * - **接收者**: 支持扩展函数/属性的接收者类型
 * - **上下文接收者**: 支持上下文相关的接收者 (实验性特性)
 *
 * ## 继承关系
 * ```
 * CjCallableDeclaration
 *   ├─ CjNamedDeclaration (命名声明)
 *   └─ CjTypeParameterListOwner (拥有类型参数列表)
 * ```
 *
 * ## 语法结构
 * ```cangjie
 * [modifiers] func [<type-parameters>] [receiver.]name([parameters])[: returnType]
 * ```
 *
 * ## 源代码示例
 * ```cangjie
 * // 普通函数
 * public func add(a: Int64, b: Int64): Int64 {
 *     return a + b
 * }
 *
 * // 泛型函数
 * public func <T> identity(value: T): T {
 *     return value
 * }
 *
 * // 扩展函数 (带接收者)
 * public func String.reverse(): String {
 *     // this 是 String 类型的接收者
 *     return this.reversed()
 * }
 *
 * // 带上下文接收者的函数 (实验性)
 * context(Logger)
 * public func logAndCompute(value: Int64): Int64 {
 *     // 可以访问 Logger 上下文
 *     log("Computing with: $value")
 *     return value * 2
 * }
 * ```
 *
 * @see CjFunction 函数声明接口
 * @see CjNamedDeclaration 命名声明接口
 * @see CjTypeParameterListOwner 拥有类型参数列表的接口
 * @see CjParameter 参数声明
 */
interface CjCallableDeclaration : CjNamedDeclaration, CjTypeParameterListOwner {
    /**
     * 获取值参数列表的 PSI 元素
     *
     * 值参数列表是包含所有函数参数的语法节点,对应源代码中的 `(param1, param2, ...)` 部分。
     *
     * @return 参数列表 PSI 元素,如果没有参数列表则返回 null
     * @see CjParameterList
     */
    val valueParameterList: CjParameterList?

    /**
     * 获取所有值参数的列表
     *
     * 返回函数的所有参数,按照声明顺序排列。
     * 这是访问参数最常用的方式。
     *
     * @return 参数列表,如果没有参数则返回空列表
     * @see CjParameter
     */
    val valueParameters: List<CjParameter>

    /**
     * 获取接收者类型引用
     *
     * 接收者类型用于定义扩展函数/属性,表示该可调用声明扩展的类型。
     * 例如,在 `func String.isEmpty(): Bool` 中,`String` 就是接收者类型。
     *
     * **扩展函数特性**:
     * - 接收者类型在函数内部可以通过 `this` 访问
     * - 接收者类型必须在调用点明确
     * - 扩展函数不能访问接收者类型的私有成员
     *
     * @return 接收者类型引用,如果不是扩展函数/属性则返回 null
     * @see CjTypeReference
     */
    val receiverTypeReference: CjTypeReference?

    /**
     * 获取上下文接收者列表
     *
     * 上下文接收者是一种实验性特性,允许函数在特定上下文中可用。
     * 使用 `context(Type1, Type2)` 语法声明。
     *
     * @return 上下文接收者列表,默认为空列表
     * @see CjContextReceiver
     */
    val contextReceivers: List<CjContextReceiver>
        get() = emptyList()

    /**
     * 获取返回类型引用
     *
     * 返回类型引用指定了该可调用声明的返回值类型。
     * 如果未显式指定返回类型,则根据上下文推断或默认为 Unit。
     *
     * @return 返回类型引用,如果未指定则返回 null
     * @see CjTypeReference
     */
    val typeReference: CjTypeReference?

    /**
     * 设置返回类型引用
     *
     * 该方法用于修改或添加返回类型,通常在代码重构或快速修复时使用。
     * 注意: 该方法会修改 PSI 树。
     *
     * @param typeRef 要设置的类型引用,null 表示移除类型引用
     * @return 设置后的类型引用
     */
    fun setTypeReference(typeRef: CjTypeReference?): CjTypeReference?

    /**
     * 获取冒号元素
     *
     * 冒号元素是返回类型声明前的 `:` 符号,用于语法高亮和导航。
     * 例如在 `func add(a: Int64): Int64` 中,最后一个 `:` 就是冒号元素。
     *
     * @return 冒号 PSI 元素,如果没有返回类型声明则返回 null
     */
    val colon: PsiElement?
}
