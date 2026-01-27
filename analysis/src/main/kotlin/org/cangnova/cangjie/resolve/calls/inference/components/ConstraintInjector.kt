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

package org.cangnova.cangjie.resolve.calls.inference.components

import com.intellij.util.SmartList
import org.cangnova.cangjie.config.LanguageVersionSettings
import org.cangnova.cangjie.resolve.calls.inference.ConstraintSystemOperation
import org.cangnova.cangjie.resolve.calls.inference.ForkPointBranchDescription
import org.cangnova.cangjie.resolve.calls.inference.ForkPointData
import org.cangnova.cangjie.resolve.calls.inference.model.*
import org.cangnova.cangjie.types.AbstractTypeApproximator
import org.cangnova.cangjie.types.AbstractTypeChecker
import org.cangnova.cangjie.types.CangJieType
import org.cangnova.cangjie.types.TypeApproximatorConfiguration
import org.cangnova.cangjie.types.TypeCheckerState
import org.cangnova.cangjie.types.model.*
import org.cangnova.cangjie.utils.addIfNotNull
import org.cangnova.cangjie.utils.popLast
import kotlin.math.max

/**
 * ═══════════════════════════════════════════════════════════════════════════════
 * 约束系统工作流程详解
 * ═══════════════════════════════════════════════════════════════════════════════
 *
 * 约束系统是仓颉语言类型推断的核心机制，负责收集、处理和求解类型约束。
 * 本文件实现的 ConstraintInjector（约束注入器）是约束系统的入口组件。
 *
 * ┌─────────────────────────────────────────────────────────────────────────────┐
 * │ 一、约束系统的整体架构                                                        │
 * └─────────────────────────────────────────────────────────────────────────────┘
 *
 * 约束系统由以下核心组件组成：
 *
 *   1. ConstraintInjector（约束注入器）- 本文件
 *      职责：接收初始约束，注入到约束系统中
 *      输入：初始类型约束（相等性约束、子类型约束）
 *      输出：处理后的约束集合
 *
 *   2. ConstraintIncorporator（约束合并器）
 *      职责：合并和传播约束，推导出新的约束
 *      输入：单个约束
 *      输出：派生的新约束
 *
 *   3. TypeCheckerState（类型检查器状态）
 *      职责：维护类型检查过程中的状态信息
 *      存储：类型变量、约束集合、错误信息
 *
 *   4. ConstraintSystem（约束系统）
 *      职责：管理整个约束求解过程
 *      功能：约束收集、约束求解、类型变量固定
 *
 * ┌─────────────────────────────────────────────────────────────────────────────┐
 * │ 二、约束的生命周期                                                            │
 * └─────────────────────────────────────────────────────────────────────────────┘
 *
 * 约束从产生到求解经历以下阶段：
 *
 *   阶段 1：约束产生
 *   ─────────────────────────────────────────────────────────────────────────
 *   来源：
 *   - 函数调用：实参类型 <: 形参类型
 *   - 变量赋值：右值类型 <: 左值类型
 *   - 返回语句：返回值类型 <: 函数返回类型
 *   - 类型注解：推断类型 == 注解类型
 *
 *   示例：
 *   ```cangjie
 *   func foo<T>(x: T): T { return x }
 *   let result = foo(42)  // 产生约束：Int <: T, T <: 推断类型
 *   ```
 *
 *   阶段 2：约束注入（ConstraintInjector）
 *   ─────────────────────────────────────────────────────────────────────────
 *   操作：
 *   - 接收初始约束
 *   - 验证约束合法性
 *   - 更新类型深度限制
 *   - 调用约束合并器
 *
 *   关键方法：
 *   - addInitialEqualityConstraint()  // 添加相等性约束
 *   - addInitialSubtypeConstraint()   // 添加子类型约束
 *
 *   阶段 3：约束合并（ConstraintIncorporator）
 *   ─────────────────────────────────────────────────────────────────────────
 *   操作：
 *   - 根据现有约束推导新约束
 *   - 传播约束到相关类型变量
 *   - 检测约束冲突
 *
 *   推导规则示例：
 *   - 如果 T <: A 且 A <: B，则推导 T <: B（传递性）
 *   - 如果 T == A 且 A <: B，则推导 T <: B（替换）
 *   - 如果 T <: Option<A>，则推导 T 的内部类型约束
 *
 *   阶段 4：约束处理（processConstraints）
 *   ─────────────────────────────────────────────────────────────────────────
 *   操作：
 *   - 循环处理所有待处理约束
 *   - 跳过冗余约束
 *   - 收集错过的约束
 *   - 处理分支点约束
 *
 *   阶段 5：约束求解（ConstraintSystem）
 *   ─────────────────────────────────────────────────────────────────────────
 *   操作：
 *   - 固定类型变量的具体类型
 *   - 检查约束一致性
 *   - 报告类型错误
 *
 * ┌─────────────────────────────────────────────────────────────────────────────┐
 * │ 三、约束的种类                                                                │
 * └─────────────────────────────────────────────────────────────────────────────┘
 *
 *   1. 相等性约束（EQUALITY）
 *      形式：T == A
 *      含义：类型变量 T 必须等于类型 A
 *      示例：let x: Int = getValue<T>()  // T == Int
 *
 *   2. 上界约束（UPPER）
 *      形式：T <: A
 *      含义：类型变量 T 必须是类型 A 的子类型
 *      示例：func foo<T: Number>(x: T)  // T <: Number
 *
 *   3. 下界约束（LOWER）
 *      形式：A <: T
 *      含义：类型 A 必须是类型变量 T 的子类型
 *      示例：func bar<T>(x: T) { let y: T = x }  // x的类型 <: T
 *
 * ┌─────────────────────────────────────────────────────────────────────────────┐
 * │ 四、分支点（Fork Point）机制                                                  │
 * └─────────────────────────────────────────────────────────────────────────────┘
 *
 * 分支点用于处理类型推断中的多种可能性，例如重载解析。
 *
 *   工作流程：
 *   1. 遇到多个候选项（如重载函数）
 *   2. 为每个候选项创建一个分支
 *   3. 在每个分支中独立收集约束
 *   4. 选择约束一致的分支
 *   5. 应用选中分支的约束
 *
 *   示例：
 *   ```cangjie
 *   func process(x: Int): String { ... }      // 候选 1
 *   func process(x: String): Int { ... }      // 候选 2
 *
 *   let result = process(getValue<T>())
 *   // 分支 1：T == Int, result: String
 *   // 分支 2：T == String, result: Int
 *   ```
 *
 *   实现细节：
 *   - runForkingPoint()：创建分支点
 *   - fork()：创建单个分支
 *   - 约束栈：stackForConstraintsSetsFromCurrentForkPoint
 *   - 分支约束：stackForConstraintSetFromCurrentForkPointBranch
 *
 * ┌─────────────────────────────────────────────────────────────────────────────┐
 * │ 五、类型深度限制                                                              │
 * └─────────────────────────────────────────────────────────────────────────────┘
 *
 * 为了防止无限递归和性能问题，约束系统限制了类型的嵌套深度。
 *
 *   深度计算：
 *   - 简单类型（Int, String）：深度 = 0
 *   - 泛型类型（List<T>）：深度 = 1 + T的深度
 *   - 嵌套类型（List<List<T>>）：深度 = 2 + T的深度
 *
 *   限制规则：
 *   - 初始约束：记录最大深度 maxTypeDepthFromInitialConstraints
 *   - 合并约束：允许深度 = 初始深度 + ALLOWED_DEPTH_DELTA_FOR_INCORPORATION
 *   - 超过限制：跳过该约束，避免无限递归
 *
 *   示例：
 *   ```cangjie
 *   // 初始约束：T <: List<Int>，深度 = 1
 *   // 允许合并：T <: List<List<Int>>，深度 = 2（1 + 1）
 *   // 禁止合并：T <: List<List<List<Int>>>，深度 = 3（超过限制）
 *   ```
 *
 * ┌─────────────────────────────────────────────────────────────────────────────┐
 * │ 六、错过的约束（Missed Constraints）                                          │
 * └─────────────────────────────────────────────────────────────────────────────┘
 *
 * 在旧的约束处理系统中，某些优化可能导致约束被错误跳过。
 * 新系统通过 ProperTypeInferenceConstraintsProcessing 特性修复了这个问题。
 *
 *   产生原因：
 *   - 过早的优化：假设某些约束已隐含
 *   - 不完整的传播：约束未传播到所有相关类型变量
 *
 *   处理方式：
 *   - 收集错过的约束
 *   - 在约束处理完成后补充处理
 *   - 新系统中已不再产生错过的约束
 *
 * ┌─────────────────────────────────────────────────────────────────────────────┐
 * │ 七、完整的类型推断流程示例                                                     │
 * └─────────────────────────────────────────────────────────────────────────────┘
 *
 * 示例代码：
 * ```cangjie
 * func identity<T>(x: T): T { return x }
 * let result = identity(42)
 * ```
 *
 * 推断流程：
 *
 *   步骤 1：创建类型变量
 *   ─────────────────────────────────────────────────────────────────────────
 *   - 为泛型参数 T 创建类型变量 T'
 *
 *   步骤 2：收集初始约束
 *   ─────────────────────────────────────────────────────────────────────────
 *   - 实参约束：Int <: T'（42 的类型是 Int）
 *   - 返回值约束：T' <: result的类型
 *
 *   步骤 3：注入约束（ConstraintInjector）
 *   ─────────────────────────────────────────────────────────────────────────
 *   - addInitialSubtypeConstraint(Int, T')
 *   - 更新类型深度：maxTypeDepthFromInitialConstraints = 0
 *
 *   步骤 4：合并约束（ConstraintIncorporator）
 *   ─────────────────────────────────────────────────────────────────────────
 *   - 从 Int <: T' 推导：T' 的下界 = Int
 *   - 从 T' <: result的类型 推导：result的类型的上界 = T'
 *
 *   步骤 5：处理约束（processConstraints）
 *   ─────────────────────────────────────────────────────────────────────────
 *   - 循环处理所有约束
 *   - 检查约束一致性
 *   - 无冲突，继续
 *
 *   步骤 6：固定类型变量
 *   ─────────────────────────────────────────────────────────────────────────
 *   - T' = Int（根据下界）
 *   - result的类型 = Int（根据返回值类型）
 *
 *   步骤 7：完成推断
 *   ─────────────────────────────────────────────────────────────────────────
 *   - 最终结果：result: Int
 *
 * ┌─────────────────────────────────────────────────────────────────────────────┐
 * │ 八、与 Kotlin 的差异                                                          │
 * └─────────────────────────────────────────────────────────────────────────────┘
 *
 * 虽然本实现借鉴了 Kotlin 的约束系统，但针对仓颉语言做了以下调整：
 *
 *   1. 可空性处理
 *      - Kotlin：使用 Nullability（NULL, NOT_NULL, UNKNOWN）
 *      - 仓颉：使用 OptionStatus（DEFINITE, OPTION, UNKNOWN）
 *      - 原因：仓颉没有 null，使用 Option<T> 枚举表示可选值
 *
 *   2. 类型系统
 *      - Kotlin：支持平台类型（Java 互操作）
 *      - 仓颉：纯粹的类型系统，无平台类型
 *
 *   3. 约束处理
 *      - Kotlin：需要处理 Java 泛型的通配符
 *      - 仓颉：简化的泛型系统，无通配符
 *
 * ═══════════════════════════════════════════════════════════════════════════════
 */

/**
 * 获取仓颉类型的类型构造器
 *
 * @param context 类型系统上下文
 * @return 类型构造器标记
 */
fun CangJieTypeMarker.typeConstructor(context: TypeSystemContext): TypeConstructorMarker =
    with(context) { typeConstructor() }
typealias TypeApproximatorCachesPerConfiguration = MutableMap<TypeApproximatorConfiguration, AbstractTypeApproximator.Cache>

/**
 * 约束注入器
 *
 * 负责将类型约束注入到约束系统中，并通过约束合并器进行处理。
 * 这是类型推断系统的核心组件之一，用于管理约束的添加、处理和合并。
 *
 * 主要功能：
 * - 处理初始约束（相等性约束和子类型约束）
 * - 管理约束的合并和传播
 * - 处理分支点（fork point）约束
 * - 维护类型深度限制以避免无限递归
 * - 处理错过的约束和错误报告
 *
 * @property constraintIncorporator 约束合并器，用于合并和处理约束
 * @property typeApproximator 类型近似器，用于类型近似计算
 * @property languageVersionSettings 语言版本设置，控制特性开关
 */
class ConstraintInjector(
    val constraintIncorporator: ConstraintIncorporator,
    val typeApproximator: AbstractTypeApproximator,
    private val languageVersionSettings: LanguageVersionSettings,
    inferenceLoggerParameter: InferenceLogger? = null,

    ) {
    /**
     * 约束合并时允许的最大类型深度增量
     *
     * 用于防止类型深度在约束合并过程中无限增长
     */
    private val ALLOWED_DEPTH_DELTA_FOR_INCORPORATION = 1
    val inferenceLogger = inferenceLoggerParameter.takeIf { it !is InferenceLogger.Dummy }

    /**
     * 处理给定分支点的约束
     *
     * 当约束系统在分支点（fork point）选择某个分支后，
     * 需要处理该分支产生的所有约束。
     *
     * @param c 约束系统上下文
     * @param constraintSet 该分支产生的约束集合
     * @param position 约束合并位置
     */
    context(c: Context)

    fun processGivenForkPointBranchConstraints(
        constraintSet: Collection<Pair<TypeVariableMarker, Constraint>>,
        position: IncorporationConstraintPosition
    ) {

        with(TypeCheckerStateForConstraintInjector(c, position)) {
            processGivenConstraints(constraintSet)
                processConstraintsIgnoringForksData()
        }
    }


    /**
     * 通过子类型关系添加初始相等性约束
     *
     * 对于某些类型（如可选类型），相等性约束通过双向子类型约束实现：
     * - 添加 a <: b
     * - 添加 b <: a
     *
     * 这确保了类型的完全相等性。
     *
     * @param a 第一个类型
     * @param b 第二个类型
     * @param typeCheckerState 类型检查器状态
     */
    context(c: Context, typeCheckerState: TypeCheckerStateForConstraintInjector)

    private fun  addInitialEqualityConstraintThroughSubtyping(
        a: CangJieTypeMarker,
        b: CangJieTypeMarker,
    ) {
        updateAllowedTypeDepth(a)
        updateAllowedTypeDepth(b)
        addSubTypeConstraintAndIncorporateIt(a, b)
        addSubTypeConstraintAndIncorporateIt(b, a)
    }


    /**
     * 添加初始相等性约束
     *
     * 当需要约束两个类型相等时（如 T == Int），此方法将创建并添加相等性约束。
     *
     * 处理流程：
     * 1. 确定哪个是类型变量，哪个是具体类型
     * 2. 创建初始约束并记录
     * 3. 根据类型特性选择合适的约束添加方式：
     *    - 对于可选类型或复杂类型，使用双向子类型约束
     *    - 对于简单类型，直接添加相等性约束
     *
     * @param c 约束系统上下文
     * @param a 第一个类型（可能是类型变量）
     * @param b 第二个类型（可能是类型变量）
     * @param position 约束位置
     */
    context(c: Context)
    fun addInitialEqualityConstraint(

        a: CangJieTypeMarker,
        b: CangJieTypeMarker,
        position: ConstraintPosition
    ) = with(c) {
        // 确定哪个是类型变量，哪个是等式右边的类型
        val (typeVariable, equalType) = when {
            a.typeConstructor(c) is TypeVariableTypeConstructorMarker -> a to b
            b.typeConstructor(c) is TypeVariableTypeConstructorMarker -> b to a
            else -> return
        }

        // 创建并记录初始约束
        val initialConstraint = InitialConstraint(typeVariable, equalType, ConstraintKind.EQUALITY, position).also {
            c.addInitialConstraint(it)
        }
        inferenceLogger?.logInitial(initialConstraint, c)

        with(TypeCheckerStateForConstraintInjector(c, IncorporationConstraintPosition(initialConstraint))) {
            // 对于可选类型或复杂类型（如 T? == Foo!），使用旧的方式（通过双向子类型约束）添加约束
            if (!typeVariable.isSimpleType() || typeVariable.isOptionType()) {
                inferenceLogger.withOrigin(initialConstraint) {
                    addInitialEqualityConstraintThroughSubtyping(typeVariable, equalType)
                }
                return
            }

            updateAllowedTypeDepth(equalType)
            inferenceLogger.withOrigin(initialConstraint) {
                addEqualityConstraintAndIncorporateIt(typeVariable, equalType)
            }
        }


    }

    /**
     * 添加相等性约束并合并
     *
     * 将相等性约束添加到类型检查器状态中，然后处理所有产生的约束。
     *
     * @param c 约束系统上下文
     * @param typeVariable 类型变量
     * @param equalType 与类型变量相等的类型
     * @param typeCheckerState 类型检查器状态
     */
    context(c: Context, typeCheckerState: TypeCheckerStateForConstraintInjector)

    private fun addEqualityConstraintAndIncorporateIt(
        typeVariable: CangJieTypeMarker,
        equalType: CangJieTypeMarker,
    ) {
        typeCheckerState.setConstrainingTypesToPrintDebugInfo(typeVariable, equalType)
        typeCheckerState.addEqualityConstraint(typeVariable.typeConstructor(c), equalType)

        processConstraints()
    }

    /**
     * 更新允许的类型深度
     *
     * 跟踪初始约束中出现的最大类型深度，用于限制约束合并过程中的类型深度。
     *
     * @param c 约束系统上下文
     * @param initialType 初始类型
     */
    context(c: Context)
    private fun updateAllowedTypeDepth(initialType: CangJieTypeMarker) = with(c) {
        c.maxTypeDepthFromInitialConstraints = max(c.maxTypeDepthFromInitialConstraints, initialType.typeDepth())
    }

    /**
     * 处理约束
     *
     * 处理类型检查器状态中的所有待处理约束。
     * 此方法会提取分支点数据并在完成状态下立即解析分支点约束。
     *
     * @param c 约束系统上下文
     * @param typeCheckerState 类型检查器状态
     * @param skipProperEqualityConstraints 是否跳过正确的相等性约束（默认为 true）
     * @return 被错过的约束列表，如果没有则返回 null
     */
    context(c: Context, typeCheckerState: TypeCheckerStateForConstraintInjector)

    private fun processConstraints(
    )  {
        processConstraintsIgnoringForksData()
        typeCheckerState.extractForkPointsData()?.let { allForkPointsData ->
            allForkPointsData.mapTo(c.constraintsFromAllForkPoints) { forkPointData ->
                typeCheckerState.position to forkPointData
            }

            c.onNewConstraintOrForkPoint()

            // During completion, we start processing fork constrains immediately
            if (c.atCompletionState) {
                c.resolveForkPointsConstraints()
            }
        }
    }

    /**
     * 判断是否应该跳过约束
     *
     * 某些约束是冗余的或已隐含的，应该被跳过以提高效率。
     *
     * 跳过的约束包括：
     * 1. T <: T 或 T? <: T! 等自反约束（除了 T? <: T 的特殊情况）
     * 2. T <: Any? 这样的上界约束（Any? 是所有类型的超类型）
     *
     * @param typeVariable 类型变量
     * @param constraint 约束
     * @return true 如果应该跳过此约束
     */
    context(c: Context)

    private fun shouldWeSkipConstraint(typeVariable: TypeVariableMarker, constraint: Constraint): Boolean {
        if (constraint.kind == ConstraintKind.EQUALITY)
            return false

        val constraintType = constraint.type

        // 自反约束: T <: T 或 T :> T
        if (constraintType.typeConstructor() == typeVariable.freshTypeConstructor()) {
            return true // T <: T 没有意义
        }

        // 顶层类型约束: T <: Any (在没有可空类型的语言中)
        if (constraint.position.from is DeclaredUpperBoundConstraintPosition<*> &&
            constraint.kind == ConstraintKind.UPPER && constraintType.isAny()  // 改为 isAny()
        ) {
            return true // T <: Any 是冗余的
        }

        return false
    }


    /**
     * 处理给定的类型约束集合
     *
     * 这是约束处理的核心方法，负责将收集到的约束添加到约束系统中，
     * 并决定哪些约束需要进一步合并处理。
     *
     * 处理流程：
     * 1. 遍历所有待处理的约束
     * 2. 跳过冗余或无效的约束（通过 shouldWeSkipConstraint 判断）
     * 3. 将约束添加到对应类型变量的约束集合中
     * 4. 判断是否需要合并该约束
     * 5. 如果需要，调用约束合并器进行合并
     *
     * 约束合并的条件：
     * - 新添加的约束（wasAdded == true）且不是可空性约束
     * - 或者是固定类型变量的相等性约束
     *
     * @param c 约束系统上下文，提供类型变量和约束的访问
     * @param typeCheckerState 类型检查器状态，用于约束合并
     * @param constraintsToProcess 待处理的约束集合，每个元素是 (类型变量, 约束) 对
     */
    context(c: Context, typeCheckerState: TypeCheckerStateForConstraintInjector)

    private fun processGivenConstraints(
        constraintsToProcess: Collection<Pair<TypeVariableMarker, Constraint>>
    ) {
        for ((typeVariable, constraint) in constraintsToProcess) {
            // 跳过冗余约束（如 T <: T, T <: Any 等）
            if ( shouldWeSkipConstraint(typeVariable, constraint)) continue

            // 获取类型变量的约束集合
            // 如果类型变量已固定，则报错（不应该为已固定的类型变量添加约束）
            val constraints =
                c.notFixedTypeVariables[typeVariable.freshTypeConstructor(c)] ?: typeCheckerState.fixedTypeVariable(
                    typeVariable
                )

            // 将约束添加到约束集合中
            // addConstraint 返回：(添加的或已存在的非冗余约束, 是否是新添加的)
            val (addedOrNonRedundantExistedConstraint, wasAdded) = constraints.addConstraint(constraint)
            val positionFrom = constraint.position.from

            // 判断是否需要合并此约束
            val constraintToIncorporate = when {
                // 情况 1：新添加的约束需要合并
                wasAdded -> addedOrNonRedundantExistedConstraint

                // 情况 2：固定类型变量的相等性约束需要合并
                // 这种情况发生在类型变量被固定为某个具体类型时
                positionFrom is FixVariableConstraintPosition<*> &&
                        positionFrom.variable == typeVariable &&
                        constraint.kind == ConstraintKind.EQUALITY ->
                    addedOrNonRedundantExistedConstraint

                // 其他情况不需要合并
                else -> null
            }

            // 如果需要合并，调用约束合并器
            if (constraintToIncorporate != null) {
                constraintIncorporator.incorporate(  typeVariable, constraintToIncorporate)
            }
        }
    }


    /**
     * 处理约束并忽略分支点数据
     *
     * 这是约束处理的主循环，负责迭代处理所有待处理的约束，
     * 直到没有新的约束产生为止。
     *
     * 处理流程：
     * 1. 循环处理：只要还有待处理的约束，就继续循环
     * 2. 提取约束：从类型检查器状态中提取所有待处理约束
     * 3. 处理约束：调用 processGivenConstraints 处理这批约束
     * 4. 检查优化：判断是否使用了不正确的优化（旧版本兼容）
     * 5. 返回结果：返回错过的约束（如果有）
     *
     * 关于"错过的约束"：
     * 在旧的约束处理系统中，存在一个优化：如果每个类型变量都有一个
     * "正确的"相等性约束（即约束右边是具体类型，不含类型变量），
     * 则可以提前终止处理。但这个优化是不正确的，可能导致某些约束被跳过。
     *
     * 新系统（properConstraintsProcessingEnabled = true）已修复此问题，
     * 不再使用这个优化，因此不会产生错过的约束。
     *
     * @param typeCheckerState 类型检查器状态，包含待处理的约束
     * @param c 约束系统上下文
     * @param skipProperEqualityConstraints 是否启用旧的优化（默认 true，但新系统会忽略）
     * @return 错过的约束列表（新系统中总是返回 null）
     */
    context(c: Context, typeCheckerState: TypeCheckerStateForConstraintInjector)

    private fun processConstraintsIgnoringForksData(
    ) {
        while (typeCheckerState.hasConstraintsToProcess()) {
            processGivenConstraints(typeCheckerState.extractAllConstraints()!!)
        }

    }


    /**
     * 添加子类型约束并合并
     *
     * 将子类型约束添加到类型检查器状态，然后处理所有产生的约束。
     *
     * @param c 约束系统上下文
     * @param lowerType 子类型（下界）
     * @param upperType 超类型（上界）
     * @param typeCheckerState 类型检查器状态
     */
    context(c: ConstraintInjector.Context, typeCheckerState: TypeCheckerStateForConstraintInjector)
    private fun addSubTypeConstraintAndIncorporateIt(
        lowerType: CangJieTypeMarker,
        upperType: CangJieTypeMarker,
    ) {
        typeCheckerState.setConstrainingTypesToPrintDebugInfo(lowerType, upperType)
        typeCheckerState.runIsSubtypeOf(lowerType, upperType)

        processConstraints()
    }

    /**
     * 添加初始子类型约束
     *
     * 当需要添加子类型关系约束时（如 T <: Number），此方法创建并处理该约束。
     *
     * @param c 约束系统上下文
     * @param lowerType 子类型
     * @param upperType 超类型
     * @param position 约束位置
     */
    context(c: Context)

    fun addInitialSubtypeConstraint(

        lowerType: CangJieTypeMarker,
        upperType: CangJieTypeMarker,
        position: ConstraintPosition
    ) {
        val initialConstraint =
            InitialConstraint(lowerType, upperType, ConstraintKind.UPPER, position).also { c.addInitialConstraint(it) }
        inferenceLogger?.logInitial(initialConstraint, c)

        updateAllowedTypeDepth(lowerType)
        updateAllowedTypeDepth(upperType)
        inferenceLogger.withOrigin(initialConstraint) {
            with(TypeCheckerStateForConstraintInjector(c, IncorporationConstraintPosition(initialConstraint))) {
                addSubTypeConstraintAndIncorporateIt(lowerType, upperType)
            }
        }

    }

    /**
     * 约束注入器的上下文接口
     *
     * 定义了约束注入器所需的所有操作接口。
     * 实现此接口的类通常是约束系统的核心实现类。
     */
    interface Context : TypeSystemInferenceExtensionContext, ConstraintSystemMarker {
        /** 所有类型变量的映射（从类型构造器到类型变量） */
        val allTypeVariables: Map<TypeConstructorMarker, TypeVariableMarker>

        /** 从初始约束中得到的最大类型深度 */
        var maxTypeDepthFromInitialConstraints: Int

        /** 未固定的类型变量及其约束 */
        val notFixedTypeVariables: MutableMap<TypeConstructorMarker, MutableVariableWithConstraints>

        /** 已固定的类型变量及其确定的类型 */
        val fixedTypeVariables: MutableMap<TypeConstructorMarker, CangJieTypeMarker>
        val approximatorCaches: TypeApproximatorCachesPerConfiguration

        /** 所有分支点的约束数据 */
        val constraintsFromAllForkPoints: MutableList<Pair<IncorporationConstraintPosition, ForkPointData>>

        /** 是否处于完成状态 */
        val atCompletionState: Boolean

        /** 添加初始约束 */
        fun addInitialConstraint(initialConstraint: InitialConstraint)

        /**
         * @see org.jetbrains.kotlin.resolve.calls.inference.model.ConstraintStorage.typeVariableDependencies
         */
        val typeVariableDependencies: Map<TypeConstructorMarker, Set<TypeConstructorMarker>>
        /** 添加错误 */
        fun addError(error: ConstraintSystemError)
        fun onNewConstraintOrForkPoint()

        /** 添加错过的约束 */
        fun addMissedConstraints(
            position: IncorporationConstraintPosition,
            constraints: MutableList<Pair<TypeVariableMarker, Constraint>>
        )

        /** 解析分支点约束 */
        fun resolveForkPointsConstraints()
    }


    /**
     * 类型检查器状态（用于约束注入器）
     *
     * 这是一个特殊的类型检查器状态实现，专门用于约束注入过程。
     * 它扩展了标准的类型检查器状态，添加了约束收集和分支点处理功能。
     *
     * 主要职责：
     * 1. 收集在类型检查过程中产生的新约束
     * 2. 管理分支点（fork point）的约束集合
     * 3. 处理约束的合并和传播
     * 4. 检测并报告类型错误
     *
     * @property c 约束系统上下文
     * @property position 约束合并位置，用于追踪约束来源
     */
    private inner class TypeCheckerStateForConstraintInjector(
        baseState: TypeCheckerState,
        val c: Context,
        val position: IncorporationConstraintPosition
    ) : TypeCheckerStateForConstraintSystem(
        c,
        baseState.cangjieTypePreparator,
        baseState.cangjieTypeRefiner
    ), ConstraintIncorporator.Context, TypeSystemInferenceExtensionContext by c {
        /**
         * 便捷构造函数，自动创建类型检查器状态
         *
         * @param c 约束系统上下文
         * @param position 约束合并位置
         */
        constructor(c: Context, position: IncorporationConstraintPosition) : this(
            c.newTypeCheckerState(errorTypesEqualToAnything = true, stubTypesEqualToAnything = true),
            c,
            position
        )

        /**
         * 可能产生的新约束列表
         *
         * 使用 var 是为了避免额外的内存分配，因为这个属性访问频繁
         */
        private var possibleNewConstraints: MutableList<Pair<TypeVariableMarker, Constraint>>? = null

        /** 分支点数据列表 */
        private var forkPointsData: MutableList<ForkPointData>? = null

        /** 当前分支点的约束集合栈 */
        private var stackForConstraintsSetsFromCurrentForkPoint: Stack<MutableList<ForkPointBranchDescription>>? = null

        /** 当前分支点分支的约束集合栈 */
        private var stackForConstraintSetFromCurrentForkPointBranch: Stack<MutableList<Pair<TypeVariableMarker, Constraint>>>? =
            null

        /** 是否允许分支（fork） */
        private val allowForking: Boolean
            get() = constraintIncorporator.utilContext.isForcedAllowForkingInferenceSystem

        /** 基础下界类型（用于调试信息） */
        private var baseLowerType = position.initialConstraint.a

        /** 基础上界类型（用于调试信息） */
        private var baseUpperType = position.initialConstraint.b

        /** 是否正在合并来自声明上界的约束 */
        private var isIncorporatingConstraintFromDeclaredUpperBound = false

        /** 当前约束的派生来源集合，用于防止循环约束 */
        private var currentDerivedFrom: Set<TypeVariableMarker> = emptySet()

        /**
         * 提取所有约束并清空约束列表
         *
         * @return 当前收集的所有约束，提取后原列表被置空
         */
        fun extractAllConstraints() = possibleNewConstraints.also { possibleNewConstraints = null }

        /**
         * 提取所有分支点数据并清空列表
         *
         * @return 当前收集的所有分支点数据，提取后原列表被置空
         */
        fun extractForkPointsData() = forkPointsData.also { forkPointsData = null }

        /**
         * 添加可能的新约束
         *
         * 根据当前是否在分支点内部，将约束添加到相应的位置：
         * - 如果在分支点内部，添加到当前分支的约束集合
         * - 否则添加到全局的约束列表
         *
         * @param variable 类型变量
         * @param constraint 约束
         */
        fun addPossibleNewConstraint(variable: TypeVariableMarker, constraint: Constraint) {
            val constraintsSetsFromCurrentFork = stackForConstraintsSetsFromCurrentForkPoint?.lastOrNull()
            if (constraintsSetsFromCurrentFork != null) {
                val currentConstraintSetForForkPointBranch =
                    stackForConstraintSetFromCurrentForkPointBranch?.lastOrNull()
                require(currentConstraintSetForForkPointBranch != null) { "Constraint has been added not under fork {...} call " }
                currentConstraintSetForForkPointBranch.add(variable to constraint)
                return
            }

            if (possibleNewConstraints == null) {
                possibleNewConstraints = SmartList()
            }
//            TODO 排除掉来自扩展的约束，因为那并不一定来自扩展
//            if (constraint.position.from is ReceiverConstraintPosition<*>) return
            possibleNewConstraints!!.add(variable to constraint)
        }

        /**
         * 添加下界约束（A <: T）
         *
         * 当类型检查器发现某个类型是类型变量的子类型时调用。
         *
         * @param typeVariable 类型变量的构造器
         * @param subType 子类型（下界）
         * @param isFromNullabilityConstraint 是否来自可空性约束
         * @param isNoInfer 是否标记为 NoInfer（不参与推断）
         */
        override fun addLowerConstraint(
            typeVariable: TypeConstructorMarker,
            subType: CangJieTypeMarker,
            isNoInfer: Boolean
        ) = addConstraint(typeVariable, subType, ConstraintKind.LOWER, isNoInfer)

        /**
         * 添加相等性约束（T == A）
         *
         * 当类型检查器确定类型变量必须等于某个类型时调用。
         *
         * @param typeVariable 类型变量的构造器
         * @param type 相等的类型
         */
        override fun addEqualityConstraint(typeVariable: TypeConstructorMarker, type: CangJieTypeMarker) =
            addConstraint(typeVariable, type, ConstraintKind.EQUALITY, false)

        /**
         * 添加上界约束（T <: A）
         *
         * 当类型检查器发现类型变量必须是某个类型的子类型时调用。
         *
         * @param typeVariable 类型变量的构造器
         * @param superType 超类型（上界）
         * @param isNoInfer 是否标记为 NoInfer（不参与推断）
         */
        override fun addUpperConstraint(
            typeVariable: TypeConstructorMarker,
            superType: CangJieTypeMarker,
            isNoInfer: Boolean
        ) =
            addConstraint(typeVariable, superType, ConstraintKind.UPPER, isNoInfer)

        /** 获取语言版本设置 */
        override val languageVersionSettings: LanguageVersionSettings
            get() = this@ConstraintInjector.languageVersionSettings

        /**
         * 判断给定类型是否是当前约束系统中的类型变量
         *
         * @param type 要检查的简单类型
         * @return true 如果该类型是约束系统中的类型变量
         */
        override fun isMyTypeVariable(type: SimpleTypeMarker): Boolean =
            c.allTypeVariables.containsKey(type.typeConstructor().unwrapStubTypeVariableConstructor())

        /**
         * 运行分支点（Fork Point）
         *
         * 分支点用于处理类型推断中的多种可能性，例如重载解析。
         * 当遇到多个候选项时，为每个候选项创建一个分支，在每个分支中
         * 独立收集约束，最后选择约束一致的分支。
         *
         * 工作流程：
         * 1. 创建分支点上下文
         * 2. 执行 block，在其中通过 fork() 创建多个分支
         * 3. 收集每个分支产生的约束
         * 4. 根据分支数量决定处理方式：
         *    - 0 个分支：直接返回
         *    - 1 个分支：立即处理该分支的约束
         *    - 多个分支：保存分支数据，延迟到完成阶段处理
         *
         * @param block 分支点执行块，在其中创建分支
         * @return true 如果至少有一个分支成功
         */
        override fun runForkingPoint(block: ForkPointContext.() -> Unit): Boolean {
            // 如果不允许分支，使用父类的默认实现
            if (!allowForking) {
                return super.runForkingPoint(block)
            }

            // 初始化分支点约束集合栈（如果尚未初始化）
            if (stackForConstraintsSetsFromCurrentForkPoint == null) {
                stackForConstraintsSetsFromCurrentForkPoint = SmartList()
            }

            // 为当前分支点创建一个新的约束集合列表
            stackForConstraintsSetsFromCurrentForkPoint!!.add(SmartList())

            // 执行分支点逻辑，收集是否有成功的分支
            val isThereSuccessfulFork = with(MyForkCreationContext()) {
                block()
                anyForkSuccessful
            }

            // 弹出当前分支点的约束集合
            val constraintSets = stackForConstraintsSetsFromCurrentForkPoint?.popLast()

            when {
                // 优化：如果没有产生任何约束，直接返回
                constraintSets.isNullOrEmpty() -> return isThereSuccessfulFork

                // 情况 1：多个分支（需要延迟处理）
                constraintSets.size > 1 -> {
                    if (forkPointsData == null) {
                        forkPointsData = SmartList()
                    }
                    // 保存分支数据，延迟到完成阶段处理
                    forkPointsData!!.addIfNotNull(constraintSets)
                    return true
                }

                // 情况 2：只有一个分支（立即处理）
                else -> {
                    // 空集合的情况已在上面处理，这里一定是单个非空分支

                    with(c) {
                        processGivenForkPointBranchConstraints(
                            constraintSets.single(),
                            position,
                        )
                    }
                }
            }

            return isThereSuccessfulFork
        }

        /**
         * 分支创建上下文
         *
         * 用于在分支点中创建和管理多个分支。
         * 每个分支独立收集约束，最后根据成功情况选择合适的分支。
         */
        private inner class MyForkCreationContext : ForkPointContext {
            /** 是否有任何分支成功 */
            var anyForkSuccessful = false

            /**
             * 创建一个分支
             *
             * 在分支中执行 block，收集该分支产生的约束。
             * 如果 block 返回 true，表示该分支成功。
             *
             * @param block 分支执行块，返回该分支是否成功
             */
            override fun fork(block: () -> Boolean) {
                // 初始化分支约束栈（如果尚未初始化）
                if (stackForConstraintSetFromCurrentForkPointBranch == null) {
                    stackForConstraintSetFromCurrentForkPointBranch = SmartList()
                }

                // 为当前分支创建约束集合
                stackForConstraintSetFromCurrentForkPointBranch!!.add(SmartList())

                // 执行分支逻辑，更新成功标志
                block().also { anyForkSuccessful = anyForkSuccessful || it }

                // 将当前分支的约束添加到分支点的约束集合中
                // 只添加非空的约束集合
                stackForConstraintsSetsFromCurrentForkPoint!!.last()
                    .addIfNotNull(
                        stackForConstraintSetFromCurrentForkPointBranch?.popLast()?.takeIf { it.isNotEmpty() }?.toSet()
                    )
            }
        }

        /**
         * 检查是否有待处理的约束
         *
         * @return true 如果有待处理的约束
         */
        fun hasConstraintsToProcess() = possibleNewConstraints != null

        /**
         * 设置约束类型（用于调试信息）
         *
         * 记录当前正在处理的约束的下界和上界类型，
         * 用于在出错时生成有意义的错误信息。
         *
         * @param lowerType 下界类型
         * @param upperType 上界类型
         */
        fun setConstrainingTypesToPrintDebugInfo(lowerType: CangJieTypeMarker, upperType: CangJieTypeMarker) {
            baseLowerType = lowerType
            baseUpperType = upperType
        }

        /**
         * 运行子类型检查
         *
         * 检查 lowerType 是否是 upperType 的子类型。
         * 如果不是，则添加约束错误到约束系统中。
         *
         * 特殊处理：
         * - 如果允许尝试不同的灵活性，会尝试将上界类型转换为灵活类型再检查
         * - 灵活类型是指可以在可选和非可选之间转换的类型
         *
         * @param lowerType 下界类型（子类型）
         * @param upperType 上界类型（超类型）
         * @param shouldTryUseDifferentFlexibilityForUpperType 是否尝试不同的灵活性
         */
        fun runIsSubtypeOf(
            lowerType: CangJieTypeMarker,
            upperType: CangJieTypeMarker,
            shouldTryUseDifferentFlexibilityForUpperType: Boolean = false,
        ) {
            // 子类型检查的辅助函数
            fun isSubtypeOf(upperType: CangJieTypeMarker) =
                AbstractTypeChecker.isSubtypeOf(
                    this@TypeCheckerStateForConstraintInjector as TypeCheckerState,

                    lowerType as CangJieType,
                    upperType as CangJieType
                )

            // 如果子类型检查失败，添加错误
            if (!isSubtypeOf(upperType)) {
                // TODO: 改进错误报告 - 添加有关基本类型的信息
                if (shouldTryUseDifferentFlexibilityForUpperType && upperType.isSimpleType()) {
                    /**
                     * 灵活性处理的特殊逻辑
                     *
                     * 注意：这是一个临时解决方案，不应在其他地方重复使用。
                     *
                     * 背景：在某些情况下，类型的灵活性（可选性）没有正确传播。
                     * 例如，类型变量 T 可能需要匹配 Option<Int> 或 Int。
                     *
                     * 解决方案：尝试将上界类型转换为灵活类型（同时支持可选和非可选），
                     * 如果仍然不匹配，才报告错误。
                     *
                     * 历史原因：旧的推断系统使用已替换的类型，自然具备正确的灵活性。
                     * 新系统需要显式处理这种情况。
                     */
                    require(upperType is SimpleTypeMarker)
                    val flexibleUpperType = createFlexibleType(upperType, upperType.withOption(true))
                    if (!isSubtypeOf(flexibleUpperType)) {
                        c.addError(ConstraintError(lowerType, flexibleUpperType, position))
                    }
                } else {
                    // 直接报告约束错误
                    c.addError(ConstraintError(lowerType, upperType, position))
                }
            }
        }


        /**
         * 判断类型是否是子类型检查产生的捕获类型
         *
         * 捕获类型（Captured Type）是在处理泛型通配符时产生的临时类型。
         * 根据捕获状态，可以分为：
         * - FROM_EXPRESSION：来自表达式的捕获类型（允许）
         * - FOR_SUBTYPING：用于子类型检查的捕获类型（需要近似处理）
         * - FOR_INCORPORATION：用于约束合并的捕获类型（不应逃逸）
         *
         * @param type 要检查的类型
         * @return true 如果是子类型检查产生的捕获类型
         */
        private fun isCapturedTypeFromSubtyping(type: CangJieTypeMarker): Boolean {
            val capturedType = type as? CapturedTypeMarker ?: return false

            // 旧的捕获类型不需要特殊处理
            if (capturedType.isOldCapturedType()) return false

            return when (capturedType.captureStatus()) {
                // 来自表达式的捕获类型是正常的
                CaptureStatus.FROM_EXPRESSION -> false
                // 用于子类型检查的捕获类型需要近似处理
                CaptureStatus.FOR_SUBTYPING -> true
                // 用于合并的捕获类型不应该逃逸到这里
                CaptureStatus.FOR_INCORPORATION ->
                    error("Captured type for incorporation shouldn't escape from incorporation: $type\n" + renderBaseConstraint())
            }
        }

        /**
         * 添加约束（内部方法）
         *
         * 将约束添加到类型检查器状态中，最终会被收集到 possibleNewConstraints 列表。
         *
         * @param typeVariableConstructor 类型变量的构造器
         * @param type 约束的类型
         * @param kind 约束种类（UPPER、LOWER 或 EQUALITY）
         * @param isNoInfer 是否标记为 NoInfer（不参与类型推断）
         */
        private fun addConstraint(
            typeVariableConstructor: TypeConstructorMarker,
            type: CangJieTypeMarker,
            kind: ConstraintKind,
            isNoInfer: Boolean = false
        ) {
            // 查找类型变量，如果不存在则报错
            val typeVariable = c.allTypeVariables[typeVariableConstructor.unwrapStubTypeVariableConstructor()]
                ?: error("Should by type variableConstructor: $typeVariableConstructor. ${c.allTypeVariables.values}")

            // 添加新的合并约束，使用当前的派生来源集合
            addNewIncorporatedConstraint(
                typeVariable,
                type,
                ConstraintContext(kind, currentDerivedFrom, isNoInfer = isNoInfer)
            )
        }

        private var currentDerivedFromSet: Set<TypeVariableMarker> = emptySet()

        private var isIncorporatingConstraintFromNoInfer = false

        /**
         * 使用新的约束合并配置执行代码块
         *
         * 这是一个辅助方法，用于在约束合并过程中临时设置派生来源集合和其他配置。
         * 执行完毕后会自动恢复之前的配置。
         *
         * @param newDerivedFromSet 新的派生来源集合
         * @param isFromDeclaredUpperBound 是否来自声明的上界
         * @param isNoInfer 是否标记为 NoInfer
         * @param b 要执行的代码块
         */
        private inline fun withNewConfigurationForIncorporationConstraints(
            newDerivedFromSet: Set<TypeVariableMarker>,
            isFromDeclaredUpperBound: Boolean,
            isNoInfer: Boolean,
            b: () -> Unit,
        ) {
            // 不应该发生立即递归的合并，因此 currentDerivedFromSet 会在 finally 中重置
            check(currentDerivedFromSet.isEmpty())

            try {
                currentDerivedFromSet = newDerivedFromSet
                isIncorporatingConstraintFromDeclaredUpperBound = isFromDeclaredUpperBound
                isIncorporatingConstraintFromNoInfer = isNoInfer
                b()
            } finally {
                // 注意：emptySet() 返回一个单例对象，因此不会产生额外的内存开销
                currentDerivedFromSet = emptySet()
                isIncorporatingConstraintFromDeclaredUpperBound = false
                isIncorporatingConstraintFromNoInfer = false
            }
        }

        /**
         * 添加新的合并约束（类型对类型）
         *
         * 这是 ConstraintIncorporator.Context 接口的实现方法。
         * 当约束合并器推导出新的类型约束时调用。
         *
         * @param lowerType 下界类型
         * @param upperType 上界类型
         * @param shouldTryUseDifferentFlexibilityForUpperType 是否尝试不同的灵活性
         * @param isFromDeclaredUpperBound 是否来自声明的上界
         * @param newDerivedFrom 新的派生来源集合，用于防止循环约束
         * @param isNoInfer 是否标记为 NoInfer（不参与类型推断）
         */
        override fun processNewInitialConstraintFromIncorporation(
            lowerType: CangJieTypeMarker,
            upperType: CangJieTypeMarker,

            shouldTryUseDifferentFlexibilityForUpperType: Boolean,
            newDerivedFrom: Set<TypeVariableMarker>,
            isFromDeclaredUpperBound: Boolean,
            isNoInfer: Boolean,
        ) = with(c) {
            // 如果两个类型相同，无需添加约束
            if (lowerType === upperType) return
            if (c.isAllowedType(lowerType) && c.isAllowedType(upperType)) {
                withNewConfigurationForIncorporationConstraints(
                    newDerivedFromSet = newDerivedFrom,
                    isFromDeclaredUpperBound = isFromDeclaredUpperBound,
                    isNoInfer = isNoInfer,
                ) {
                    runIsSubtypeOf(lowerType, upperType, shouldTryUseDifferentFlexibilityForUpperType)
                }
            }

        }

        /**
         * 添加新的合并约束（类型变量对类型）
         *
         * 这是 ConstraintIncorporator.Context 接口的实现方法。
         * 当约束合并器推导出涉及类型变量的新约束时调用。
         *
         * 处理流程：
         * 1. 检查类型是否有效（未推断参数、错误类型）
         * 2. 处理捕获类型（通过类型近似转换）
         * 3. 创建新约束并添加到待处理列表
         *
         * @param typeVariable 类型变量
         * @param type 约束的类型
         * @param constraintContext 约束上下文（包含约束种类、派生信息等）
         */
        override fun addNewIncorporatedConstraint(
            typeVariable: TypeVariableMarker,
            type: CangJieTypeMarker,
            constraintContext: ConstraintContext
        ) {
            val (kind, derivedFrom, inputTypePosition, isNoInfer) = constraintContext

            var targetType = type

            // 如果是未推断参数，跳过（已经有错误报告）
            if (targetType.isUninferredParameter()) {
                return
            }

            // 如果是错误类型，报告错误并返回
            if (targetType.isError()) {
                c.addError(ConstrainingTypeIsError(typeVariable, targetType, position))
                return
            }

            // 处理包含捕获类型的情况
            if (type.contains(this::isCapturedTypeFromSubtyping)) {
                // 上界约束：TypeVariable <: type
                // 如果 type 包含捕获类型，近似为子类型
                if (kind == ConstraintKind.UPPER) {
                    val subType =
                        typeApproximator.approximateToSubType(
                            type,
                            TypeApproximatorConfiguration.SubtypeCapturedTypesApproximation
                        )
                    if (subType != null) {
                        targetType = subType
                    }
                }

                // 下界约束：type <: TypeVariable
                // 如果 type 包含捕获类型，近似为超类型
                if (kind == ConstraintKind.LOWER) {
                    val superType =
                        typeApproximator.approximateToSuperType(
                            type,
                            TypeApproximatorConfiguration.SubtypeCapturedTypesApproximation
                        )
                    if (superType != null) {
                        // TODO: 重新考虑 Any 情况的错误报告
                        targetType = superType
                    }
                }

                // 如果无法近似，报告错误
                if (targetType === type) {
                    c.addError(CapturedTypeFromSubtyping(typeVariable, type, position))
                    return
                }
            }

            // 如果来自声明上界，标记位置
            val position =
                if (isIncorporatingConstraintFromDeclaredUpperBound) position.copy(isFromDeclaredUpperBound = true) else position

            // 创建新约束
            val newConstraint = Constraint(
                kind,
                targetType,
                position,
                derivedFrom = derivedFrom,
                inputTypePositionBeforeIncorporation = inputTypePosition,
                isNoInfer = isNoInfer
            )

            // 添加到待处理约束列表
            addPossibleNewConstraint(typeVariable, newConstraint)
            inferenceLogger?.log(typeVariable, newConstraint, c)

        }

        /** 获取所有带约束的类型变量 */
        override val allTypeVariablesWithConstraints: Collection<VariableWithConstraints>
            get() = c.notFixedTypeVariables.values

        /**
         * 根据类型构造器获取类型变量
         *
         * @param typeConstructor 类型构造器
         * @return 对应的类型变量，如果不存在则返回 null
         */
        override fun getTypeVariable(typeConstructor: TypeConstructorMarker): TypeVariableMarker? {
            val typeVariable = c.allTypeVariables[typeConstructor]
            // 如果类型变量已固定，报错
            if (typeVariable != null && !c.notFixedTypeVariables.containsKey(typeConstructor)) {
                fixedTypeVariable(typeVariable)
            }
            return typeVariable
        }

        /**
         * 获取类型变量的约束集合
         *
         * @param typeVariable 类型变量
         * @return 该类型变量的所有约束
         */
        override fun getConstraintsForVariable(typeVariable: TypeVariableMarker) =
            c.notFixedTypeVariables[typeVariable.freshTypeConstructor()]?.constraints
                ?: fixedTypeVariable(typeVariable)

        /**
         * 报告类型变量已固定的错误
         *
         * 当尝试为已固定的类型变量添加约束时调用。
         * 这通常表示约束系统的内部错误。
         *
         * @param variable 已固定的类型变量
         * @throws IllegalStateException 总是抛出异常
         */
        fun fixedTypeVariable(variable: TypeVariableMarker): Nothing {
            error(
                "Type variable $variable should not be fixed!\n" +
                        renderBaseConstraint()
            )
        }

        /**
         * 渲染基础约束信息（用于错误报告）
         *
         * @return 基础约束的字符串表示
         */
        private fun renderBaseConstraint() =
            "Base constraint: $baseLowerType <: $baseUpperType from position: $position"
    }

    /**
     * 检查类型是否在允许的深度范围内
     *
     * 为了避免无限递归和性能问题，约束合并过程中限制了类型深度。
     * 允许的深度 = 初始约束中的最大深度 + 允许的增量
     *
     * @param type 要检查的类型
     * @return true 如果类型深度在允许范围内
     */
    private fun Context.isAllowedType(type: CangJieTypeMarker) =
        type.typeDepth() <= maxTypeDepthFromInitialConstraints + ALLOWED_DEPTH_DELTA_FOR_INCORPORATION

}

/**
 * 栈类型别名
 *
 * 使用可变列表作为栈的实现
 */
private typealias Stack<E> = MutableList<E>

/**
 * 约束上下文
 *
 * 封装约束的上下文信息，包括约束种类、派生关系和位置信息。
 *
 * @property kind 约束种类（UPPER、LOWER 或 EQUALITY）
 * @property derivedFrom 该约束派生自哪些类型变量
 * @property inputTypePositionBeforeIncorporation 合并前的输入类型位置（如果有）
 * @property isNoInfer 是否标记为 NoInfer（不参与类型推断）
 */
data class ConstraintContext(
    val kind: ConstraintKind,
    val derivedFrom: Set<TypeVariableMarker>,
    val inputTypePositionBeforeIncorporation: OnlyInputTypeConstraintPosition? = null,
    val isNoInfer: Boolean = false,
)

/**
 * 获取类型变量的新鲜类型构造器
 *
 * @param c 类型系统推断扩展上下文
 * @return 新鲜类型构造器
 */
context(c: TypeSystemInferenceExtensionContext)
fun TypeVariableMarker.freshTypeConstructor(): TypeVariableTypeConstructorMarker = with(c) { freshTypeConstructor() }
fun TypeVariableMarker.freshTypeConstructor(c: TypeSystemInferenceExtensionContext) = with(c) { freshTypeConstructor() }
