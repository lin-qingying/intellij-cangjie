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

package org.cangnova.cangjie.resolve.calls.results

import org.cangnova.cangjie.descriptors.CallableDescriptor
import org.cangnova.cangjie.descriptors.VariableDescriptor
import org.cangnova.cangjie.descriptors.synthetic.SyntheticMemberDescriptor
import org.cangnova.cangjie.resolve.calls.components.hasDefaultValue
import org.cangnova.cangjie.types.UnwrappedType
import org.cangnova.cangjie.types.getValueParameterTypesFromCallableReflectionType
import org.cangnova.cangjie.types.model.CangJieTypeMarker
import org.cangnova.cangjie.types.model.TypeParameterMarker

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
 * ## 使用说明
 *
 * FlatSignature 本身只是数据结构，特异性比较逻辑由 ECS (Existential Constraint System) 提供：
 *
 * ```kotlin
 * val ecs = ExistentialConstraintSystem.create(builtIns, specificityComparator)
 * val isNotLessSpecific = ecs.isNotLessSpecific(signatureA, signatureB)
 * ```
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
