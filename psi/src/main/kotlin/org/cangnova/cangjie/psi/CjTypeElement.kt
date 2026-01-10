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

import com.intellij.util.ArrayFactory

/**
 * 仓颉语言中类型元素的基础接口
 *
 * 类型元素表示源代码中所有表示类型的语法结构,是类型系统在 PSI 树中的表示。
 * 所有具体的类型语法元素都应实现该接口。
 *
 * ## 类型元素的种类
 * 仓颉语言中的类型元素包括:
 * - **用户类型** (CjUserType): 类、接口、结构体等用户定义的类型
 *   - 例如: `String`, `ArrayList<Int64>`, `Result<T, E>`
 * - **函数类型** (CjFunctionType): 函数类型表示
 *   - 例如: `(Int64, Int64) -> Int64`, `() -> Unit`
 * - **可空类型** (CjNullableType): 可空类型
 *   - 例如: `String?`, `Int64?`
 * - **元组类型** (CjTupleType): 元组类型
 *   - 例如: `(Int64, String, Bool)`
 * - **动态类型** (CjDynamicType): 动态类型 `dynamic`
 * - **类型投影** (CjTypeProjection): 泛型类型参数
 *   - 例如: `out T`, `in E`, `*`
 *
 * ## 类型参数
 * 类型元素可以拥有类型参数(泛型参数),通过 [typeArgumentsAsTypes] 属性访问。
 * 例如,在 `ArrayList<String>` 中,`String` 就是类型参数。
 *
 * ## 源代码示例
 * ```cangjie
 * // 用户类型
 * let name: String = "Alice"
 * let numbers: ArrayList<Int64> = ArrayList()
 *
 * // 函数类型
 * let callback: (Int64) -> String = { x => x.toString() }
 * let predicate: (T) -> Bool = { item => item.isValid() }
 *
 * // 可空类型
 * let optionalValue: String? = null
 * let maybeNumber: Int64? = 42
 *
 * // 元组类型
 * let pair: (Int64, String) = (1, "one")
 * let triple: (Bool, Int64, String) = (true, 1, "one")
 *
 * // 泛型类型
 * class Box<T> {
 *     let value: T
 * }
 *
 * // 类型投影
 * func process(list: ArrayList<out Number>) { }
 * func add(collection: MutableList<in Int64>) { }
 * ```
 *
 * ## 使用示例
 *
 * ### 示例 1: 检查类型是否有类型参数
 * ```kotlin
 * fun hasTypeArguments(typeElement: CjTypeElement): Boolean {
 *     return typeElement.typeArgumentsAsTypes.isNotEmpty()
 * }
 * ```
 *
 * ### 示例 2: 获取泛型类型的参数
 * ```kotlin
 * fun getTypeArguments(typeElement: CjTypeElement): List<String> {
 *     return typeElement.typeArgumentsAsTypes.map { it.text }
 * }
 * // 例如: ArrayList<String, Int64> -> ["String", "Int64"]
 * ```
 *
 * ### 示例 3: 判断类型种类
 * ```kotlin
 * fun analyzeType(typeElement: CjTypeElement) {
 *     when (typeElement) {
 *         is CjUserType -> println("用户定义类型: ${typeElement.referencedName}")
 *         is CjFunctionType -> println("函数类型")
 *         is CjNullableType -> println("可空类型")
 *         is CjTupleType -> println("元组类型")
 *         else -> println("其他类型")
 *     }
 * }
 * ```
 *
 * ## 与 CjTypeReference 的关系
 * - **CjTypeElement**: 类型本身的语法表示 (如 `String`, `Int64?`)
 * - **CjTypeReference**: 对类型的引用,包含类型元素和相关注解
 *
 * 在语法树中:
 * ```
 * CjTypeReference (类型引用)
 *   ├─ annotations (类型注解,如 @Nullable)
 *   └─ typeElement: CjTypeElement (实际的类型)
 * ```
 *
 * @see CjTypeReference 类型引用
 * @see CjUserType 用户定义类型
 * @see CjFunctionType 函数类型
 * @see CjNullableType 可空类型
 */
interface CjTypeElement : CjElement {
    /**
     * 获取该类型的类型参数列表
     *
     * 返回该类型元素的所有类型参数(泛型参数)。
     * 例如,对于 `Map<String, Int64>`,会返回包含 `String` 和 `Int64` 两个类型引用的列表。
     *
     * **注意**: 这是类型参数的语义表示,而非语法表示。
     * 语法表示应通过 `CjTypeArgumentList` 访问。
     *
     * @return 类型参数列表,如果没有类型参数则返回空列表
     * @see CjTypeReference
     */
    val typeArgumentsAsTypes: List<CjTypeReference> get() = emptyList()

    companion object {
        /**
         * 空类型元素数组常量
         *
         * 用于避免重复创建空数组,提升性能。
         */
        @JvmStatic
        val EMPTY_ARRAY: Array<CjTypeElement?> = arrayOfNulls(0)

        /**
         * 类型元素数组工厂
         *
         * 用于创建指定大小的类型元素数组。
         * 在需要批量创建类型元素数组时使用,例如在集合操作中。
         */
        val ARRAY_FACTORY: ArrayFactory<CjTypeElement?> =
            ArrayFactory { count: Int -> if (count == 0) EMPTY_ARRAY else arrayOfNulls(count) }
    }
}
