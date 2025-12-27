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
import org.cangnova.cangjie.types.AbstractTypeChecker
import org.cangnova.cangjie.types.UnwrappedType
import org.cangnova.cangjie.types.getValueParameterTypesFromCallableReflectionType
import org.cangnova.cangjie.types.model.*

interface SpecificityComparisonCallbacks {
    fun isNonSubtypeNotLessSpecific(specific: CangJieTypeMarker, general: CangJieTypeMarker): Boolean
}

class TypeWithConversion(val resultType: CangJieTypeMarker?, val originalTypeIfWasConverted: CangJieTypeMarker? = null)

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
        valueParameterTypes =   parameterTypes,
        hasVarargs = descriptor.valueParameters.any { it.varargElementType != null },
        numDefaults = numDefaults,
        isSyntheticMember = descriptor is SyntheticMemberDescriptor<*>
    )
}

class FlatSignature<out T>(
    val origin: T,
    val typeParameters: Collection<TypeParameterMarker>,
    val hasVarargs: Boolean,

    val numDefaults: Int,

    val isSyntheticMember: Boolean,
    val valueParameterTypes: List<TypeWithConversion?>,
) {
    val isGeneric = typeParameters.isNotEmpty()

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

interface SimpleConstraintSystem {
    fun registerTypeVariables(typeParameters: Collection<TypeParameterMarker>): TypeSubstitutorMarker
    fun addSubtypeConstraint(subType: CangJieTypeMarker, superType: CangJieTypeMarker)
    fun hasContradiction(): Boolean

    // todo hack for migration
    val captureFromArgument get() = false

    val context: TypeSystemInferenceExtensionContext
}

fun <D : CallableDescriptor> FlatSignature.Companion.createForPossiblyShadowedExtension(descriptor: D): FlatSignature<D> =
    FlatSignature(
        descriptor,
        descriptor.typeParameters,
        valueParameterTypes = descriptor.valueParameters.map { it.argumentValueType },
        hasVarargs = descriptor.valueParameters.any { it.varargElementType != null },
        numDefaults = descriptor.valueParameters.count { it.hasDefaultValue() },
        isSyntheticMember = descriptor is SyntheticMemberDescriptor<*>
    )

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


fun <T> SimpleConstraintSystem.isSignatureNotLessSpecific(
    specific: FlatSignature<T>,
    general: FlatSignature<T>,
    callbacks: SpecificityComparisonCallbacks,
    specificityComparator: TypeSpecificityComparator,
    useOriginalSamTypes: Boolean = false
): Boolean {
    if (specific.valueParameterTypes.size   != general.valueParameterTypes.size  )
        return false

    if (!isValueParameterTypeNotLessSpecific(specific, general, callbacks, specificityComparator) { it?.resultType }) {
        return false
    }

    if (useOriginalSamTypes && !isValueParameterTypeNotLessSpecific(
            specific, general, callbacks, specificityComparator
        ) { it?.originalTypeIfWasConverted }
    ) {
        return false
    }

    return !hasContradiction()
}

private fun <T> SimpleConstraintSystem.isValueParameterTypeNotLessSpecific(
    specific: FlatSignature<T>,
    general: FlatSignature<T>,
    callbacks: SpecificityComparisonCallbacks,
    specificityComparator: TypeSpecificityComparator,
    typeKindSelector: (TypeWithConversion?) -> CangJieTypeMarker?
): Boolean {
    val typeParameters = general.typeParameters
    val typeSubstitutor = registerTypeVariables(typeParameters)


    var specificValueParameterTypes = specific.valueParameterTypes
    var generalValueParameterTypes = general.valueParameterTypes


    for (index in specificValueParameterTypes.indices) {
        val specificType = typeKindSelector(specificValueParameterTypes[index]) ?: continue
        val generalType = typeKindSelector(generalValueParameterTypes[index]) ?: continue

        if (specificityComparator.isDefinitelyLessSpecific(specificType, generalType)) {
            return false
        }

        if (typeParameters.isEmpty() || !generalType.dependsOnTypeParameters(context, typeParameters)) {
            if (!AbstractTypeChecker.isSubtypeOf(context, specificType, generalType)) {
                if (!callbacks.isNonSubtypeNotLessSpecific(specificType, generalType)) {
                    return false
                }
            }
        } else {
            val substitutedGeneralType = typeSubstitutor.safeSubstitute(context, generalType)

            /**
             * Example:
             * fun <X> Array<out X>.sort(): Unit {}
             * fun <Y: Comparable<Y>> Array<out Y>.sort(): Unit {}
             * Here, when we try solve this CS(Y is variables) then Array<out X> <: Array<out Y> and this system impossible to solve,
             * so we capture types from receiver and value parameters.
             */
            val specificCapturedType = AbstractTypeChecker.prepareType(context, specificType)
                .let { if (captureFromArgument) context.captureFromExpression(it) ?: it else it }
            addSubtypeConstraint(specificCapturedType, substitutedGeneralType)
        }
    }

    return true
}

/**
 * 从反射类型信息创建一个 [FlatSignature] 实例。
 * 此函数专为处理可调用引用而设计，处理反射类型中的接收者类型、上下文接收者类型、参数类型等信息。
 *
 * @param origin 原始对象，可以是任何类型的对象。
 * @param descriptor 可调用描述符，描述了方法或属性的元数据。
 * @param numDefaults 默认参数的数量。
 * @param hasBoundExtensionReceiver 是否有绑定的扩展接收者。
 * @param reflectionType 反射类型，用于获取可调用引用的类型信息。
 * @return 创建的 [FlatSignature] 实例。
 */
fun <T> FlatSignature.Companion.createFromReflectionType(
    origin: T,
    descriptor: CallableDescriptor,
    numDefaults: Int,
    // 可调用引用的反射类型不包含接收者类型
    hasBoundExtensionReceiver: Boolean,
    reflectionType: UnwrappedType
): FlatSignature<T> {


    // 根据描述符类型确定参数列表
    val parameters = if (descriptor is VariableDescriptor) {
        emptyList()
    } else {
        reflectionType.getValueParameterTypesFromCallableReflectionType(
            !hasBoundExtensionReceiver
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


object OverloadabilitySpecificityCallbacks : SpecificityComparisonCallbacks {
    override fun isNonSubtypeNotLessSpecific(specific: CangJieTypeMarker, general: CangJieTypeMarker): Boolean =
        false
}
