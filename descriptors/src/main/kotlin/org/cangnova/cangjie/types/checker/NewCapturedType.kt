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

package org.cangnova.cangjie.types.checker

import org.cangnova.cangjie.builtins.CangJieBuiltIns
import org.cangnova.cangjie.descriptors.ClassifierDescriptor
import org.cangnova.cangjie.descriptors.TypeParameterDescriptor
import org.cangnova.cangjie.resolve.call.inference.CapturedTypeConstructor
import org.cangnova.cangjie.resolve.scopes.MemberScope
import org.cangnova.cangjie.types.*

import org.cangnova.cangjie.types.error.ErrorScopeKind
import org.cangnova.cangjie.types.model.CaptureStatus
import org.cangnova.cangjie.types.model.CapturedTypeMarker

/**
 * 捕获类型构造器
 *
 * 捕获类型用于表示泛型通配符的实际类型。例如：
 * - `List<*>` 会被捕获为 `List<Captured(*)>`
 * - `List<out Number>` 会被捕获为 `List<Captured(out Number)>`
 *
 * @param projection 类型投影（通配符）
 * @param supertypesComputation 父类型计算（延迟计算）
 * @param original 原始构造器（用于 refine 操作）
 * @param typeParameter 对应的类型参数
 */
class NewCapturedTypeConstructor(
    override val projection: TypeProjection,
    private var supertypesComputation: (() -> List<UnwrappedType>)? = null,
    private val original: NewCapturedTypeConstructor? = null,
    val typeParameter: TypeParameterDescriptor? = null
) : CapturedTypeConstructor {

    /**
     * 辅助构造函数，直接接收父类型列表
     */
    constructor(
        projection: TypeProjection,
        supertypes: List<UnwrappedType>,
        original: NewCapturedTypeConstructor? = null
    ) : this(projection, { supertypes }, original)

    private val _supertypes by lazy(LazyThreadSafetyMode.PUBLICATION) {
        supertypesComputation?.invoke()
    }

    /**
     * 初始化父类型
     * 用于在捕获类型创建后延迟设置其父类型
     */
    fun initializeSupertypes(supertypes: List<UnwrappedType>) {
        assert(this.supertypesComputation == null) {
            "Already initialized! oldValue = ${this.supertypesComputation}, newValue = $supertypes"
        }
        this.supertypesComputation = { supertypes }
    }

    override val supertypes
        get() = _supertypes ?: emptyList()

    /** 捕获类型没有类型参数 */
    override val parameters: List<TypeParameterDescriptor>
        get() = emptyList()

    /** 捕获类型不是 final 的 */
    override val isFinal: Boolean
        get() = false

    /** 捕获类型不可表示（不能在源代码中直接写出） */
    override val isDenotable: Boolean
        get() = false

    /** 捕获类型没有声明描述符 */
    override val declarationDescriptor: ClassifierDescriptor?
        get() = null

    override val builtIns: CangJieBuiltIns
        get() = projection.type.builtIns

    /**
     * 精化（refine）捕获类型构造器
     * 用于在类型精化过程中创建新的捕获类型构造器
     */
    override fun refine(cangjieTypeRefiner: CangJieTypeRefiner) =
        NewCapturedTypeConstructor(
            projection.refine(cangjieTypeRefiner),
            supertypesComputation?.let {
                {

                    supertypes.map { it.refine(cangjieTypeRefiner) }

                }
            },
            original ?: this,
            typeParameter = typeParameter
        )

    /**
     * 相等性比较基于原始构造器
     * 同一个原始构造器的所有精化版本都被认为是相等的
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as NewCapturedTypeConstructor

        return (original ?: this) === (other.original ?: other)
    }

    override fun hashCode(): Int = original?.hashCode() ?: super.hashCode()
    override fun toString() = "CapturedType($projection)"
}

/**
 * 捕获类型
 *
 * 用于表示泛型通配符捕获后的具体类型。
 *
 * 关于 [lowerType]：
 * - 目前仅对 `in` 投影非空
 * - 示例：`Inv<in String>` 对于 `in String` 创建的 CapturedType，其 [lowerType] = String
 *
 * TODO: 对于复杂的泛型约束，需要使用约束系统设置 [lowerType]
 *   例如：`interface D<T, S: List<T>>, D<*, List<Number>>` -> `D<Q, List<Number>>`
 *   应该为 Q 设置 [lowerType] = Number
 *
 * @param captureStatus 捕获状态（FROM_EXPRESSION 或 FOR_SUBTYPING）
 * @param constructor 捕获类型构造器
 * @param lowerType 下界类型（仅用于 in 投影）
 * @param attributes 类型属性
 * @param isOption 是否为可选类型
 * @param isProjectionNotNull 投影是否非空
 */
class NewCapturedType(
    val captureStatus: CaptureStatus,
    override val constructor: NewCapturedTypeConstructor,
    val lowerType: UnwrappedType?, // todo check lower type for option captured types
    override val attributes: TypeAttributes = TypeAttributes.Empty,
    override val isOption: Boolean = false,
    val isProjectionNotNull: Boolean = false
) : SimpleType(), CapturedTypeMarker {
    /**
     * 内部构造函数，用于简化捕获类型的创建
     */
    internal constructor(
        captureStatus: CaptureStatus,
        lowerType: UnwrappedType?,
        projection: TypeProjection,
        typeParameter: TypeParameterDescriptor
    ) : this(captureStatus, NewCapturedTypeConstructor(projection, typeParameter = typeParameter), lowerType)

    /** 捕获类型没有类型参数 */
    override val arguments: List<TypeProjection> get() = listOf()

    /**
     * 成员作用域
     * TODO: 处理类似 `foo().bar()` 的情况，其中 `foo()` 返回捕获类型
     */
    override val memberScope: MemberScope
        get() = ErrorUtils.createErrorScope(ErrorScopeKind.CAPTURED_TYPE_SCOPE, throwExceptions = true)

    override fun replaceAttributes(newAttributes: TypeAttributes): SimpleType =
        NewCapturedType(captureStatus, constructor, lowerType, newAttributes, isOption, isProjectionNotNull)

    override fun makeOptionAsSpecified(isOption: Boolean): SimpleType =
        NewCapturedType(captureStatus, constructor, lowerType, attributes, isOption, isProjectionNotNull)

    /**
     * 精化捕获类型
     */
    override fun refine(cangjieTypeRefiner: CangJieTypeRefiner) =
        NewCapturedType(
            captureStatus,
            constructor.refine(cangjieTypeRefiner),
            lowerType?.let { cangjieTypeRefiner.refineType(it).unwrap() },
            attributes,
            isOption
        )
}

/**
 * 捕获类型参数
 *
 * 将类型的通配符参数转换为捕获类型。
 * 例如：`List<*>` -> `List<Captured(*)>`
 *
 * @param type 要捕获的类型
 * @param status 捕获状态
 * @return 捕获后的类型参数列表，如果不需要捕获则返回 null
 */
private fun captureArguments(type: UnwrappedType, status: CaptureStatus): List<TypeProjection>? {
    // 类型参数数量不匹配，无法捕获
    if (type.arguments.size != type.constructor.parameters.size) return null

    val arguments = type.arguments
    // 所有参数都是不变的，不需要捕获
    if (arguments.all { it.projectionKind == Variance.INVARIANT }) return null

    // 为每个类型参数创建捕获类型
    val capturedArguments = arguments.zip(type.constructor.parameters).map { (projection, parameter) ->
        // 不变参数直接返回
        if (projection.projectionKind == Variance.INVARIANT) return@map projection

        // 下界类型（当前已注释，未来可能启用）
        val lowerType = null
//            if (!projection.isStarProjection && projection.projectionKind == Variance.IN_VARIANCE) {
//                projection.type.unwrap()
//            } else {
//                null
//            }

        NewCapturedType(
            status,
            lowerType,
            projection,
            parameter
        ).asTypeProjection() // todo optimization: do not create type projection
    }

    // 创建替换器，用于替换类型参数
    val substitutor = TypeConstructorSubstitution.create(type.constructor, capturedArguments).buildSubstitutor()

    // 为每个捕获类型初始化其父类型
    for (index in arguments.indices) {
        val oldProjection = arguments[index]
        val newProjection = capturedArguments[index]

        if (oldProjection.projectionKind == Variance.INVARIANT) continue
        // 获取类型参数的上界，并进行类型替换
        val capturedTypeSupertypes = type.constructor.parameters[index].upperBounds.mapTo(mutableListOf()) {
            CangJieTypePreparator.Default.prepareType(substitutor.safeSubstitute(it, Variance.INVARIANT).unwrap())
        }
//
//        if (!oldProjection.isStarProjection && oldProjection.projectionKind == Variance.OUT_VARIANCE) {
//            capturedTypeSupertypes += CangJieTypePreparator.Default.prepareType(oldProjection.type.unwrap())
//        }

        val capturedType = newProjection.type as NewCapturedType
        capturedType.constructor.initializeSupertypes(capturedTypeSupertypes)
    }

    return capturedArguments
}

/**
 * 从简单类型捕获类型参数
 *
 * 该函数假定输入类型是简单分类器类型
 */
internal fun captureFromArguments(type: SimpleType, status: CaptureStatus) =
    captureArguments(type, status)?.let { type.replaceArguments(it) }

/**
 * 替换类型参数
 */
private fun UnwrappedType.replaceArguments(arguments: List<TypeProjection>) =
    CangJieTypeFactory.simpleType(attributes, constructor, arguments, isOption)

/**
 * 从任意非包装类型捕获类型参数
 */
private fun captureFromArguments(type: UnwrappedType, status: CaptureStatus): UnwrappedType? {
    val capturedArguments = captureArguments(type, status) ?: return null

    return if (type is FlexibleType) {
        CangJieTypeFactory.flexibleType(
            type.lowerBound.replaceArguments(capturedArguments),
            type.upperBound.replaceArguments(capturedArguments)
        )
    } else {
        type.replaceArguments(capturedArguments)
    }
}

/**
 * 准备参数类型（关于捕获类型）
 *
 * @return null 表示类型应该保持原样
 */
fun prepareArgumentTypeRegardingCaptureTypes(argumentType: UnwrappedType): UnwrappedType? {
    return null
//    return
//    if (argumentType is NewCapturedType) null else
//        captureFromExpression(argumentType)
}

/**
 * 从表达式捕获类型
 *
 * 将表达式中的通配符类型转换为捕获类型。
 * 对于交集类型，使用特殊的处理逻辑。
 *
 * @param type 要捕获的类型
 * @return 捕获后的类型，如果不需要捕获则返回 null
 */
fun captureFromExpression(type: UnwrappedType): UnwrappedType? {
    val typeConstructor = type.constructor

    // 非交集类型，直接捕获参数
    if (typeConstructor !is IntersectionTypeConstructor) {
        return captureFromArguments(type, CaptureStatus.FROM_EXPRESSION)
    }

    /*
     * 交集类型的捕获处理：
     *  1) 首先，为所有类型参数创建捕获参数，按类型构造器*和类型参数类型分组。
     *     这意味着，对于灵活类型中的 `Foo<*>` 和 `Foo<*>?`，我们只创建一个捕获参数。
     *     * 除了按类型构造器分组，我们还考虑两个类型是否可能位于同一灵活类型的不同边界中。
     *       这对于创建相同的捕获参数是必要的，
     *       例如，对于灵活类型下界的 `MutableList` 和上界的 `List`。
     *       示例：MutableList<*>..List<*>? -> MutableList<Captured1(*)>..List<Captured2(*)>?
     *             Captured1(*) 和 Captured2(*) 是相同的。
     *  2) 其次，根据给定的类型构造器和类型参数，用捕获参数替换类型参数。
     */
    val capturedArgumentsByComponents = captureArgumentsForIntersectionType(type) ?: return null

    // 我们为某些类型重用 `TypeToCapture`，是否适合重用由 `isSuitableForType` 定义
    fun findCorrespondingCapturedArgumentsForType(type: CangJieType) =
        capturedArgumentsByComponents.find { typeToCapture -> typeToCapture.isSuitableForType(type) }?.capturedArguments

    /**
     * 按交集组件替换类型参数为捕获参数
     */
    fun replaceArgumentsWithCapturedArgumentsByIntersectionComponents(typeToReplace: UnwrappedType): List<SimpleType> {
        return if (typeToReplace.constructor is IntersectionTypeConstructor) {
            // 处理交集类型的每个组件
            typeToReplace.constructor.supertypes.map { componentType ->
                val capturedArguments = findCorrespondingCapturedArgumentsForType(componentType)
                    ?: return@map componentType.asSimpleType()
                componentType.unwrap().replaceArguments(capturedArguments)
            }
        } else {
            // 处理非交集类型
            val capturedArguments = findCorrespondingCapturedArgumentsForType(typeToReplace)
                ?: return listOf(typeToReplace.asSimpleType())
            listOf(typeToReplace.unwrap().replaceArguments(capturedArguments))
        }
    }

    return if (type is FlexibleType) {
        // 处理灵活类型的上下界
        val lowerIntersectedType =
            intersectTypes(replaceArgumentsWithCapturedArgumentsByIntersectionComponents(type.lowerBound))
                .makeOptionAsSpecified(type.lowerBound.isOption)
        val upperIntersectedType =
            intersectTypes(replaceArgumentsWithCapturedArgumentsByIntersectionComponents(type.upperBound))
                .makeOptionAsSpecified(type.upperBound.isOption)

        CangJieTypeFactory.flexibleType(lowerIntersectedType, upperIntersectedType)
    } else {
        intersectTypes(replaceArgumentsWithCapturedArgumentsByIntersectionComponents(type)).makeOptionAsSpecified(type.isOption)
    }
}

/**
 * 为交集类型捕获类型参数
 *
 * @return 捕获的参数列表，如果不需要捕获则返回 null
 */
private fun captureArgumentsForIntersectionType(type: CangJieType): List<CapturedArguments>? {
    // 获取需要捕获的类型列表
    // 交集类型的边界可能是非交集类型
    fun getTypesToCapture(type: CangJieType) =
        if (type.constructor is IntersectionTypeConstructor) type.constructor.supertypes else listOf(type)

    val filteredTypesToCapture =
        if (type is FlexibleType) {
            // 合并灵活类型的上下界类型，并去重
            val typesToCapture = getTypesToCapture(type.lowerBound) + getTypesToCapture(type.upperBound)
            typesToCapture.distinctBy {
                (/*FlexibleTypeBoundsChecker.getBaseBoundFqNameByMutability(it) ?: */it.constructor) to it.arguments
            }
        } else type.constructor.supertypes

    var changed = false

    val capturedArgumentsByTypes = filteredTypesToCapture.mapNotNull { typeToCapture ->
        val capturedArguments = captureArguments(typeToCapture.unwrap(), CaptureStatus.FROM_EXPRESSION)
            ?: return@mapNotNull null
        changed = true
        CapturedArguments(capturedArguments, originalType = typeToCapture)
    }

    if (!changed) return null

    return capturedArgumentsByTypes
}

/**
 * 捕获的参数信息
 *
 * 存储捕获的类型参数及其原始类型，用于判断是否适用于其他类型
 */
private class CapturedArguments(val capturedArguments: List<TypeProjection>, private val originalType: CangJieType) {
    /**
     * 判断捕获的参数是否适用于给定类型
     *
     * 要求：
     * 1. 类型参数匹配
     * 2. 类型构造器匹配
     */
    fun isSuitableForType(type: CangJieType): Boolean {
        // 检查类型参数是否匹配
        val areArgumentsMatched = type.arguments.withIndex().all { (i, typeArgumentsType) ->
            originalType.arguments.size > i && typeArgumentsType == originalType.arguments[i]
        }

        if (!areArgumentsMatched) return false

        // 检查类型构造器是否匹配
        val areConstructorsMatched = originalType.constructor == type.constructor
//                || areTypesMayBeLowerAndUpperBoundsOfSameFlexibleTypeByMutability(originalType, type)

        return areConstructorsMatched
    }
}
