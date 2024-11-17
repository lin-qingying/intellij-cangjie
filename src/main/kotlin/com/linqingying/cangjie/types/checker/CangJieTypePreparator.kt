/*
 * Copyright 2024 LinQingYing. and contributors.
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

package com.linqingying.cangjie.types.checker

import com.linqingying.cangjie.container.DefaultImplementation
import com.linqingying.cangjie.types.*
import com.linqingying.cangjie.types.model.CangJieTypeMarker

abstract class AbstractTypePreparator {
    abstract fun prepareType(type: CangJieTypeMarker): CangJieTypeMarker

    object Default : AbstractTypePreparator() {
        override fun prepareType(type: CangJieTypeMarker): CangJieTypeMarker {
            return type
        }
    }
}

@DefaultImplementation(impl = CangJiePreparator.Default::class)
abstract class CangJiePreparator : AbstractTypePreparator() {
    private fun transformToNewType(type: SimpleType): SimpleType {
//        when (val constructor = type.constructor) {
        // Type itself can be just SimpleTypeImpl, not CapturedType.
//            is CapturedTypeConstructorImpl -> {
//                val lowerType = constructor.projection.takeIf { it.projectionKind == Variance.IN_VARIANCE }?.type?.unwrap()
//
//                // it is incorrect calculate this type directly because of recursive star projections
//                if (constructor.newTypeConstructor == null) {
//                    constructor.newTypeConstructor =
//                        NewCapturedTypeConstructor(constructor.projection, constructor.supertypes.map { it.unwrap() })
//                }
//                return NewCapturedType(
//                    CaptureStatus.FOR_SUBTYPING, constructor.newTypeConstructor!!,
//                    lowerType, type.attributes, type.isMarkedOption
//                )
//            }
//
//            is IntegerValueTypeConstructor -> {
//                val newConstructor =
//                    IntersectionTypeConstructor(constructor.supertypes.map { TypeUtils.makeOptionalAsSpecified(it, type.isMarkedOption) })
//                return CangJieFactory.simpleTypeWithNonTrivialMemberScope(
//                    type.attributes,
//                    newConstructor,
//                    listOf(),
//                    false,
//                    type.memberScope
//                )
//            }
//
//            is IntersectionTypeConstructor -> if (type.isMarkedOption) {
//                val newConstructor = constructor.transformComponents(transform = { it.makeOptional() }) ?: constructor
//                return newConstructor.createType()
//
//            }
//        }

        return type
    }

    override fun prepareType(type: CangJieTypeMarker): UnwrappedType {
        require(type is CangJieType)
        val unwrappedType = type.unwrap()
        return when (unwrappedType) {
            is SimpleType -> transformToNewType(unwrappedType)
            is FlexibleType -> {
                val newLower = transformToNewType(unwrappedType.lowerBound)
                val newUpper = transformToNewType(unwrappedType.upperBound)
                if (newLower !== unwrappedType.lowerBound || newUpper !== unwrappedType.upperBound) {
                    CangJieTypeFactory.flexibleType(newLower, newUpper)
                } else {
                    unwrappedType
                }
            }
        }.inheritEnhancement(unwrappedType, ::prepareType)
    }

    object Default : CangJiePreparator()
}

@DefaultImplementation(impl = CangJieTypePreparator.Default::class)
abstract class CangJieTypePreparator : AbstractTypePreparator() {
    private fun transformToNewType(type: SimpleType): SimpleType {
//        when (val constructor = type.constructor) {
            // Type itself can be just SimpleTypeImpl, not CapturedType.
//            is CapturedTypeConstructorImpl -> {
//                val lowerType =
//                    constructor.projection.takeIf { it.projectionKind == Variance.IN_VARIANCE }?.type?.unwrap()
//
//                // it is incorrect calculate this type directly because of recursive star projections
//                if (constructor.newTypeConstructor == null) {
//                    constructor.newTypeConstructor =
//                        NewCapturedTypeConstructor(constructor.projection, constructor.supertypes.map { it.unwrap() })
//                }
//                return NewCapturedType(
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
//                val newConstructor = constructor.transformComponents(transform = { it.makeOptional() }) ?: constructor
//                return newConstructor.createType()
//
//            }
//        }

        return type
    }

    override fun prepareType(type: CangJieTypeMarker): UnwrappedType {
        require(type is CangJieType)
        val unwrappedType = type.unwrap()
        return when (unwrappedType) {
            is SimpleType -> transformToNewType(unwrappedType)
            is FlexibleType -> {
                val newLower = transformToNewType(unwrappedType.lowerBound)
                val newUpper = transformToNewType(unwrappedType.upperBound)
                if (newLower !== unwrappedType.lowerBound || newUpper !== unwrappedType.upperBound) {
                    CangJieTypeFactory.flexibleType(newLower, newUpper)
                } else {
                    unwrappedType
                }
            }
        }.inheritEnhancement(unwrappedType, ::prepareType)
    }

    object Default : CangJieTypePreparator()
}
