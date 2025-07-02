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

package cn.cangnova.cangjie.types;


import cn.cangnova.cangjie.descriptors.ClassifierDescriptor;
import cn.cangnova.cangjie.descriptors.DeclarationDescriptor;
import cn.cangnova.cangjie.descriptors.SourceElement;
import cn.cangnova.cangjie.descriptors.TypeParameterDescriptor;
import cn.cangnova.cangjie.descriptors.impl.TypeParameterDescriptorImpl;
import cn.cangnova.cangjie.utils.ReadOnly;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static cn.cangnova.cangjie.types.util.TypeUtilKt.hasTypeParameterRecursiveBounds;

public class DescriptorSubstitutor {
    private DescriptorSubstitutor() {
    }

    @NotNull
    public static TypeSubstitutor substituteTypeParameters(
            @ReadOnly @NotNull List<TypeParameterDescriptor> typeParameters,
            @NotNull TypeSubstitution originalSubstitution,
            @NotNull DeclarationDescriptor newContainingDeclaration,
            @NotNull List<TypeParameterDescriptor> result
    ) {
        TypeSubstitutor substitutor = substituteTypeParameters(
                typeParameters, originalSubstitution, newContainingDeclaration, result, null
        );
        if (substitutor == null) throw new AssertionError("Substitution failed");
        return substitutor;
    }

    @Nullable
    public static TypeSubstitutor substituteTypeParameters(
            @ReadOnly @NotNull List<TypeParameterDescriptor> typeParameters,
            @NotNull TypeSubstitution originalSubstitution,
            @NotNull DeclarationDescriptor newContainingDeclaration,
            @NotNull List<TypeParameterDescriptor> result,
            @Nullable boolean[] wereChanges
    ) {
        Map<TypeConstructor, TypeProjection> mutableSubstitutionMap = new HashMap<TypeConstructor, TypeProjection>();

        Map<TypeParameterDescriptor, TypeParameterDescriptorImpl> substitutedMap = new HashMap<TypeParameterDescriptor, TypeParameterDescriptorImpl>();
        int index = 0;
        for (TypeParameterDescriptor descriptor : typeParameters) {
            TypeParameterDescriptorImpl substituted = TypeParameterDescriptorImpl.createForFurtherModification(
                    newContainingDeclaration,
                    descriptor.getAnnotations(),
//                    descriptor.isReified(),
                    descriptor.getVariance(),
                    descriptor.getName(),
                    index++,
                    SourceElement.NO_SOURCE,
                    descriptor.getStorageManager()
            );

            mutableSubstitutionMap.put(descriptor.getTypeConstructor(), new TypeProjectionImpl(substituted.getDefaultType()));

            substitutedMap.put(descriptor, substituted);
            result.add(substituted);
        }

        TypeConstructorSubstitution mutableSubstitution = TypeConstructorSubstitution.createByConstructorsMap(mutableSubstitutionMap);
        TypeSubstitutor substitutor = TypeSubstitutor.createChainedSubstitutor(originalSubstitution, mutableSubstitution);
        TypeSubstitutor nonApproximatingSubstitutor =
                TypeSubstitutor.createChainedSubstitutor(originalSubstitution.replaceWithNonApproximating(), mutableSubstitution);

        for (TypeParameterDescriptor descriptor : typeParameters) {
            TypeParameterDescriptorImpl substituted = substitutedMap.get(descriptor);
            for (CangJieType upperBound : descriptor.getUpperBounds()) {
                ClassifierDescriptor upperBoundDeclaration = upperBound.getConstructor().getDeclarationDescriptor();
                TypeSubstitutor boundSubstitutor = upperBoundDeclaration instanceof TypeParameterDescriptor &&  hasTypeParameterRecursiveBounds((TypeParameterDescriptor) upperBoundDeclaration)
                        ? substitutor
                        : nonApproximatingSubstitutor;

                CangJieType substitutedBound = boundSubstitutor.substitute(upperBound, Variance.INVARIANT);
                if (substitutedBound == null) return null;

                if (substitutedBound != upperBound && wereChanges != null) {
                    wereChanges[0] = true;
                }

                substituted.addUpperBound(substitutedBound);
            }
            substituted.setInitialized();
        }

        return substitutor;
    }
}
