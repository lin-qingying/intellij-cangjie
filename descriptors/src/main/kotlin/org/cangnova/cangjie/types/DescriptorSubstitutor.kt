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

import org.cangnova.cangjie.descriptors.DeclarationDescriptor
import org.cangnova.cangjie.descriptors.SourceElement
import org.cangnova.cangjie.descriptors.TypeParameterDescriptor
import org.cangnova.cangjie.descriptors.impl.TypeParameterDescriptorImpl
import org.cangnova.cangjie.descriptors.impl.TypeParameterDescriptorImpl.Companion.createForFurtherModification


/**
 * 描述符替换器工具
 *
 * 提供描述符相关的类型替换功能，主要用于泛型类型参数的替换。
 *
 * ## 主要功能
 *
 * ### substituteTypeParameters
 * 替换类型参数列表，创建新的类型参数并建立替换映射。
 *
 * 这个方法在泛型类实例化时被调用，用于：
 * 1. 创建新的类型参数描述符
 * 2. 建立原始类型参数到新类型参数的映射
 * 3. 替换类型参数的上界
 *
 * ## 使用场景
 *
 * ### LazySubstitutingClassDescriptor
 * ```kotlin
 * // 原始类: class Box<T>
 * // 实例化: Box<Int>
 * val substitutor = DescriptorSubstitutor.substituteTypeParameters(
 *     originalTypeParameters = [T],
 *     originalSubstitutor = ComposableTypeSubstitutor { T -> Int },
 *     newContainingDeclaration = boxIntDescriptor,
 *     result = mutableListOf()
 * )
 * // result 包含新的类型参数描述符
 * // substitutor 可用于替换成员类型
 * ```
 */
object DescriptorSubstitutor {
    /**
     * 替换类型参数（简化版本）
     *
     * 如果替换失败则抛出 AssertionError
     *
     * @param typeParameters 原始类型参数列表
     * @param originalSubstitutor 原始替换器
     * @param newContainingDeclaration 新的包含声明
     * @param result 输出参数，用于存储新创建的类型参数
     * @return 组合后的替换器
     */
    fun substituteTypeParameters(
        typeParameters: List<TypeParameterDescriptor>,
        originalSubstitutor: ComposableTypeSubstitutor,
        newContainingDeclaration: DeclarationDescriptor,
        result: MutableList<TypeParameterDescriptor>
    ): ComposableTypeSubstitutor {
        val substitutor = substituteTypeParameters(
            typeParameters, originalSubstitutor, newContainingDeclaration, result, null
        )
        if (substitutor == null) throw AssertionError("Substitution failed")
        return substitutor
    }

    /**
     * 替换类型参数（完整版本）
     *
     * 创建新的类型参数描述符，并替换它们的上界。
     *
     * @param typeParameters 原始类型参数列表
     * @param originalSubstitutor 原始替换器
     * @param newContainingDeclaration 新的包含声明
     * @param result 输出参数，用于存储新创建的类型参数
     * @param wereChanges 输出参数，如果发生了替换则设置为 true
     * @return 组合后的替换器，如果替换失败则返回 null
     */
    fun substituteTypeParameters(
        typeParameters: List<TypeParameterDescriptor>,
        originalSubstitutor: ComposableTypeSubstitutor,
        newContainingDeclaration: DeclarationDescriptor,
        result: MutableList<TypeParameterDescriptor>,
        wereChanges: BooleanArray?
    ): ComposableTypeSubstitutor? {
        // 第一阶段: 创建新的类型参数描述符
        val mutableSubstitutionMap: MutableMap<TypeConstructor, UnwrappedType> = HashMap()
        val substitutedMap: MutableMap<TypeParameterDescriptor?, TypeParameterDescriptorImpl> = HashMap()

        var index = 0
        for (descriptor in typeParameters) {
            val substituted = createForFurtherModification(
                newContainingDeclaration,
                descriptor.annotations,
                descriptor.name,
                index++,
                SourceElement.NO_SOURCE,
                descriptor.storageManager
            )

            mutableSubstitutionMap[descriptor.typeConstructor] = substituted.defaultType.unwrap()
            substitutedMap[descriptor] = substituted
            result.add(substituted)
        }

        // 第二阶段: 创建组合替换器
        // 新的映射 (原类型参数 -> 新类型参数)
        val newParamSubstitutor = ComposableTypeSubstitutor.create(mutableSubstitutionMap)

        // 组合原始替换器和新参数映射
        val substitutor = originalSubstitutor.compose(newParamSubstitutor)

        // 创建不进行近似的版本 (用于递归类型参数)
        val nonApproximatingSubstitutor = originalSubstitutor
            .withOptions { copy(approximateCapturedTypes = false) }
            .compose(newParamSubstitutor)

        // 第三阶段: 替换类型参数的上界
        for (descriptor in typeParameters) {
            val substituted: TypeParameterDescriptorImpl = substitutedMap[descriptor]!!
            for (upperBound in descriptor.upperBounds) {
                val upperBoundDeclaration = upperBound.constructor.declarationDescriptor

                // 如果上界是递归类型参数，使用普通替换器；否则使用非近似替换器
                val boundSubstitutor = if (upperBoundDeclaration is TypeParameterDescriptor &&
                    hasTypeParameterRecursiveBounds(upperBoundDeclaration)
                ) {
                    substitutor
                } else {
                    nonApproximatingSubstitutor
                }

                val substitutedBound = boundSubstitutor.safeSubstitute(upperBound.unwrap()).asFlexibleOrSimple()

                if (substitutedBound !== upperBound && wereChanges != null) {
                    wereChanges[0] = true
                }

                substituted.addUpperBound(substitutedBound)
            }
            substituted.setInitialized()
        }

        return substitutor
    }
}

/**
 * 辅助扩展函数: 将 UnwrappedType 转换为 CangJieType
 */
private fun UnwrappedType.asFlexibleOrSimple(): CangJieType {
    return when (this) {
        is SimpleType -> this
        is FlexibleType -> this
        else -> this as CangJieType
    }
}
