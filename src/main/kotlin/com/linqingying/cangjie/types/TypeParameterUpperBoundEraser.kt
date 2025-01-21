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

import com.linqingying.cangjie.builtins.CangJieBuiltIns
import com.linqingying.cangjie.descriptors.ClassDescriptor
import com.linqingying.cangjie.descriptors.ClassifierDescriptorWithTypeParameters
import com.linqingying.cangjie.descriptors.FunctionDescriptor
import com.linqingying.cangjie.descriptors.TypeParameterDescriptor
import com.linqingying.cangjie.storage.LockBasedStorageManager
import com.linqingying.cangjie.types.checker.intersectTypes
import com.linqingying.cangjie.types.error.ErrorModuleDescriptor.builtIns
import com.linqingying.cangjie.types.error.ErrorTypeKind
import com.linqingying.cangjie.types.util.*
import com.linqingying.cangjie.types.util.TypeUtils.makeProjection

open class ErasureProjectionComputer {
    open fun computeProjection(
        parameter: TypeParameterDescriptor,
        typeAttr: ErasureTypeAttributes,
        typeParameterUpperBoundEraser: TypeParameterUpperBoundEraser,
        erasedUpperBound: CangJieType = typeParameterUpperBoundEraser.getErasedUpperBound(parameter, typeAttr)
    ): TypeProjection = TypeProjectionImpl(Variance.INVARIANT, erasedUpperBound)
}
class TypeParameterErasureOptions(
    val leaveNonTypeParameterTypes: Boolean,
    val intersectUpperBounds: Boolean,
)

class TypeParameterUpperBoundEraser(
    val projectionComputer: ErasureProjectionComputer,
    val options: TypeParameterErasureOptions = TypeParameterErasureOptions(leaveNonTypeParameterTypes = false, intersectUpperBounds = false)
) {
    private val storage = LockBasedStorageManager("Type parameter upper bound erasure results")

    private val erroneousErasedBound by lazy {
        ErrorUtils.createErrorType(ErrorTypeKind.CANNOT_COMPUTE_ERASED_BOUND, this.toString())
    }

    private data class DataToEraseUpperBound(
        val typeParameter: TypeParameterDescriptor,
        val typeAttr: ErasureTypeAttributes
    ) {
        override fun equals(other: Any?): Boolean {
            if (other !is DataToEraseUpperBound) return false
            return other.typeParameter == this.typeParameter && other.typeAttr == this.typeAttr
        }

        override fun hashCode(): Int {
            var result = typeParameter.hashCode()
            result += 31 * result + typeAttr.hashCode()
            return result
        }
    }

    private val getErasedUpperBound = storage.createMemoizedFunction<DataToEraseUpperBound, CangJieType> {
        with(it) { getErasedUpperBoundInternal(typeParameter, typeAttr) }
    }

    fun getErasedUpperBound(
        typeParameter: TypeParameterDescriptor,
        typeAttr: ErasureTypeAttributes
    ): CangJieType = getErasedUpperBound(DataToEraseUpperBound(typeParameter, typeAttr))

    private fun getDefaultType(typeAttr: ErasureTypeAttributes) =
        typeAttr.defaultType?.replaceArgumentsWithProjections() ?: erroneousErasedBound

    // Definition:
    // ErasedUpperBound(T : G<t>) = G<*> // UpperBound(T) is a type G<t> with arguments
    // ErasedUpperBound(T : A) = A // UpperBound(T) is a type A without arguments
    // ErasedUpperBound(T : F) = UpperBound(F) // UB(T) is another type parameter F
    private fun getErasedUpperBoundInternal(
        // Calculation of `potentiallyRecursiveTypeParameter.upperBounds` may recursively depend on `this.getErasedUpperBound`
        // E.g. `class A<T extends A, F extends A>`
        // To prevent recursive calls return defaultValue() instead
        typeParameter: TypeParameterDescriptor,
        typeAttr: ErasureTypeAttributes
    ): CangJieType {
        val visitedTypeParameters = typeAttr.visitedTypeParameters

        if (visitedTypeParameters != null && typeParameter.original in visitedTypeParameters)
            return getDefaultType(typeAttr)

        /*
         * We should do erasure of containing type parameters with their erasure to avoid creating inconsistent types.
         * E.g. for `class Foo<T: Foo<B>, B>`, we'd have erasure for lower bound: Foo<Foo<*>, Any>,
         * but it's wrong type: projection(*) != projection(Any).
         * So we should substitute erasure of the corresponding type parameter: `Foo<Foo<Any>, Any>` or `Foo<Foo<*>, *>`.
         */
        val erasedTypeParameters = typeParameter.defaultType.extractTypeParametersFromUpperBounds(visitedTypeParameters).associate {
            val boundProjection = if (visitedTypeParameters == null || it !in visitedTypeParameters) {
                projectionComputer.computeProjection(
                    it,
                    typeAttr,
                    typeParameterUpperBoundEraser = this,
                    getErasedUpperBound(it, typeAttr.withNewVisitedTypeParameter(typeParameter))
                )
            } else makeProjection(it, typeAttr)

            it.typeConstructor to boundProjection
        }
        val erasedTypeParametersSubstitutor =
            TypeSubstitutor.create(TypeConstructorSubstitution.createByConstructorsMap(erasedTypeParameters))
        val erasedUpperBounds =
            erasedTypeParametersSubstitutor.substituteErasedUpperBounds(typeParameter.upperBounds, typeAttr)

        if (erasedUpperBounds.isNotEmpty()) {
            if (!options.intersectUpperBounds) {
                require(erasedUpperBounds.size == 1) { "Should only be one computed upper bound if no need to intersect all bounds" }
                return erasedUpperBounds.single()
            } else {
                @Suppress("UNCHECKED_CAST")
                return intersectTypes(erasedUpperBounds.toList().map { it.unwrap() })
            }
        }

        return getDefaultType(typeAttr)
    }

    private fun TypeSubstitutor.substituteErasedUpperBounds(
        upperBounds: List<CangJieType>,
        typeAttr: ErasureTypeAttributes
    ): Set<CangJieType> = buildSet {
        for (upperBound in upperBounds) {
            when (val declaration = upperBound.constructor.declarationDescriptor) {
                is ClassDescriptor -> add(
                    upperBound.replaceArgumentsOfUpperBound(
                        substitutor = this@substituteErasedUpperBounds,
                        typeAttr.visitedTypeParameters,
                        options.leaveNonTypeParameterTypes
                    )
                )
                is TypeParameterDescriptor -> {
                    if (typeAttr.visitedTypeParameters?.contains(declaration) == true) {
                        add(getDefaultType(typeAttr))
                    } else {
                        addAll(substituteErasedUpperBounds(declaration.upperBounds, typeAttr))
                    }
                }
            }

            // If no need to intersect, take only the first bound
            if (!options.intersectUpperBounds) break
        }
    }

    companion object {
        fun CangJieType.replaceArgumentsOfUpperBound(
            substitutor: TypeSubstitutor,
            visitedTypeParameters: Set<TypeParameterDescriptor>?,
            leaveNonTypeParameterTypes: Boolean = false
        ): CangJieType {
            val replacedArguments = replaceArgumentsByParametersWith { typeParameterDescriptor ->
                val argument = arguments.getOrNull(typeParameterDescriptor.index)
                if (leaveNonTypeParameterTypes && argument?.type?.containsTypeParameter() == false)
                    return@replaceArgumentsByParametersWith argument

                val isTypeParameterVisited = visitedTypeParameters != null && typeParameterDescriptor in visitedTypeParameters

                if (argument == null || isTypeParameterVisited || substitutor.substitution[argument.type] == null) {
                typeParameterDescriptor .asProjection()
                } else {
                    argument
                }
            }

            return substitutor.safeSubstitute(replacedArguments, Variance.INVARIANT)
        }
    }
}


private fun buildProjectionTypeByTypeParameters(
    typeParameters: List<TypeConstructor>,
    upperBounds: List<CangJieType>,
    builtIns: CangJieBuiltIns
) = TypeSubstitutor.create(
    object : TypeConstructorSubstitution() {
        override fun get(key: TypeConstructor) =
            if (key in typeParameters)
                makeProjection(key.declarationDescriptor as TypeParameterDescriptor)

            else null

    }
).substitute(upperBounds.first(), Variance.INVARIANT) ?: builtIns.defaultBound
fun TypeParameterDescriptor.asProjection():TypeProjection{
    return TypeProjectionImpl(this.projectionType())
}
fun TypeParameterDescriptor.projectionType(): CangJieType {
    return when (val descriptor = this.containingDeclaration) {
        is ClassifierDescriptorWithTypeParameters -> {
            buildProjectionTypeByTypeParameters(
                typeParameters = descriptor.typeConstructor.parameters.map { it.typeConstructor },
                upperBounds,
                builtIns
            )
        }
        is FunctionDescriptor -> {
            buildProjectionTypeByTypeParameters(
                typeParameters = descriptor.typeParameters.map { it.typeConstructor },
                upperBounds,
                builtIns
            )
        }
        else -> throw IllegalArgumentException("Unsupported descriptor type to build star projection type based on type parameters of it")
    }
}
