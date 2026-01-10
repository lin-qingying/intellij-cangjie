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

package org.cangnova.cangjie.types.checker

import org.cangnova.cangjie.container.DefaultImplementation
import org.cangnova.cangjie.types.*
import org.cangnova.cangjie.types.model.CangJieTypeMarker


/**
 * 仓颉类型准备器
 *
 * 在类型检查前对类型进行预处理和标准化。
 * 这是类型检查流程中的重要一环，确保所有类型都处于一致的状态。
 *
 * 主要处理：
 * 1. 捕获类型的转换（FOR_SUBTYPING 状态）
 * 2. 整数值类型到交集类型的转换
 * 3. 可选类型的交集类型处理
 * 4. 保留类型增强信息（nullability annotations 等）
 *
 * 注意：大部分转换逻辑当前已注释，可能在未来启用
 */
@DefaultImplementation(impl = CangJieTypePreparator.Default::class)
abstract class CangJieTypePreparator : AbstractTypePreparator() {
    /**
     * 将简单类型转换为新的类型表示
     *
     * 注释部分包含以下转换逻辑（当前未启用）：
     * - 捕获类型构造器转换为新的捕获类型
     * - 整数值类型构造器转换为交集类型
     * - 可选的交集类型的特殊处理
     */
    private fun transformToNewType(type: SimpleType): SimpleType {
//        when (val constructor = type.constructor) {
            // 类型本身可能只是 SimpleTypeImpl，而不是 CapturedType
//            is CapturedTypeConstructorImpl -> {
//                val lowerType =
//                    constructor.argument.takeIf { it.projectionKind == Variance.IN_VARIANCE }?.type?.unwrap()
//
//                // 由于递归星投影，直接计算此类型是不正确的
//                if (constructor.newTypeConstructor == null) {
//                    constructor.newTypeConstructor =
//                        CapturedTypeConstructor(constructor.argument, constructor.supertypes.map { it.unwrap() })
//                }
//                return CapturedType(
//                    CaptureStatus.FOR_SUBTYPING, constructor.newTypeConstructor!!,
//                    lowerType, type.attributes, type.isMarkedOption
//                )
//            }
//
//            is IntegerValueTypeConstructor -> {
//                val newConstructor =
//                    IntersectionTypeConstructor(constructor.supertypes.map {
//                        TypeUtils.makeOptionalAsSpecified(
//                            it,
//                            type.isMarkedOption
//                        )
//                    })
//                return CangJieTypeFactory.simpleTypeWithNonTrivialMemberScope(
//                    type.attributes,
//                    newConstructor,
//                    listOf(),
//                    false,
//                    type.memberScope
//                )
//            }
//
//            is IntersectionTypeConstructor -> if (type.isMarkedOption) {
//                val newConstructor = constructor.transformComponents(transform = { it.makeOption() }) ?: constructor
//                return newConstructor.createType()
//
//            }
//        }

        return type
    }

    /**
     * 准备类型用于类型检查
     *
     * 处理流程：
     * 1. 解包类型（移除别名等包装）
     * 2. 根据类型种类（简单类型或灵活类型）进行转换
     * 3. 对于灵活类型，分别转换上下界
     * 4. 继承原类型的增强信息（如 nullability annotations）
     *
     * @param type 要准备的类型
     * @return 准备好的未包装类型
     */
    override fun prepareType(type: CangJieTypeMarker): UnwrappedType {
        require(type is CangJieType)
        val unwrappedType = type.unwrap()
        return when (unwrappedType) {
            is SimpleType -> transformToNewType(unwrappedType)
            is FlexibleType -> {
                val newLower = transformToNewType(unwrappedType.lowerBound)
                val newUpper = transformToNewType(unwrappedType.upperBound)
                // 只有在转换后的类型与原类型不同时才创建新的灵活类型
                if (newLower !== unwrappedType.lowerBound || newUpper !== unwrappedType.upperBound) {
                    CangJieTypeFactory.flexibleType(newLower, newUpper)
                } else {
                    unwrappedType
                }
            }
        }.inheritEnhancement(unwrappedType, ::prepareType)
    }

    /** 默认的类型准备器实现 */
    object Default : CangJieTypePreparator()
}
