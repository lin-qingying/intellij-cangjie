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

package org.cangnova.cangjie.resolve.calls.inference.components

import org.cangnova.cangjie.config.LanguageFeature
import org.cangnova.cangjie.config.LanguageVersionSettings
import org.cangnova.cangjie.resolve.calls.inference.ForkPointData
import org.cangnova.cangjie.resolve.calls.inference.hasRecursiveTypeParametersWithGivenSelfType
import org.cangnova.cangjie.resolve.calls.inference.isRecursiveTypeParameter
import org.cangnova.cangjie.resolve.calls.inference.model.Constraint
import org.cangnova.cangjie.resolve.calls.inference.model.DeclaredUpperBoundConstraintPosition
import org.cangnova.cangjie.resolve.calls.inference.model.IncorporationConstraintPosition
import org.cangnova.cangjie.resolve.calls.inference.model.VariableWithConstraints
import org.cangnova.cangjie.resolve.calls.model.PostponedResolvedAtomMarker
import org.cangnova.cangjie.types.model.*

/**
 * ═══════════════════════════════════════════════════════════════════════════════
 * 类型变量固定查找器说明
 * ═══════════════════════════════════════════════════════════════════════════════
 *
 * VariableFixationFinder 负责在类型推导过程中确定哪些类型变量可以被固定（fixation）。
 * 类型变量固定是类型推导的关键步骤，它将类型变量从未确定状态转换为具体类型。
 *
 * 核心职责
 * ─────────────────────────────────────────────────────────────────────────────
 *
 * 1. **固定顺序确定**
 *    - 分析类型变量之间的依赖关系
 *    - 确定哪些类型变量可以优先固定
 *    - 避免固定依赖于其他未固定变量的类型变量
 *
 * 2. **就绪状态评估**
 *    - 检查类型变量是否有足够的约束信息
 *    - 评估约束的质量（proper vs non-proper）
 *    - 检测循环依赖和复杂依赖
 *
 * 3. **外部变量处理**
 *    - 处理嵌套推导场景中的外部类型变量
 *    - 管理内外约束系统之间的依赖关系
 *
 * 固定就绪状态（TypeVariableFixationReadiness）
 * ─────────────────────────────────────────────────────────────────────────────
 *
 * 类型变量的固定就绪状态按优先级从低到高排列：
 *
 * 1. **FORBIDDEN（禁止固定）**
 *    - 变量已被固定
 *    - 变量与顶层类型相关
 *    - 变量在分支点中有未处理的约束
 *
 * 2. **WITHOUT_PROPER_ARGUMENT_CONSTRAINT（缺少正确参数约束）**
 *    - 没有来自参数的正确约束
 *    - 只有来自类型参数上界的约束
 *
 * 3. **OUTER_TYPE_VARIABLE_DEPENDENCY（外部类型变量依赖）**
 *    - 依赖于外部约束系统的类型变量
 *
 * 4. **WITH_COMPLEX_DEPENDENCY（复杂依赖）**
 *    - 约束中包含其他未固定的类型变量
 *    - 例如：T <: List<S>，其中 S 未固定
 *
 * 5. **ALL_CONSTRAINTS_TRIVIAL_OR_NON_PROPER（所有约束平凡或非正确）**
 *    - 所有约束都是平凡的（如 Nothing <: T）
 *    - 或者都不是正确的参数约束
 *
 * 6. **RELATED_TO_ANY_OUTPUT_TYPE（与输出类型相关）**
 *    - 类型变量与某个输出类型相关
 *
 * 7. **FROM_INCORPORATION_OF_DECLARED_UPPER_BOUND（来自声明上界的合并）**
 *    - 只有来自声明上界合并的约束
 *
 * 8. **READY_FOR_FIXATION（准备固定）**
 *    - 有足够的正确约束
 *    - 没有复杂依赖
 *    - 可以安全固定
 *
 * 固定策略
 * ─────────────────────────────────────────────────────────────────────────────
 *
 * 1. **优先级排序**
 *    - 选择就绪状态最高的类型变量
 *    - 优先固定没有依赖的变量
 *
 * 2. **依赖分析**
 *    - 检查类型变量之间的依赖关系
 *    - 避免固定依赖于未固定变量的类型变量
 *
 * 3. **约束质量评估**
 *    - 区分正确约束和非正确约束
 *    - 区分参数约束和上界约束
 *
 * 示例
 * ─────────────────────────────────────────────────────────────────────────────
 *
 * ```
 * // 场景 1: 简单固定
 * func foo<T>(x: T): T { return x }
 * foo(42)
 * // T 有约束 Int <: T（来自参数）
 * // T 可以立即固定为 Int
 *
 * // 场景 2: 依赖关系
 * func bar<T, U>(x: T, y: List<U>): T { ... }
 * bar(42, [1, 2, 3])
 * // T 有约束 Int <: T（可以固定）
 * // U 有约束 Int <: U（可以固定）
 * // T 和 U 独立，可以任意顺序固定
 *
 * // 场景 3: 复杂依赖
 * func baz<T, U>(x: T): List<U> where T <: List<U> { ... }
 * baz([1, 2, 3])
 * // T 有约束 List<Int> <: T
 * // U 有约束 T <: List<U>（依赖 T）
 * // 必须先固定 T，再固定 U
 * ```
 *
 * ═══════════════════════════════════════════════════════════════════════════════
 */

/**
 * 检查类型是否适合用于固定
 *
 * 即使 `isProper(type) == true`，对于已固定的类型变量类型也返回 `false`。
 * 这样只允许非类型变量类型用于顶层固定。
 *
 * 虽然这个限制很重要，但它并不真正限制最终结果，因为当我们有约束 T <: E 或 E <: T
 * 并且要将 T 固定为 E 时，我们假设如果 E 有其他约束，它们会被合并到 T 中，
 * 所以我们会选择这些约束而不是 E 本身。
 *
 * @param type 要检查的类型
 * @param notFixedTypeVariables 未固定的类型变量集合
 * @param isProper 判断类型是否为正确类型的函数
 * @return 如果类型适合用于固定返回 true，否则返回 false
 *
 * 限制说明：
 * - 不允许将 T 固定为任何顶层类型变量类型，如 T := F 或 T := F & Any
 * - 即使 F 被 `isProper` 认为是正确的（例如，它属于外部约束系统）
 * - 但同时，我们不禁止固定为 T := MutableList<F>
 *
 * 示例：
 * ```
 * // 禁止的固定
 * T := U  // U 是另一个类型变量
 * T := U & Any  // U 是顶层类型变量
 *
 * // 允许的固定
 * T := Int  // Int 不是类型变量
 * T := List<U>  // U 不在顶层
 * ```
 */
inline fun TypeSystemInferenceExtensionContext.isProperTypeForFixation(
    type: CangJieTypeMarker,
    notFixedTypeVariables: Set<TypeConstructorMarker>,
    isProper: (CangJieTypeMarker) -> Boolean
): Boolean {
    // 不允许将 T 固定为任何顶层类型变量类型，如 T := F 或 T := F & Any
    // 即使 F 被 `isProper` 认为是正确的（例如，它属于外部约束系统）
    // 但同时，我们不禁止固定为 T := MutableList<F>
    if (type.typeConstructor() in notFixedTypeVariables) return false

    return isProper(type) && extractProjectionsForAllCapturedTypes(type).all(isProper)
}

/**
 * 类型变量固定查找器
 *
 * 负责在类型推导过程中查找可以被固定的类型变量。
 * 通过分析约束和依赖关系，确定类型变量的固定顺序。
 *
 * @property trivialConstraintTypeInferenceOracle 平凡约束类型推导预言器，用于判断约束是否平凡
 * @property languageVersionSettings 语言版本设置
 */
class VariableFixationFinder(
    private val trivialConstraintTypeInferenceOracle: TrivialConstraintTypeInferenceOracle,
    private val languageVersionSettings: LanguageVersionSettings,
) {
    /**
     * 类型变量固定查找器上下文接口
     *
     * 提供类型变量固定所需的上下文信息，包括：
     * - 未固定和已固定的类型变量
     * - 延迟的类型变量
     * - 分支点约束
     * - 外部类型变量信息
     */
    interface Context : TypeSystemInferenceExtensionContext {
        /**
         * 未固定的类型变量映射
         *
         * 键：类型构造器
         * 值：类型变量及其约束
         */
        val notFixedTypeVariables: Map<TypeConstructorMarker, VariableWithConstraints>

        /**
         * 已固定的类型变量映射
         *
         * 键：类型构造器
         * 值：固定后的具体类型
         */
        val fixedTypeVariables: Map<TypeConstructorMarker, CangJieTypeMarker>

        /**
         * 延迟的类型变量列表
         *
         * 这些类型变量的固定被延迟到后续阶段
         */
        val postponedTypeVariables: List<TypeVariableMarker>

        /**
         * 来自所有分支点的约束
         *
         * 分支点（Fork Point）是类型推导中的决策点，
         * 每个分支可能产生不同的约束集合
         */
        val constraintsFromAllForkPoints: MutableList<Pair<IncorporationConstraintPosition, ForkPointData>>

        /**
         * 所有类型变量映射
         *
         * 包括当前约束系统和外部约束系统的所有类型变量
         */
        val allTypeVariables: Map<TypeConstructorMarker, TypeVariableMarker>

        /**
         * 外部系统变量前缀大小
         *
         * 用于区分当前约束系统和外部约束系统的类型变量。
         * 前 N 个类型变量属于外部系统。
         *
         * @see org.cangnova.cangjie.resolve.calls.inference.model.ConstraintStorage.outerSystemVariablesPrefixSize
         */
        val outerSystemVariablesPrefixSize: Int

        /**
         * 外部类型变量集合
         *
         * 如果 [outerSystemVariablesPrefixSize] > 0，返回前 N 个类型变量作为外部变量。
         * 否则返回 null，表示没有外部类型变量。
         *
         * 外部类型变量来自嵌套推导场景中的外层约束系统。
         */
        val outerTypeVariables: Set<TypeConstructorMarker>?
            get() =
                when {
                    outerSystemVariablesPrefixSize > 0 -> allTypeVariables.keys.take(outerSystemVariablesPrefixSize)
                        .toSet()

                    else -> null
                }

        /**
         * 被视为正确类型的类型变量集合
         *
         * 如果不为 null，表示在固定某些变量时，应该临时将这些类型变量视为正确类型。
         *
         * 默认情况下，如果此属性为 null，我们将所有 [allTypeVariables] 视为非正确类型。
         *
         * 当前仅用于 `provideDelegate` 解析，参见：
         * [org.cangnova.cangjie.fir.resolve.transformers.body.resolve.FirDeclarationsResolveTransformer.fixInnerVariablesForProvideDelegateIfNeeded]
         *
         * 使用场景：
         * - 在某些特殊情况下，需要将内部类型变量视为正确类型
         * - 这允许在外部变量未固定时固定内部变量
         */
        val typeVariablesThatAreCountedAsProperTypes: Set<TypeConstructorMarker>?

//        fun isReified(variable: TypeVariableMarker): Boolean
    }


    /**
     * 待固定的类型变量
     *
     * 表示一个可以被固定的类型变量及其就绪状态。
     *
     * @property variable 类型变量的类型构造器
     * @property hasProperConstraint 是否有正确的约束（来自参数的约束）
     * @property hasDependencyOnOuterTypeVariable 是否依赖于外部类型变量
     */
    class VariableForFixation(
        val variable: TypeConstructorMarker,
        private val hasProperConstraint: Boolean,
        private val hasDependencyOnOuterTypeVariable: Boolean = false,
    ) {
        /**
         * 类型变量是否准备好被固定
         *
         * 只有当类型变量有正确的约束且不依赖于外部类型变量时，才准备好被固定。
         *
         * @return true 如果准备好被固定，否则返回 false
         */
        val isReady: Boolean get() = hasProperConstraint && !hasDependencyOnOuterTypeVariable
    }

    /**
     * 查找第一个可以固定的类型变量
     *
     * 从所有类型变量中选择就绪状态最高的类型变量进行固定。
     *
     * @param c 类型变量固定查找器上下文
     * @param allTypeVariables 所有类型变量列表
     * @param postponedCjPrimitives 延迟的仓颉原语列表
     * @param completionMode 约束系统完成模式
     * @param topLevelType 顶层类型
     * @return 待固定的类型变量，如果没有可固定的变量则返回 null
     */
    fun findFirstVariableForFixation(
        c: Context,
        allTypeVariables: List<TypeConstructorMarker>,
        postponedCjPrimitives: List<PostponedResolvedAtomMarker>,
        completionMode: ConstraintSystemCompletionMode,
        topLevelType: CangJieTypeMarker,
    ): VariableForFixation? =
        c.findTypeVariableForFixation(allTypeVariables, postponedCjPrimitives, completionMode, topLevelType)

    /**
     * 类型变量固定就绪状态枚举
     *
     * 定义类型变量的固定就绪状态，按优先级从低到高排列。
     * 就绪状态越高，类型变量越优先被固定。
     */
    enum class TypeVariableFixationReadiness {
        /**
         * 禁止固定
         *
         * 类型变量不能被固定，原因可能是：
         * - 变量已被固定
         * - 变量与顶层类型相关
         * - 变量在分支点中有未处理的约束
         */
        FORBIDDEN,

        /**
         * 缺少正确的参数约束
         *
         * 类型变量没有来自参数的正确约束，只有来自类型参数上界的约束。
         * 这种情况下，类型变量不能被固定，因为缺少足够的信息。
         */
        WITHOUT_PROPER_ARGUMENT_CONSTRAINT,

        /**
         * 外部类型变量依赖
         *
         * 类型变量依赖于外部约束系统的类型变量。
         * 需要等待外部类型变量固定后才能固定此变量。
         */
        OUTER_TYPE_VARIABLE_DEPENDENCY,

        /**
         * 准备固定（声明上界包含自类型）
         *
         * 类型变量的声明上界包含自类型（Self Type），可以固定。
         */
        READY_FOR_FIXATION_DECLARED_UPPER_BOUND_WITH_SELF_TYPES,

        /**
         * 复杂依赖
         *
         * 类型变量的约束中包含其他未固定的类型变量（非顶层）。
         * 例如：T <: Foo<S>，其中 S 未固定。
         * 需要等待依赖的类型变量固定后才能固定此变量。
         */
        WITH_COMPLEX_DEPENDENCY,

        /**
         * 所有约束平凡或非正确
         *
         * 类型变量的所有约束都是平凡的（如 Nothing <: T）或非正确的。
         * 这种情况下，类型变量可以固定，但优先级较低。
         */
        ALL_CONSTRAINTS_TRIVIAL_OR_NON_PROPER,

        /**
         * 与输出类型相关
         *
         * 类型变量与某个输出类型相关。
         */
        RELATED_TO_ANY_OUTPUT_TYPE,

        /**
         * 来自声明上界的合并
         *
         * 类型变量只有来自声明上界合并的约束。
         */
        FROM_INCORPORATION_OF_DECLARED_UPPER_BOUND,

        /**
         * 准备固定（上界）
         *
         * 类型变量有足够的上界约束，可以固定。
         */
        READY_FOR_FIXATION_UPPER,

        /**
         * 准备固定（下界）
         *
         * 类型变量有足够的下界约束，可以固定。
         */
        READY_FOR_FIXATION_LOWER,

        /**
         * 准备固定
         *
         * 类型变量有足够的约束，没有复杂依赖，可以安全固定。
         */
        READY_FOR_FIXATION,

        /**
         * 准备固定（具体化）
         *
         * 类型变量是具体化的（reified），可以固定。
         * 具体化类型变量在运行时保留类型信息。
         */
        READY_FOR_FIXATION_REIFIED,
    }

    /**
     * 检查类型变量是否有正确的参数约束
     *
     * 正确的参数约束是指来自函数参数的约束，而不是来自类型参数上界的约束。
     *
     * @param variable 类型变量的类型构造器
     * @return true 如果有正确的参数约束，否则返回 false
     *
     * 注意：
     * - 临时的 hack：如果约束中包含未推导的类型参数，则认为没有正确的参数约束
     * - 这是为了让包含通过 OI 解析的可调用引用且类型参数未推导的调用失败
     */
    private fun Context.variableHasProperArgumentConstraints(variable: TypeConstructorMarker): Boolean {
        val constraints = notFixedTypeVariables[variable]?.constraints ?: return false
        // 临时的 hack：让包含通过 OI 解析的可调用引用且类型参数未推导的调用失败
        val areThereConstraintsWithUninferredTypeParameter =
            constraints.any { c -> c.type.contains { it.isUninferredParameter() } }
        return constraints.any { isProperArgumentConstraint(it) } && !areThereConstraintsWithUninferredTypeParameter
    }

    /**
     * 检查类型是否为正确类型
     *
     * 正确类型是指不包含未固定的相关类型变量的类型。
     *
     * @param type 要检查的类型
     * @return true 如果是正确类型，否则返回 false
     */
    private fun Context.isProperType(type: CangJieTypeMarker): Boolean =
        isProperTypeForFixation(
            type,
            notFixedTypeVariables.keys
        ) { t -> !t.contains { isNotFixedRelevantVariable(it) } }

    /**
     * 检查类型是否为未固定的相关类型变量
     *
     * 相关类型变量是指：
     * - 在未固定类型变量集合中
     * - 且不在被视为正确类型的类型变量集合中
     *
     * @param it 要检查的类型
     * @return true 如果是未固定的相关类型变量，否则返回 false
     */
    private fun Context.isNotFixedRelevantVariable(it: CangJieTypeMarker): Boolean {
        val key = it.typeConstructor()
        if (!notFixedTypeVariables.containsKey(key)) return false
        if (typeVariablesThatAreCountedAsProperTypes?.contains(key) == true) return false
        return true
    }

    /**
     * 检查约束是否为正确的参数约束
     *
     * 正确的参数约束需要满足：
     * 1. 约束的类型是正确类型
     * 2. 约束不是来自声明的上界
     *
     * @param c 要检查的约束
     * @return true 如果是正确的参数约束，否则返回 false
     */
    private fun Context.isProperArgumentConstraint(c: Constraint) =
        isProperType(c.type)
                && c.position.initialConstraint.position !is DeclaredUpperBoundConstraintPosition<*>


    /**
     * 获取类型变量的固定就绪状态
     *
     * 根据类型变量的约束和依赖关系，确定其固定就绪状态。
     * 就绪状态决定了类型变量的固定优先级。
     *
     * @param variable 类型变量的类型构造器
     * @param dependencyProvider 类型变量依赖信息提供者
     * @return 类型变量的固定就绪状态
     *
     * 判断逻辑（按优先级从低到高）：
     * 1. FORBIDDEN - 变量已固定、与顶层类型相关、或在分支点有未处理约束
     * 2. WITHOUT_PROPER_ARGUMENT_CONSTRAINT - 没有正确的参数约束
     * 3. OUTER_TYPE_VARIABLE_DEPENDENCY - 依赖于外部类型变量
     * 4. WITH_COMPLEX_DEPENDENCY - 依赖于其他未固定的类型变量
     * 5. ALL_CONSTRAINTS_TRIVIAL_OR_NON_PROPER - 所有约束都是平凡的或非正确的
     * 6. RELATED_TO_ANY_OUTPUT_TYPE - 与输出类型相关
     * 7. FROM_INCORPORATION_OF_DECLARED_UPPER_BOUND - 只有来自声明上界的合并约束
     * 8. READY_FOR_FIXATION - 准备固定
     */
    private fun Context.getTypeVariableReadiness(
        variable: TypeConstructorMarker,
        dependencyProvider: TypeVariableDependencyInformationProvider,
    ): TypeVariableFixationReadiness = when {
        !notFixedTypeVariables.contains(variable) || dependencyProvider.isVariableRelatedToTopLevelType(variable) ||
                variableHasUnprocessedConstraintsInForks(variable) ->
            TypeVariableFixationReadiness.FORBIDDEN

        !variableHasProperArgumentConstraints(variable) -> TypeVariableFixationReadiness.WITHOUT_PROPER_ARGUMENT_CONSTRAINT
        dependencyProvider.isRelatedToOuterTypeVariable(variable) -> TypeVariableFixationReadiness.OUTER_TYPE_VARIABLE_DEPENDENCY
        hasDependencyToOtherTypeVariables(variable) -> TypeVariableFixationReadiness.WITH_COMPLEX_DEPENDENCY
//        // TODO: 考虑移除这种就绪状态
        allConstraintsTrivialOrNonProper(variable) -> TypeVariableFixationReadiness.ALL_CONSTRAINTS_TRIVIAL_OR_NON_PROPER
        dependencyProvider.isVariableRelatedToAnyOutputType(variable) -> TypeVariableFixationReadiness.RELATED_TO_ANY_OUTPUT_TYPE
        variableHasOnlyIncorporatedConstraintsFromDeclaredUpperBound(variable) ->
            TypeVariableFixationReadiness.FROM_INCORPORATION_OF_DECLARED_UPPER_BOUND
//        isReified(variable) -> TypeVariableFixationReadiness.READY_FOR_FIXATION_REIFIED
        else -> TypeVariableFixationReadiness.READY_FOR_FIXATION
    }

    /**
     * 检查类型变量是否依赖于其他类型变量
     *
     * 如果类型变量的约束中包含其他未固定的类型变量（非顶层），则认为存在依赖。
     * 例如：T <: List<S>，其中 S 是未固定的类型变量。
     *
     * @param typeVariable 类型变量的类型构造器
     * @return true 如果依赖于其他类型变量，否则返回 false
     *
     * 检查逻辑：
     * 1. 遍历类型变量的所有约束
     * 2. 对于每个约束，检查其类型是否包含其他未固定的类型变量
     * 3. 只检查有类型参数的类型（argumentsCount != 0）
     * 4. 如果找到依赖，返回 true
     */
    private fun Context.hasDependencyToOtherTypeVariables(typeVariable: TypeConstructorMarker): Boolean {
        for (constraint in notFixedTypeVariables[typeVariable]?.constraints ?: return false) {
            val dependencyPresenceCondition = { type: CangJieTypeMarker ->
                type.typeConstructor() != typeVariable && notFixedTypeVariables.containsKey(type.typeConstructor())
            }
            if (constraint.type.lowerBoundIfFlexible().argumentsCount() != 0 && constraint.type.contains(
                    dependencyPresenceCondition
                )
            )
                return true
        }
        return false
    }

    /**
     * 检查类型变量的所有约束是否都是平凡的或非正确的
     *
     * 平凡约束是指不提供有用信息的约束，例如 Nothing <: T。
     * 非正确约束是指包含未固定类型变量的约束。
     *
     * @param variable 类型变量的类型构造器
     * @return true 如果所有约束都是平凡的或非正确的，否则返回 false
     */
    private fun Context.allConstraintsTrivialOrNonProper(variable: TypeConstructorMarker): Boolean {
        return notFixedTypeVariables[variable]?.constraints?.all { constraint ->
            trivialConstraintTypeInferenceOracle.isNotInterestingConstraint(constraint) || !isProperArgumentConstraint(
                constraint
            )
        } ?: false
    }

    /**
     * 检查类型变量是否只有来自声明上界的合并约束
     *
     * 声明上界是指类型参数声明时指定的上界约束。
     * 合并约束是指通过约束合并过程产生的约束。
     *
     * @param variable 类型变量的类型构造器
     * @return true 如果只有来自声明上界的合并约束，否则返回 false
     */
    private fun Context.variableHasOnlyIncorporatedConstraintsFromDeclaredUpperBound(variable: TypeConstructorMarker): Boolean {
        val constraints = notFixedTypeVariables[variable]?.constraints ?: return false

        return constraints.filter { isProperArgumentConstraint(it) }.all { it.position.isFromDeclaredUpperBound }
    }

    /**
     * 检查类型变量是否有非 Nothing 的下界正确约束
     *
     * 下界约束是指 A <: T 形式的约束，表示 T 的下界是 A。
     * 非 Nothing 约束是指约束类型不是 Nothing 类型。
     *
     * @param variable 类型变量的类型构造器
     * @return true 如果有非 Nothing 的下界正确约束，否则返回 false
     *
     * 检查条件：
     * 1. 约束是下界约束（kind.isLower()）
     * 2. 约束是正确的参数约束
     * 3. 约束类型不是 Nothing
     */
    private fun Context.variableHasLowerNonNothingProperConstraint(variable: TypeConstructorMarker): Boolean {
        val constraints = notFixedTypeVariables[variable]?.constraints ?: return false

        return constraints.any {
            it.kind.isLower() && isProperArgumentConstraint(it) && !it.type.typeConstructor().isNothingConstructor()
        }
    }

    /**
     * 检查类型变量在分支点中是否有未处理的约束
     *
     * 分支点（Fork Point）是类型推导中的决策点，每个分支可能产生不同的约束集合。
     * 如果类型变量在某个分支点中有未处理的约束，则不能固定该类型变量。
     *
     * @param variableConstructor 类型变量的类型构造器
     * @return true 如果在分支点中有未处理的约束，否则返回 false
     *
     * 检查逻辑：
     * 1. 如果没有分支点约束，返回 false
     * 2. 遍历所有分支点的约束
     * 3. 检查约束是否直接涉及该类型变量
     * 4. 检查约束的类型是否包含该类型变量
     */
    private fun Context.variableHasUnprocessedConstraintsInForks(variableConstructor: TypeConstructorMarker): Boolean {
        if (constraintsFromAllForkPoints.isEmpty()) return false

        for ((_, forkPointData) in constraintsFromAllForkPoints) {
            for (constraints in forkPointData) {
                for ((typeVariableFromConstraint, constraint) in constraints) {
                    if (typeVariableFromConstraint.freshTypeConstructor() == variableConstructor) return true
                    if (containsTypeVariable(constraint.type, variableConstructor)) return true
                }
            }
        }

        return false
    }

    /**
     * 查找可以固定的类型变量
     *
     * 从所有类型变量中选择就绪状态最高的类型变量进行固定。
     * 这是类型变量固定的核心方法。
     *
     * @param allTypeVariables 所有类型变量列表
     * @param postponedArguments 延迟的参数列表
     * @param completionMode 约束系统完成模式
     * @param topLevelType 顶层类型
     * @return 待固定的类型变量，如果没有可固定的变量则返回 null
     *
     * 处理流程：
     * 1. 如果没有类型变量，返回 null
     * 2. 创建依赖信息提供者，分析类型变量之间的依赖关系
     * 3. 选择就绪状态最高的类型变量作为候选
     * 4. 根据候选变量的就绪状态，返回相应的固定信息：
     *    - FORBIDDEN：返回 null（不能固定）
     *    - WITHOUT_PROPER_ARGUMENT_CONSTRAINT：返回无正确约束的固定信息
     *    - OUTER_TYPE_VARIABLE_DEPENDENCY：返回有外部依赖的固定信息
     *    - 其他：返回准备固定的固定信息
     */
    private fun Context.findTypeVariableForFixation(
        allTypeVariables: List<TypeConstructorMarker>,
        postponedArguments: List<PostponedResolvedAtomMarker>,
        completionMode: ConstraintSystemCompletionMode,
        topLevelType: CangJieTypeMarker,
    ): VariableForFixation? {
        if (allTypeVariables.isEmpty()) return null

        val dependencyProvider = TypeVariableDependencyInformationProvider(
            notFixedTypeVariables,
            postponedArguments,
            topLevelType.takeIf { completionMode == ConstraintSystemCompletionMode.PARTIAL },
            this,
        )

        val candidate =
            allTypeVariables.maxByOrNull { getTypeVariableReadiness(it, dependencyProvider) } ?: return null

        return when (getTypeVariableReadiness(candidate, dependencyProvider)) {
            TypeVariableFixationReadiness.FORBIDDEN -> null
            TypeVariableFixationReadiness.WITHOUT_PROPER_ARGUMENT_CONSTRAINT -> VariableForFixation(candidate, false)
            TypeVariableFixationReadiness.OUTER_TYPE_VARIABLE_DEPENDENCY ->
                VariableForFixation(candidate, hasProperConstraint = true, hasDependencyOnOuterTypeVariable = true)

            else -> VariableForFixation(candidate, true)
        }
    }

}

/**
 * 提取所有捕获类型的投影
 *
 * 捕获类型（Captured Type）是在处理泛型通配符时产生的临时类型。
 * 此函数递归提取类型中所有捕获类型的投影。
 *
 * @param baseType 基础类型
 * @return 所有捕获类型的投影集合
 *
 * 注意：
 * - 当前实现返回空集合
 * - 原始实现（已注释）会递归提取灵活类型和捕获类型的投影
 * - 这可能是因为仓颉语言的类型系统不需要处理捕获类型投影
 *
 * 原始逻辑（已注释）：
 * 1. 如果是灵活类型，递归提取上下界的投影
 * 2. 如果是捕获类型，提取其类型构造器投影
 * 3. 递归提取所有类型参数的投影
 */
fun TypeSystemInferenceExtensionContext.extractProjectionsForAllCapturedTypes(baseType: CangJieTypeMarker): Set<CangJieTypeMarker> {
//    if (baseType.isFlexible()) {
//        val flexibleType = baseType.asFlexibleType()!!
//        return buildSet {
//            addAll(extractProjectionsForAllCapturedTypes(flexibleType.lowerBound()))
//            addAll(extractProjectionsForAllCapturedTypes(flexibleType.upperBound()))
//        }
//    }
//    val simpleBaseType = baseType.asSimpleType()?.originalIfDefinitelyNotNullable()
//
//    return buildSet {
//        val projectionType = if (simpleBaseType is CapturedTypeMarker) {
//            val typeArgument = simpleBaseType.typeConstructorProjection().takeIf { !it.isStarProjection() } ?: return@buildSet
//            typeArgument.getType().also(::add)
//        } else baseType
//        val argumentsCount = projectionType.argumentsCount().takeIf { it != 0 } ?: return@buildSet
//
//        for (i in 0 until argumentsCount) {
//            val typeArgument = projectionType.getArgument(i).takeIf { !it.isStarProjection() } ?: continue
//            addAll(extractProjectionsForAllCapturedTypes(typeArgument.getType()))
//        }
//    }
    return emptySet()
}

/**
 * 检查类型是否包含指定的类型变量
 *
 * 递归检查类型及其捕获类型投影中是否包含指定的类型变量。
 *
 * @param type 要检查的类型
 * @param typeVariable 类型变量的类型构造器
 * @return true 如果包含指定的类型变量，否则返回 false
 *
 * 检查逻辑：
 * 1. 首先检查类型本身是否包含指定的类型变量
 *    - 使用 unwrapStubTypeVariableConstructor() 解包存根类型变量构造器
 * 2. 然后提取所有捕获类型的投影
 * 3. 检查投影中是否包含指定的类型变量
 *
 * 注意：
 * - 由于 extractProjectionsForAllCapturedTypes 返回空集合，
 *   实际上只检查类型本身是否包含类型变量
 */
fun TypeSystemInferenceExtensionContext.containsTypeVariable(
    type: CangJieTypeMarker,
    typeVariable: TypeConstructorMarker
): Boolean {
    if (type.contains { it.typeConstructor().unwrapStubTypeVariableConstructor() == typeVariable }) return true

    val typeProjections = extractProjectionsForAllCapturedTypes(type)

    return typeProjections.any { typeProjectionsType ->
        typeProjectionsType.contains { it.typeConstructor().unwrapStubTypeVariableConstructor() == typeVariable }
    }
}
