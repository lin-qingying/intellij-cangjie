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
package org.cangnova.cangjie.types.checker

import org.cangnova.cangjie.types.CangJieType
import org.cangnova.cangjie.types.TypeArgument

/**
 * 约束检查上下文
 *
 * 在类型检查过程中收集和处理嵌套约束。主要用于约束系统的类型推导，
 * 当检查类型关系时需要递归添加子约束。
 *
 * ## 使用场景
 *
 * ### 1. 递归添加子类型约束
 * 当检查 `List<T> <: List<String>` 时，需要递归检查 `T <: String`
 *
 * ### 2. 递归添加相等约束
 * 当检查 `Pair<T, U> == Pair<Int, String>` 时，需要递归检查：
 * - `T == Int`
 * - `U == String`
 *
 * ### 3. 处理类型捕获
 * 在类型推导中遇到需要捕获的类型参数时调用
 *
 * ### 4. 报告约束错误
 * 当类型关系不满足时，报告具体的约束错误
 *
 * ## 示例
 * ```kotlin
 * val context = object : ConstraintCheckContext {
 *     override fun addSubtypeConstraint(subtype: CangJieType, supertype: CangJieType) {
 *         // 将子类型约束添加到约束系统
 *     }
 * }
 *
 * val typeChecker = CangJieTypeChecker.DEFAULT
 * typeChecker.checkSubtypeConstraint(type1, type2, context)
 * ```
 */
interface ConstraintCheckContext {
    /**
     * 添加子类型约束
     *
     * 当类型检查过程中发现需要添加嵌套的子类型约束时调用。
     * 例如检查 `List<T> <: List<String>` 时会调用此方法添加 `T <: String` 约束。
     *
     * @param subtype 子类型
     * @param supertype 父类型
     */
    fun addSubtypeConstraint(subtype: CangJieType, supertype: CangJieType)

    /**
     * 添加相等约束
     *
     * 当类型检查过程中发现需要添加嵌套的相等约束时调用。
     * 例如检查 `Pair<T, U> == Pair<Int, String>` 时会调用此方法添加：
     * - `T == Int`
     * - `U == String`
     *
     * @param type1 第一个类型
     * @param type2 第二个类型
     */
    fun addEqualityConstraint(type1: CangJieType, type2: CangJieType)

    /**
     * 尝试捕获类型参数
     *
     * 当遇到需要捕获的类型参数时调用。类型捕获用于处理泛型类型的特殊情况。
     *
     * ## 仓颉语言特性
     * 由于仓颉语言不支持类型投影（in/out variance），类型捕获相对简化：
     * - 只处理不变类型参数
     * - 不需要区分协变/逆变捕获
     *
     * @param type 包含类型变量的类型
     * @param typeArgument 待捕获的类型参数
     * @return true 如果成功捕获，false 如果不需要或无法捕获
     */
    fun tryCaptureTypeArgument(type: CangJieType, typeArgument: TypeArgument): Boolean

    /**
     * 报告约束错误
     *
     * 当类型约束不满足时调用，向约束系统报告错误。
     * 错误信息将包含约束位置等上下文信息。
     */
    fun reportConstraintError()
}
