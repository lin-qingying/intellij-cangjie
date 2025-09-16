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
package org.cangnova.cangjie.types

import org.cangnova.cangjie.descriptors.DeclarationDescriptor
import org.cangnova.cangjie.descriptors.SourceElement
import org.cangnova.cangjie.descriptors.TypeParameterDescriptor
import org.cangnova.cangjie.descriptors.impl.TypeParameterDescriptorImpl
import org.cangnova.cangjie.descriptors.impl.TypeParameterDescriptorImpl.Companion.createForFurtherModification
import org.cangnova.cangjie.types.TypeSubstitutor.Companion.createChainedSubstitutor


object DescriptorSubstitutor {
    fun substituteTypeParameters(
        typeParameters: List<TypeParameterDescriptor>,
        originalSubstitution: TypeSubstitution,
        newContainingDeclaration: DeclarationDescriptor,
        result: MutableList<TypeParameterDescriptor >
    ): TypeSubstitutor {
        val substitutor = DescriptorSubstitutor.substituteTypeParameters(
            typeParameters, originalSubstitution, newContainingDeclaration, result, null
        )
        if (substitutor == null) throw AssertionError("Substitution failed")
        return substitutor
    }

    fun substituteTypeParameters(
        typeParameters: List<TypeParameterDescriptor>,
        originalSubstitution: TypeSubstitution,
        newContainingDeclaration: DeclarationDescriptor,
        result: MutableList<TypeParameterDescriptor>,
        wereChanges: BooleanArray?
    ): TypeSubstitutor? {
        val mutableSubstitutionMap: MutableMap<TypeConstructor, TypeProjection> =
            HashMap()

        val substitutedMap: MutableMap<TypeParameterDescriptor?, TypeParameterDescriptorImpl> =
            HashMap<TypeParameterDescriptor?, TypeParameterDescriptorImpl>()
        var index = 0
        for (descriptor in typeParameters) {
            val substituted = createForFurtherModification(
                newContainingDeclaration,
                descriptor.annotations,  //                    descriptor.isReified(),
                descriptor.variance,
                descriptor.name,
                index++,
                SourceElement.NO_SOURCE,
                descriptor.storageManager
            )

            mutableSubstitutionMap.put(descriptor.typeConstructor, TypeProjectionImpl(substituted.defaultType))

            substitutedMap.put(descriptor, substituted)
            result.add(substituted)
        }

        val mutableSubstitution = TypeConstructorSubstitution.createByConstructorsMap(mutableSubstitutionMap)
        val substitutor = createChainedSubstitutor(originalSubstitution, mutableSubstitution)
        val nonApproximatingSubstitutor =
            createChainedSubstitutor(originalSubstitution.replaceWithNonApproximating(), mutableSubstitution)

        for (descriptor in typeParameters) {
            val substituted: TypeParameterDescriptorImpl = substitutedMap.get(descriptor)!!
            for (upperBound in descriptor.upperBounds) {
                val upperBoundDeclaration = upperBound.constructor.declarationDescriptor
                val boundSubstitutor =
                    if (upperBoundDeclaration is TypeParameterDescriptor && hasTypeParameterRecursiveBounds(
                            upperBoundDeclaration
                        )
                    )
                        substitutor
                    else
                        nonApproximatingSubstitutor

                val substitutedBound = boundSubstitutor.substitute(upperBound, Variance.INVARIANT)
                if (substitutedBound == null) return null

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
