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

import org.cangnova.cangjie.descriptors.TypeParameterDescriptor
import org.cangnova.cangjie.descriptors.annotations.Annotations
import org.cangnova.cangjie.descriptors.annotations.FilteredAnnotations

/**
 * 类型替换抽象类
 *
 * 定义了类型替换的基本接口和功能，用于在泛型类型系统中进行类型参数的替换操作。
 * 类型替换是类型系统中的核心机制，用于将泛型类型的类型参数替换为具体的类型。
 */
@Deprecated("使用SubstitutorFunction")
abstract class TypeSubstitution{
    companion object {
        /**
         * 空类型替换实例
         * 表示不进行任何类型替换的默认实例
         */
        @JvmField
        val EMPTY: TypeSubstitution = object : TypeSubstitution() {
            override fun get(key: CangJieType): Nothing? = null
            override fun isEmpty() = true
            override fun toString() = "Empty TypeSubstitution"
        }
    }

    /**
     * 预处理顶级类型
     * 可以用于对顶级类型执行初步的操作
     *
     * @param topLevelType 顶级类型
     * @param position 类型的变异位置
     * @return 处理后的类型
     */
    open fun prepareTopLevelType(topLevelType: CangJieType): CangJieType = topLevelType

    /**
     * 过滤注解
     * 对类型上的注解进行过滤处理
     *
     * @param annotations 要过滤的注解
     * @return 过滤后的注解
     */
    open fun filterAnnotations(annotations: Annotations) = annotations

    /**
     * 根据给定的类型键获取对应的类型投影
     *
     * @param key 要替换的类型键
     * @return 对应的类型投影，如果没有替换则返回null
     */
    abstract operator fun get(key: CangJieType): TypeArgument?

    /**
     * 构建类型替换器
     *
     * @return 基于当前替换的类型替换器
     */
    fun buildSubstitutor(): DefaultTypeSubstitutor = DefaultTypeSubstitutor.create(this)

    /**
     * 创建非近似的替换
     * 返回一个不进行捕获类型近似的新替换实例
     *
     * @return 非近似的类型替换
     */
    fun replaceWithNonApproximating() = object : TypeSubstitution() {
        override fun get(key: CangJieType) = this@TypeSubstitution[key]
        override fun approximateCapturedTypes() = false
        override fun approximateContravariantCapturedTypes() = false
        override fun filterAnnotations(annotations: Annotations) = this@TypeSubstitution.filterAnnotations(annotations)
        override fun prepareTopLevelType(topLevelType: CangJieType ) =
            this@TypeSubstitution.prepareTopLevelType(topLevelType)

        override fun isEmpty() = this@TypeSubstitution.isEmpty()
    }

    /**
     * 检查替换是否为空
     *
     * @return 如果没有任何类型替换则返回true
     */
    open fun isEmpty(): Boolean = false

    /**
     * 是否对捕获类型进行近似
     *
     * @return 如果需要对捕获类型进行近似则返回true
     */
    open fun approximateCapturedTypes(): Boolean = false

    /**
     * 是否对逆变捕获类型进行近似
     *
     * @deprecated 仓颉语言不支持协变/逆变，此方法已废弃，始终返回 false
     * @return 始终返回 false
     */
    @Deprecated(
        "Cangjie language does not support covariance/contravariance",
        ReplaceWith("false")
    )
    open fun approximateContravariantCapturedTypes(): Boolean = false
}


abstract class TypeConstructorSubstitution : TypeSubstitution() {
    override fun get(key: CangJieType) = get(key.constructor)

    abstract fun get(key: TypeConstructor): TypeArgument?

    companion object {
        @JvmStatic
        @JvmOverloads
        fun createByConstructorsMap(
            map: Map<TypeConstructor, TypeArgument>,
            approximateCapturedTypes: Boolean = false
        ): TypeConstructorSubstitution =
            object : TypeConstructorSubstitution() {
                override fun get(key: TypeConstructor) = map[key]
                override fun isEmpty() = map.isEmpty()
                override fun approximateCapturedTypes() = approximateCapturedTypes
            }

        @JvmStatic
        fun createByParametersMap(map: Map<TypeParameterDescriptor, TypeArgument>): TypeConstructorSubstitution =
            object : TypeConstructorSubstitution() {
                override fun get(key: TypeConstructor) = map[key.declarationDescriptor]
                override fun isEmpty() = map.isEmpty()
            }

        @JvmStatic
        fun create(CangJieType: CangJieType) = create(CangJieType.constructor, CangJieType.arguments)

        @JvmStatic
        fun create(typeConstructor: TypeConstructor, arguments: List<TypeArgument>): TypeSubstitution {
            val parameters = typeConstructor.parameters

            if (parameters.lastOrNull()?.isCapturedFromOuterDeclaration == true) {
                return createByConstructorsMap(typeConstructor.parameters.map { it.typeConstructor }.zip(arguments).toMap())
            }

            return IndexedParametersSubstitution(parameters, arguments)
        }
    }
}

class IndexedParametersSubstitution(
    val parameters: Array<TypeParameterDescriptor>,
    val arguments: Array<TypeArgument>,
    private val approximateContravariantCapturedTypes: Boolean = false
) : TypeSubstitution() {
    init {
        assert(parameters.size <= arguments.size) {
            "Number of arguments should not be less than number of parameters, but: parameters=${parameters.size}, args=${arguments.size}"
        }
    }

    constructor(
        parameters: List<TypeParameterDescriptor>,
        argumentsList: List<TypeArgument>
    ) : this(parameters.toTypedArray(), argumentsList.toTypedArray())

    override fun isEmpty(): Boolean = arguments.isEmpty()

    override fun approximateContravariantCapturedTypes() = approximateContravariantCapturedTypes

    override fun get(key: CangJieType): TypeArgument? {
        val parameter = key.constructor.declarationDescriptor as? TypeParameterDescriptor ?: return null
        val index = parameter.index

        if (index < parameters.size && parameters[index].typeConstructor == parameter.typeConstructor) {
            return arguments[index]
        }

        return null
    }
}
@JvmOverloads
fun SimpleType.replace(
    newArguments: List<TypeArgument> = arguments,
    newAttributes: TypeAttributes = attributes
): SimpleType {
    if (newArguments.isEmpty() && newAttributes === attributes) return this

    if (newArguments.isEmpty()) {
        return replaceAttributes(newAttributes)
    }

//    if (this is ErrorType) {
//        return replaceArguments(newArguments)
//    }

    return CangJieTypeFactory.simpleType(
        newAttributes,
        constructor,
        newArguments,
        isOption
    )
}

open class DelegatedTypeSubstitution(val substitution: TypeSubstitution) : TypeSubstitution() {
    override fun get(key: CangJieType) = substitution[key]
//    override fun prepareTopLevelType(topLevelType: CangJieType, position: Variance) =
//        substitution.prepareTopLevelType(topLevelType, position)

    override fun isEmpty() = substitution.isEmpty()

    override fun approximateCapturedTypes() = substitution.approximateCapturedTypes()
    override fun approximateContravariantCapturedTypes() = substitution.approximateContravariantCapturedTypes()

//    override fun filterAnnotations(annotations: Annotations) = substitution.filterAnnotations(annotations)
}
@JvmOverloads
fun CangJieType.replace(
    newArguments: List<TypeArgument> = arguments,
    newAnnotations: Annotations = annotations,
    newArgumentsForUpperBound: List<TypeArgument> = newArguments
): CangJieType {
    if ((newArguments.isEmpty() || newArguments === arguments) && newAnnotations === annotations) return this

    val newAttributes = attributes.replaceAnnotations(
        // Specially handle FilteredAnnotations here due to FilteredAnnotations.isEmpty()
        if (newAnnotations is FilteredAnnotations && newAnnotations.isEmpty()) Annotations.EMPTY else newAnnotations
    )

    return when (val unwrapped = unwrap()) {
        is FlexibleType -> CangJieTypeFactory.flexibleType(
            unwrapped.lowerBound.replace(newArguments, newAttributes),
            unwrapped.upperBound.replace(newArgumentsForUpperBound, newAttributes)
        )
        is SimpleType -> unwrapped.replace(newArguments, newAttributes)

    }
}

class SubstitutionWithCapturedTypeApproximation(substitution: TypeSubstitution) : DelegatedTypeSubstitution(substitution) {
    override fun approximateCapturedTypes() = true
}
