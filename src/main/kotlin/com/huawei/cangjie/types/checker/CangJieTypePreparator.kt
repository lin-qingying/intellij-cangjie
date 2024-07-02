package com.huawei.cangjie.types.checker

import com.huawei.cangjie.container.DefaultImplementation
import com.huawei.cangjie.types.*
import com.huawei.cangjie.types.model.CangJieTypeMarker

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
        // Type itself can be just SimpleTypeImpl, not CapturedType. see KT-16147
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
//                    lowerType, type.attributes, type.isMarkedNullable
//                )
//            }
//
//            is IntegerValueTypeConstructor -> {
//                val newConstructor =
//                    IntersectionTypeConstructor(constructor.supertypes.map { TypeUtils.makeNullableAsSpecified(it, type.isMarkedNullable) })
//                return CangJieFactory.simpleTypeWithNonTrivialMemberScope(
//                    type.attributes,
//                    newConstructor,
//                    listOf(),
//                    false,
//                    type.memberScope
//                )
//            }
//
//            is IntersectionTypeConstructor -> if (type.isMarkedNullable) {
//                val newConstructor = constructor.transformComponents(transform = { it.makeNullable() }) ?: constructor
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
            // Type itself can be just SimpleTypeImpl, not CapturedType. see KT-16147
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
//                    lowerType, type.attributes, type.isMarkedNullable
//                )
//            }
//
//            is IntegerValueTypeConstructor -> {
//                val newConstructor =
//                    IntersectionTypeConstructor(constructor.supertypes.map {
//                        TypeUtils.makeNullableAsSpecified(
//                            it,
//                            type.isMarkedNullable
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
//            is IntersectionTypeConstructor -> if (type.isMarkedNullable) {
//                val newConstructor = constructor.transformComponents(transform = { it.makeNullable() }) ?: constructor
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
