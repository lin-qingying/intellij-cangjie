/*
 * Copyright 2026 LinQingYing. and contributors.
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
 * 仓颉语言中函数声明的基础接口
 *
 * 该接口表示所有类型的函数声明,包括:
 * - 顶层函数 (定义在文件顶层)
 * - 成员函数 (定义在类、接口、结构体中)
 * - 局部函数 (定义在函数或代码块内部)
 * - 扩展函数 (扩展已有类型的功能)
 * - 运算符函数 (operator 修饰的函数)
 *
 * ## 继承关系
 * ```
 * CjFunction
 *   ├─ CjDeclarationWithBody (拥有函数体的声明)
 *   ├─ CjCallableDeclaration (可调用的声明,包含参数和返回类型)
 *   └─ CjLocalNamedDeclaration (局部命名声明)
 * ```
 *
 * ## 函数特性
 * 该接口提供了查询函数各种特性的属性:
 * - [isLocal]: 是否为局部函数
 * - [isStatic]: 是否为静态函数
 * - [isUnsafe]: 是否为不安全函数
 * - [isOperator]: 是否为运算符函数
 * - [isMut]: 是否可修改接收者状态
 * - [isConst]: 是否为编译期常量函数
 *
 * ## 源代码示例
 * ```cangjie
 * // 顶层函数
 * public func add(a: Int64, b: Int64): Int64 {
 *     return a + b
 * }
 *
 * // 成员函数
 * class Calculator {
 *     public func multiply(a: Int64, b: Int64): Int64 {
 *         return a * b
 *     }
 * }
 *
 * // 扩展函数
 * public func String.isEmpty(): Bool {
 *     return this.length == 0
 * }
 *
 * // 运算符函数
 * public operator func +(a: Vector, b: Vector): Vector {
 *     return Vector(a.x + b.x, a.y + b.y)
 * }
 *
 * // 局部函数
 * func outer() {
 *     func inner() {  // 这是局部函数
 *         println("inside inner")
 *     }
 *     inner()
 * }
 *
 * // 不安全函数
 * public unsafe func dangerousOperation(): Unit {
 *     // 可能包含不安全操作
 * }
 * ```
 *
 * ## 使用示例
 *
 * ### 示例 1: 检查函数类型
 * ```kotlin
 * fun analyzeFunction(function: CjFunction) {
 *     when {
 *         function.isLocal -> println("局部函数")
 *         function.isOperator -> println("运算符函数")
 *         function.isStatic -> println("静态函数")
 *         function.isUnsafe -> println("不安全函数")
 *     }
 * }
 * ```
 *
 * ### 示例 2: 获取函数签名
 * ```kotlin
 * fun getFunctionSignature(function: CjFunction): String {
 *     val name = function.name ?: "anonymous"
 *     val params = function.valueParameters.joinToString(", ") {
 *         "${it.name}: ${it.typeReference?.text ?: "?"}"
 *     }
 *     val returnType = function.typeReference?.text ?: "Unit"
 *     return "$name($params): $returnType"
 * }
 * ```
 *
 * @see CjDeclarationWithBody 拥有函数体的声明
 * @see CjCallableDeclaration 可调用的声明
 * @see CjNamedFunction 命名函数的具体实现
 */
interface CjFunction : CjDeclarationWithBody, CjCallableDeclaration, CjLocalNamedDeclaration {
    /**
     * 判断是否为局部函数
     *
     * 局部函数是定义在函数或代码块内部的函数,其作用域仅限于定义它的代码块。
     *
     * @return 如果是局部函数返回 true,否则返回 false
     */
    val isLocal: Boolean

    /**
     * 判断是否为静态函数
     *
     * 静态函数属于类型本身,而不是类型的实例,可以通过类型名直接调用。
     *
     * @return 如果是静态函数返回 true,否则返回 false
     */
    val isStatic: Boolean
        get() = false

    /**
     * 判断是否为不安全函数
     *
     * 不安全函数可能包含不安全操作(如原生内存访问、类型转换等),
     * 需要使用 `unsafe` 关键字修饰。调用不安全函数时也需要在 unsafe 上下文中。
     *
     * @return 如果是不安全函数返回 true,否则返回 false
     */
    val isUnsafe: Boolean
        get() = false

    /**
     * 判断是否为运算符函数
     *
     * 运算符函数使用 `operator` 关键字修饰,可以通过运算符符号调用,
     * 例如 `+`, `-`, `*`, `/`, `[]`, `()` 等。
     *
     * @return 如果是运算符函数返回 true,否则返回 false
     */
    val isOperator: Boolean
        get() = false

    /**
     * 判断函数是否可以修改接收者状态
     *
     * 使用 `mut` 修饰的成员函数可以修改接收者(this)的可变状态。
     * 非 mut 函数只能调用接收者的其他非 mut 函数。
     *
     * @return 如果是 mut 函数返回 true,否则返回 false
     */
    val isMut get() = false

    /**
     * 判断是否为编译期常量函数
     *
     * 使用 `const` 修饰的函数可以在编译期求值,通常用于常量表达式计算。
     *
     * @return 如果是 const 函数返回 true,否则返回 false
     */
    val isConst get() = false

    /**
     * 获取函数关键字元素
     *
     * 返回 `func` 关键字对应的 PSI 元素,用于导航和高亮。
     *
     * @return 函数关键字 PSI 元素,如果不存在则返回 null
     */
    val keyword: PsiElement? get() = null
}
