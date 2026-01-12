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

package org.cangnova.cangjie.types


import org.cangnova.cangjie.config.LanguageVersionSettings
import org.cangnova.cangjie.resolve.calls.CommonSuperTypeCalculator.commonSuperType
import org.cangnova.cangjie.types.checker.CangJieTypeChecker
import org.cangnova.cangjie.types.checker.CangJieTypePreparator
import org.cangnova.cangjie.types.model.*
import org.cangnova.cangjie.utils.runIf
import java.util.concurrent.ConcurrentHashMap

/**
 * 抽象类型近似器
 *
 * 用于在类型推导过程中将复杂的类型近似（Approximate）为更简单或更通用的类型。
 * 类型近似是类型系统中的重要概念，主要用于处理不能直接表示或不适合公开的类型。
 *
 * ## 主要功能
 *
 * 1. **向超类型近似（Approximate to Supertype）**：
 *    - 将类型近似为更通用的超类型
 *    - `type <: resultType`（输入类型是结果类型的子类型）
 *    - 例如：`Captured(out T)` 近似为 `T`
 *
 * 2. **向子类型近似（Approximate to Subtype）**：
 *    - 将类型近似为更具体的子类型
 *    - `resultType <: type`（结果类型是输入类型的子类型）
 *    - 例如：`Captured(in T)` 近似为 `Nothing`
 *
 * ## 需要近似的类型场景
 *
 * - **捕获类型（Captured Type）**：泛型通配符捕获产生的类型
 * - **交集类型（Intersection Type）**：多个类型的交集，如 `T & Comparable<T>`
 * - **局部类型（Local Type）**：局部类或匿名类的类型
 * - **类型变量（Type Variable）**：类型推导中的未知类型
 * - **整数字面量类型（Integer Literal Type）**：整数字面量的特殊类型
 * - **明确非空类型（Definitely Non-Null Type）**：形如 `T & Any` 的类型
 *
 * ## 近似策略
 *
 * 不同的场景使用不同的近似策略，由 [TypeApproximatorConfiguration] 控制：
 * - 交集类型：可以保持、转换为第一个类型、或计算公共超类型
 * - 灵活类型（Flexible Type）：可以保持或取上/下界
 * - 捕获类型：可以保持或近似为其边界
 * - 错误类型：可以保持或替换为 Any/Nothing
 *
 * ## 性能优化
 *
 * - 使用 [ConcurrentHashMap] 缓存近似结果
 * - 缓存仅用于最常用的配置（IncorporationConfiguration）
 * - 缓存大小限制为 [CACHE_FOR_INCORPORATION_MAX_SIZE]
 * - 递归深度限制防止栈溢出
 *
 * @param ctx 类型系统推导扩展上下文
 * @param languageVersionSettings 语言版本设置，用于控制特定版本的行为
 *
 * @see TypeApproximatorConfiguration 类型近似配置
 * @see TypeSystemInferenceExtensionContext 类型系统推导扩展上下文
 */
abstract class AbstractTypeApproximator(
    val ctx: TypeSystemInferenceExtensionContext,
    protected val languageVersionSettings: LanguageVersionSettings,
) : TypeSystemInferenceExtensionContext by ctx {

    /**
     * 近似结果的包装类
     *
     * @property type 近似后的类型，null 表示输入类型本身就是结果（无需近似）
     */
    private class ApproximationResult(val type: CangJieTypeMarker?)

    /** 向超类型近似时的缓存（仅用于 IncorporationConfiguration） */
    private val cacheForIncorporationConfigToSuperDirection =
        ConcurrentHashMap<CangJieTypeMarker, ApproximationResult>()

    /** 向子类型近似时的缓存（仅用于 IncorporationConfiguration） */
    private val cacheForIncorporationConfigToSubtypeDirection =
        ConcurrentHashMap<CangJieTypeMarker, ApproximationResult>()

    /** 向超类型近似的简单类型处理函数引用 */
    private val referenceApproximateToSuperType: (SimpleTypeMarker, TypeApproximatorConfiguration, Int) -> CangJieTypeMarker?
        get() = this::approximateSimpleToSuperType

    /** 向子类型近似的简单类型处理函数引用 */
    private val referenceApproximateToSubType: (SimpleTypeMarker, TypeApproximatorConfiguration, Int) -> CangJieTypeMarker?
        get() = this::approximateSimpleToSubType

    companion object {
        /** 合并配置缓存的最大大小 */
        const val CACHE_FOR_INCORPORATION_MAX_SIZE = 500
    }

    /**
     * 将类型近似为超类型
     *
     * 结果满足：`type <: resultType`
     *
     * @param type 输入类型
     * @param conf 近似配置
     * @return 近似后的超类型，null 表示输入类型本身就是结果（无需近似）
     */
    fun approximateToSuperType(type: CangJieTypeMarker, conf: TypeApproximatorConfiguration): CangJieTypeMarker? =
        approximateToSuperType(type, conf, -type.typeDepth())

    /**
     * 将类型近似为子类型
     *
     * 结果满足：`resultType <: type`
     *
     * @param type 输入类型
     * @param conf 近似配置
     * @return 近似后的子类型，null 表示输入类型本身就是结果（无需近似）
     */
    fun approximateToSubType(type: CangJieTypeMarker, conf: TypeApproximatorConfiguration): CangJieTypeMarker? =
        approximateToSubType(type, conf, -type.typeDepth())

    /**
     * 清除所有缓存
     */
    fun clearCache() {
        cacheForIncorporationConfigToSubtypeDirection.clear()
        cacheForIncorporationConfigToSuperDirection.clear()
    }

    /**
     * 检查异常情况并返回特殊处理结果
     *
     * 处理以下特殊情况：
     * - 特殊类型（Special Type）：直接返回 null
     * - 错误类型（Error Type）：根据配置决定是否保留
     * - 递归深度超限（depth > 3）：返回默认结果防止栈溢出
     *
     * @param type 待检查的类型
     * @param depth 当前递归深度
     * @param conf 近似配置
     * @param toSuper 是否向超类型近似
     * @return 如果是异常情况则返回对应的处理结果，否则返回 null
     */
    private fun checkExceptionalCases(
        type: CangJieTypeMarker, depth: Int, conf: TypeApproximatorConfiguration, toSuper: Boolean
    ): ApproximationResult? {
        return when {
            type.isSpecial() ->
                null.toApproximationResult()

            type.isError() ->
                // TODO -- 修复 builtIns。当前 builtIns 是 DefaultBuiltIns
                (if (conf.errorType) null else type.defaultResult(toSuper)).toApproximationResult()

            depth > 3 ->
                type.defaultResult(toSuper).toApproximationResult()

            else -> null
        }
    }

    /** 将可空类型转换为近似结果 */
    private fun CangJieTypeMarker?.toApproximationResult(): ApproximationResult = ApproximationResult(this)

    /**
     * 带缓存的类型近似计算
     *
     * 缓存策略：
     * - 仅对 IncorporationConfiguration 配置启用缓存
     * - 缓存大小超过限制时不使用缓存
     * - 向超类型和向子类型使用不同的缓存
     *
     * @param type 输入类型
     * @param conf 近似配置
     * @param toSuper 是否向超类型近似
     * @param approximate 实际的近似计算函数
     * @return 近似结果
     */
    private inline fun cachedValue(
        type: CangJieTypeMarker,
        conf: TypeApproximatorConfiguration,
        toSuper: Boolean,
        approximate: () -> CangJieTypeMarker?
    ): CangJieTypeMarker? {
        // 近似器依赖于配置，因此缓存也需要考虑配置
        // 这里仅缓存 "from incorporation" 配置的类型，因为这是最频繁使用的配置
        if (conf !is TypeApproximatorConfiguration.IncorporationConfiguration) return approximate()

        val cache =
            if (toSuper) cacheForIncorporationConfigToSuperDirection else cacheForIncorporationConfigToSubtypeDirection

        if (cache.size > CACHE_FOR_INCORPORATION_MAX_SIZE) return approximate()

        return cache.getOrPut(type, { approximate().toApproximationResult() }).type
    }

    /**
     * 向超类型近似的内部实现（带深度参数）
     */
    private fun approximateToSuperType(
        type: CangJieTypeMarker,
        conf: TypeApproximatorConfiguration,
        depth: Int
    ): CangJieTypeMarker? {
        checkExceptionalCases(type, depth, conf, toSuper = true)?.let { return it.type }

        return cachedValue(type, conf, toSuper = true) {
            approximateTo(
                CangJieTypePreparator.Default.prepareType(type), conf, { upperBound() },
                referenceApproximateToSuperType, depth
            )
        }
    }

    /**
     * 向子类型近似的内部实现（带深度参数）
     */
    private fun approximateToSubType(
        type: CangJieTypeMarker,
        conf: TypeApproximatorConfiguration,
        depth: Int
    ): CangJieTypeMarker? {
        checkExceptionalCases(type, depth, conf, toSuper = false)?.let { return it.type }

        return cachedValue(type, conf, toSuper = false) {
            approximateTo(
                CangJieTypePreparator.Default.prepareType(type), conf, { lowerBound() },
                referenceApproximateToSubType, depth
            )
        }
    }

    /**
     * 通用的类型近似实现
     *
     * 注意：不要直接调用此方法，应该使用 approximateToSuperType/approximateToSubType
     * 此方法仅包含类型近似的详细实现，不检查异常情况也不使用缓存
     *
     * 处理两种类型：
     * 1. 简单类型（SimpleType）：直接调用近似函数
     * 2. 灵活类型（FlexibleType）：根据配置处理上下界
     *
     * @param type 输入类型
     * @param conf 近似配置
     * @param bound 灵活类型的界提取函数（upperBound 或 lowerBound）
     * @param approximateTo 简单类型的近似函数
     * @param depth 当前递归深度
     * @return 近似结果
     */
    private fun approximateTo(
        type: CangJieTypeMarker,
        conf: TypeApproximatorConfiguration,
        bound: FlexibleTypeMarker.() -> SimpleTypeMarker,
        approximateTo: (SimpleTypeMarker, TypeApproximatorConfiguration, depth: Int) -> CangJieTypeMarker?,
        depth: Int
    ): CangJieTypeMarker? {
        when (type) {
            is SimpleTypeMarker -> return approximateTo(type, conf, depth)
            is FlexibleTypeMarker -> {
                if (type.isDynamic()) {
                    return if (conf.dynamic) null else type.bound()
                } else if (type.isRawType()) {
                    return if (conf.rawType) null else type.bound()
                }

//              TODO: 恢复检查
//              TODO: 当前可能会丢失增强信息，稍后需要修复
//              assert(type is FlexibleTypeImpl || type is FlexibleTypeWithEnhancement) {
//                  "Unexpected subclass of FlexibleType: ${type::class.java.canonicalName}, type = $type"
//              }

                if (conf.flexible) {
                    /**
                     * 灵活类型的近似逻辑：
                     *
                     * 设 inputType = L_1..U_1；resultType = L_2..U_2
                     * 我们需要创建 resultType 使得 inputType <: resultType
                     *
                     * 这意味着如果 A <: inputType，则 A <: U_1。
                     * 因为 inputType <: resultType，所以 A <: resultType => A <: U_2
                     * 即对于所有 A 满足 A <: U_1，都有 A <: U_2 => U_1 <: U_2
                     *
                     * 类似地对于 L_1 <: L_2：设 B 满足 resultType <: B，则 L_2 <: B 且 L_1 <: B
                     * 即对于所有 B 满足 L_2 <: B，都有 L_1 <: B。例如 B = L_2
                     */
                    val lowerBound = type.lowerBound()
                    val upperBound = type.upperBound()

                    val lowerResult = approximateTo(lowerBound, conf, depth)

                    val upperResult =
                        if (!type.isRawType() && !shouldApproximateUpperBoundSeparately(lowerBound, upperBound, conf)) {
                            // 如果类型构造器匹配，跳过上界近似作为优化
                            lowerResult?.withOption(upperBound.isMarkedOption())
                        } else {
                            approximateTo(upperBound, conf, depth)
                        }
                    if (lowerResult == null && upperResult == null) return null

                    /**
                     * 如果 C <: L..U，则 C <: L
                     * inputType.lower <: lowerResult => inputType.lower <: lowerResult?.lowerIfFlexible()
                     * 即这个类型是正确的。我们使用这个类型，因为它更灵活。
                     *
                     * 如果 U_1 <: U_2.lower .. U_2.upper，则我们只知道 U_1 <: U_2.upper
                     */
                    return createFlexibleType(
                        lowerResult?.lowerBoundIfFlexible() ?: lowerBound,
                        upperResult?.upperBoundIfFlexible() ?: upperBound
                    )
                } else {
                    return type.bound().let { approximateTo(it, conf, depth) ?: it }
                }
            }

            else -> error("sealed")
        }
    }

    /**
     * 判断是否需要单独近似上界
     *
     * 灵活数组的形式是 `Array<X>..Array<out X>?`
     * 当这种类型被捕获时，结果是 `Array<X>..Array<Captured(out X)>?`，
     * 因此需要单独近似上界。
     *
     * 作为重要的性能优化，我们明确检查类型是否是带有需要近似的捕获类型参数的数组。
     * 这避免了在许多情况下不必要地执行两次工作。
     *
     * @return 如果需要单独近似上界则返回 true
     */
    private fun shouldApproximateUpperBoundSeparately(
        lowerBound: SimpleTypeMarker,
        upperBound: SimpleTypeMarker,
        conf: TypeApproximatorConfiguration,
    ): Boolean {
        val upperBoundConstructor = upperBound.typeConstructor()
        if (lowerBound.typeConstructor() != upperBoundConstructor) return true

        return upperBoundConstructor.isArrayConstructor() &&
                upperBound.getArgumentOrNull(0).let { it is CapturedTypeMarker && conf.capturedType(ctx, it) }
    }

    /**
     * 近似局部类型
     *
     * 将局部类型或匿名类型近似为其第一个超类型。
     *
     * @param type 待近似的类型
     * @param conf 近似配置
     * @param toSuper 是否向超类型近似（局部类型仅在向超类型近似时处理）
     * @param depth 当前递归深度
     * @return 近似结果，如果不需要近似则返回 null
     */
    private fun approximateLocalTypes(
        type: SimpleTypeMarker,
        conf: TypeApproximatorConfiguration,
        toSuper: Boolean,
        depth: Int,
    ): SimpleTypeMarker? {
        if (!toSuper) return null
        if (!conf.localTypes && !conf.anonymous) return null
        val constructor = type.typeConstructor()
        val needApproximate =
            (conf.localTypes && constructor.isLocalType()) || (conf.anonymous && constructor.isAnonymous())
        if (!needApproximate) return null
        val superConstructor = constructor.supertypes().first().typeConstructor()
        val typeCheckerContext = newTypeCheckerState(
            errorTypesEqualToAnything = false,
            stubTypesEqualToAnything = false
        )
        val result = AbstractTypeChecker.findCorrespondingSupertypes(typeCheckerContext, type, superConstructor)
            .firstOrNull()
            ?.withOption(type.isMarkedOption())
            ?: return null
        /*
         * AbstractTypeChecker 默认会捕获超类型中的任何投影，这可能导致某些带投影的局部类型
         * 被近似为带有捕获类型参数（从子类型关系捕获的）的公共类型（这显然是不正确的）
         *
         * interface Invariant<A>
         * private fun <B> Invariant<B>.privateFunc() = object : Invariant<B> {}
         *
         * fun Invariant<in Number>.publicFunc() = privateFunc()
         *
         * 这里 `privateFunc()` 的类型是 _anonymous_<in Number>，
         * 对它和 `Invariant` 作为类型构造器调用 `findCorrespondingSupertypes`
         * 会返回 `Invariant<Captured(in Number)>`
         */
        /*        return if (ctx.isK2) {
                    (approximateTo(result, TypeApproximatorConfiguration.SubtypeCapturedTypesApproximation, toSuper, depth) ?: result) as SimpleTypeMarker?
                } else {*/
//            result
//        }
        return result
    }

    /**
     * 判断交集类型是否实际上等价于 Nothing
     *
     * 我们仅在交集的某个组件是原始数字类型时才认为交集等价于 Nothing
     * 这是有意为之，我们不会像旧推导那样尝试证明某种类型的数量
     *
     * @param constructor 交集类型构造器
     * @return 如果交集类型实际上等价于 Nothing 则返回 true
     */
    private fun isIntersectionTypeEffectivelyNothing(constructor: IntersectionTypeConstructorMarker): Boolean {
        return constructor.supertypes().any {
            !it.isMarkedOption() && it.isSignedOrUnsignedNumberType()
        }
    }

    /**
     * 近似交集类型
     *
     * 交集类型的处理策略取决于配置：
     * - ALLOWED：保持交集，递归近似每个组件
     * - TO_FIRST：取第一个类型
     * - TO_COMMON_SUPERTYPE/TO_UPPER_BOUND_IF_SUPERTYPE：计算公共超类型
     *
     * @param type 交集类型
     * @param conf 近似配置
     * @param toSuper 是否向超类型近似
     * @param depth 当前递归深度
     * @return 近似结果
     */
    private fun approximateIntersectionType(
        type: SimpleTypeMarker,
        conf: TypeApproximatorConfiguration,
        toSuper: Boolean,
        depth: Int
    ): CangJieTypeMarker? {
        val typeConstructor = type.typeConstructor()
        assert(typeConstructor.isIntersection()) {
            "Should be intersection type: $type, typeConstructor class: ${typeConstructor::class.java.canonicalName}"
        }
        assert(typeConstructor.supertypes().isNotEmpty()) {
            "Supertypes for intersection type should not be empty: $type"
        }

        var thereIsApproximation = false
        val newTypes = typeConstructor.supertypes().map {
            val newType =
                if (toSuper) approximateToSuperType(it, conf, depth) else approximateToSubType(it, conf, depth)
            if (newType != null) {
                thereIsApproximation = true
                newType
            } else it
        }

        /**
         * 对于 ALLOWED 策略：
         * A <: A', B <: B' => A & B <: A' & B'
         *
         * 对于其他策略 -- 除了 Nothing 之外不可能找到交集类型的子类型
         */
        val baseResult = when (conf.intersection) {
            TypeApproximatorConfiguration.IntersectionStrategy.ALLOWED -> if (!thereIsApproximation) return null else intersectTypes(
                newTypes
            )

            TypeApproximatorConfiguration.IntersectionStrategy.TO_FIRST -> if (toSuper) newTypes.first() else return type.defaultResult(
                toSuper = false
            )
            // commonSupertypeCalculator 应该正确处理灵活类型
            TypeApproximatorConfiguration.IntersectionStrategy.TO_COMMON_SUPERTYPE,
            TypeApproximatorConfiguration.IntersectionStrategy.TO_UPPER_BOUND_IF_SUPERTYPE -> {
                if (!toSuper) return type.defaultResult(toSuper = false)
                val resultType = commonSuperType(newTypes)
                approximateToSuperType(resultType, conf) ?: resultType
//                type
            }
        }

        return if (type.isMarkedOption()) baseResult.withOption(true) else baseResult
    }

    /**
     * 近似捕获类型
     *
     * 捕获类型是泛型通配符捕获产生的类型。
     * 根据超类型数量选择基础超类型，然后进行近似。
     *
     * @param type 捕获类型
     * @param conf 近似配置
     * @param toSuper 是否向超类型近似
     * @param depth 当前递归深度
     * @return 近似结果
     */
    private fun approximateCapturedType(
        type: CapturedTypeMarker,
        conf: TypeApproximatorConfiguration,
        toSuper: Boolean,
        depth: Int
    ): CangJieTypeMarker? {
        val supertypes = type.typeConstructor().supertypes()
        val baseSuperType = when (supertypes.size) {
            0 -> anyType() // 设 C = in Int，则 C 和 C? 的超类型是 Any?
            1 -> supertypes.single()

            // 考虑以下示例：
            // A.getA()::class.java，其中 `getA()` 返回来自 Java 的某个类
            // 从 `::class` 我们得到类型 KClass<Cap<out A!>>，其中 Cap<out A!> 有两个超类型：
            // - Any（来自 KClass 的类型参数的声明上界）
            // - (A..A?) -- 来自 A!，捕获类型的投影类型

            // 现在，经过近似后我们得到类型 `KClass<out A>`，因为 { Any & (A..A?) } = A，
            // 但在旧推导中类型等于 `KClass<out A!>`。

            // 重要提示：从类型系统的角度来看，第一个类型更具体：
            // 这里，KClass<Cap<out A!>> 的近似是类型 KClass<T>，使得 KClass<Cap<out A!>> <: KClass<out T> =>
            // 因此，T 的更具体类型应该是"A 的某个非空（因为声明的上界类型）子类型"，即 `out A`

            // 但现在，为了减少旧推导和新推导行为的差异，我们将此类类型近似为 `KClass<out A!>`

            // 一旦新推导更加稳定，我们将使用更具体的类型

            else -> {
                val projection = type.typeConstructorProjection()

                projection.getType()
            }
        }
        val baseSubType = type.lowerType() ?: nothingType()

        val approximatedSuperType by lazy(LazyThreadSafetyMode.NONE) {
            approximateToSuperType(
                baseSuperType,
                conf,
                depth
            )
        }
        val approximatedSubType by lazy(LazyThreadSafetyMode.NONE) { approximateToSubType(baseSubType, conf, depth) }

        if (!conf.capturedType(ctx, type)) {
            /**
             * 如果不应该近似捕获类型的边界，这里一切正常。
             * 但是，如果这些边界包含一些未授权的类型，那么我们不能保留此捕获类型"原样"。
             * 而且我们不能创建新的捕获类型，因为新捕获类型的含义不清楚。
             * 因此，我们将直接近似这些类型
             *
             * TODO 处理灵活类型
             */
            if (approximatedSuperType == null && approximatedSubType == null) {
                return null
            }
        }
        val baseResult = if (toSuper) approximatedSuperType ?: baseSuperType else approximatedSubType ?: baseSubType

        // C = in Int, Int <: C => Int? <: C?
        // C = out Number, C <: Number => C? <: Number?
        return when {
            type.isMarkedOption() -> baseResult.withOption(true)

            else -> baseResult
        }.let {
            when {
                // 这只是一个为了保持与 K1 兼容性而必需的 hack，
                // 在 K1 中，如果调用包含带有 RAW 超类型的捕获类型，返回类型会被近似为常规的非原始灵活类型
                // 参见 CapturedTypeApproximationCj.approximateCapturedTypes 以及之前的注释
                // "// tod*: approximateDynamic & raw type?" :)
                conf.convertToNonRawVersionAfterApproximation && it.isRawType() -> {
                    it.convertToNonRaw()
                }

                else -> it
            }
        }
    }

    /** 近似简单类型到超类型 */
    private fun approximateSimpleToSuperType(type: SimpleTypeMarker, conf: TypeApproximatorConfiguration, depth: Int) =
        approximateTo(type, conf, toSuper = true, depth = depth)

    /** 近似简单类型到子类型 */
    private fun approximateSimpleToSubType(type: SimpleTypeMarker, conf: TypeApproximatorConfiguration, depth: Int) =
        approximateTo(type, conf, toSuper = false, depth = depth)

    /**
     * 近似简单类型的通用实现
     *
     * 根据类型的特征分发到不同的处理方法：
     * - 有类型参数：使用 approximateParametrizedType
     * - 明确非空类型：使用 approximateDefinitelyNotNullType
     * - 捕获类型：使用 approximateCapturedType
     * - 交集类型：使用 approximateIntersectionType
     * - 类型变量：根据配置决定是否保留
     * - 整数字面量类型：近似为具体整数类型
     * - 局部类型/匿名类型：近似为其超类型
     *
     * @param type 简单类型
     * @param conf 近似配置
     * @param toSuper 是否向超类型近似
     * @param depth 当前递归深度
     * @return 近似结果
     */
    private fun approximateTo(
        type: SimpleTypeMarker,
        conf: TypeApproximatorConfiguration,
        toSuper: Boolean,
        depth: Int
    ): CangJieTypeMarker? {
        if (type.argumentsCount() != 0) {
            return approximateParametrizedType(type, conf, toSuper, depth + 1)
        }

        val typeConstructor = type.typeConstructor()

        if (typeConstructor.isCapturedTypeConstructor()) {
            val capturedType = type.asCapturedType()
            require(capturedType != null) {

                "Type is inconsistent -- somewhere we create type with typeConstructor = $typeConstructor " +
                        "and class: ${type::class.java.canonicalName}. type.toString() = $type"
            }
            return approximateCapturedType(capturedType, conf, toSuper, depth)
        }

        if (typeConstructor.isIntersection()) {
            return approximateIntersectionType(type, conf, toSuper, depth)
        }

        if (typeConstructor is TypeVariableTypeConstructorMarker) {
            return if (conf.shouldKeepTypeVariableBasedType(typeConstructor)) null else type.defaultResult(toSuper)
        }

        if (typeConstructor.isIntegerLiteralConstantTypeConstructor()) {
            return runIf(conf.integerLiteralConstantType) {
                typeConstructor.getApproximatedIntegerLiteralType().withOption(type.isMarkedOption())
            }
        }

        if (typeConstructor.isIntegerConstantOperatorTypeConstructor()) {
            return runIf(conf.integerConstantOperatorType) {
                typeConstructor.getApproximatedIntegerLiteralType().withOption(type.isMarkedOption())
            }
        }

        return approximateLocalTypes(type, conf, toSuper, depth) // 简单分类器类型
    }

    /**
     * 近似参数化类型
     *
     * 处理带有类型参数的类型,如 `List<T>`、`Map<K, V>` 等。
     * 仓颉语言的所有类型参数都是不变的(invariant),因此简化了近似处理逻辑。
     *
     * @param type 参数化类型
     * @param conf 近似配置
     * @param toSuper 是否向超类型近似
     * @param depth 当前递归深度
     * @return 近似结果
     */
    private fun approximateParametrizedType(
        type: SimpleTypeMarker,
        conf: TypeApproximatorConfiguration,
        toSuper: Boolean,
        depth: Int
    ): SimpleTypeMarker? {
        val typeConstructor = type.typeConstructor()
        if (typeConstructor.parametersCount() != type.argumentsCount()) {
            return if (conf.errorType) {
                createErrorType(
                    "Inconsistent type: $type (parameters.size = ${typeConstructor.parametersCount()}, arguments.size = ${type.argumentsCount()})",
                    type
                )
            } else type.defaultResult(toSuper)
        }

        val newArguments = arrayOfNulls<TypeArgumentMarker?>(type.argumentsCount())

        loop@ for (index in 0 until type.argumentsCount()) {
            val parameter = typeConstructor.getParameter(index)
            val argument = type.getArgument(index)


            // 仓颉语言所有类型参数都是不变的(invariant)
            val argumentType = newArguments[index]?.getType() ?: argument.getType()

            val capturedType = argumentType.lowerBoundIfFlexible().asCapturedType()

            // 捕获具有自身上界的递归类型时,其超类型可能包含捕获类型
            // 在 approximateCapturedType 中,即使捕获类型本身不需要近似,
            // 我们也会检查捕获类型的超/子类型是否需要近似,然后会到达这里
            // 为了支持这种情况,如果配置指示,我们也不想在这里近似捕获类型
            // TODO 重构捕获类型近似逻辑
            if (capturedType != null &&

                !conf.capturedType(ctx, capturedType) /*&&
                ctx.hasRecursiveTypeParametersWithGivenSelfType(capturedType.typeConstructor())*/
            ) {
                continue@loop
            }

            // 仓颉语言所有类型参数都是不变的,简化处理逻辑
            if (!toSuper) {
                // Inv<Foo> 无法近似为子类型
                val toSubType = approximateToSubType(argumentType, conf, depth) ?: continue@loop

                // Inv<Option<Foo>> 是 Inv<Foo> 的超类型
                if (!CangJieTypeChecker.DEFAULT.equalTypes(
                        argumentType as CangJieType,
                        toSubType as CangJieType
                    )
                ) return type.defaultResult(toSuper)

                // Captured(Nothing) = Nothing
                newArguments[index] = toSubType.asTypeArgument()
                continue@loop
            }

            // 对于 Inv<C> 其中 C = Captured(Int),我们选择 Inv<Int> 作为结果近似
            // 因为 Inv<C> <: Inv<Int>,其中 Int 是捕获类型 C 的下界
            //
            // 当存在非平凡上界时,选择非平凡下界的行为是关键的
            // 例如 Inv 声明为 `interface Inv<T : CharSequence>`
            //
            // 如果同时存在非平凡边界,下一个条件无法处理,因为它会选择上界
            if (argumentType.typeConstructor().isCapturedTypeConstructor()) {
                val subType = approximateToSubType(argumentType, conf, depth) ?: continue@loop
                if (shouldUseSubTypeForCapturedArgument(subType, argumentType, conf, depth)) {
                    newArguments[index] = createTypeArgument(subType)
                    continue@loop
                }
            }

            // 同时存在非平凡上下界的近似示例:
            //  Inv<In<C>> 其中 C = Captured(Int)
            //  Inv<In<C>> <: Inv<In<Int>> (使用下界)
            //  Inv<In<C>> <: Inv<In<Any>> (使用上界)
            //
            // 两种选择都可行,但由于这种情况很少见,我们选择 Inv<In<Int>>
            val approximatedSuperType =
                approximateToSuperType(argumentType, conf, depth)
                    ?: continue@loop // null 表示该类型可以保持原样
            if (approximatedSuperType.isTrivialSuper()) {
                val approximatedSubType =
                    approximateToSubType(argumentType, conf, depth)
                        ?: continue@loop // 这种情况应该永远不会是 null
                if (!approximatedSubType.isTrivialSub()) {
                    newArguments[index] = createTypeArgument(approximatedSubType)
                    continue@loop
                }
            }

            if (CangJieTypeChecker.DEFAULT.equalTypes(argumentType as CangJieType, approximatedSuperType as CangJieType)) {
                newArguments[index] = approximatedSuperType.asTypeArgument()
            } else {
                newArguments[index] = createTypeArgument(approximatedSuperType)
            }
        }

        if (newArguments.all { it == null }) return approximateLocalTypes(type, conf, toSuper, depth)

        val newArgumentsList = List(type.argumentsCount()) { index -> newArguments[index] ?: type.getArgument(index) }
        val approximatedType = type.replaceArguments(newArgumentsList)
        return approximateLocalTypes(approximatedType, conf, toSuper, depth) ?: approximatedType
    }

    /**
     * 判断是否应该为捕获类型参数使用子类型
     *
     * 这个方法用于决定在不变类型参数位置，对于捕获类型是应该使用其下界（子类型）
     * 还是上界（超类型）进行近似。
     *
     * @param subType 捕获类型的子类型近似
     * @param capturedArgumentType 捕获类型参数
     * @param conf 近似配置
     * @param depth 当前递归深度
     * @return 如果应该使用子类型则返回 true
     */
    private fun shouldUseSubTypeForCapturedArgument(
        subType: CangJieTypeMarker,
        capturedArgumentType: CangJieTypeMarker,
        conf: TypeApproximatorConfiguration,
        depth: Int,
    ): Boolean {
        if (subType.isTrivialSub()) return false
        // 对于 K1，结果总是 `!subType.isTrivialSub()`（保留旧行为）
//        if (!isK2) return true

        // 基本上，下面的内容可以简化为：
        // return !approximateToSubType(capturedArgumentType.withNullability(false), conf, depth)!!.isTrivialSub()
        // 但现在这样看起来更清晰，可能性能也更好，因此前两个 if 本质上是快速路径

        // 如果不是 `Nothing?`，则下界确实是非平凡的
        if (!subType.lowerBoundIfFlexible().isOptionNothing()) return true

        // 这里 subType 是 `Nothing?`，只有在可空性由捕获类型本身的可空性引起时，它才可能是平凡的

        // 如果捕获类型未标记为可空，则 subType 的可空性来自捕获类型的下界。
        // 因此，下界肯定是非平凡的
        if (!capturedArgumentType.isMarkedOption()) return true

        val notMarkedNullableSubType =
            approximateToSubType(capturedArgumentType.withOption(false), conf, depth)
                ?: error("Not-marked-nullable version of captured type approximation should also return not-null")

        return !notMarkedNullableSubType.isTrivialSub()
    }

    /**
     * 返回类型的默认近似结果
     *
     * - 向超类型近似：返回 Any
     * - 向子类型近似：返回 Nothing
     */
    private fun CangJieTypeMarker.defaultResult(toSuper: Boolean) = if (toSuper) anyType() else {
        nothingType()
    }

    /** 检查类型是否是平凡的超类型（Any? 或 Any!） */
    private fun CangJieTypeMarker.isTrivialSuper() = upperBoundIfFlexible().isOptionAny()

    /** 检查类型是否是平凡的子类型（Nothing 或 Nothing!） */
    private fun CangJieTypeMarker.isTrivialSub() = lowerBoundIfFlexible().isNothing()

    override fun CapturedTypeMarker.typeParameter(): TypeParameterMarker? {
        with(ctx) {
            return typeParameter()
        }
    }
}
