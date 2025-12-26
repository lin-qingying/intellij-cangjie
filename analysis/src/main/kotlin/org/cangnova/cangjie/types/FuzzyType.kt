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
 * @receiver 模糊类型
 * @return 展示用的类型，如果没有自由参数则返回原始类型
 */
fun FuzzyType.presentationType(): CangJieType {
    if (freeParameters.isEmpty()) return type

    val map = HashMap<TypeConstructor, TypeProjection>()
    for ((argument, typeParameter) in type.arguments.zip(type.constructor.parameters)) {
        if (argument.projectionKind == Variance.INVARIANT) {
            val equalToFreeParameter = freeParameters.firstOrNull {
                StrictEqualityTypeChecker.strictEqualTypes(it.defaultType, argument.type.unwrap())
            } ?: continue

            map[equalToFreeParameter.typeConstructor] =
                createProjection(typeParameter.defaultType, Variance.INVARIANT, null)
        }
    }
    val substitutor = TypeSubstitutor.create(map)
    return substitutor.substitute(type, Variance.INVARIANT)!!
}

/**
 * 模糊类型
 *
 * 表示包含自由类型参数的类型，用于类型推导和匹配。
 * 模糊类型在类型检查过程中支持灵活的子类型判定和类型替换。
 *
 * 主要用途：
 * - 支持泛型类型的推导和匹配
 * - 处理函数签名中的类型参数
 * - 在重载解析中比较类型兼容性
 *
 * 核心功能：
 * - [checkIsSubtypeOf]: 检查是否是另一个类型的子类型
 * - [checkIsSuperTypeOf]: 检查是否是另一个类型的超类型
 * - [presentationType]: 转换为用户可读的展示类型
 *
 * @property type 底层的仓颉类型
 * @property freeParameters 自由类型参数集合
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
            val usedTypeParameters = HashSet<TypeParameterDescriptor>().apply { addUsedTypeParameters(type) }
            if (usedTypeParameters.isNotEmpty()) {
                val originalFreeParameters = freeParameters.map { it.toOriginal() }.toSet()
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
     *
     * @param type 要分析的类型
     */
    private fun MutableSet<TypeParameterDescriptor>.addUsedTypeParameters(type: CangJieType) {
        val typeParameter = type.constructor.declarationDescriptor as? TypeParameterDescriptor
        if (typeParameter != null && add(typeParameter)) {
            typeParameter.upperBounds.forEach { addUsedTypeParameters(it) }
        }

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
    fun checkIsSuperTypeOf(otherType: FuzzyType): TypeSubstitutor? =
        matchedSubstitutor(otherType, MatchKind.IS_SUPERTYPE)

    /**
     * 获取类型参数的原始描述符
     *
     * 如果类型参数来自成员函数，返回原始函数中对应的类型参数。
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
     * @param otherType 另一个类型
     * @return 如果是子类型返回类型替换器，否则返回 null
     */
    fun checkIsSubtypeOf(otherType: CangJieType): TypeSubstitutor? =
        checkIsSubtypeOf(otherType.toFuzzyType(emptyList()))

    /**
     * 检查是否是另一个模糊类型的子类型
     *
     * @param otherType 另一个模糊类型
     * @return 如果是子类型返回类型替换器，否则返回 null
     */
    fun checkIsSubtypeOf(otherType: FuzzyType): TypeSubstitutor? = matchedSubstitutor(otherType, MatchKind.IS_SUBTYPE)

    /**
     * 匹配类型枚举
     *
     * 定义类型匹配的方向：
     * - [IS_SUBTYPE]: 检查当前类型是否是另一个类型的子类型
     * - [IS_SUPERTYPE]: 检查当前类型是否是另一个类型的超类型
     */
    private enum class MatchKind {
        IS_SUBTYPE,
        IS_SUPERTYPE
    }

    /**
     * 检查是否是普通类型的超类型
     *
     * @param otherType 另一个类型
     * @return 如果是超类型返回类型替换器，否则返回 null
     */
    fun checkIsSuperTypeOf(otherType: CangJieType): TypeSubstitutor? =
        checkIsSuperTypeOf(otherType.toFuzzyType(emptyList()))

    /**
     * 匹配类型并返回替换器
     *
     * 使用约束系统进行类型匹配，处理自由类型参数的推导。
     * 这是核心的类型匹配算法，支持泛型类型的灵活匹配。
     *
     * 算法步骤：
     * 1. 处理错误类型和特殊情况（Unit 类型等）
     * 2. 如果没有自由参数，直接检查继承关系
     * 3. 创建约束系统并注册类型变量
     * 4. 添加子类型约束
     * 5. 求解约束系统
     * 6. 验证替换结果的正确性
     *
     * @param otherType 另一个模糊类型
     * @param matchKind 匹配类型（子类型或超类型）
     * @return 如果匹配成功返回类型替换器，否则返回 null
     */
    private fun matchedSubstitutor(otherType: FuzzyType, matchKind: MatchKind): TypeSubstitutor? {
        if (type.isError) return null
        if (otherType.type.isError) return null
        if (otherType.type.isUnit && matchKind == MatchKind.IS_SUBTYPE) return TypeSubstitutor.Companion.EMPTY

        fun CangJieType.checkInheritance(otherType: CangJieType): Boolean {
            return when (matchKind) {
                MatchKind.IS_SUBTYPE -> this.isSubtypeOf(otherType)
                MatchKind.IS_SUPERTYPE -> otherType.isSubtypeOf(this)
            }
        }

        if (freeParameters.isEmpty() && otherType.freeParameters.isEmpty()) {
            return if (type.checkInheritance(otherType.type)) TypeSubstitutor.Companion.EMPTY else null
        }

        val builder = ConstraintSystemBuilderImpl()
        val typeVariableSubstitutor =
            builder.registerTypeVariables(CallHandle.NONE, freeParameters + otherType.freeParameters)

        val typeInSystem = typeVariableSubstitutor.substitute(type, Variance.INVARIANT)
        val otherTypeInSystem = typeVariableSubstitutor.substitute(otherType.type, Variance.INVARIANT)

        when (matchKind) {
            MatchKind.IS_SUBTYPE ->
                builder.addSubtypeConstraint(
                    typeInSystem,
                    otherTypeInSystem,
                    ConstraintPositionKind.RECEIVER_POSITION.position()
                )

            MatchKind.IS_SUPERTYPE ->
                builder.addSubtypeConstraint(
                    otherTypeInSystem,
                    typeInSystem,
                    ConstraintPositionKind.RECEIVER_POSITION.position()
                )
        }

        builder.fixVariables()

        val constraintSystem = builder.build()

        if (constraintSystem.status.hasContradiction()) return null

        // 当前约束系统在可空性有问题时也返回成功状态
        // 所以我们必须手动检查子类型关系
        val substitutor = constraintSystem.resultingSubstitutor
        val substitutedType = substitutor.substitute(type, Variance.INVARIANT) ?: return null
        if (substitutedType.isError) return TypeSubstitutor.Companion.EMPTY
        val otherSubstitutedType = substitutor.substitute(otherType.type, Variance.INVARIANT) ?: return null
        if (otherSubstitutedType.isError) return TypeSubstitutor.Companion.EMPTY
        if (!substitutedType.checkInheritance(otherSubstitutedType)) return null

        val substitutorToKeepCapturedTypes = object : DelegatedTypeSubstitution(substitutor.substitution) {
            override fun approximateCapturedTypes() = false
        }.buildSubstitutor()

        val substitutionMap: Map<TypeConstructor, TypeProjection> = constraintSystem.typeVariables
            .map { it.originalTypeParameter }
            .associateBy(
                keySelector = { it.typeConstructor },
                valueTransform = { parameterDescriptor ->
                    val typeProjection = TypeProjectionImpl(Variance.INVARIANT, parameterDescriptor.defaultType)
                    val substitutedProjection = substitutorToKeepCapturedTypes.substitute(typeProjection)
                    substitutedProjection?.takeUnless { ErrorUtils.containsUninferredTypeVariable(it.type) }
                        ?: typeProjection
                })
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
 * @return 模糊类型
 */
fun CangJieType.toFuzzyType(freeParameters: Collection<TypeParameterDescriptor>) = FuzzyType(this, freeParameters)

/**
 * 获取可调用对象的模糊返回类型
 *
 * @receiver 可调用描述符
 * @return 模糊返回类型，如果没有返回类型则为 null
 */
fun CallableDescriptor.fuzzyReturnType() = returnType?.toFuzzyType(typeParameters)

/**
 * 检查模糊类型是否几乎是 "任意类型"
 *
 * 如果类型是单个自由类型参数且上界为 Any，则认为是 "几乎任意类型"。
 *
 * @receiver 模糊类型
 * @return 如果是 "几乎任意类型" 返回 true，否则返回 false
 */
fun FuzzyType.isAlmostEverything(): Boolean {
    if (freeParameters.isEmpty()) return false
    val typeParameter = type.constructor.declarationDescriptor as? TypeParameterDescriptor ?: return false
    if (typeParameter !in freeParameters) return false
    return typeParameter.upperBounds.singleOrNull()?.isAny() ?: false
}

/**
 * 将模糊类型转换为非可选类型
 *
 * @receiver 模糊类型
 * @return 非可选的模糊类型
 */
fun FuzzyType.makeNonOption() = type.makeNonOption().toFuzzyType(freeParameters)

/**
 * 获取可调用对象的模糊扩展接收者类型
 *
 * @receiver 可调用描述符
 * @return 模糊扩展接收者类型，如果没有扩展接收者则为 null
 */
fun CallableDescriptor.fuzzyExtensionReceiverType() = extensionReceiverParameter?.type?.toFuzzyType(typeParameters)
fun FuzzyType.optionality()  = type.optionality()
