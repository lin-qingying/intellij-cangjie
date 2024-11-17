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

package com.linqingying.cangjie.types

import com.linqingying.cangjie.descriptors.TypeParameterDescriptor
import com.linqingying.cangjie.descriptors.impl.TypeParameterDescriptorImpl
import com.linqingying.cangjie.resolve.scopes.MemberScope
import com.linqingying.cangjie.storage.StorageManager
import com.linqingying.cangjie.types.checker.CangJieTypeRefiner
import com.linqingying.cangjie.types.checker.NewCapturedType
import com.linqingying.cangjie.types.checker.NewTypeVariableConstructor
import com.linqingying.cangjie.types.checker.NullabilityChecker

import com.linqingying.cangjie.types.model.DefinitelyNotNullTypeMarker
import com.linqingying.cangjie.types.util.TypeUtils

fun SimpleType.withAbbreviation(abbreviatedType: SimpleType): SimpleType {
    if (isError) return this
    return AbbreviatedType(this, abbreviatedType)
}
val CangJieType.isDefinitelyNotNullType: Boolean
    get() = unwrap() is DefinitelyNotNullType
fun CangJieType.getAbbreviation(): SimpleType? = getAbbreviatedType()?.abbreviation
fun CangJieType.getAbbreviatedType(): AbbreviatedType? = unwrap() as? AbbreviatedType

class AbbreviatedType(override val delegate: SimpleType, val abbreviation: SimpleType) : DelegatingSimpleType() {
    val expandedType: SimpleType get() = delegate

    override fun replaceAttributes(newAttributes: TypeAttributes): SimpleType =
        AbbreviatedType(delegate.replaceAttributes(newAttributes), abbreviation)

    override fun makeOptionalAsSpecified(newNullability: Boolean) =
        AbbreviatedType(
            delegate.makeOptionalAsSpecified(newNullability),
            abbreviation.makeOptionalAsSpecified(newNullability)
        )

    @TypeRefinement
    override fun replaceDelegate(delegate: SimpleType) = AbbreviatedType(delegate, abbreviation)

//    @TypeRefinement
//    @OptIn(TypeRefinement::class)
//    override fun refine(cangjieTypeRefiner: CangJieTypeRefiner): AbbreviatedType =
//        AbbreviatedType(
//            cangjieTypeRefiner.refineType(delegate) as SimpleType,
//            cangjieTypeRefiner.refineType(abbreviation) as SimpleType
//        )
}

abstract class DelegatingSimpleType : SimpleType() {
    protected abstract val delegate: SimpleType

    override val constructor: TypeConstructor get() = delegate.constructor
    override val arguments: List<TypeProjection> get() = delegate.arguments
    override val isMarkedOption: Boolean get() = delegate.isMarkedOption
    override val memberScope: MemberScope get() = delegate.memberScope
    override val attributes: TypeAttributes get() = delegate.attributes

    @TypeRefinement
    abstract fun replaceDelegate(delegate: SimpleType): DelegatingSimpleType

    @TypeRefinement
    override fun refine(cangjieTypeRefiner: CangJieTypeRefiner): SimpleType =
        replaceDelegate(cangjieTypeRefiner.refineType(delegate) as SimpleType)
}

abstract class WrappedType : CangJieType() {
    open fun isComputed(): Boolean = true
    protected abstract val delegate: CangJieType

    override val constructor: TypeConstructor get() = delegate.constructor
    override val arguments: List<TypeProjection> get() = delegate.arguments
    override val isMarkedOption: Boolean get() = delegate.isMarkedOption
    override val memberScope: MemberScope get() = delegate.memberScope
    override val attributes: TypeAttributes get() = delegate.attributes

    final override fun unwrap(): UnwrappedType {
        var result = delegate
        while (result is WrappedType) {
            result = result.delegate
        }
        return result as UnwrappedType
    }

    override fun toString(): String {
        return if (isComputed()) {
            delegate.toString()
        } else {
            "<Not computed yet>"
        }
    }
}

class LazyWrappedType(
    private val storageManager: StorageManager,
    private val computation: () -> CangJieType
) : WrappedType() {
    private val lazyValue = storageManager.createLazyValue(computation)

    override val delegate: CangJieType get() = lazyValue()

    override fun isComputed(): Boolean = lazyValue.isComputed()

    //
    @TypeRefinement
    @OptIn(TypeRefinement::class)
    override fun refine(cangjieTypeRefiner: CangJieTypeRefiner) = LazyWrappedType(storageManager) {
        cangjieTypeRefiner.refineType(computation())
    }
}

class DefinitelyNotNullType private constructor(
    val original: SimpleType,
    private val useCorrectedNullabilityForTypeParameters: Boolean
) : DelegatingSimpleType(), CustomTypeParameter,
    DefinitelyNotNullTypeMarker {

    companion object {
        // Having `@JvmOverloads` just to make sure we don't break ABI compatibility
        @JvmOverloads
        fun makeDefinitelyNotNull(
            type: UnwrappedType,
            useCorrectedNullabilityForTypeParameters: Boolean = false,
            // Should be used when we are sure that original type is nullable, i.e. makesSenseToBeDefinitelyNotNull would return true,
            // but we can't actually call it because otherwise we would fail with StackOverFlow because supertypes are being computed recursively
            // and there's no easy way to prevent recursion.
            // NB: makesSenseToBeDefinitelyNotNull is mostly needed as an optimization because nothing really bad would happen even if we
            // create DNN for a type parameter with non-nullable bound.
            avoidCheckingActualTypeNullability: Boolean = false,
        ): DefinitelyNotNullType? {
            return when {
                type is DefinitelyNotNullType -> type

                avoidCheckingActualTypeNullability || makesSenseToBeDefinitelyNotNull(
                    type,
                    useCorrectedNullabilityForTypeParameters
                ) -> {
                    if (type is FlexibleType) {
                        assert(type.lowerBound.constructor == type.upperBound.constructor) {
                            "DefinitelyNotNullType for flexible type ($type) can be created only from type variable with the same constructor for bounds"
                        }
                    }


                    DefinitelyNotNullType(
                        type.lowerIfFlexible().makeOptionalAsSpecified(false),
                        useCorrectedNullabilityForTypeParameters
                    )
                }

                else -> null
            }
        }

        private fun makesSenseToBeDefinitelyNotNull(
            type: UnwrappedType,
            useCorrectedNullabilityForFlexibleTypeParameters: Boolean
        ): Boolean {
            if (!type.canHaveUndefinedNullability()) return false

            if (type is StubTypeForBuilderInference) return TypeUtils.isNullableType(type)

            if ((type.constructor.declarationDescriptor as? TypeParameterDescriptorImpl)?.isInitialized == false) {
                return true
            }


            if (useCorrectedNullabilityForFlexibleTypeParameters && type.constructor.declarationDescriptor is TypeParameterDescriptor) {
                // Effectively checks if the type is flexible or has nullable bound
                return TypeUtils.isNullableType(type)
            }

            // Actually, this code should work for type parameters as well, but it breaks some cases

            return !NullabilityChecker.isSubtypeOfAny(type)
        }

        private fun UnwrappedType.canHaveUndefinedNullability(): Boolean =
            constructor is NewTypeVariableConstructor
                    || constructor.declarationDescriptor is TypeParameterDescriptor
                    || this is NewCapturedType
                    || this is StubTypeForBuilderInference

    }

    override val delegate: SimpleType
        get() = original

    override val isMarkedOption: Boolean
        get() = false

    override val isTypeParameter: Boolean
        get() = delegate.constructor is NewTypeVariableConstructor ||
                delegate.constructor.declarationDescriptor is TypeParameterDescriptor

    override fun substitutionResult(replacement: CangJieType): CangJieType =
        replacement.unwrap().makeDefinitelyNotNullOrNotNull(useCorrectedNullabilityForTypeParameters)

    override fun replaceAttributes(newAttributes: TypeAttributes): SimpleType =
        DefinitelyNotNullType(delegate.replaceAttributes(newAttributes), useCorrectedNullabilityForTypeParameters)

    override fun makeOptionalAsSpecified(newNullability: Boolean): SimpleType =
        if (newNullability) delegate.makeOptionalAsSpecified(newNullability) else this

    override fun toString(): String = "$delegate & Any"

    @TypeRefinement
    override fun replaceDelegate(delegate: SimpleType) =
        DefinitelyNotNullType(delegate, useCorrectedNullabilityForTypeParameters)
}

fun SimpleType.makeSimpleTypeDefinitelyNotNullOrNotNull(useCorrectedNullabilityForTypeParameters: Boolean = false): SimpleType =
    DefinitelyNotNullType.makeDefinitelyNotNull(this, useCorrectedNullabilityForTypeParameters)
        ?: makeIntersectionTypeDefinitelyNotNullOrNotNull()
        ?: makeOptionalAsSpecified(false)

fun NewCapturedType.withNotNullProjection() =
    NewCapturedType(captureStatus, constructor, lowerType, attributes, isMarkedOption, isProjectionNotNull = true)

fun UnwrappedType.makeDefinitelyNotNullOrNotNull(useCorrectedNullabilityForTypeParameters: Boolean = false): UnwrappedType =
    DefinitelyNotNullType.makeDefinitelyNotNull(this, useCorrectedNullabilityForTypeParameters)
        ?: makeIntersectionTypeDefinitelyNotNullOrNotNull()
        ?: makeOptionalAsSpecified(false)

private fun IntersectionTypeConstructor.makeDefinitelyNotNullOrNotNull(): IntersectionTypeConstructor? {
    return transformComponents({ TypeUtils.isNullableType(it) }, { it.unwrap().makeDefinitelyNotNullOrNotNull() })
}

private fun CangJieType.makeIntersectionTypeDefinitelyNotNullOrNotNull(): SimpleType? {
    val typeConstructor = constructor as? IntersectionTypeConstructor ?: return null
    val definitelyNotNullConstructor = typeConstructor.makeDefinitelyNotNullOrNotNull() ?: return null

    return definitelyNotNullConstructor.createType()
}

