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

package org.cangnova.cangjie.resolve.calls.components

import org.cangnova.cangjie.resolve.calls.inference.ConstraintSystemBuilder
import org.cangnova.cangjie.resolve.calls.inference.model.ConstraintStorage
import org.cangnova.cangjie.resolve.calls.inference.model.VariableWithConstraints
import org.cangnova.cangjie.types.model.*

/**
 * 延迟参数分析器上下文 (Postponed Arguments Analyzer Context)
 *
 * 这个接口定义了分析延迟参数所需的上下文环境。在类型推导过程中,某些参数(如 lambda 表达式)
 * 的类型分析会被延迟到获得更多类型信息后再进行,以提供更好的类型推导体验。
 *
 * ## 延迟参数的概念
 * 延迟参数是指在初始解析阶段无法完全确定类型的参数,主要包括:
 * - **Lambda 表达式**: 参数类型可能需要从上下文推导
 * - **可调用引用**: 需要根据期望类型选择重载
 * - **复杂表达式**: 类型依赖于其他类型变量的解析结果
 *
 * ## 工作原理
 * 1. **初始阶段**: 为延迟参数创建类型变量,生成初步约束
 * 2. **固定阶段**: 根据其他约束固定某些类型变量
 * 3. **分析阶段**: 使用固定的类型信息分析延迟参数
 * 4. **完成阶段**: 整合延迟参数的约束,完成整体类型推导
 *
 * ## 示例场景
 * ```kotlin
 * fun <T> foo(x: T, block: (T) -> Unit) { ... }
 *
 * foo(42) { it ->  // lambda 的参数类型需要推导
 *     println(it)  // 需要知道 T = Int 才能确定 it 的类型
 * }
 * ```
 * 在这个例子中,lambda 的分析会被延迟,直到从第一个参数推导出 T = Int。
 *
 * @see TypeSystemInferenceExtensionContext 类型系统推断扩展上下文
 * @see ConstraintSystemBuilder 约束系统构建器
 * @see VariableWithConstraints 带约束的类型变量
 */
interface PostponedArgumentsAnalyzerContext : TypeSystemInferenceExtensionContext {

    /**
     * 未固定的类型变量映射
     *
     * 存储当前还未确定具体类型的类型变量及其约束信息。
     * 键是类型构造器(代表类型变量),值是该变量的所有约束集合。
     *
     * 类型变量在以下情况会保持"未固定"状态:
     * - 约束不足以唯一确定类型
     * - 变量依赖于其他未固定的变量
     * - 分析延迟参数需要该变量保持灵活性
     *
     * 示例:
     * ```
     * fun <T> example(x: T, block: (T) -> Unit)
     * example(???) { it -> ... }
     * ```
     * 在分析 lambda 之前,T 会作为未固定变量存在于此映射中。
     */
    val notFixedTypeVariables: Map<TypeConstructorMarker, VariableWithConstraints>

    /**
     * 构建当前替换器
     *
     * 创建一个类型替换器,将类型变量替换为已知的具体类型。
     * 这个替换器会综合考虑:
     * 1. 已固定的类型变量 → 它们的固定类型
     * 2. 额外绑定参数提供的映射
     * 3. 未固定的类型变量 → 保持不变或使用占位类型
     *
     * @param additionalBindings 额外的类型变量到类型的绑定,
     *                          用于临时假设某些变量的类型(如在分析 lambda 时)
     * @return 类型替换器,可用于将类型变量应用到表达式上
     */
    fun buildCurrentSubstitutor(additionalBindings: Map<TypeConstructorMarker, CangJieTypeMarker>): TypeSubstitutorMarker

    /**
     * 构建未固定变量到存根类型的替换器
     *
     * 为所有未固定的类型变量创建存根类型(stub type)作为占位符。
     * 存根类型是一种特殊的类型,表示"这里有一个类型但我们还不知道它是什么"。
     *
     * 使用场景:
     * - 在分析延迟参数前,需要为未知类型提供占位符
     * - 在错误恢复中,提供临时类型以继续分析
     * - 在诊断信息中,显示哪些类型还未确定
     *
     * @return 将未固定类型变量映射到存根类型的替换器
     */
    fun buildNotFixedVariablesToStubTypesSubstitutor(): TypeSubstitutorMarker

    /**
     * 获取延迟变量的存根类型绑定
     *
     * 返回一个映射,将每个未固定的类型变量关联到对应的存根类型。
     * 这与 [buildNotFixedVariablesToStubTypesSubstitutor] 类似,但返回的是映射而非替换器。
     *
     * @return 类型变量到存根类型的映射
     */
    fun bindingStubsForPostponedVariables(): Map<TypeVariableMarker, StubTypeMarker>

    /**
     * 判断类型是否可以是确定的 (Proper Type)
     *
     * 在类型推导中,"确定的类型"(proper type)是指不包含未固定类型变量的类型。
     * 例如:
     * - `Int` 是确定的
     * - `List<String>` 是确定的
     * - `T` (未固定的类型变量)不是确定的
     * - `List<T>` (包含未固定变量)不是确定的
     *
     * 这个判断对于决定是否可以完成类型推导很重要。
     *
     * @param type 要检查的类型
     * @return 如果类型不包含未固定的类型变量则返回 true
     */
    fun canBeProper(type: CangJieTypeMarker): Boolean

    /**
     * 检查类型是否有上界或等于 Unit 的约束
     *
     * 判断给定类型是否被约束为 Unit 类型或其子类型。
     * 这在推导 lambda 返回类型时特别有用:
     * - 如果约束为 Unit,lambda 体不需要返回值
     * - 如果约束不是 Unit,可能需要推导返回值类型
     *
     * @param type 要检查的类型
     * @return 如果类型有 Unit 约束则返回 true
     */
    fun hasUpperOrEqualUnitConstraint(type: CangJieTypeMarker): Boolean

    /**
     * 从约束中移除延迟的类型变量
     *
     * 当延迟参数分析完成后,某些类型变量可能不再需要。
     * 此方法从约束系统中清理这些变量,以简化后续处理。
     *
     * 使用场景:
     * - Lambda 分析完成后,移除 lambda 内部引入的类型变量
     * - 嵌套调用解析完成后,清理临时变量
     *
     * @param postponedTypeVariables 要移除的类型变量的构造器集合
     */
    fun removePostponedTypeVariablesFromConstraints(postponedTypeVariables: Set<TypeConstructorMarker>)

    // === 可变操作 (Mutable Operations) ===

    /**
     * 添加其他约束系统的约束
     *
     * 将另一个约束系统的所有约束合并到当前系统中。
     * 用于整合延迟参数分析产生的新约束。
     *
     * 典型流程:
     * 1. 创建临时约束系统分析 lambda
     * 2. 分析完成后,调用此方法合并约束
     * 3. 继续整体的类型推导
     *
     * @param otherSystem 要合并的约束存储
     */
    fun addOtherSystem(otherSystem: ConstraintStorage)

    /**
     * 获取约束系统构建器
     *
     * 返回用于添加新约束的构建器实例。
     * 在分析延迟参数时,可能需要添加新的约束(如 lambda 参数的类型约束)。
     *
     * @return 约束系统构建器
     */
    fun getBuilder(): ConstraintSystemBuilder

    /**
     * 解析分叉点约束
     *
     * 在类型推导过程中,某些约束可能产生多个可能的解。
     * 分叉点(fork point)记录了这些选择点。
     * 此方法尝试解析这些分叉,选择最合适的类型。
     *
     * 分叉点的例子:
     * - 类型变量可以是 Int 或 String
     * - 类型参数可以是 List<T> 或 Array<T>
     *
     * 解析策略通常是:
     * - 选择最具体的类型
     * - 如果无法决定,保持未确定状态
     */
    fun resolveForkPointsConstraints()
}
