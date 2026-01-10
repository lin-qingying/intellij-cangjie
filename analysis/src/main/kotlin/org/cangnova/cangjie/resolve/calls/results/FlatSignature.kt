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

package org.cangnova.cangjie.resolve.calls.results

import org.cangnova.cangjie.descriptors.CallableDescriptor
import org.cangnova.cangjie.descriptors.MemberDescriptor
import org.cangnova.cangjie.descriptors.ValueParameterDescriptor
import org.cangnova.cangjie.descriptors.VariableDescriptor
import org.cangnova.cangjie.descriptors.synthetic.SyntheticMemberDescriptor
import org.cangnova.cangjie.resolve.calls.components.hasDefaultValue
import org.cangnova.cangjie.resolve.calls.inference.ConstraintSystem
import org.cangnova.cangjie.types.CangJieType
import org.cangnova.cangjie.types.UnwrappedType
import org.cangnova.cangjie.types.checker.CangJieTypeChecker
import org.cangnova.cangjie.types.checker.CangJieTypePreparator
import org.cangnova.cangjie.types.getValueParameterTypesFromCallableReflectionType
import org.cangnova.cangjie.types.model.*

/**
 * 特异性比较回调接口
 *
 * 用于在重载解析过程中比较两个候选函数的特异性时提供自定义行为。
 * 特异性比较是确定哪个重载函数更具体（更适合当前调用）的关键机制。
 */
interface SpecificityComparisonCallbacks {
    /**
     * 判断即使不是子类型关系，特定类型是否也不比一般类型更不特异
     *
     * 在某些情况下，即使两个类型之间不存在子类型关系，
     * 我们仍然可能认为一个类型比另一个更特异。
     *
     * @param specific 更特异的类型候选
     * @param general 更一般的类型候选
     * @return 如果specific即使不是general的子类型也不算更不特异，则返回true
     */
    fun isNonSubtypeNotLessSpecific(specific: CangJieTypeMarker, general: CangJieTypeMarker): Boolean
}

/**
 * 带转换信息的类型
 *
 * 封装了类型及其可能的原始类型（转换前的类型）。
 * 这在处理 SAM 转换等类型转换场景时很有用。
 *
 * @property resultType 结果类型（转换后的类型）
 * @property originalTypeIfWasConverted 如果发生了转换，这是原始类型；否则为null
 */
class TypeWithConversion(val resultType: CangJieTypeMarker?, val originalTypeIfWasConverted: CangJieTypeMarker? = null)

/**
 * 使用转换后的类型列表创建 FlatSignature
 *
 * @param origin 原始对象
 * @param descriptor 可调用描述符
 * @param numDefaults 有默认值的参数数量
 * @param parameterTypes 参数类型列表（包含转换信息）
 * @return 创建的扁平签名
 */
@JvmName("createWithConvertedTypes")
fun <T> FlatSignature.Companion.create(
    origin: T,
    descriptor: CallableDescriptor,
    numDefaults: Int,
    parameterTypes: List<TypeWithConversion?>,
): FlatSignature<T> {

    return FlatSignature(
        origin,
        descriptor.typeParameters,
        valueParameterTypes = parameterTypes,
        hasVarargs = descriptor.valueParameters.any { it.varargElementType != null },
        numDefaults = numDefaults,
        isSyntheticMember = descriptor is SyntheticMemberDescriptor<*>
    )
}

/**
 * 扁平签名类
 *
 * 表示函数或属性的签名的扁平化表示，用于重载解析和特异性比较。
 * "扁平"是指将所有相关信息（类型参数、值参数类型等）收集到一个结构中，
 * 便于统一处理和比较。
 *
 * @param T 原始对象的类型
 * @property origin 原始对象（通常是描述符）
 * @property typeParameters 类型参数集合
 * @property hasVarargs 是否有可变参数
 * @property numDefaults 有默认值的参数数量
 * @property isSyntheticMember 是否是合成成员（编译器生成的）
 * @property valueParameterTypes 值参数类型列表（包含转换信息）
 */
class FlatSignature<out T>(
    val origin: T,
    val typeParameters: Collection<TypeParameterMarker>,
    val hasVarargs: Boolean,

    val numDefaults: Int,

    val isSyntheticMember: Boolean,
    val valueParameterTypes: List<TypeWithConversion?>,
) {
    /** 是否是泛型（有类型参数） */
    val isGeneric = typeParameters.isNotEmpty()

    /**
     * 使用简单类型列表的构造函数
     *
     * @param origin 原始对象
     * @param typeParameters 类型参数集合
     * @param valueParameterTypes 值参数类型列表（不包含转换信息）
     * @param hasVarargs 是否有可变参数
     * @param numDefaults 有默认值的参数数量
     * @param isSyntheticMember 是否是合成成员
     */
    constructor(
        origin: T,
        typeParameters: Collection<TypeParameterMarker>,
        valueParameterTypes: List<CangJieTypeMarker?>,
        hasVarargs: Boolean,

        numDefaults: Int,

        isSyntheticMember: Boolean,
    ) : this(
        origin, typeParameters, hasVarargs, numDefaults,
        isSyntheticMember, valueParameterTypes.map(::TypeWithConversion)
    )

    companion object
}


/**
 * 为可能被遮蔽的扩展函数创建扁平签名
 *
 * 扩展函数可能被同名的成员函数遮蔽，这个方法创建用于比较的签名。
 *
 * @param descriptor 可调用描述符
 * @return 扁平签名
 */
fun <D : CallableDescriptor> FlatSignature.Companion.createForPossiblyShadowedExtension(descriptor: D): FlatSignature<D> =
    FlatSignature(
        descriptor,
        descriptor.typeParameters,
        valueParameterTypes = descriptor.valueParameters.map { it.argumentValueType },
        hasVarargs = descriptor.valueParameters.any { it.varargElementType != null },
        numDefaults = descriptor.valueParameters.count { it.hasDefaultValue() },
        isSyntheticMember = descriptor is SyntheticMemberDescriptor<*>
    )

/**
 * 从可调用描述符创建扁平签名
 *
 * 这是创建扁平签名的标准方法，直接从描述符提取所有必要信息。
 *
 * @param descriptor 可调用描述符
 * @return 扁平签名
 */
fun <D : CallableDescriptor> FlatSignature.Companion.createFromCallableDescriptor(descriptor: D): FlatSignature<D> =
    FlatSignature(
        descriptor,
        descriptor.typeParameters,
        valueParameterTypes =
            descriptor.valueParameters.map { it.argumentValueType },
        hasVarargs = descriptor.valueParameters.any { it.varargElementType != null },

        numDefaults = 0,
        isSyntheticMember = descriptor is SyntheticMemberDescriptor<*>
    )


/**
 * 判断一个签名是否不比另一个签名更不特异
 *
 * 这是重载解析中的核心比较逻辑。通过建立约束系统来判断 specific 签名
 * 是否至少和 general 签名一样特异（或更特异）。
 *
 * 比较过程：
 * 1. 检查参数数量是否相同
 * 2. 对每个参数位置，检查 specific 的参数类型是否不比 general 的更不特异
 * 3. 如果使用原始 SAM 类型，还需要对转换前的类型进行检查
 * 4. 确保约束系统没有矛盾
 *
 * @param specific 更特异的签名候选
 * @param general 更一般的签名候选
 * @param callbacks 特异性比较回调
 * @param specificityComparator 类型特异性比较器
 * @param useOriginalSamTypes 是否使用原始 SAM 类型进行比较
 * @return 如果 specific 不比 general 更不特异则返回true
 */
fun <T> ConstraintSystem.isSignatureNotLessSpecific(
    specific: FlatSignature<T>,
    general: FlatSignature<T>,
    callbacks: SpecificityComparisonCallbacks,
    specificityComparator: TypeSpecificityComparator,
    useOriginalSamTypes: Boolean = false
): Boolean {
    // 参数数量必须相同
    if (specific.valueParameterTypes.size != general.valueParameterTypes.size)
        return false

    // 检查结果类型（转换后的类型）
    if (!isValueParameterTypeNotLessSpecific(specific, general, callbacks, specificityComparator) { it?.resultType }) {
        return false
    }

    // 如果需要，检查原始类型（转换前的类型）
    if (useOriginalSamTypes && !isValueParameterTypeNotLessSpecific(
            specific, general, callbacks, specificityComparator
        ) { it?.originalTypeIfWasConverted }
    ) {
        return false
    }

    // 确保约束系统没有矛盾
    return !hasContradiction
}

/**
 * 检查值参数类型是否不比另一个更不特异
 *
 * 这是签名比较的核心实现，逐个比较参数位置的类型特异性。
 *
 * @param specific 更特异的签名候选
 * @param general 更一般的签名候选
 * @param callbacks 特异性比较回调
 * @param specificityComparator 类型特异性比较器
 * @param typeKindSelector 类型选择器，用于选择要比较的类型（结果类型或原始类型）
 * @return 如果所有参数位置的类型都不比对应位置更不特异则返回true
 */
private fun <T> ConstraintSystem.isValueParameterTypeNotLessSpecific(
    specific: FlatSignature<T>,
    general: FlatSignature<T>,
    callbacks: SpecificityComparisonCallbacks,
    specificityComparator: TypeSpecificityComparator,
    typeKindSelector: (TypeWithConversion?) -> CangJieTypeMarker?
): Boolean {
    val typeParameters = general.typeParameters
    val csBuilder = getBuilder()
    // 注册类型参数为类型变量
    val typeSubstitutor = csBuilder.registerTypeVariables(typeParameters)


    var specificValueParameterTypes = specific.valueParameterTypes
    var generalValueParameterTypes = general.valueParameterTypes


    // 遍历每个参数位置
    for (index in specificValueParameterTypes.indices) {
        val specificType = typeKindSelector(specificValueParameterTypes[index]) ?: continue
        val generalType = typeKindSelector(generalValueParameterTypes[index]) ?: continue

        // 如果 specific 的类型明确更不特异，直接返回 false
        if (specificityComparator.isDefinitelyLessSpecific(specificType, generalType)) {
            return false
        }

        // 如果 general 没有类型参数，或者 generalType 不依赖于类型参数
        if (typeParameters.isEmpty() || !generalType.dependsOnTypeParameters(this, typeParameters)) {
            // 直接检查子类型关系
            if (!CangJieTypeChecker.DEFAULT.isSubtypeOf(specificType as CangJieType, generalType as CangJieType)) {
                // 如果不是子类型，使用回调判断是否仍然不算更不特异
                if (!callbacks.isNonSubtypeNotLessSpecific(specificType, generalType)) {
                    return false
                }
            }
        } else {
            // general 的类型包含类型参数，需要替换后再比较
            val substitutedGeneralType = typeSubstitutor.safeSubstitute(this, generalType)

            /**
             * 示例：
             * fun <X> Array<out X>.sort(): Unit {}
             * fun <Y: Comparable<Y>> Array<out Y>.sort(): Unit {}
             *
             * 当我们尝试解决约束系统 CS(Y 是变量) 时，
             * Array<out X> <: Array<out Y> 这个系统无法求解，
             * 因此我们需要从接收者和值参数中捕获类型。
             */
            val specificCapturedType = CangJieTypePreparator.Default.prepareType(specificType)
                .let { this.captureFromExpression(it) ?: it }

            // 添加子类型约束
            csBuilder.addSubtypeConstraint(specificCapturedType, substitutedGeneralType)
        }
    }

    return true
}

/**
 * 从反射类型信息创建一个 [FlatSignature] 实例
 *
 * 此函数专为处理可调用引用而设计，处理反射类型中的接收者类型、
 * 上下文接收者类型、参数类型等信息。
 *
 * 可调用引用的反射类型不包含接收者类型，这些信息需要从其他地方获取。
 *
 * @param origin 原始对象，可以是任何类型的对象
 * @param descriptor 可调用描述符，描述了方法或属性的元数据
 * @param numDefaults 默认参数的数量
 * @param hasBoundExtensionReceiver 是否有绑定的扩展接收者
 * @param reflectionType 反射类型，用于获取可调用引用的类型信息
 * @return 创建的 [FlatSignature] 实例
 */
fun <T> FlatSignature.Companion.createFromReflectionType(
    origin: T,
    descriptor: CallableDescriptor,
    numDefaults: Int,
    hasBoundExtensionReceiver: Boolean,
    reflectionType: UnwrappedType
): FlatSignature<T> {


    // 根据描述符类型确定参数列表
    // 如果是变量描述符（属性），则没有参数
    // 否则从反射类型中提取参数类型
    val parameters = if (descriptor is VariableDescriptor) {
        emptyList()
    } else {
        reflectionType.getValueParameterTypesFromCallableReflectionType(
            !hasBoundExtensionReceiver  // 如果没有绑定扩展接收者，则第一个参数可能是接收者
        ).map { it.type }
    }

    // 返回构建的 FlatSignature 实例
    return FlatSignature(
        origin,
        descriptor.typeParameters,
        parameters,
        hasVarargs = descriptor.valueParameters.any { it.varargElementType != null },
        numDefaults = numDefaults,
        isSyntheticMember = descriptor is SyntheticMemberDescriptor<*>
    )
}


/**
 * 重载能力特异性比较回调
 *
 * 这是用于重载解析的默认回调实现。
 * 它采用保守策略：如果两个类型之间不存在子类型关系，
 * 就认为它们的特异性无法比较。
 */
object OverloadabilitySpecificityCallbacks : SpecificityComparisonCallbacks {
    /**
     * 对于重载解析，如果类型之间不是子类型关系，
     * 就认为 specific 比 general 更不特异
     *
     * @param specific 更特异的类型候选
     * @param general 更一般的类型候选
     * @return 总是返回 false，表示非子类型关系时认为更不特异
     */
    override fun isNonSubtypeNotLessSpecific(specific: CangJieTypeMarker, general: CangJieTypeMarker): Boolean =
        false
}