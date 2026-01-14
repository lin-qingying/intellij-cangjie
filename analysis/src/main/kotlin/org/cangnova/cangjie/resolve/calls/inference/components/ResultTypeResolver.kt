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

import org.cangnova.cangjie.config.LanguageFeature
import org.cangnova.cangjie.config.LanguageVersionSettings
import org.cangnova.cangjie.resolve.calls.CommonSuperTypeCalculator
import org.cangnova.cangjie.resolve.calls.inference.ConstraintSystemBuilder
import org.cangnova.cangjie.resolve.calls.inference.model.*
import org.cangnova.cangjie.resolve.calls.inference.runTransaction
import org.cangnova.cangjie.types.AbstractTypeApproximator
import org.cangnova.cangjie.types.AbstractTypeChecker
import org.cangnova.cangjie.types.CangJieType
import org.cangnova.cangjie.types.TypeApproximatorConfiguration
import org.cangnova.cangjie.types.checker.CangJieTypeChecker
import org.cangnova.cangjie.types.error.ErrorType
import org.cangnova.cangjie.types.model.*


/**
 * ═══════════════════════════════════════════════════════════════════════════════
 * 结果类型解析器说明
 * ═══════════════════════════════════════════════════════════════════════════════
 *
 * ResultTypeResolver 负责从约束集合中推导出类型变量的最终具体类型。
 * 这是类型推导系统的核心组件之一，在类型变量固定过程中起关键作用。
 *
 * 核心职责
 * ─────────────────────────────────────────────────────────────────────────────
 *
 * 1. **结果类型查找**
 *    - 从约束集合中确定类型变量的最佳具体类型
 *    - 处理相等约束、上界约束、下界约束
 *    - 在多个候选类型中选择最合适的类型
 *
 * 2. **类型近似**
 *    - 将内部类型（如捕获类型、整数字面量类型）近似为公共类型
 *    - 处理类型近似可能导致的矛盾
 *    - 在精确性和可用性之间取得平衡
 *
 * 3. **子类型和超类型计算**
 *    - 从下界约束计算公共超类型（最小上界）
 *    - 从上界约束计算交集类型（最大下界）
 *    - 处理整数字面量类型的特殊情况
 *
 * 4. **灵活类型处理**
 *    - 根据约束传播可空性灵活性
 *    - 创建灵活类型以避免过度限制
 *
 * 类型解析策略
 * ─────────────────────────────────────────────────────────────────────────────
 *
 * 解析优先级（从高到低）：
 *
 * 1. **相等约束** (EQUALITY)
 *    - 如果存在相等约束 T == A，直接使用 A
 *    - 相等约束具有最高优先级
 *    - 示例：T == Int → 结果类型 = Int
 *
 * 2. **方向性约束** (UPPER/LOWER)
 *    - 根据解析方向选择子类型或超类型
 *    - TO_SUBTYPE：优先使用下界约束（子类型）
 *    - TO_SUPERTYPE：优先使用上界约束（超类型）
 *    - 示例：Int <: T <: Number
 *      - TO_SUBTYPE → 结果类型 = Int
 *      - TO_SUPERTYPE → 结果类型 = Number
 *
 * 3. **默认类型**
 *    - 如果没有合适的约束，使用默认类型
 *    - TO_SUBTYPE → Nothing
 *    - TO_SUPERTYPE → Any
 *
 * 类型近似规则
 * ─────────────────────────────────────────────────────────────────────────────
 *
 * 1. **整数字面量类型 (ILT)**
 *    - 总是进行近似
 *    - 根据期望类型选择最精确的整数类型
 *    - 示例：ILT(42) → Int（如果期望类型是 Int）
 *
 * 2. **捕获类型**
 *    - 通常进行近似，除非导致矛盾
 *    - 矛盾检测：如果近似后的类型无法满足约束，则不近似
 *    - 示例：A<CapturedType(*)> → A<*>（如果不导致矛盾）
 *
 * 3. **相等约束的类型**
 *    - 永远不近似
 *    - 近似会导致矛盾
 *
 * 示例场景
 * ─────────────────────────────────────────────────────────────────────────────
 *
 * 场景 1：简单类型推导
 * ```
 * fun <T> identity(x: T): T = x
 * val result = identity(42)
 *
 * // 约束：
 * // Int <: T (来自参数)
 * // T <: T (来自返回值)
 * // 结果：T = Int
 * ```
 *
 * 场景 2：多个下界约束
 * ```
 * fun <T> choose(x: T, y: T): T = if (true) x else y
 * val result = choose(42, 3.14)
 *
 * // 约束：
 * // Int <: T
 * // Double <: T
 * // 结果：T = Number（公共超类型）
 * ```
 *
 * 场景 3：整数字面量类型
 * ```
 * fun <T> box(x: T): Box<T> = Box(x)
 * val result: Box<Short> = box(42)
 *
 * // 约束：
 * // ILT(42) <: T
 * // T <: Short (来自期望类型)
 * // 结果：T = Short（根据期望类型近似）
 * ```
 *
 * 场景 4：交集类型
 * ```
 * interface A { fun foo() }
 * interface B { fun bar() }
 * fun <T> combine(x: T) where T : A, T : B = x
 *
 * // 约束：
 * // T <: A
 * // T <: B
 * // 结果：T = A & B（交集类型）
 * ```
 *
 * 与 Kotlin 的差异
 * ─────────────────────────────────────────────────────────────────────────────
 *
 * 1. **可空性处理**
 *    - Kotlin：使用平台类型和可空性灵活性
 *    - 仓颉：使用 Option<T> 类型，更明确
 *
 * 2. **整数字面量类型**
 *    - Kotlin：ILT 可以近似为 Int、Long、Short、Byte
 *    - 仓颉：类似机制，但类型系统更简单
 *
 * 3. **捕获类型近似**
 *    - Kotlin：有复杂的捕获类型近似规则
 *    - 仓颉：简化的捕获类型处理
 *
 * ═══════════════════════════════════════════════════════════════════════════════
 */

/**
 * 结果类型解析器
 *
 * 负责从约束集合中推导出类型变量的最终具体类型。
 *
 * @property typeApproximator 类型近似器，用于将内部类型近似为公共类型
 * @property trivialConstraintTypeInferenceOracle 平凡约束类型推导预言器，用于判断类型是否合适
 * @property languageVersionSettings 语言版本设置，控制特性开关
 */
class ResultTypeResolver(
    val typeApproximator: AbstractTypeApproximator,
    val trivialConstraintTypeInferenceOracle: TrivialConstraintTypeInferenceOracle,
    private val languageVersionSettings: LanguageVersionSettings
) {
    /**
     * 结果类型解析器上下文接口
     *
     * 提供类型推导和约束系统构建所需的上下文信息。
     */
    interface Context : TypeSystemInferenceExtensionContext, ConstraintSystemBuilder {
        /**
         * 未固定的类型变量映射
         *
         * 键：类型构造器
         * 值：类型变量及其约束
         */
        val notFixedTypeVariables: Map<TypeConstructorMarker, VariableWithConstraints>

        /**
         * 外部系统变量前缀大小
         *
         * 用于 PCLA（后置 Lambda 分析）场景，标识外部系统的类型变量数量。
         */
        val outerSystemVariablesPrefixSize: Int

        /**
         * 构建未固定变量到桩类型的替换器
         *
         * 用于将未固定的类型变量替换为桩类型，以便进行类型计算。
         *
         * @return 类型替换器
         */
        fun buildNotFixedVariablesToStubTypesSubstitutor(): TypeSubstitutorMarker
//        fun isReified(variable: TypeVariableMarker): Boolean
    }
 //
//    private fun Context.getDefaultTypeForSelfType(
//        constraints: List<Constraint>,
//        typeVariable: TypeVariableMarker
//    ): CangJieTypeMarker? {
//        val typeVariableConstructor = typeVariable.freshTypeConstructor() as TypeVariableTypeConstructorMarker
//        val typesForRecursiveTypeParameters = constraints.mapNotNull { constraint ->
//            if (constraint.position.from !is DeclaredUpperBoundConstraintPosition<*>) return@mapNotNull null
//            val typeParameter = typeVariableConstructor.typeParameter ?: return@mapNotNull null
//            extractTypeForGivenRecursiveTypeParameter(constraint.type, typeParameter)
//        }.takeIf { it.isNotEmpty() } ?: return null
//
//        return createCapturedStarProjectionForSelfType(typeVariableConstructor, typesForRecursiveTypeParameters)
//    }

    /**
     * 获取默认类型
     *
     * 当没有合适的约束可以推导出具体类型时，返回默认类型。
     *
     * @param direction 解析方向
     * @param constraints 约束列表（当前未使用，保留用于未来扩展）
     * @param typeVariable 类型变量（当前未使用，保留用于未来扩展）
     * @return 默认类型
     *
     * 默认类型规则：
     * - TO_SUBTYPE：返回 Nothing（最小类型）
     * - TO_SUPERTYPE 或 UNKNOWN：返回 Any（最大类型）
     *
     * 示例：
     * ```
     * fun <T> foo(): T = ...
     * // 没有约束，TO_SUBTYPE → T = Nothing
     * // 没有约束，TO_SUPERTYPE → T = Any
     * ```
     */
    private fun Context.getDefaultType(
        direction: TypeVariableDirectionCalculator.ResolveDirection,
        constraints: List<Constraint>,
        typeVariable: TypeVariableMarker
    ): CangJieTypeMarker {
//        if (isTypeInferenceForSelfTypesSupported) {
//            getDefaultTypeForSelfType(constraints, typeVariable)?.let { return it }
//        }

        return if (direction == TypeVariableDirectionCalculator.ResolveDirection.TO_SUBTYPE) nothingType() else anyType()
    }

    /**
     * 查找结果类型
     *
     * 从约束集合中推导出类型变量的最终具体类型。
     * 这是结果类型解析器的主要入口方法。
     *
     * @param c 解析上下文
     * @param variableWithConstraints 类型变量及其约束
     * @param direction 解析方向（TO_SUBTYPE 或 TO_SUPERTYPE）
     * @return 推导出的结果类型
     *
     * 解析流程：
     * 1. 尝试通过 findResultTypeOrNull 推导结果类型
     * 2. 如果推导成功，返回推导出的类型
     * 3. 如果推导失败（没有合适的约束），返回默认类型
     *
     * 示例：
     * ```
     * fun <T> identity(x: T): T = x
     * identity(42)
     *
     * // 约束：Int <: T, T <: T
     * // 方向：TO_SUBTYPE
     * // 结果：T = Int
     * ```
     */
    fun findResultType(
        c: Context,
        variableWithConstraints: VariableWithConstraints,
        direction: TypeVariableDirectionCalculator.ResolveDirection
    ): CangJieTypeMarker {
        findResultTypeOrNull(c, variableWithConstraints, direction)?.let { return it }

        // 没有合适的约束，返回默认类型
        return c.getDefaultType(direction, variableWithConstraints.constraints, variableWithConstraints.typeVariable)
    }

    /**
     * 将类型近似为超类型或保持不变
     *
     * 用于处理内部类型（如整数字面量类型、捕获类型）的近似。
     *
     * @param c 解析上下文
     * @param superTypeCandidate 超类型候选（用于整数字面量类型的精确近似）
     * @return 近似后的类型或原类型
     *
     * 近似规则：
     * 1. 如果是整数字面量类型 (ILT)：
     *    - 使用超类型候选作为期望类型进行近似
     *    - 这比总是近似为 Int 或 UInt 更精确
     *    - 示例：ILT(42) + 期望类型 Short → Short
     *
     * 2. 其他类型：
     *    - 使用内部类型近似配置进行近似
     *    - 主要处理捕获类型等内部类型
     *
     * 注意：
     * - 不应该有嵌套的 ILT，因为它们只能作为类型变量的约束出现
     * - 如果近似失败，返回原类型
     */
    private fun CangJieTypeMarker.approximateToSuperTypeOrSelf(
        c: Context,
        superTypeCandidate: CangJieTypeMarker?
    ): CangJieTypeMarker {
        // 如果子类型是整数字面量类型，使用上界类型作为期望类型进行近似
        // 这比总是近似为 Int 或 UInt 更精确
        // 注意：不应该有嵌套的 ILT，因为它们只能作为类型变量的约束出现，
        // 而这些类型变量应该已经被固定了
        if (typeConstructor(c).isIntegerLiteralTypeConstructor(c)) {
            return typeApproximator.approximateToSuperType(
                this,
                TypeApproximatorConfiguration.TopLevelIntegerLiteralTypeApproximationWithExpectedType(superTypeCandidate)
            ) ?: this
        }

        return typeApproximator.approximateToSuperType(this, TypeApproximatorConfiguration.InternalTypesApproximation)
            ?: this
    }

    /**
     * 将类型近似为子类型或保持不变
     *
     * 用于处理内部类型的近似，主要用于上界约束的处理。
     *
     * @return 近似后的类型或原类型
     *
     * 使用内部类型近似配置，如果近似失败则返回原类型。
     */
    private fun CangJieTypeMarker.approximateToSubTypeOrSelf(): CangJieTypeMarker {
        return typeApproximator.approximateToSubType(this, TypeApproximatorConfiguration.InternalTypesApproximation)
            ?: this
    }

//    private val useImprovedCapturedTypeApproximation: Boolean =
//        languageVersionSettings.supportsFeature(LanguageFeature.ImprovedCapturedTypeApproximationInInference)

    /**
     * 查找结果类型（可能返回 null）
     *
     * 从约束集合中推导出类型变量的具体类型，如果无法推导则返回 null。
     * 这是类型推导的核心方法，实现了完整的类型解析逻辑。
     *
     * @param c 解析上下文
     * @param variableWithConstraints 类型变量及其约束
     * @param direction 解析方向
     * @return 推导出的结果类型，如果无法推导则返回 null
     *
     * 解析流程：
     *
     * 1. **检查相等约束**
     *    - 查找是否存在相等约束 (T == A)
     *    - 如果存在且不包含整数字面量常量类型构造器，直接返回
     *
     * 2. **计算子类型和超类型**
     *    - 从下界约束计算子类型（公共超类型）
     *    - 从上界约束计算超类型（交集类型）
     *
     * 3. **准备类型（近似处理）**
     *    - 对子类型和超类型进行近似处理
     *    - 处理整数字面量类型和捕获类型
     *
     * 4. **根据方向选择结果类型**
     *    - TO_SUBTYPE 或 UNKNOWN：优先使用子类型
     *    - TO_SUPERTYPE：优先使用超类型
     *
     * 5. **合并相等约束和方向约束的结果**
     *    - 如果只有一种结果，返回该结果
     *    - 如果两种结果都存在，选择更精确的类型
     *    - 优先选择非 Nothing 且更具体的类型
     *
     * 类型选择逻辑：
     * - 如果相等约束结果为 null，返回方向约束结果
     * - 如果方向约束结果为 null，返回相等约束结果
     * - 如果方向约束结果不是 Nothing 且是相等约束结果的子类型，返回方向约束结果
     *   （实际上只允许 Int/Short/Byte/Long 等精确类型）
     * - 否则返回相等约束结果（基于整数字面量类型）
     *
     * 示例：
     * ```
     * // 场景 1：只有相等约束
     * fun <T> foo(x: T, y: T) = ...
     * foo(1, 2)
     * // 约束：T == Int
     * // 结果：T = Int
     *
     * // 场景 2：只有方向约束
     * fun <T> bar(x: T): Number = x
     * bar(42)
     * // 约束：Int <: T, T <: Number
     * // 方向：TO_SUBTYPE
     * // 结果：T = Int
     *
     * // 场景 3：相等约束 + 方向约束
     * fun <T> baz(x: T, y: T): Short = ...
     * baz(1, 2)
     * // 相等约束：T == ILT(1,2)
     * // 方向约束：T <: Short
     * // 结果：T = Short（更精确）
     * ```
     */
    fun findResultTypeOrNull(
        c: Context,
        variableWithConstraints: VariableWithConstraints,
        direction: TypeVariableDirectionCalculator.ResolveDirection,
    ): CangJieTypeMarker? {
        // 1. 检查是否存在相等约束
        val resultTypeFromEqualConstraint = findResultIfThereIsEqualsConstraint(c, variableWithConstraints)
        if (resultTypeFromEqualConstraint != null) {
            with(c) {
                // 如果相等约束的结果不包含整数字面量常量类型构造器，直接返回
                if (!resultTypeFromEqualConstraint.contains { type ->
                        type.typeConstructor().isIntegerLiteralConstantTypeConstructor()
                    }
                ) {

                    return resultTypeFromEqualConstraint
                }
            }
        }

        // 2. 计算子类型和超类型
        val subType = c.findSubType(variableWithConstraints)
        val superType = c.findSuperType(variableWithConstraints)

        // 3. 准备类型（近似处理）
        val (preparedSubType, preparedSuperType) = c.prepareSubAndSuperTypesLegacy(
            subType,
            superType,
            variableWithConstraints
        )


        // 4. 根据方向选择结果类型
        val resultTypeFromDirection =
            if (direction == TypeVariableDirectionCalculator.ResolveDirection.TO_SUBTYPE || direction == TypeVariableDirectionCalculator.ResolveDirection.UNKNOWN) {
                c.resultType(preparedSubType, preparedSuperType, variableWithConstraints)
            } else {
                c.resultType(preparedSuperType, preparedSubType, variableWithConstraints)
            }

        // 5. 合并相等约束和方向约束的结果
        // 一般情况下，我们可能有两种类型：
        // - 一种来自相等约束，必须是基于整数字面量类型的
        // - 另一种来自上界/下界约束（基于子类型/超类型）
        // 选择逻辑：
        // - 如果一种类型为 null，返回另一种
        // - 如果方向约束的类型更精确（实际上只允许 Int/Short/Byte/Long），返回方向约束的类型
        // - 否则返回基于整数字面量类型的类型
        return when {
            resultTypeFromEqualConstraint == null -> resultTypeFromDirection
            resultTypeFromDirection == null -> resultTypeFromEqualConstraint
            with(c) { !resultTypeFromDirection.typeConstructor().isNothingConstructor() } &&
                    CangJieTypeChecker.DEFAULT.isSubtypeOf(
                        resultTypeFromDirection as CangJieType,
                        resultTypeFromEqualConstraint as CangJieType
                    ) -> resultTypeFromDirection

            else -> resultTypeFromEqualConstraint
        }
    }

    /**
     * 准备子类型和超类型（K2 版本）
     *
     * K2 中结果类型近似的一般方法：
     * - 总是近似整数字面量类型 (ILT)
     * - 总是近似捕获类型，除非这会导致矛盾
     *
     * 矛盾场景：
     * 如果子类型和超类型中都有相同的捕获类型 C = CapturedType(*)，
     * 近似可能导致矛盾。
     *
     * 示例：A<C> <: T <: A<C>
     * 如果近似结果类型，会得到矛盾：A<*> </: A<C>
     *
     * 对比：
     * - 来自相等约束的类型永远不近似，因为近似总是会导致矛盾
     * - 我们评估过"永不近似"的方法，但发现不可行，因为会引入许多新错误
     *   （类型不匹配、REIFIED_TYPE_FORBIDDEN_SUBSTITUTION、TYPE_INFERENCE_ONLY_INPUT_TYPES_ERROR 等）
     *
     * @param subType 子类型（来自下界约束）
     * @param superType 超类型（来自上界约束）
     * @param variableWithConstraints 类型变量及其约束
     * @return 准备好的子类型和超类型对
     *
     * 注意：此方法在当前实现中未使用，保留用于未来的 K2 风格近似。
     */
    private fun Context.prepareSubAndSuperTypes(
        subType: CangJieTypeMarker?,
        superType: CangJieTypeMarker?,
        variableWithConstraints: VariableWithConstraints,
    ): Pair<CangJieTypeMarker?, CangJieTypeMarker?> {
        // 近似子类型和超类型
        val approximatedSubType = subType?.approximateToSuperTypeOrSelf(this, superType)
        val approximatedSuperType = superType?.approximateToSubTypeOrSelf()

        // 检查子类型是否应该不近似使用
        val preparedSubType = when {
            approximatedSubType == null -> null
            shouldBeUsedWithoutApproximation(subType, approximatedSubType, variableWithConstraints, this) -> subType
            else -> approximatedSubType
        }

        // 检查超类型是否应该不近似使用
        val preparedSuperType = when {
            approximatedSuperType == null -> null
            shouldBeUsedWithoutApproximation(
                superType,
                approximatedSuperType,
                variableWithConstraints,
                this
            ) -> superType
//            hasRecursiveTypeParametersWithGivenSelfType(superType.typeConstructor(this)) -> superType
            else -> approximatedSuperType
            // 超类型应该是最灵活的，子类型应该是最不灵活的
        }.makeFlexibleIfNecessary(this, variableWithConstraints.constraints)

        return preparedSubType to preparedSuperType
    }

    /**
     * 检查是否应该不近似使用原类型
     *
     * 如果使用近似后的类型作为结果类型会导致矛盾，则返回 true。
     *
     * @param resultType 原始结果类型
     * @param approximatedResultType 近似后的结果类型
     * @param variableWithConstraints 类型变量及其约束
     * @param c 解析上下文
     * @return 如果应该使用原类型（不近似）返回 true，否则返回 false
     *
     * 判断逻辑：
     * 1. 如果原类型和近似类型引用相等，说明没有进行近似，返回 false
     * 2. 如果已经存在矛盾，返回 false
     * 3. 如果原类型是整数字面量类型，返回 false（总是近似）
     * 4. 通过事务测试：将近似类型作为相等约束添加，检查是否产生矛盾
     *    - 如果产生矛盾，返回 true（应该使用原类型）
     *    - 如果不产生矛盾，返回 false（可以使用近似类型）
     *
     * 注意：
     * - 仅在启用 LanguageFeature.ImprovedCapturedTypeApproximationInInference 时使用
     * - 使用事务机制确保测试不会影响实际的约束系统状态
     */
    private fun shouldBeUsedWithoutApproximation(
        resultType: CangJieTypeMarker,
        approximatedResultType: CangJieTypeMarker,
        variableWithConstraints: VariableWithConstraints,
        c: Context,
    ): Boolean {
        // 如果引用相等或已有矛盾，返回 false
        if (resultType === approximatedResultType || c.hasContradiction) return false

        // 整数字面量类型总是近似
        if (resultType.typeConstructor(c).isIntegerLiteralTypeConstructor(c)) return false

        // 通过事务测试近似是否会导致矛盾
        var createsContradiction = false
        c.runTransaction {
            addEqualityConstraint(
                approximatedResultType,
                variableWithConstraints.typeVariable.defaultType(c),
                SimpleConstraintSystemConstraintPosition
            )
            createsContradiction = hasContradiction
            false // 回滚事务
        }
        return createsContradiction
    }

    /**
     * 准备子类型和超类型（传统版本）
     *
     * 这是传统的类型准备方法，使用简单的近似策略。
     * 与 K2 版本的 prepareSubAndSuperTypes 不同，此方法不检查近似是否导致矛盾。
     *
     * @param subType 子类型（来自下界约束）
     * @param superType 超类型（来自上界约束）
     * @param variableWithConstraints 类型变量及其约束
     * @return 准备好的子类型和超类型对
     *
     * 处理流程：
     * 1. **准备子类型**
     *    - 如果为 null，保持 null
     *    - 否则，尝试近似为超类型
     *    - 如果近似失败，使用原类型
     *
     * 2. **准备超类型**
     *    - 如果为 null，保持 null
     *    - 否则，尝试近似为子类型
     *    - 如果近似失败，使用原类型
     *    - 根据约束添加可空性灵活性
     *
     * 注意：
     * - 超类型应该是最灵活的，子类型应该是最不灵活的
     * - 这是当前使用的主要类型准备方法
     */
    private fun Context.prepareSubAndSuperTypesLegacy(
        subType: CangJieTypeMarker?,
        superType: CangJieTypeMarker?,
        variableWithConstraints: VariableWithConstraints,
    ): Pair<CangJieTypeMarker?, CangJieTypeMarker?> {


        // 准备子类型：近似为超类型或保持不变
        val preparedSubType = when {
            subType == null -> null

            else -> typeApproximator.approximateToSuperType(
                subType,
                TypeApproximatorConfiguration.InternalTypesApproximation
            ) ?: subType
        }

        // 准备超类型：近似为子类型或保持不变，并添加灵活性
        val preparedSuperType = when {
            superType == null -> null

            else -> typeApproximator.approximateToSubType(
                superType,
                TypeApproximatorConfiguration.InternalTypesApproximation
            ) ?: superType
            // 超类型应该是最灵活的，子类型应该是最不灵活的
        }.makeFlexibleIfNecessary(this, variableWithConstraints.constraints)

        return preparedSubType to preparedSuperType
    }

    /**
     * 检查是否为相似或紧密绑定的捕获类型（旧启发式方法）
     *
     * 这是用于确定何时应该近似来自下界/上界约束的结果类型的旧启发式方法。
     *
     * @param subType 子类型
     * @param superType 超类型
     * @return 如果是相似或紧密绑定的捕获类型返回 true
     *
     * 判断条件：
     * 1. 子类型和超类型都不为 null
     * 2. 子类型的下界是捕获类型构造器
     * 3. 满足以下任一条件：
     *    a) 超类型在子类型下界构造器的超类型中，且超类型包含捕获类型
     *    b) 子类型的下界和上界构造器相同，且与超类型的下界和上界构造器都相同
     *
     * 注意：
     * - 在启用 LanguageFeature.ImprovedCapturedTypeApproximationInInference 后，此方法已过时
     * - 保留用于向后兼容
     */
    private fun Context.similarOrCloselyBoundCapturedTypes(
        subType: CangJieTypeMarker?,
        superType: CangJieTypeMarker?
    ): Boolean {
        if (subType == null) return false
        if (superType == null) return false
        val subTypeLowerConstructor = subType.lowerBoundIfFlexible().typeConstructor()
        if (!subTypeLowerConstructor.isCapturedTypeConstructor()) return false

        // 检查超类型是否在子类型的超类型中，且包含捕获类型
        if (superType in subTypeLowerConstructor.supertypes() && superType.contains {
                it.typeConstructor().isCapturedTypeConstructor()
            }) {
            return true
        }

        // 检查子类型和超类型的边界是否都相同
        return subTypeLowerConstructor == subType.upperBoundIfFlexible().typeConstructor() &&
                subTypeLowerConstructor == superType.lowerBoundIfFlexible().typeConstructor() &&
                subTypeLowerConstructor == superType.upperBoundIfFlexible().typeConstructor()
    }

    /**
     * 根据需要使类型变为灵活类型
     *
     * 我们将可空性灵活性从其他约束中的类型变量传播到结果类型中，
     * 以防止变量固定为不够灵活的类型。
     *
     * @param c 解析上下文
     * @param constraints 约束列表
     * @return 可能添加了灵活性的类型
     *
     * 传播规则：
     * - 如果约束中存在类型变量且该类型变量具有灵活的 Option 类型，
     *   则将结果类型转换为灵活类型
     *
     * 示例：
     * ```
     * // 约束：
     * //   UPPER(TypeVariable(T)..TypeVariable(T)?)
     * //   UPPER(Foo?)
     * // 结果类型 = makeFlexibleIfNecessary(Foo?) = Foo!
     * ```
     *
     * 注意：
     * - 我们不在深度上传播可空性灵活性，因为目前尚未确定：
     *   - CST(Bar<Foo>, Bar<Foo!>) = Bar<Foo!>
     *   - CST(Bar<Foo!>, Bar<Foo>) = Bar<Foo>
     *   - 但是：CST(Foo, Foo!) = CST(Foo!, Foo) = Foo!
     * - 仓颉语言特殊处理：Option 是确切的类型，直接使用原始类型
     */
    private fun CangJieTypeMarker?.makeFlexibleIfNecessary(c: Context, constraints: List<Constraint>) = with(c) {
        when (val type = this@makeFlexibleIfNecessary) {
            is SimpleTypeMarker -> {
                // 检查约束中是否有类型变量具有灵活的 Option 类型
                if (constraints.any {
                        it.type.typeConstructor().isTypeVariable() && it.type.hasFlexibleOption()
                    }) {
                    // 仓颉语言：移除 makeSimpleTypeDefinitelyNonOptionOrNonOption 调用
                    // 在仓颉中，Option 是确切的类型，直接使用原始类型即可
                    createFlexibleType(type, type.withOption(true))
                } else type
            }

            else -> type
        }
    }

    /**
     * 从两个候选类型中选择结果类型
     *
     * 根据类型的适用性选择最合适的结果类型。
     *
     * @param firstCandidate 第一个候选类型
     * @param secondCandidate 第二个候选类型
     * @param variableWithConstraints 类型变量及其约束
     * @return 选择的结果类型
     *
     * 选择逻辑：
     * 1. 如果任一候选为 null，返回非 null 的候选
     * 2. 如果第一个候选是错误类型，直接返回（保留错误信息）
     * 3. 检查是否为交集类型的特殊情况
     * 4. 如果第一个候选是合适的类型，返回第一个候选
     * 5. 如果第二个候选是合适的类型，返回第二个候选
     * 6. 否则返回第一个候选（作为默认选择）
     *
     * 示例：
     * ```
     * // 场景 1：一个为 null
     * resultType(Int, null, ...) → Int
     *
     * // 场景 2：第一个合适
     * resultType(Int, Number, ...) → Int（如果 Int 满足所有约束）
     *
     * // 场景 3：第二个合适
     * resultType(Nothing, Int, ...) → Int（如果 Nothing 不合适但 Int 合适）
     * ```
     */
    private fun Context.resultType(
        firstCandidate: CangJieTypeMarker?,
        secondCandidate: CangJieTypeMarker?,
        variableWithConstraints: VariableWithConstraints
    ): CangJieTypeMarker? {
        // 如果任一候选为 null，返回非 null 的候选
        if (firstCandidate == null || secondCandidate == null) return firstCandidate ?: secondCandidate

        // 如果第一个候选是错误类型，直接返回
        if (firstCandidate is ErrorType) return firstCandidate

        // 检查交集类型的特殊情况
        specialResultForIntersectionType(firstCandidate, secondCandidate)?.let { intersectionWithAlternative ->
            return intersectionWithAlternative
        }

        // 如果第一个候选合适，返回第一个候选
        if (isSuitableType(firstCandidate, variableWithConstraints)) return firstCandidate

        // 如果第二个候选合适，返回第二个候选；否则返回第一个候选
        return if (isSuitableType(secondCandidate, variableWithConstraints)) {
            secondCandidate
        } else {
            firstCandidate
        }
    }

    /**
     * 处理交集类型的特殊结果
     *
     * 当第一个候选是交集类型时，检查是否需要创建带上界的交集类型。
     *
     * @param firstCandidate 第一个候选类型
     * @param secondCandidate 第二个候选类型
     * @return 如果需要特殊处理，返回带上界的交集类型；否则返回 null
     *
     * 处理逻辑：
     * - 如果第一个候选是交集类型
     * - 且第一个候选的公共类型不是第二个候选的子类型
     * - 则创建带上界的交集类型
     *
     * 这用于处理交集类型无法直接满足约束的情况。
     */
    private fun Context.specialResultForIntersectionType(
        firstCandidate: CangJieTypeMarker,
        secondCandidate: CangJieTypeMarker,
    ): CangJieTypeMarker? {
        // 如果第一个候选是交集类型
        if (firstCandidate.typeConstructor().isIntersection()) {
            // 检查第一个候选的公共类型是否是第二个候选的子类型
            if (!CangJieTypeChecker.DEFAULT.isSubtypeOf(firstCandidate.toPublicType() as CangJieType, secondCandidate.toPublicType() as CangJieType)) {
                // 如果不是子类型，创建带上界的交集类型
                return createTypeWithUpperBoundForIntersectionResult(firstCandidate, secondCandidate)
            }
        }

        return null
    }

    /**
     * 将类型转换为公共类型
     *
     * 使用类型近似器将内部类型转换为可以公开声明的类型。
     *
     * @return 公共类型，如果近似失败则返回原类型
     *
     * 使用场景：
     * - 将交集类型转换为可以在公共 API 中使用的类型
     * - 保留匿名类型以避免信息丢失
     */
    private fun CangJieTypeMarker.toPublicType(): CangJieTypeMarker =
        typeApproximator.approximateToSuperType(
            this,
            TypeApproximatorConfiguration.PublicDeclaration.SaveAnonymousTypes
        ) ?: this

    /**
     * 检查类型是否适合作为结果类型
     *
     * 验证给定的类型是否满足所有约束，并且符合类型固定的要求。
     *
     * @param resultType 待检查的结果类型
     * @param variableWithConstraints 类型变量及其约束
     * @return 如果类型适合返回 true，否则返回 false
     *
     * 检查流程：
     * 1. 过滤出所有正确类型的约束
     * 2. 验证结果类型是否满足所有约束
     * 3. 如果结果类型不是 Nothing，检查是否为合适的结果类型
     * 4. 对于 Nothing 类型，进行特殊处理：
     *    - Nothing 和 Nothing? 不允许用于具体化参数（当前已注释）
     *    - 非可空的 Nothing 可以作为结果类型（如果参数不是具体化的）
     *    - 可空的 Nothing? 不适合作为结果类型
     *
     * 示例：
     * ```
     * // 场景 1：满足所有约束
     * // 约束：Int <: T, T <: Number
     * // 结果类型：Int
     * // 返回：true
     *
     * // 场景 2：不满足约束
     * // 约束：Int <: T, T <: Number
     * // 结果类型：String
     * // 返回：false
     *
     * // 场景 3：Nothing 类型
     * // 结果类型：Nothing
     * // 返回：true（非可空）
     *
     * // 场景 4：Nothing? 类型
     * // 结果类型：Nothing?
     * // 返回：false（可空）
     * ```
     */
    private fun Context.isSuitableType(
        resultType: CangJieTypeMarker,
        variableWithConstraints: VariableWithConstraints
    ): Boolean {
        // 过滤出所有正确类型的约束
        val filteredConstraints = variableWithConstraints.constraints.filter { isProperTypeForFixation(it.type) }

        // 验证结果类型是否满足所有约束
        for (constraint in filteredConstraints) {
            if (!checkConstraint(this, constraint.type, constraint.kind, resultType)) return false
        }

        // 如果结果类型不是 Nothing，检查是否为合适的结果类型
        if (trivialConstraintTypeInferenceOracle.isSuitableResultedType(resultType)) return true

        // Nothing 和 Nothing? 不允许用于具体化参数
//        if (isReified(variableWithConstraints.typeVariable)) return false

        // 非可空的 Nothing 可以作为结果类型（如果参数不是具体化的）
        if (!resultType.isOptionType()) return true

        return false
    }



    /**
     * 检查所有上界约束是否都来自类型参数边界
     *
     * 用于判断约束是否都是从类型参数的声明边界派生的。
     *
     * @param constraints 约束列表
     * @return 如果所有上界约束都来自边界返回 true
     *
     * 检查逻辑：
     * - 对于非上界约束（下界约束），总是返回 true
     * - 对于上界约束，检查是否来自类型参数的声明边界
     *
     * 注意：
     * - 对于正确的代码，下界约束应该具有 `Nothing?` 类型
     * - 否则如果有 `Nothing? <: T` 和 `SomethingElse <: T`，
     *   最终会得到 `SomethingElse? <: T`
     */
    private fun allUpperConstraintsAreFromBounds(constraints: List<Constraint>): Boolean =
        constraints.all {
            // 实际上，至少对于正确的代码，下界约束（!isUpper）应该具有 `Nothing?` 类型
            // 因为否则如果我们有 `Nothing? <: T` 和 `SomethingElse <: T`，
            // 那么最终会得到 `SomethingElse? <: T`
            !it.kind.isUpper() || isFromTypeParameterUpperBound(it)
        }

    /**
     * 检查约束是否来自类型参数的上界
     *
     * @param constraint 约束
     * @return 如果约束来自类型参数的声明上界返回 true
     *
     * 判断条件：
     * - 约束位置标记为来自声明上界
     * - 或约束位置的来源是 DeclaredUpperBoundConstraintPosition
     */
    private fun isFromTypeParameterUpperBound(constraint: Constraint): Boolean =
        constraint.position.isFromDeclaredUpperBound || constraint.position.from is DeclaredUpperBoundConstraintPosition<*>



    // ═══════════════════════════════════════════════════════════════════════════════
    // 类型推导方法
    // ═══════════════════════════════════════════════════════════════════════════════

    /**
     * 查找子类型（从下界约束推导）
     *
     * 从类型变量的下界约束中计算公共超类型，作为类型变量的子类型候选。
     *
     * @param variableWithConstraints 类型变量及其约束
     * @return 计算出的子类型，如果没有下界约束则返回 null
     *
     * 计算流程：
     * 1. 准备下界约束列表（过滤和替换）
     * 2. 如果没有正确的下界约束，返回 null
     * 3. 将整数字面量类型下沉到列表末尾（优先使用具体类型）
     * 4. 计算所有下界类型的公共超类型
     * 5. 如果结果包含桩类型，进行特殊处理：
     *    - 过滤掉包含桩类型的约束
     *    - 如果还有剩余约束，重新计算公共超类型
     *    - 如果没有剩余约束（仅 PCLA 场景），替换桩类型为类型变量
     *
     * 示例：
     * ```
     * // 场景 1：单个下界约束
     * // 约束：Int <: T
     * // 结果：Int
     *
     * // 场景 2：多个下界约束
     * // 约束：Int <: T, Double <: T
     * // 结果：Number（公共超类型）
     *
     * // 场景 3：包含整数字面量类型
     * // 约束：ILT(42) <: T, Int <: T
     * // 结果：Int（ILT 被下沉，优先使用 Int）
     * ```
     */
    private fun Context.findSubType(variableWithConstraints: VariableWithConstraints): CangJieTypeMarker? {
        // 1. 准备下界约束列表
        val lowerConstraintTypes = prepareLowerConstraints(variableWithConstraints.constraints)

        if (lowerConstraintTypes.isNotEmpty()) {
            // 3. 将整数字面量类型下沉到列表末尾
            val types = sinkIntegerLiteralTypes(lowerConstraintTypes)
            // 4. 计算公共超类型
            var commonSuperType = computeCommonSuperType(types)

            // 5. 如果结果包含桩类型，进行特殊处理
            if (commonSuperType.contains { it.asSimpleType()?.isStubTypeForVariableInSubtyping() == true }) {
                // 过滤掉包含桩类型的约束
                val typesWithoutStubs = types.filter { lowerType ->
                    !lowerType.contains { it.asSimpleType()?.isStubTypeForVariableInSubtyping() == true }
                }

                when {
                    // 如果还有不包含桩类型的约束，重新计算公共超类型
                    typesWithoutStubs.isNotEmpty() -> {
                        commonSuperType = computeCommonSuperType(typesWithoutStubs)
                    }
                    // `typesWithoutStubs.isEmpty()` 意味着没有不包含类型变量的下界约束
                    // 这只在 PCLA 场景中可能发生，因为否则没有约束会被视为正确的
                    // 所以，我们获取当前计算的 `commonSuperType` 并将所有局部桩类型
                    // 替换为相应的类型变量
                    outerSystemVariablesPrefixSize > 0 -> {
                        // outerSystemVariablesPrefixSize > 0 仅用于 PCLA (K2)

                        commonSuperType =
                            createSubstitutionFromSubtypingStubTypesToTypeVariables().safeSubstitute(commonSuperType)
                    }
                }
            }

            return commonSuperType
        }

        // 2. 如果没有下界约束，返回 null
        return null
    }

    /**
     * 计算公共超类型
     *
     * 使用公共超类型计算器计算给定类型列表的公共超类型。
     *
     * @param types 类型列表
     * @return 公共超类型
     */
    private fun Context.computeCommonSuperType(types: List<CangJieTypeMarker>): CangJieTypeMarker =
        with(CommonSuperTypeCalculator) { commonSuperType(types) }

    /**
     * 准备下界约束列表
     *
     * 从约束列表中提取和准备下界约束的类型。
     *
     * @param constraints 约束列表
     * @return 准备好的下界约束类型列表
     *
     * 处理流程：
     * 1. 遍历所有约束，提取下界约束（LOWER）
     * 2. 检查是否至少有一个正确类型的约束
     * 3. 如果没有正确类型的约束，返回空列表
     * 4. 对于 PCLA 慢路径（outerSystemVariablesPrefixSize > 0）：
     *    - 将所有未固定的类型变量替换为桩类型
     * 5. 对于普通情况：
     *    - 如果所有约束都是正确类型，直接返回
     *    - 否则，将非正确类型的约束替换为桩类型
     *
     * 示例：
     * ```
     * // 场景 1：所有约束都是正确类型
     * // 约束：Int <: T, Double <: T
     * // 结果：[Int, Double]
     *
     * // 场景 2：包含类型变量
     * // 约束：Int <: T, U <: T（U 未固定）
     * // 结果：[Int, StubType(U)]
     * ```
     */
    private fun Context.prepareLowerConstraints(constraints: List<Constraint>): List<CangJieTypeMarker> {
        var atLeastOneProper = false
        var atLeastOneNonProper = false

        val lowerConstraintTypes = mutableListOf<CangJieTypeMarker>()

        // 1. 遍历所有约束，提取下界约束
        for (constraint in constraints) {
            if (constraint.kind != ConstraintKind.LOWER) continue

            val type = constraint.type

            lowerConstraintTypes.add(type)

            // 检查类型是否为正确类型
            if (isProperTypeForFixation(type)) {
                atLeastOneProper = true
            } else {
                atLeastOneNonProper = true
            }
        }

        // 3. 如果没有正确类型的约束，返回空列表
        if (!atLeastOneProper) return emptyList()

        // 4. PCLA 慢路径
        // 我们只允许在嵌套的 PCLA 调用中使用类型变量固定
        if (outerSystemVariablesPrefixSize > 0) {
            val notFixedToStubTypesSubstitutor = buildNotFixedVariablesToStubTypesSubstitutor()
            return lowerConstraintTypes.map { notFixedToStubTypesSubstitutor.safeSubstitute(it) }
        }

        // 5. 如果所有约束都是正确类型，直接返回
        if (!atLeastOneNonProper) return lowerConstraintTypes

        // 否则，将非正确类型的约束替换为桩类型
        val notFixedToStubTypesSubstitutor = buildNotFixedVariablesToStubTypesSubstitutor()

        return lowerConstraintTypes.map {
            if (isProperTypeForFixation(it)) it else notFixedToStubTypesSubstitutor.safeSubstitute(
                it
            )
        }
    }

    /**
     * 将整数字面量类型下沉到列表末尾
     *
     * 对类型列表进行排序，将包含整数字面量类型的类型移到列表末尾。
     * 这样可以优先使用更具体的类型（如 Int、Short）而不是整数字面量类型。
     *
     * @param types 类型列表
     * @return 排序后的类型列表
     *
     * 排序规则：
     * - 不包含整数字面量类型的类型排在前面（优先级 0）
     * - 包含整数字面量类型的类型排在后面（优先级 1）
     *
     * 示例：
     * ```
     * // 输入：[ILT(42), Int, ILT(100), Double]
     * // 输出：[Int, Double, ILT(42), ILT(100)]
     * ```
     */
    private fun Context.sinkIntegerLiteralTypes(types: List<CangJieTypeMarker>): List<CangJieTypeMarker> {
        return types.sortedBy { type ->
            // 检查类型是否包含整数字面量类型
            val containsILT = type.contains { it.asSimpleType()?.isIntegerLiteralType() ?: false }
            if (containsILT) 1 else 0
        }
    }

    /**
     * 计算上界类型（从上界约束推导）
     *
     * 从上界约束计算交集类型，作为类型变量的超类型候选。
     *
     * @param upperConstraints 上界约束列表
     * @return 计算出的上界类型（交集类型）
     *
     * 计算流程：
     * 1. 提取所有上界约束的类型
     * 2. 计算这些类型的交集类型
     * 3. 检查交集类型是否包含不希望的类型组合：
     *    - 多个非接口的类类型
     *    - Final 类（已注释）
     * 4. 如果包含不希望的类型，过滤掉期望类型位置的约束并重新计算
     *
     * 注意：
     * - 当前实现总是允许空交集（LanguageFeature.AllowEmptyIntersectionsInResultTypeResolver）
     * - 不希望的交集类型检测逻辑已部分注释
     *
     * 示例：
     * ```
     * // 场景 1：接口交集
     * // 约束：T <: Comparable, T <: Serializable
     * // 结果：Comparable & Serializable
     *
     * // 场景 2：类和接口交集
     * // 约束：T <: Number, T <: Comparable
     * // 结果：Number & Comparable
     * ```
     */
    private fun Context.computeUpperType(upperConstraints: List<Constraint>): CangJieTypeMarker {
        // 当前实现：总是允许空交集
        return if (/*languageVersionSettings.supportsFeature(LanguageFeature.AllowEmptyIntersectionsInResultTypeResolver)*/ true) {
            intersectTypes(upperConstraints.map { it.type })
        } else {
            // 旧实现：检查不希望的交集类型
            val intersectionUpperType = intersectTypes(upperConstraints.map { it.type })
            val resultIsActuallyIntersection = intersectionUpperType.typeConstructor().isIntersection()

            // 检查是否存在不希望的交集类型
            val isThereUnwantedIntersectedTypes = if (resultIsActuallyIntersection) {
                val intersectionSupertypes = intersectionUpperType.typeConstructor().supertypes()
                // 计算交集中非接口的类类型数量
                val intersectionClasses = intersectionSupertypes.count {
                    it.typeConstructor().isClassTypeConstructor() && !it.typeConstructor().isInterface()
                }
//                val areThereIntersectionFinalClasses = intersectionSupertypes.any { it.typeConstructor().isCommonFinalClassConstructor() }
                intersectionClasses > 1 /*|| areThereIntersectionFinalClasses*/
            } else false

//            val upperType = if (isThereUnwantedIntersectedTypes) {
//                /*
//                 * 如果存在显式的期望类型，我们不应该将类型变量推导为交集类型，
//                 * 否则可能导致如下情况：
//                 *
//                 * fun <T : String> materialize(): T = null as T
//                 * val bar: Int = materialize() // 没有错误，T 被推导为 String & Int
//                 */
//                val filteredUpperConstraints = upperConstraints.filterNot { it.isExpectedTypePosition() }.map { it.type }
//                if (filteredUpperConstraints.isNotEmpty()) intersectTypes(filteredUpperConstraints) else intersectionUpperType
//            } else intersectionUpperType
//            upperType

            intersectionUpperType
        }
    }

    /**
     * 查找超类型（从上界约束推导）
     *
     * 从类型变量的上界约束中计算交集类型，作为类型变量的超类型候选。
     *
     * @param variableWithConstraints 类型变量及其约束
     * @return 计算出的超类型，如果没有上界约束则返回 null
     *
     * 计算流程：
     * 1. 过滤出所有上界约束（UPPER）
     * 2. 只保留正确类型的约束
     * 3. 如果有正确的上界约束，计算交集类型
     * 4. 如果没有正确的上界约束，返回 null
     *
     * 示例：
     * ```
     * // 场景 1：单个上界约束
     * // 约束：T <: Number
     * // 结果：Number
     *
     * // 场景 2：多个上界约束
     * // 约束：T <: Comparable, T <: Serializable
     * // 结果：Comparable & Serializable（交集类型）
     * ```
     */
    private fun Context.findSuperType(variableWithConstraints: VariableWithConstraints): CangJieTypeMarker? {
        // 过滤出正确类型的上界约束
        val upperConstraints =
            variableWithConstraints.constraints.filter {
                it.kind == ConstraintKind.UPPER && this@findSuperType.isProperTypeForFixation(
                    it.type
                )
            }

        // 如果有上界约束，计算交集类型
        if (upperConstraints.isNotEmpty()) {
            return computeUpperType(upperConstraints)
        }

        return null
    }

    /**
     * 检查类型是否为正确类型（用于固定）
     *
     * 判断给定类型是否可以用于类型变量的固定。
     *
     * @param type 待检查的类型
     * @return 如果是正确类型返回 true
     *
     * 正确类型的定义：
     * - 不包含未固定的类型变量
     * - 所有类型组件都是正确类型
     *
     * 委托给通用的 isProperTypeForFixation 函数，传入：
     * - 待检查的类型
     * - 未固定的类型变量集合
     * - 正确类型判断函数
     */
    private fun Context.isProperTypeForFixation(type: CangJieTypeMarker): Boolean =
        isProperTypeForFixation(type, notFixedTypeVariables.keys) { isProperType(it) }

    /**
     * 从相等约束中查找结果类型
     *
     * 如果存在相等约束（T == A），则直接使用该类型作为结果。
     *
     * @param c 解析上下文
     * @param variableWithConstraints 类型变量及其约束
     * @return 从相等约束推导的类型，如果没有相等约束则返回 null
     *
     * 处理流程：
     * 1. 过滤出所有相等约束（EQUALITY）
     * 2. 只保留正确类型的约束
     * 3. 从相等约束中选择代表性类型
     *
     * 示例：
     * ```
     * // 场景 1：单个相等约束
     * // 约束：T == Int
     * // 结果：Int
     *
     * // 场景 2：多个相等约束（矛盾）
     * // 约束：T == Int, T == String
     * // 结果：Int（选择第一个，但约束系统应该已检测到矛盾）
     * ```
     */
    private fun findResultIfThereIsEqualsConstraint(
        c: Context,
        variableWithConstraints: VariableWithConstraints
    ): CangJieTypeMarker? {
        // 过滤出正确类型的相等约束
        val properEqualityConstraints = variableWithConstraints.constraints.filter {
            it.kind == ConstraintKind.EQUALITY && c.isProperTypeForFixation(it.type)
        }

        // 从相等约束中选择代表性类型
        return c.representativeFromEqualityConstraints(properEqualityConstraints)
    }

    /**
     * 从相等约束中选择代表性类型
     *
     * 从多个相等约束中选择最佳的代表性类型。
     * 优先选择非整数字面量类型，因为它们比整数字面量类型更具体。
     *
     * @param constraints 相等约束列表
     * @return 代表性类型，如果没有约束则返回 null
     *
     * 选择策略：
     * 1. 如果约束列表为空，返回 null
     * 2. 提取所有约束的类型
     * 3. 过滤出非整数字面量类型
     * 4. 尝试从非整数字面量类型中选择单一最佳代表
     * 5. 如果失败，尝试从所有类型中选择单一最佳代表
     * 6. 如果仍然失败，返回第一个类型（可能存在矛盾）
     *
     * 示例：
     * ```
     * // 场景 1：包含整数字面量类型和具体类型
     * // 约束：T == ILT(42), T == Int
     * // 结果：Int（优先选择具体类型）
     *
     * // 场景 2：只有整数字面量类型
     * // 约束：T == ILT(42), T == ILT(100)
     * // 结果：ILT(42)（选择第一个）
     *
     * // 场景 3：只有一个约束
     * // 约束：T == Int
     * // 结果：Int
     * ```
     */
    private fun Context.representativeFromEqualityConstraints(constraints: List<Constraint>): CangJieTypeMarker? {
        if (constraints.isEmpty()) return null

        val constraintTypes = constraints.map { it.type }
        // 过滤出非整数字面量类型（它们比整数字面量类型更具体）
        val nonLiteralTypes = constraintTypes.filter { !it.typeConstructor().isIntegerLiteralTypeConstructor() }
        return nonLiteralTypes.singleBestRepresentative()
            ?: constraintTypes.singleBestRepresentative()
            ?: constraintTypes.first() // 看起来约束系统存在矛盾
    }
}
