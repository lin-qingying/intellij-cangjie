package com.huawei.cangjie.types;


import com.huawei.cangjie.descriptors.ClassifierDescriptor;
import com.huawei.cangjie.descriptors.DeclarationDescriptor;
import com.huawei.cangjie.descriptors.SourceElement;
import com.huawei.cangjie.descriptors.TypeParameterDescriptor;
import com.huawei.cangjie.descriptors.impl.TypeParameterDescriptorImpl;
import com.huawei.cangjie.utils.ReadOnly;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.huawei.cangjie.types.util.TypeUtilKt.hasTypeParameterRecursiveBounds;

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

                CangJieType substitutedBound = boundSubstitutor.substitute(upperBound, Variance.OUT_VARIANCE);
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
