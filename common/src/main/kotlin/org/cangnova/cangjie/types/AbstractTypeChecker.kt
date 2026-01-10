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

import com.intellij.util.SmartList
import org.cangnova.cangjie.types.model.*
import org.cangnova.cangjie.utils.SmartSet
import java.util.*

/**
 * 类型检查器状态
 *
 * 提供类型检查过程中需要的上下文和状态管理。
 * 这是一个精简版本，只包含 AbstractTypeChecker 所需的最小功能。
 */
open class TypeCheckerState(
    val typeSystemContext: TypeSystemContext
) {
    /**
     * 超类型遍历策略
     *
     * 定义如何处理类型的超类型遍历。
     */
    sealed class SupertypesPolicy {
        abstract fun transformType(state: TypeCheckerState, type: CangJieTypeMarker): SimpleTypeMarker

        /** 不遍历超类型 */
        data object None : SupertypesPolicy() {
            override fun transformType(state: TypeCheckerState, type: CangJieTypeMarker) =
                throw UnsupportedOperationException("Should not be called")
        }

        /** 如果是 Flexible 类型，取下界 */
        data object LowerIfFlexible : SupertypesPolicy() {
            override fun transformType(state: TypeCheckerState, type: CangJieTypeMarker) =
                with(state.typeSystemContext) { type.lowerBoundIfFlexible() }
        }
    }

    private var supertypesLocked = false
    var supertypesDeque: ArrayDeque<SimpleTypeMarker>? = null
        private set
    var supertypesSet: MutableSet<SimpleTypeMarker>? = null
        private set

    fun initialize() {
        assert(!supertypesLocked) {
            "Supertypes were locked for ${this::class}"
        }
        supertypesLocked = true

        if (supertypesDeque == null) {
            supertypesDeque = ArrayDeque(4)
        }
        if (supertypesSet == null) {
            supertypesSet = SmartSet.create()
        }
    }

    /**
     * 遍历超类型
     *
     * 从起始类型开始，广度优先遍历所有超类型，直到找到满足条件的类型或遍历完所有超类型。
     *
     * @param start 起始类型
     * @param predicate 判断条件
     * @param supertypesPolicy 超类型遍历策略
     * @return 如果找到满足条件的类型返回 true，否则返回 false
     */
    inline fun anySupertype(
        start: SimpleTypeMarker,
        predicate: (SimpleTypeMarker) -> Boolean,
        supertypesPolicy: (SimpleTypeMarker) -> SupertypesPolicy
    ): Boolean {
        if (predicate(start)) return true

        initialize()

        val deque = supertypesDeque!!
        val visitedSupertypes = supertypesSet!!

        deque.push(start)
        while (deque.isNotEmpty()) {
            if (visitedSupertypes.size > 1000) {
                error("Too many supertypes for type: $start. Supertypes = ${visitedSupertypes.joinToString()}")
            }
            val current = deque.pop()
            if (!visitedSupertypes.add(current)) continue

            val policy = supertypesPolicy(current).takeIf { it != SupertypesPolicy.None } ?: continue
            val supertypes = with(typeSystemContext) { current.typeConstructor().supertypes() }
            for (supertype in supertypes) {
                val newType = policy.transformType(this, supertype)
                if (predicate(newType)) {
                    clear()
                    return true
                }
                deque.add(newType)
            }
        }

        clear()
        return false
    }

    fun clear() {
        supertypesDeque!!.clear()
        supertypesSet!!.clear()
        supertypesLocked = false
    }
}

/**
 * 抽象类型检查器
 *
 * 为 common 模块提供基本的类型层次查询功能。
 * 这个精简版本只保留了 common 模块实际使用的方法。
 *
 * ## 保留的核心功能
 *
 * ### 1. 类类型子类型关系检查 (isSubtypeOfClass)
 * - 判断一个类型构造器是否是另一个类型构造器的子类（忽略类型参数）
 * - 用于可见性检查中的继承关系判断
 *
 * ### 2. 超类型查找 (findCorrespondingSupertypes)
 * - 在类型的继承层次中查找特定类型构造器的所有超类型
 * - 用于空交集类型检查
 *
 * ## 使用位置
 *
 * - `EffectiveVisibility.kt` 使用 `isSubtypeOfClass()`
 * - `EmptyIntersectionTypeChecker.kt` 使用 `findCorrespondingSupertypes()`
 *
 * @see TypeCheckerState 类型检查器状态
 * @see TypeSystemContext 类型系统上下文
 */
object AbstractTypeChecker {
    /**
     * 是否启用慢速断言检查
     *
     * 在开发和测试阶段用于额外的类型系统一致性检查。
     * 生产环境建议禁用以提升性能。
     */
    const val RUN_SLOW_ASSERTIONS = false

    /**
     * 选择纯仓颉超类型
     *
     * 当存在多条继承路径时,优先选择纯仓颉路径（不包含 FlexibleType）。
     * 这在 Java 互操作场景中非常重要。
     *
     * ## 使用场景
     *
     * 考虑以下 Java 互操作场景:
     * ```cangjie
     * class MyList : AbstractList<String>(), MutableList<String>
     * ```
     *
     * 如果 `AbstractList` 是 Java 类（使用 FlexibleType），而 `MutableList` 是仓颉接口,
     * 我们应该优先使用 `MutableList<String>` 路径。
     *
     * @param state 类型检查器状态
     * @param supertypes 待选择的超类型列表
     * @return 优先选择的超类型列表（如果存在纯仓颉超类型则返回它们，否则返回原列表）
     */
    private fun selectOnlyPureCangJieSupertypes(
        state: TypeCheckerState,
        supertypes: List<SimpleTypeMarker>
    ): List<SimpleTypeMarker> = with(state.typeSystemContext) {
        if (supertypes.size < 2) return supertypes

        val allPureSupertypes = supertypes.filter {
            it.asArgumentList().all(this) { argumentMarker -> argumentMarker.getType().asFlexibleType() == null }
        }
        return allPureSupertypes.ifEmpty { supertypes }
    }

    /**
     * 检查类类型的子类型关系（忽略类型参数）
     *
     * 此方法只匹配类类型本身，忽略类型参数的具体值。
     * 用于快速判断类继承关系。
     *
     * ## 与 isSubtypeOf 的区别
     *
     * 考虑以下示例:
     * ```cangjie
     * abstract class Foo<T>
     * class FooBar : Foo<Any>()
     * ```
     *
     * - `isSubtypeOfClass(FooBar, Foo<T>)` 返回 `true`  ← 只看类，不看类型参数
     * - `isSubtypeOf(FooBar, Foo<String>)` 返回 `false` ← 需要检查类型参数 Any vs String
     *
     * ## 使用场景
     *
     * - 可见性检查中判断 protected 的容器类型关系
     * - 快速判断类是否在某个类层次结构中
     *
     * ## 实现
     *
     * 使用深度优先搜索遍历类型的超类型层次结构:
     * 1. 如果类型构造器相等，直接返回 true
     * 2. 递归检查所有直接超类型
     * 3. 如果任意超类型匹配，返回 true
     *
     * @param state 类型检查器状态
     * @param typeConstructor 待检查的类型构造器
     * @param superConstructor 目标超类的类型构造器
     * @return 如果 typeConstructor 是 superConstructor 的子类（忽略类型参数），返回 true
     */
    fun isSubtypeOfClass(
        state: TypeCheckerState,
        typeConstructor: TypeConstructorMarker,
        superConstructor: TypeConstructorMarker
    ): Boolean {
        if (typeConstructor == superConstructor) return true
        with(state.typeSystemContext) {
            for (superType in typeConstructor.supertypes()) {
                if (isSubtypeOfClass(state, superType.typeConstructor(), superConstructor)) {
                    return true
                }
            }
        }
        return false
    }

    /**
     * 收集并过滤超类型
     *
     * 此方法是 collectAllSupertypesWithGivenTypeConstructor 和 selectOnlyPureCangJieSupertypes 的组合。
     *
     * @param state 类型检查器状态
     * @param classType 待检查的类类型
     * @param constructor 目标类型构造器
     * @return 过滤后的超类型列表（优先选择纯仓颉超类型）
     */
    private fun collectAndFilter(
        state: TypeCheckerState,
        classType: SimpleTypeMarker,
        constructor: TypeConstructorMarker
    ) =
        selectOnlyPureCangJieSupertypes(
            state,
            collectAllSupertypesWithGivenTypeConstructor(state, classType, constructor)
        )

    /**
     * 收集具有指定类型构造器的所有超类型
     *
     * 在类型的继承层次结构中查找所有匹配给定类型构造器的超类型。
     * 这是泛型类型参数匹配的核心方法。
     *
     * ## 使用场景
     *
     * 考虑以下示例:
     * ```cangjie
     * interface Comparable<T>
     * class String : Comparable<String>
     *
     * // 查找 String 的所有 Comparable<T> 超类型
     * // 结果: [Comparable<String>]
     * ```
     *
     * ## 实现策略
     *
     * 1. **快速路径**: 先尝试 fastCorrespondingSupertypes 缓存
     * 2. **类型过滤**: 如果目标是类类型构造器，而当前类型不是类类型，返回空
     * 3. **遍历搜索**: 使用 anySupertype 遍历类型层次结构，收集匹配的超类型
     * 4. **捕获处理**: 对找到的超类型应用捕获（CaptureStatus.FOR_SUBTYPING）
     *
     * @param state 类型检查器状态
     * @param subType 待检查的类型
     * @param superConstructor 目标超类型的类型构造器
     * @return 所有匹配的超类型列表
     */
    private fun collectAllSupertypesWithGivenTypeConstructor(
        state: TypeCheckerState,
        subType: SimpleTypeMarker,
        superConstructor: TypeConstructorMarker
    ): List<SimpleTypeMarker> = with(state.typeSystemContext) {
        subType.fastCorrespondingSupertypes(superConstructor)?.let {
            return it
        }

        if (!superConstructor.isClassTypeConstructor() && subType.isClassType()) return emptyList()

        val result: MutableList<SimpleTypeMarker> = SmartList()

        state.anySupertype(subType, { false }) {

            val current = captureFromArguments(it, CaptureStatus.FOR_SUBTYPING) ?: it

            when {
                areEqualTypeConstructors(current.typeConstructor(), superConstructor) -> {
                    result.add(current)
                    TypeCheckerState.SupertypesPolicy.None
                }

                current.argumentsCount() == 0 -> {
                    TypeCheckerState.SupertypesPolicy.LowerIfFlexible
                }

                else -> {
                    state.typeSystemContext.substitutionSupertypePolicy(current)
                }
            }
        }

        return result
    }

    /**
     * 查找对应的超类型
     *
     * 在类型的继承层次中查找所有匹配指定类型构造器的超类型。
     * 这是类型参数匹配和泛型子类型检查的基础。
     *
     * ## 关键特性
     *
     * - **可空性已检查**: 调用此方法前已通过 nullabilityChecker 检查
     * - **仅限内部使用**: 调用方必须确保可空性已正确处理
     *
     * ## 处理逻辑
     *
     * ### 1. 类类型的处理
     * 如果 subType 是类类型:
     * - 直接使用 collectAndFilter 收集并过滤超类型
     * - 优先选择纯仓颉超类型路径
     *
     * ### 2. 非类类型的处理
     * 如果 superConstructor 不是类类型构造器也不是整数字面量类型构造器:
     * - 使用 collectAllSupertypesWithGivenTypeConstructor 收集所有超类型
     * - 允许类型参数、交集类型等特殊类型
     *
     * ### 3. 混合类型的处理
     * 如果 subType 是非类类型，但 superConstructor 是类类型:
     * - 先收集 subType 的所有类类型超类型
     * - 再对每个类类型超类型执行 collectAndFilter
     * - 将结果展平（flatMap）
     *
     * ## 使用场景
     *
     * ```cangjie
     * // 场景1: 类类型
     * class StringList : List<String>
     * findCorrespondingSupertypes(StringList, List) -> [List<String>]
     *
     * // 场景2: 类型参数
     * func foo<T : Comparable<T>>(x: T)
     * findCorrespondingSupertypes(T, Comparable) -> [Comparable<T>]
     *
     * // 场景3: 交集类型
     * T & Comparable<T>
     * findCorrespondingSupertypes(T & Comparable<T>, Comparable) -> [Comparable<T>]
     * ```
     *
     * @param state 类型检查器状态
     * @param subType 待查找的子类型
     * @param superConstructor 目标超类型的类型构造器
     * @return 所有匹配的超类型列表
     */
    fun findCorrespondingSupertypes(
        state: TypeCheckerState,
        subType: SimpleTypeMarker,
        superConstructor: TypeConstructorMarker
    ): List<SimpleTypeMarker> = with(state.typeSystemContext) {
        if (subType.isClassType()) {
            return collectAndFilter(state, subType, superConstructor)
        }

        // i.e. superType is not a classType
        if (!superConstructor.isClassTypeConstructor() && !superConstructor.isIntegerLiteralTypeConstructor()) {
            return collectAllSupertypesWithGivenTypeConstructor(state, subType, superConstructor)
        }

        val classTypeSupertypes = SmartList<SimpleTypeMarker>()
        state.anySupertype(subType, { false }) {
            if (it.isClassType()) {
                classTypeSupertypes.add(it)
                TypeCheckerState.SupertypesPolicy.None
            } else {
                TypeCheckerState.SupertypesPolicy.LowerIfFlexible
            }
        }

        return classTypeSupertypes.flatMap { collectAndFilter(state, it, superConstructor) }
    }
}
