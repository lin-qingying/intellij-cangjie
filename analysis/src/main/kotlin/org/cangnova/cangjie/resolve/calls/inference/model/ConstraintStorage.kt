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

package org.cangnova.cangjie.resolve.calls.inference.model

import org.cangnova.cangjie.resolve.calls.inference.ForkPointData
import org.cangnova.cangjie.resolve.calls.inference.components.TypeApproximatorCachesPerConfiguration
import org.cangnova.cangjie.types.CangJieType
import org.cangnova.cangjie.types.checker.CangJieTypeChecker
import org.cangnova.cangjie.types.model.CangJieTypeMarker
import org.cangnova.cangjie.types.model.TypeCheckerProviderContext
import org.cangnova.cangjie.types.model.TypeConstructorMarker
import org.cangnova.cangjie.types.model.TypeVariableMarker


/**
 * ═══════════════════════════════════════════════════════════════════════════════
 * 约束存储系统说明
 * ═══════════════════════════════════════════════════════════════════════════════
 *
 * 类型变量的状态管理
 * ─────────────────────────────────────────────────────────────────────────────
 *
 * 每个类型变量可以处于以下状态之一：
 *
 * 1. 未固定状态（Not Fixed）
 *    - 该类型变量有若干约束（可能没有约束）
 *    - 在 notFixedTypeVariables 映射中存在对应的 VariableWithConstraints
 *    - 示例：T 有约束 T <: Number, Int <: T，但尚未确定具体类型
 *
 * 2. 已固定状态（Fixed）
 *    - 类型变量已固定为具体类型（可以是正确类型或非正确类型）
 *    - 在 notFixedTypeVariables 中不存在对应的 VariableWithConstraints
 *    - 必须保证其他 VariableWithConstraints 中不存在依赖此固定类型变量的约束
 *    - 示例：T 已固定为 Int
 *
 * 注意：fixedTypeVariables 可以包含正确类型和非正确类型。
 *
 * 类型变量固定流程（固定为正确类型）
 * ─────────────────────────────────────────────────────────────────────────────
 *
 * 步骤 1：确定固定顺序
 *    - 分析类型变量之间的依赖关系
 *    - 按照依赖顺序确定固定顺序
 *
 * 步骤 2：对每个类型变量执行以下操作
 *
 *    a) 确定结果类型
 *       - 根据约束推导出最合适的具体类型
 *       - 例如：从 Int <: T <: Number 推导出 T = Int
 *
 *    b) 添加相等性约束
 *       - 添加约束：T = Int
 *       - 这确保了类型变量与其固定类型的一致性
 *
 *    c) 运行约束合并
 *       - 执行约束合并过程
 *       - 生成所有新的派生约束
 *
 *    d) 从 notFixedTypeVariables 中移除
 *       - 移除类型变量 T 的 VariableWithConstraints
 *       - 标记该类型变量已完成固定
 *
 *    e) 清理依赖约束
 *       - 移除其他类型变量中所有包含 T 的约束
 *       - 确保不再有约束依赖已固定的类型变量
 *
 *    f) 添加到 fixedTypeVariables
 *       - 将结果类型添加到 fixedTypeVariables 映射
 *       - 记录 T -> Int 的映射关系
 *
 * 注意：固定为非正确类型的流程相同，唯一区别在于结果类型的确定方式。
 *
 * 示例：
 * ─────────────────────────────────────────────────────────────────────────────
 * ```
 * // 初始状态
 * notFixedTypeVariables = {
 *   T -> VariableWithConstraints(constraints = [Int <: T, T <: Number])
 * }
 *
 * // 固定过程
 * 1. 确定 T 的结果类型为 Int
 * 2. 添加约束 T = Int
 * 3. 运行约束合并
 * 4. 移除 T 的 VariableWithConstraints
 * 5. 清理其他变量中包含 T 的约束
 * 6. 添加到 fixedTypeVariables: T -> Int
 *
 * // 最终状态
 * notFixedTypeVariables = {}
 * fixedTypeVariables = { T -> Int }
 * ```
 *
 * ═══════════════════════════════════════════════════════════════════════════════
 */

/**
 * 约束存储接口
 *
 * 该接口定义了类型推导系统中约束存储的核心数据结构。
 * 它维护了类型变量、约束、错误等所有推导过程中的状态信息。
 */
interface ConstraintStorage {
    /**
     * 所有类型变量的映射
     *
     * 键：类型构造器（TypeConstructorMarker）
     * 值：对应的类型变量（TypeVariableMarker）
     *
     * 包含推导过程中涉及的所有类型变量，无论是否已固定。
     *
     * 示例：
     * ```
     * {
     *   T -> TypeVariable(T),
     *   U -> TypeVariable(U),
     *   V -> TypeVariable(V)
     * }
     * ```
     */
    val allTypeVariables: Map<TypeConstructorMarker, TypeVariableMarker>
    /**
     * For a type variable X (its type constructor) as a key, the map contains a set of type variables
     * that may have constraints referring to X (containing it inside the type).
     *
     * Mostly, this property is necessary for the sake of incorporation optimizations.
     *
     * Note that the resulting set might contain some false positives, i.e., there might be some variables that actually don't contain
     * the constraints containing the requested variable X. That situation might occur due to a situation
     * when constraints have been added and then removed during a transaction rollback.
     */
    val typeVariableDependencies: Map<TypeConstructorMarker, Set<TypeConstructorMarker>>
    val approximatorCaches: TypeApproximatorCachesPerConfiguration

    /**
     * 未固定的类型变量及其约束
     *
     * 键：类型构造器（TypeConstructorMarker）
     * 值：类型变量及其约束集合（VariableWithConstraints）
     *
     * 只包含尚未确定具体类型的类型变量。
     * 当类型变量被固定后，会从此映射中移除，并添加到 fixedTypeVariables 中。
     *
     * 示例：
     * ```
     * {
     *   T -> VariableWithConstraints(
     *     typeVariable = T,
     *     constraints = [Int <: T, T <: Number]
     *   )
     * }
     * ```
     */
    val notFixedTypeVariables: Map<TypeConstructorMarker, VariableWithConstraints>

    /**
     * 遗漏的约束列表
     *
     * 这是一个历史遗留问题的修复机制。
     * 在旧的优化实现中，某些约束被错误地跳过了。
     * 此列表记录了这些被跳过的约束，以便后续重新处理。
     *
     * 结构：List<Pair<位置信息, List<Pair<类型变量, 约束>>>>
     *
     * 每个元素包含：
     * - 约束的位置信息（IncorporationConstraintPosition）
     * - 该位置相关的类型变量和约束对列表
     *
     * 参见：ConstraintInjector.processMissedConstraints()
     */
    val missedConstraints: List<Pair<IncorporationConstraintPosition, List<Pair<TypeVariableMarker, Constraint>>>>

    /**
     * 初始约束列表
     *
     * 记录了推导过程开始时的所有初始约束。
     * 这些约束来自于：
     * - 函数调用的参数类型
     * - 返回值类型
     * - 显式类型注解
     * - 上下文类型信息
     *
     * 示例：
     * ```
     * [
     *   InitialConstraint(Int, T, LOWER, position1),  // Int <: T
     *   InitialConstraint(T, Number, UPPER, position2) // T <: Number
     * ]
     * ```
     */
    val initialConstraints: List<InitialConstraint>

    /**
     * 初始约束中的最大类型深度
     *
     * 用于限制类型推导的递归深度，防止无限递归。
     *
     * 类型深度的计算：
     * - 简单类型（如 Int）：深度 = 1
     * - 泛型类型（如 List<Int>）：深度 = 1 + max(参数深度)
     * - 嵌套泛型（如 List<List<Int>>）：深度 = 1 + 1 + 1 = 3
     *
     * 默认值：1（表示没有嵌套类型）
     */
    val maxTypeDepthFromInitialConstraints: Int

    /**
     * 约束系统中的错误列表
     *
     * 记录了推导过程中发现的所有错误，包括：
     * - 类型不匹配错误
     * - 约束冲突错误
     * - 无法推导的类型变量
     * - 循环依赖错误
     *
     * 这些错误会在推导完成后报告给用户。
     */
    val errors: List<ConstraintSystemError>

    /**
     * 是否存在矛盾约束
     *
     * 如果为 true，表示约束系统中存在无法同时满足的约束。
     *
     * 示例矛盾约束：
     * ```
     * T == Int
     * T == String
     * ```
     *
     * 当检测到矛盾时，推导过程会提前终止。
     */
    val hasContradiction: Boolean

    /**
     * 已固定的类型变量映射
     *
     * 键：类型构造器（TypeConstructorMarker）
     * 值：固定后的具体类型（CangJieTypeMarker）
     *
     * 记录了已经确定具体类型的类型变量。
     *
     * 示例：
     * ```
     * {
     *   T -> Int,
     *   U -> String,
     *   V -> List<Int>
     * }
     * ```
     *
     * 注意：此映射可以包含正确类型和非正确类型（错误恢复场景）。
     */
    val fixedTypeVariables: Map<TypeConstructorMarker, CangJieTypeMarker>

    /**
     * 延迟处理的类型变量列表
     *
     * 某些类型变量的推导需要延迟到后续阶段。
     * 常见场景：
     * - Lambda 表达式的类型推导
     * - 需要更多上下文信息的类型变量
     * - 依赖于其他未固定类型变量的变量
     */
    val postponedTypeVariables: List<TypeVariableMarker>

    /**
     * 为延迟参数构建的函数类型（按顶层类型变量索引）
     *
     * 键：Pair<顶层类型变量构造器, List<Pair<类型变量构造器, 参数位置>>>
     * 值：构建的函数类型（CangJieTypeMarker）
     *
     * 用于处理高阶函数和 Lambda 表达式的类型推导。
     * 当 Lambda 的类型依赖于多个类型变量时，需要记录这些依赖关系。
     */
    val builtFunctionalTypesForPostponedArgumentsByTopLevelTypeVariables: Map<Pair<TypeConstructorMarker, List<Pair<TypeConstructorMarker, Int>>>, CangJieTypeMarker>

    /**
     * 为延迟参数构建的函数类型（按期望类型变量索引）
     *
     * 键：类型变量构造器（TypeConstructorMarker）
     * 值：构建的函数类型（CangJieTypeMarker）
     *
     * 用于根据期望类型推导 Lambda 表达式的类型。
     *
     * 示例：
     * ```
     * val f: (Int) -> String = { ... }
     * // 期望类型变量 T 对应的函数类型为 (Int) -> String
     * ```
     */
    val builtFunctionalTypesForPostponedArgumentsByExpectedTypeVariables: Map<TypeConstructorMarker, CangJieTypeMarker>

    /**
     * 所有分支点的约束列表
     *
     * 记录了推导过程中所有分支点（Fork Point）产生的约束。
     *
     * 结构：List<Pair<约束位置, 分支点数据>>
     *
     * 分支点用于处理多种可能性的场景：
     * - 函数重载解析
     * - 类型推导的多个候选方案
     * - 条件类型推导
     *
     * 参见：ConstraintInjector 中关于 Fork Point 的详细说明
     */
    val constraintsFromAllForkPoints: List<Pair<IncorporationConstraintPosition, ForkPointData>>

    /**
     * 外部约束系统的变量前缀大小
     *
     * 在嵌套的类型推导场景中，内部推导可能依赖于外部推导的类型变量。
     *
     * 概念说明：
     * - 外部系统（Outer System）：调用点或其参数之外定义的类型变量集合
     * - 当某个候选的约束系统在外部约束系统的上下文中构建时，
     *   [allTypeVariables] 列表中的前 [outerSystemVariablesPrefixSize] 个变量属于外部系统
     *
     * 使用场景（非常有限）：
     * 1. 完成 `provideDelegate` 调用时
     *    - 将外部变量视为正确类型
     *    - 参见：fixInnerVariablesForProvideDelegateIfNeeded
     *
     * 2. 检查内部候选的变量一致性时
     *    - 参见：checkNotFixedTypeVariablesCountConsistency
     *
     * 示例：
     * ```
     * fun <T> outer(x: T) {
     *   // T 是外部变量
     *   inner { y -> ... }  // 内部推导可能依赖 T
     * }
     * ```
     *
     * 更多信息参见：docs/fir/delegated_property_inference.md
     */
    val outerSystemVariablesPrefixSize: Int

    /**
     * 是否使用外部约束系统
     *
     * 标识当前约束系统是否在外部约束系统的上下文中构建。
     *
     * - true：当前系统依赖外部系统的类型变量
     * - false：当前系统是独立的顶层推导
     */
    val usesOuterCs: Boolean

    /**
     * 空约束存储对象
     *
     * 提供一个不包含任何约束和类型变量的空实现。
     * 用于初始化或表示没有约束的场景。
     *
     * 所有集合属性返回空集合，所有标志属性返回默认值。
     */
    object Empty : ConstraintStorage {
        /** 空的类型变量映射 */
        override val allTypeVariables: Map<TypeConstructorMarker, TypeVariableMarker> get() = emptyMap()
        override val typeVariableDependencies: Map<TypeConstructorMarker, Set<TypeConstructorMarker>>
            get() = emptyMap()
        override val approximatorCaches: TypeApproximatorCachesPerConfiguration
            get() = mutableMapOf()

        /** 空的未固定类型变量映射 */
        override val notFixedTypeVariables: Map<TypeConstructorMarker, VariableWithConstraints> get() = emptyMap()

        /** 空的遗漏约束列表 */
        override val missedConstraints: List<Pair<IncorporationConstraintPosition, List<Pair<TypeVariableMarker, Constraint>>>> get() = emptyList()

        /** 空的初始约束列表 */
        override val initialConstraints: List<InitialConstraint> get() = emptyList()

        /** 默认最大类型深度为 1 */
        override val maxTypeDepthFromInitialConstraints: Int get() = 1

        /** 空的错误列表 */
        override val errors: List<ConstraintSystemError> get() = emptyList()

        /** 没有矛盾约束 */
        override val hasContradiction: Boolean get() = false

        /** 空的已固定类型变量映射 */
        override val fixedTypeVariables: Map<TypeConstructorMarker, CangJieTypeMarker> get() = emptyMap()

        /** 空的延迟类型变量列表 */
        override val postponedTypeVariables: List<TypeVariableMarker> get() = emptyList()

        /** 空的按顶层类型变量索引的函数类型映射 */
        override val builtFunctionalTypesForPostponedArgumentsByTopLevelTypeVariables: Map<Pair<TypeConstructorMarker, List<Pair<TypeConstructorMarker, Int>>>, CangJieTypeMarker> =
            emptyMap()

        /** 空的按期望类型变量索引的函数类型映射 */
        override val builtFunctionalTypesForPostponedArgumentsByExpectedTypeVariables: Map<TypeConstructorMarker, CangJieTypeMarker> =
            emptyMap()

        /** 空的分支点约束列表 */
        override val constraintsFromAllForkPoints: List<Pair<IncorporationConstraintPosition, ForkPointData>> =
            emptyList()

        /** 外部系统变量前缀大小为 0（没有外部变量） */
        override val outerSystemVariablesPrefixSize: Int get() = 0

        /** 不使用外部约束系统 */
        override val usesOuterCs: Boolean get() = false
    }
}

/**
 * 约束类型枚举
 *
 * 定义了类型推导系统中三种基本的约束类型。
 * 这些约束类型用于表达类型变量与具体类型之间的关系。
 *
 * 约束类型说明：
 * - LOWER（下界约束）：A <: T，表示 A 是 T 的子类型
 * - UPPER（上界约束）：T <: A，表示 T 是 A 的子类型
 * - EQUALITY（相等约束）：T == A，表示 T 与 A 是相同类型
 *
 * 示例：
 * ```
 * fun <T> example(x: T): Number {
 *   // 从参数推导：Int <: T  (LOWER 约束)
 *   // 从返回值推导：T <: Number  (UPPER 约束)
 *   // 如果显式指定：T == Int  (EQUALITY 约束)
 * }
 * example(42)  // 推导出 T = Int
 * ```
 */
enum class ConstraintKind {
    /**
     * 下界约束（Lower Bound）
     *
     * 表示形式：A <: T
     * 含义：A 是 T 的子类型，T 至少要是 A 类型
     *
     * 常见来源：
     * - 函数参数的实际类型
     * - 赋值语句的右侧类型
     *
     * 示例：
     * ```
     * fun <T> foo(x: T) { }
     * foo(42)  // 产生约束：Int <: T
     * ```
     */
    LOWER,

    /**
     * 上界约束（Upper Bound）
     *
     * 表示形式：T <: A
     * 含义：T 是 A 的子类型，T 最多是 A 类型
     *
     * 常见来源：
     * - 函数返回值的期望类型
     * - 泛型参数的类型边界
     * - 赋值语句的左侧类型
     *
     * 示例：
     * ```
     * fun <T> bar(): T { ... }
     * let x: Number = bar()  // 产生约束：T <: Number
     * ```
     */
    UPPER,

    /**
     * 相等约束（Equality）
     *
     * 表示形式：T == A
     * 含义：T 与 A 必须是完全相同的类型
     *
     * 常见来源：
     * - 显式类型注解
     * - 类型固定过程
     * - 多个约束合并后的结果
     *
     * 示例：
     * ```
     * fun <T> baz(x: T, y: T) { }
     * baz(1, 2)  // 产生约束：T == Int
     * ```
     */
    EQUALITY;

    /**
     * 检查当前约束类型是否为下界约束
     *
     * @return 如果是 LOWER 约束返回 true，否则返回 false
     *
     * 使用场景：
     * - 判断约束是否来自参数类型
     * - 确定类型变量的下界
     */
    fun isLower(): Boolean = this == LOWER

    /**
     * 检查当前约束类型是否为上界约束
     *
     * @return 如果是 UPPER 约束返回 true，否则返回 false
     *
     * 使用场景：
     * - 判断约束是否来自返回值类型
     * - 确定类型变量的上界
     */
    fun isUpper(): Boolean = this == UPPER

    /**
     * 检查当前约束类型是否为相等约束
     *
     * @return 如果是 EQUALITY 约束返回 true，否则返回 false
     *
     * 使用场景：
     * - 判断类型是否已完全确定
     * - 检查是否需要精确类型匹配
     */
    fun isEqual(): Boolean = this == EQUALITY

    /**
     * 获取当前约束类型的相反类型
     *
     * 转换规则：
     * - LOWER（A <: T）的相反是 UPPER（T <: A）
     * - UPPER（T <: A）的相反是 LOWER（A <: T）
     * - EQUALITY（T == A）的相反仍是 EQUALITY（A == T）
     *
     * @return 相反的约束类型
     *
     * 使用场景：
     * - 约束传播时反转约束方向
     * - 处理逆变和协变
     *
     * 示例：
     * ```
     * // 如果有约束 Int <: T (LOWER)
     * // 反转后得到 T <: Int (UPPER)
     * ```
     */
    fun opposite() = when (this) {
        LOWER -> UPPER
        UPPER -> LOWER
        EQUALITY -> EQUALITY
    }
}


/**
 * 约束类
 *
 * 表示类型推导系统中的一个具体约束。
 * 约束描述了类型变量与具体类型之间的关系。
 *
 * @property kind 约束类型（LOWER、UPPER 或 EQUALITY）
 * @property type 约束涉及的类型（允许灵活类型 flexible types）
 * @property position 约束的位置信息，用于错误报告和调试
 * @property typeHashCode 类型的哈希码，用于优化相等性比较
 * @property derivedFrom 此约束派生自哪些类型变量
 * @property inputTypePositionBeforeIncorporation 合并前的输入类型位置
 * @property isNoInfer 是否标记为 NoInfer（不参与类型推断）
 *
 * 关于 inputTypePositionBeforeIncorporation：
 * 此值为 true 表示约束形式为 `Nothing? <: Tv`，
 * 且该约束是在合并阶段从 `Kv? <: Tv` 形式的约束创建的（其中 Kv 是另一个类型变量）。
 *
 * 该参数的主要作用：
 * 1. 我们不将这类约束视为"正确的"约束（表示变量已准备好完成）
 * 2. K1 中有额外的逻辑，如果只有这类下界约束，不允许将变量固定为 `Nothing?`
 *
 * 关于 isNoInfer：
 * 当此值为 true 时，该约束不应该影响类型变量的最终推断结果。
 * 常用于标记不希望参与类型推断的约束，例如某些辅助函数的类型约束。
 *
 * 示例：
 * ```
 * // 约束：Int <: T
 * Constraint(
 *   kind = LOWER,
 *   type = Int,
 *   position = ...,
 *   derivedFrom = setOf(T)
 * )
 * ```
 */
class Constraint(
    val kind: ConstraintKind,
    val type: CangJieTypeMarker,
    val position: IncorporationConstraintPosition,
    val typeHashCode: Int = type.hashCode(),
    val derivedFrom: Set<TypeVariableMarker>,
    val inputTypePositionBeforeIncorporation: OnlyInputTypeConstraintPosition? = null,
    val isNoInfer: Boolean = false
) {
    /**
     * 判断两个约束是否相等
     *
     * 相等条件：
     * 1. 类型哈希码相同
     * 2. 约束类型相同
     * 3. 位置信息相同
     * 4. 类型对象相等
     *
     * 注意：为了性能优化，首先比较哈希码，然后才比较实际类型对象。
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other?.javaClass != javaClass) return false

        other as Constraint

        if (typeHashCode != other.typeHashCode) return false
        if (kind != other.kind) return false
        if (position != other.position) return false
        if (type != other.type) return false

        return true
    }

    /**
     * 返回约束的哈希码
     *
     * 使用类型的哈希码作为约束的哈希码，以优化集合操作性能。
     */
    override fun hashCode() = typeHashCode

    /**
     * 返回约束的字符串表示
     *
     * 格式：约束类型(类型) from 位置
     * 示例：LOWER(Int) from ArgumentConstraintPosition(0)
     */
    override fun toString() = "$kind($type) from $position"
}

/**
 * 带约束的类型变量接口
 *
 * 表示一个类型变量及其关联的所有约束。
 * 这是约束系统中的核心数据结构，用于跟踪每个类型变量的约束集合。
 *
 * 示例：
 * ```
 * // 类型变量 T 有以下约束：
 * VariableWithConstraints(
 *   typeVariable = T,
 *   constraints = [
 *     Constraint(LOWER, Int, ...),      // Int <: T
 *     Constraint(UPPER, Number, ...)    // T <: Number
 *   ]
 * )
 * ```
 */
interface VariableWithConstraints {
    /**
     * 类型变量
     *
     * 表示被约束的类型变量（如 T、U、V 等）。
     */
    val typeVariable: TypeVariableMarker

    /**
     * 约束列表
     *
     * 该类型变量的所有约束集合。
     * 包括：
     * - 初始约束（来自函数签名、参数类型等）
     * - 派生约束（通过约束合并产生）
     *
     * 约束列表用于：
     * 1. 推导类型变量的具体类型
     * 2. 检测约束冲突
     * 3. 确定类型变量的上下界
     */
    val constraints: List<Constraint>
    /**
     * Only necessary for incorporation optimization
     */
    fun getConstraintsContainedSpecifiedTypeVariable(typeVariableConstructor: TypeConstructorMarker): Collection< Constraint>

}

/**
 * 初始约束类
 *
 * 表示类型推导开始时的原始约束。
 * 初始约束来自于函数调用、赋值语句等源代码位置。
 *
 * @property a 约束的左侧类型
 * @property b 约束的右侧类型
 * @property constraintKind 约束类型（参见 [checkConstraint]）
 * @property position 约束的源代码位置
 *
 * 约束表示：
 * - EQUALITY: a == b（a 与 b 类型相等）
 * - LOWER: a :> b（a 是 b 的超类型，即 b <: a）
 * - UPPER: a <: b（a 是 b 的子类型）
 *
 * 示例：
 * ```
 * fun <T> foo(x: T): Number { return x }
 * foo(42)
 *
 * // 产生初始约束：
 * InitialConstraint(Int, T, LOWER, ...)     // Int <: T
 * InitialConstraint(T, Number, UPPER, ...)  // T <: Number
 * ```
 */
class InitialConstraint(
    val a: CangJieTypeMarker,
    val b: CangJieTypeMarker,
    val constraintKind: ConstraintKind,
    val position: ConstraintPosition
) {
    /**
     * 返回约束的完整字符串表示（包含位置信息）
     *
     * 格式：约束表达式 from 位置
     * 示例：Int <: T from ArgumentConstraintPosition(0)
     */
    override fun toString(): String = "${asStringWithoutPosition()} from $position"

    /**
     * 返回约束的字符串表示（不包含位置信息）
     *
     * 格式：a 符号 b
     * 符号说明：
     * - "==" 表示相等约束
     * - ":>" 表示下界约束（a 是 b 的超类型）
     * - "<:" 表示上界约束（a 是 b 的子类型）
     *
     * 示例：
     * - "Int <: T"（Int 是 T 的子类型）
     * - "T <: Number"（T 是 Number 的子类型）
     * - "T == Int"（T 等于 Int）
     */
    fun asStringWithoutPosition(): String {
        val sign =
            when (constraintKind) {
                ConstraintKind.EQUALITY -> "=="
                ConstraintKind.LOWER -> ":>"
                ConstraintKind.UPPER -> "<:"
            }
        return "$a $sign $b"
    }
}

//fun InitialConstraint.checkConstraint(substitutor: DefaultTypeSubstitutor): Boolean {
//    val newA = substitutor.substitute(a)
//    val newB = substitutor.substitute(b)
//    return checkConstraint(newB as CangJieTypeMarker, constraintKind, newA as CangJieTypeMarker)
//}

/**
 * 检查约束是否满足
 *
 * 验证给定的约束类型和结果类型之间是否满足指定的约束关系。
 * 使用 [CangJieTypeChecker] 来执行实际的类型检查。
 *
 * @param context 类型检查器提供者上下文（当前未使用，保留用于未来扩展）
 * @param constraintType 约束中的类型（约束的一侧）
 * @param constraintKind 约束类型（EQUALITY、LOWER 或 UPPER）
 * @param resultType 结果类型（约束的另一侧）
 * @return 如果约束满足返回 true，否则返回 false
 *
 * 约束检查规则：
 * - EQUALITY: 检查 constraintType 和 resultType 是否相等
 * - LOWER: 检查 constraintType 是否是 resultType 的子类型（constraintType <: resultType）
 * - UPPER: 检查 resultType 是否是 constraintType 的子类型（resultType <: constraintType）
 *
 * 示例：
 * ```
 * // 检查约束 Int <: T，其中 T 被推导为 Number
 * checkConstraint(context, Int, LOWER, Number)  // 返回 true
 *
 * // 检查约束 T <: Number，其中 T 被推导为 Int
 * checkConstraint(context, Int, UPPER, Number)  // 返回 true
 *
 * // 检查约束 T == Int，其中 T 被推导为 Int
 * checkConstraint(context, Int, EQUALITY, Int)  // 返回 true
 * ```
 *
 * 注意：
 * - 如果类型无法转换为 CangJieType，则默认返回 true（宽松处理）
 * - 这用于在类型固定后验证约束是否仍然满足
 */
@Suppress("UNUSED_PARAMETER")
fun checkConstraint(
    context: TypeCheckerProviderContext,
    constraintType: CangJieTypeMarker,
    constraintKind: ConstraintKind,
    resultType: CangJieTypeMarker
): Boolean {
    // 获取默认的类型检查器
    val typeChecker = CangJieTypeChecker.DEFAULT

    // 将 CangJieTypeMarker 转换为 CangJieType
    // 如果转换失败，返回 true（宽松处理，避免误报错误）
    val cType = constraintType as? CangJieType ?: return true
    val rType = resultType as? CangJieType ?: return true

    // 根据约束类型执行相应的类型检查
    return when (constraintKind) {
        ConstraintKind.EQUALITY -> typeChecker.equalTypes(cType, rType)
        ConstraintKind.LOWER -> typeChecker.isSubtypeOf(cType, rType)
        ConstraintKind.UPPER -> typeChecker.isSubtypeOf(rType, cType)
    }
}

/**
 * 替换约束中的类型
 *
 * 创建一个新的约束对象，使用新的类型替换原有类型，但保留其他所有属性。
 *
 * @param newType 新的类型
 * @return 新的约束对象，类型已被替换
 *
 * 使用场景：
 * - 类型替换（Type Substitution）
 * - 类型变量实例化
 * - 约束传播过程中的类型更新
 *
 * 示例：
 * ```
 * // 原约束：T <: Number
 * val originalConstraint = Constraint(UPPER, T, ...)
 *
 * // 将 T 替换为 Int
 * val newConstraint = originalConstraint.replaceType(Int)
 * // 新约束：Int <: Number
 * ```
 *
 * 注意：
 * - 保留原约束的 kind、position、typeHashCode、derivedFrom 等属性
 * - 只更新 type 字段
 * - 返回新对象，不修改原对象（不可变性）
 */
fun Constraint.replaceType(newType: CangJieTypeMarker) =
    Constraint(
        kind,
        newType,
        position,
        typeHashCode,
        derivedFrom,
        inputTypePositionBeforeIncorporation,
        isNoInfer
    )
