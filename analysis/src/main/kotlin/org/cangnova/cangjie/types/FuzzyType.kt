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

import org.cangnova.cangjie.descriptors.CallableDescriptor
import org.cangnova.cangjie.descriptors.CallableMemberDescriptor
import org.cangnova.cangjie.descriptors.TypeParameterDescriptor
import org.cangnova.cangjie.resolve.calls.inference.CallHandle
import org.cangnova.cangjie.resolve.calls.inference.ConstraintSystemBuilderImpl
import org.cangnova.cangjie.resolve.calls.inference.constraintPosition.ConstraintPositionKind
import org.cangnova.cangjie.types.checker.StrictEqualityTypeChecker

/**
 * 将模糊类型转换为展示类型
 *
 * 尽可能将类型中的自由类型参数替换为对应的类类型参数。
 * 这用于在 IDE 中向用户展示更易理解的类型信息。
 *
 * 例如，对于一个模糊类型 `List<T>`，其中 T 是自由参数，
 * 如果能找到对应的类类型参数（如 String），
 * 则会转换为 `List<String>` 以便用户理解。
 *
 * @receiver 模糊类型
 * @return 展示用的类型，如果没有自由参数则返回原始类型
 */
fun FuzzyType.presentationType(): CangJieType {
    // 如果没有自由参数，直接返回原始类型
    if (freeParameters.isEmpty()) return type

    // 构建类型替换映射
    val map = HashMap<TypeConstructor, TypeArgument>()
    for ((argument, typeParameter) in type.arguments.zip(type.constructor.parameters)) {
        // 查找与当前参数匹配的自由类型参数
        val equalToFreeParameter = freeParameters.firstOrNull {
            StrictEqualityTypeChecker.strictEqualTypes(it.defaultType, argument.type.unwrap())
        } ?: continue

        // 将自由参数映射到对应的类型参数
        map[equalToFreeParameter.typeConstructor] =
            TypeArgumentImpl(typeParameter.defaultType)
    }

    // 创建替换器并执行替换
    val substitutor = DefaultTypeSubstitutor.create(map)
    return substitutor.substitute(type)!!
}

/**
 * 模糊类型
 *
 * 表示包含自由类型参数的类型，用于类型推导和匹配。
 * 模糊类型在类型检查过程中支持灵活的子类型判定和类型替换。
 *
 * ## 主要用途
 * - 支持泛型类型的推导和匹配
 * - 处理函数签名中的类型参数
 * - 在重载解析中比较类型兼容性
 *
 * ## 核心功能
 * - [checkIsSubtypeOf]: 检查是否是另一个类型的子类型
 * - [checkIsSuperTypeOf]: 检查是否是另一个类型的超类型
 * - [presentationType]: 转换为用户可读的展示类型
 *
 * ## 示例
 * ```kotlin
 * // 创建一个带自由参数 T 的模糊类型 List<T>
 * val fuzzyType = listType.toFuzzyType(listOf(typeParameterT))
 *
 * // 检查 List<T> 是否是 Collection<String> 的子类型
 * val substitutor = fuzzyType.checkIsSubtypeOf(collectionOfString)
 * ```
 *
 * @property type 底层的仓颉类型
 * @property freeParameters 自由类型参数集合，在初始化时会过滤掉未使用的参数
 * @constructor 创建模糊类型实例
 * @param type 底层的仓颉类型
 * @param freeParameters 自由类型参数集合
 */
class FuzzyType(val type: CangJieType, freeParameters: Collection<TypeParameterDescriptor>) {

    /**
     * 自由类型参数集合
     *
     * 包含在类型中实际使用的自由类型参数。
     * 在初始化时会过滤掉未在类型中使用的参数。
     */
    val freeParameters: Set<TypeParameterDescriptor>

    init {
        if (freeParameters.isNotEmpty()) {
            // 我们允许传递与原始函数相同但来自另一个函数的类型参数
            // 因此需要收集类型中实际使用的类型参数
            val usedTypeParameters = HashSet<TypeParameterDescriptor>().apply { addUsedTypeParameters(type) }
            if (usedTypeParameters.isNotEmpty()) {
                // 将自由参数转换为原始参数以便比较
                val originalFreeParameters = freeParameters.map { it.toOriginal() }.toSet()
                // 只保留实际使用的自由参数
                this.freeParameters = usedTypeParameters.filter { it.toOriginal() in originalFreeParameters }.toSet()
            } else {
                this.freeParameters = emptySet()
            }
        } else {
            this.freeParameters = emptySet()
        }
    }

    /**
     * 收集类型中使用的类型参数
     *
     * 递归遍历类型及其上界，收集所有使用的类型参数。
     * 这确保了我们只保留真正在类型表达式中出现的类型参数。
     *
     * @receiver 用于收集类型参数的可变集合
     * @param type 要分析的类型
     */
    private fun MutableSet<TypeParameterDescriptor>.addUsedTypeParameters(type: CangJieType) {
        // 检查类型构造器是否是类型参数
        val typeParameter = type.constructor.declarationDescriptor as? TypeParameterDescriptor
        if (typeParameter != null && add(typeParameter)) {
            // 递归处理类型参数的上界
            typeParameter.upperBounds.forEach { addUsedTypeParameters(it) }
        }

        // 递归处理类型参数
        for (argument in type.arguments) {
            addUsedTypeParameters(argument.type)
        }
    }

    /**
     * 检查是否是另一个模糊类型的超类型
     *
     * 使用约束系统判定类型关系，如果成功返回类型替换器。
     *
     * @param otherType 另一个模糊类型
     * @return 如果是超类型返回类型替换器，否则返回 null
     */
    fun checkIsSuperTypeOf(otherType: FuzzyType): DefaultTypeSubstitutor? =
        matchedSubstitutor(otherType, MatchKind.IS_SUPERTYPE)

    /**
     * 获取类型参数的原始描述符
     *
     * 如果类型参数来自成员函数，返回原始函数中对应的类型参数。
     * 这用于处理重写函数中的类型参数映射。
     *
     * 例如，对于重写的函数 `override fun <T> foo()`，
     * 该函数的类型参数 T 会被映射回原始函数中的对应类型参数。
     *
     * @receiver 类型参数描述符
     * @return 原始类型参数描述符
     */
    @Suppress("USELESS_ELVIS")
    private fun TypeParameterDescriptor.toOriginal(): TypeParameterDescriptor {
        val callableDescriptor = containingDeclaration as? CallableMemberDescriptor ?: return this
        val original = callableDescriptor.original ?: error("original = null for $callableDescriptor")
        val typeParameters = original.typeParameters ?: error("typeParameters = null for $original")
        return typeParameters[index]
    }

    /**
     * 检查是否是普通类型的子类型
     *
     * 将普通类型转换为没有自由参数的模糊类型后进行检查。
     *
     * @param otherType 另一个类型
     * @return 如果是子类型返回类型替换器，否则返回 null
     */
    fun checkIsSubtypeOf(otherType: CangJieType): DefaultTypeSubstitutor? =
        checkIsSubtypeOf(otherType.toFuzzyType(emptyList()))

    /**
     * 检查是否是另一个模糊类型的子类型
     *
     * @param otherType 另一个模糊类型
     * @return 如果是子类型返回类型替换器，否则返回 null
     */
    fun checkIsSubtypeOf(otherType: FuzzyType): DefaultTypeSubstitutor? = matchedSubstitutor(otherType, MatchKind.IS_SUBTYPE)

    /**
     * 匹配类型枚举
     *
     * 定义类型匹配的方向：
     * - [IS_SUBTYPE]: 检查当前类型是否是另一个类型的子类型（当前类型 <: 另一个类型）
     * - [IS_SUPERTYPE]: 检查当前类型是否是另一个类型的超类型（当前类型 :> 另一个类型）
     */
    private enum class MatchKind {
        /** 子类型检查 */
        IS_SUBTYPE,
        /** 超类型检查 */
        IS_SUPERTYPE
    }

    /**
     * 检查是否是普通类型的超类型
     *
     * 将普通类型转换为没有自由参数的模糊类型后进行检查。
     *
     * @param otherType 另一个类型
     * @return 如果是超类型返回类型替换器，否则返回 null
     */
    fun checkIsSuperTypeOf(otherType: CangJieType): DefaultTypeSubstitutor? =
        checkIsSuperTypeOf(otherType.toFuzzyType(emptyList()))

    /**
     * 匹配类型并返回替换器
     *
     * 使用约束系统进行类型匹配，处理自由类型参数的推导。
     * 这是核心的类型匹配算法，支持泛型类型的灵活匹配。
     *
     * ## 算法步骤
     * 1. 处理错误类型和特殊情况（Unit 类型等）
     * 2. 如果没有自由参数，直接检查继承关系
     * 3. 创建约束系统并注册类型变量
     * 4. 添加子类型约束
     * 5. 求解约束系统
     * 6. 验证替换结果的正确性
     * 7. 构建最终的类型替换器，保留捕获类型
     *
     * ## 特殊处理
     * - 对于 Unit 类型作为子类型的情况，直接返回空替换器
     * - 对于错误类型，返回 null 表示匹配失败
     * - 手动验证可空性，因为约束系统可能无法正确处理
     * - 保留捕获类型以维持类型系统的完整性
     *
     * @param otherType 另一个模糊类型
     * @param matchKind 匹配类型（子类型或超类型）
     * @return 如果匹配成功返回类型替换器，否则返回 null
     */
    private fun matchedSubstitutor(otherType: FuzzyType, matchKind: MatchKind): DefaultTypeSubstitutor? {
        // 错误类型无法匹配
        if (type.isError) return null
        if (otherType.type.isError) return null

        // Unit 类型作为子类型总是匹配成功
        if (otherType.type.isUnit && matchKind == MatchKind.IS_SUBTYPE) return DefaultTypeSubstitutor.Companion.EMPTY

        /**
         * 检查继承关系的辅助函数
         *
         * @receiver 当前类型
         * @param otherType 另一个类型
         * @return 是否满足指定的继承关系
         */
        fun CangJieType.checkInheritance(otherType: CangJieType): Boolean {
            return when (matchKind) {
                MatchKind.IS_SUBTYPE -> this.isSubtypeOf(otherType)
                MatchKind.IS_SUPERTYPE -> otherType.isSubtypeOf(this)
            }
        }

        // 如果两边都没有自由参数，直接检查继承关系
        if (freeParameters.isEmpty() && otherType.freeParameters.isEmpty()) {
            return if (type.checkInheritance(otherType.type)) DefaultTypeSubstitutor.Companion.EMPTY else null
        }

        // 创建约束系统构建器
        val builder = ConstraintSystemBuilderImpl()

        // 注册所有自由类型参数为类型变量
        val typeVariableSubstitutor =
            builder.registerTypeVariables(CallHandle.NONE, freeParameters + otherType.freeParameters)

        // 将类型替换到约束系统中
        val typeInSystem = typeVariableSubstitutor.substitute(type)
        val otherTypeInSystem = typeVariableSubstitutor.substitute(otherType.type)

        // 根据匹配类型添加相应的约束
        when (matchKind) {
            MatchKind.IS_SUBTYPE ->
                // 添加约束：typeInSystem <: otherTypeInSystem
                builder.addSubtypeConstraint(
                    typeInSystem,
                    otherTypeInSystem,
                    ConstraintPositionKind.RECEIVER_POSITION.position()
                )

            MatchKind.IS_SUPERTYPE ->
                // 添加约束：otherTypeInSystem <: typeInSystem
                builder.addSubtypeConstraint(
                    otherTypeInSystem,
                    typeInSystem,
                    ConstraintPositionKind.RECEIVER_POSITION.position()
                )
        }

        // 固定类型变量（完成约束收集）
        builder.fixVariables()

        // 构建约束系统
        val constraintSystem = builder.build()

        // 如果约束系统有矛盾，匹配失败
        if (constraintSystem.status.hasContradiction()) return null

        // 当前约束系统在可空性有问题时也返回成功状态
        // 所以我们必须手动检查子类型关系
        val substitutor = constraintSystem.resultingSubstitutor
        val substitutedType = substitutor.substitute(type) ?: return null
        if (substitutedType.isError) return DefaultTypeSubstitutor.Companion.EMPTY
        val otherSubstitutedType = substitutor.substitute(otherType.type) ?: return null
        if (otherSubstitutedType.isError) return DefaultTypeSubstitutor.Companion.EMPTY

        // 验证替换后的类型是否满足继承关系
        if (!substitutedType.checkInheritance(otherSubstitutedType)) return null

        // 创建保留捕获类型的替换器
        // 捕获类型用于处理通配符类型的类型安全
        val substitutorToKeepCapturedTypes = object : DelegatedTypeSubstitution(substitutor.substitution) {
            override fun approximateCapturedTypes() = false
        }.buildSubstitutor()

        // 构建最终的替换映射
        val substitutionMap: Map<TypeConstructor, TypeArgument> = constraintSystem.typeVariables
            .map { it.originalTypeParameter }
            .associateBy(
                keySelector = { it.typeConstructor },
                valueTransform = { parameterDescriptor ->
                    // 使用默认类型作为初始投影
                    val typeProjection = TypeArgumentImpl(parameterDescriptor.defaultType)
                    // 应用替换器
                    val substitutedProjection = substitutorToKeepCapturedTypes.substitute(typeProjection)
                    // 如果替换结果包含未推导的类型变量，使用默认类型
                    substitutedProjection?.takeUnless { ErrorUtils.containsUninferredTypeVariable(it.type) }
                        ?: typeProjection
                })

        // 创建最终的类型替换器
        return TypeConstructorSubstitution.Companion.createByConstructorsMap(
            substitutionMap,
            approximateCapturedTypes = true
        ).buildSubstitutor()
    }
}

/**
 * 将普通类型转换为模糊类型
 *
 * @receiver 仓颉类型
 * @param freeParameters 自由类型参数集合
 * @return 包含指定自由参数的模糊类型
 */
fun CangJieType.toFuzzyType(freeParameters: Collection<TypeParameterDescriptor>) = FuzzyType(this, freeParameters)

/**
 * 获取可调用对象的模糊返回类型
 *
 * 将返回类型转换为模糊类型，使用可调用对象的类型参数作为自由参数。
 * 这对于泛型函数的类型推导特别有用。
 *
 * @receiver 可调用描述符
 * @return 模糊返回类型，如果没有返回类型则为 null
 */
fun CallableDescriptor.fuzzyReturnType() = returnType?.toFuzzyType(typeParameters)

/**
 * 检查模糊类型是否几乎是 "任意类型"
 *
 * 如果类型是单个自由类型参数且上界为 Any，则认为是 "几乎任意类型"。
 * 这种类型可以匹配几乎任何类型（除了平台类型等特殊情况）。
 *
 * ## 判定条件
 * 1. 必须有自由参数
 * 2. 类型构造器必须是类型参数
 * 3. 该类型参数必须在自由参数集合中
 * 4. 该类型参数的上界必须是单个 Any 类型
 *
 * @receiver 模糊类型
 * @return 如果是 "几乎任意类型" 返回 true，否则返回 false
 */
fun FuzzyType.isAlmostEverything(): Boolean {
    // 必须有自由参数
    if (freeParameters.isEmpty()) return false

    // 类型构造器必须是类型参数
    val typeParameter = type.constructor.declarationDescriptor as? TypeParameterDescriptor ?: return false

    // 该类型参数必须在自由参数集合中
    if (typeParameter !in freeParameters) return false

    // 上界必须是单个 Any 类型
    return typeParameter.upperBounds.singleOrNull()?.isAny() ?: false
}

/**
 * 将模糊类型转换为非可选类型
 *
 * 移除类型的可选性标记（?），但保留相同的自由参数集合。
 *
 * @receiver 模糊类型
 * @return 非可选的模糊类型
 */
fun FuzzyType.makeNonOption() = type.makeNonOption().toFuzzyType(freeParameters)

/**
 * 获取模糊类型的可选性
 *
 * 返回底层类型的可选性状态。
 *
 * @receiver 模糊类型
 * @return 类型的可选性
 */
fun FuzzyType.optionality() = type.optionality()