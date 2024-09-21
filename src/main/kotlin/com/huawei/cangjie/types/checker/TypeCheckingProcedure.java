package com.huawei.cangjie.types.checker;

import com.huawei.cangjie.builtins.CangJieBuiltIns;
import com.huawei.cangjie.descriptors.TypeParameterDescriptor;
import com.huawei.cangjie.resolve.descriptorUtil.DescriptorUtilsKt;
import com.huawei.cangjie.types.*;
import com.huawei.cangjie.types.util.TypeUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class TypeCheckingProcedure {

    private final TypeCheckingProcedureCallbacks constraints;

    public TypeCheckingProcedure(TypeCheckingProcedureCallbacks constraints) {
        this.constraints = constraints;
    }

    // This method returns the supertype of the first parameter that has the same constructor
    // as the second parameter, applying the substitution of type arguments to it
    @Nullable
    public static CangJieType findCorrespondingSupertype(@NotNull CangJieType subtype, @NotNull CangJieType supertype, @NotNull TypeCheckingProcedureCallbacks typeCheckingProcedureCallbacks) {
        return UtilsKt.findCorrespondingSupertype(subtype, supertype, typeCheckingProcedureCallbacks);
    }
    // This method returns the supertype of the first parameter that has the same constructor
    // as the second parameter, applying the substitution of type arguments to it
    @Nullable
    public static CangJieType findCorrespondingSupertype(@NotNull CangJieType subtype, @NotNull CangJieType supertype) {
        return findCorrespondingSupertype(subtype, supertype, new TypeCheckerProcedureCallbacksImpl());
    }

    // If class C<out T> then C<T> and C<out T> mean the same
    // out * out = out
    // out * in  = *
    // out * inv = out
    //
    // in * out  = *
    // in * in   = in
    // in * inv  = in
    //
    // inv * out = out
    // inv * in  = out
    // inv * inv = inv
    public static EnrichedProjectionKind getEffectiveProjectionKind(
            @NotNull Variance typeParameterVariance,
            @NotNull Variance typeArgumentVariance
    ) {
        return EnrichedProjectionKind.Companion.getEffectiveProjectionKind(typeParameterVariance, typeArgumentVariance);
    }

    public static EnrichedProjectionKind getEffectiveProjectionKind(
            @NotNull TypeParameterDescriptor typeParameter,
            @NotNull TypeProjection typeArgument
    ) {
        return getEffectiveProjectionKind(typeParameter.getVariance(), typeArgument.getProjectionKind());
    }


    @NotNull
    private static CangJieType getOutType(@NotNull TypeParameterDescriptor parameter, @NotNull TypeProjection argument) {
               return   DescriptorUtilsKt.getBuiltIns(parameter).getAnyType()  ;
    }

    public boolean isSubtypeOf(@NotNull CangJieType subtype, @NotNull CangJieType supertype) {
        if (TypeCapabilitiesKt.sameTypeConstructors(subtype, supertype)) {
            return !subtype.isMarkedOption() || supertype.isMarkedOption();
        }
        CangJieType subtypeRepresentative = TypeCapabilitiesKt.getSubtypeRepresentative(subtype);
        CangJieType supertypeRepresentative = TypeCapabilitiesKt.getSupertypeRepresentative(supertype);
        if (subtypeRepresentative != subtype || supertypeRepresentative != supertype) {
            // recursive invocation for possible chain of representatives
            return isSubtypeOf(subtypeRepresentative, supertypeRepresentative);
        }
        return isSubtypeOfForRepresentatives(subtype, supertype);
    }

    protected boolean heterogeneousEquivalence(CangJieType inflexibleType, CangJieType flexibleType) {
        // This is to account for the case when we have Collection<X> vs (Mutable)Collection<X>! or K(java.util.Collection<? extends X>)
        assert !FlexibleTypesKt.isFlexible(inflexibleType) : "Only inflexible types are allowed here: " + inflexibleType;
        return isSubtypeOf(FlexibleTypesKt.asFlexibleType(flexibleType).getLowerBound(), inflexibleType)
                && isSubtypeOf(inflexibleType, FlexibleTypesKt.asFlexibleType(flexibleType).getUpperBound());
    }

    public boolean equalTypes(@NotNull CangJieType type1, @NotNull CangJieType type2) {
        if (type1 == type2) return true;
        if (FlexibleTypesKt.isFlexible(type1)) {
            if (FlexibleTypesKt.isFlexible(type2)) {
                return !CangJieTypeKt.isError(type1) && !CangJieTypeKt.isError(type2) &&
                        isSubtypeOf(type1, type2) && isSubtypeOf(type2, type1);
            }
            return heterogeneousEquivalence(type2, type1);
        } else if (FlexibleTypesKt.isFlexible(type2)) {
            return heterogeneousEquivalence(type1, type2);
        }

        if (type1.isMarkedOption() != type2.isMarkedOption()) {
            return false;
        }

        if (type1.isMarkedOption()) {
            // Then type2 is nullable, too (see the previous condition
            return constraints.assertEqualTypes(TypeUtils.makeNotNullable(type1), TypeUtils.makeNotNullable(type2), this);
        }

        TypeConstructor constructor1 = type1.getConstructor();
        TypeConstructor constructor2 = type2.getConstructor();

        if (!constraints.assertEqualTypeConstructors(constructor1, constructor2)) {
            return false;
        }

        List<TypeProjection> type1Arguments = type1.getArguments();
        List<TypeProjection> type2Arguments = type2.getArguments();
        if (type1Arguments.size() != type2Arguments.size()) {
            return false;
        }

        for (int i = 0; i < type1Arguments.size(); i++) {
            TypeProjection typeProjection1 = type1Arguments.get(i);
            TypeProjection typeProjection2 = type2Arguments.get(i);

            TypeParameterDescriptor typeParameter1 = constructor1.getParameters().get(i);
            TypeParameterDescriptor typeParameter2 = constructor2.getParameters().get(i);

            if (capture(typeProjection1, typeProjection2, typeParameter1)) {
                continue;
            }
            if (getEffectiveProjectionKind(typeParameter1, typeProjection1) != getEffectiveProjectionKind(typeParameter2, typeProjection2)) {
                return false;
            }

            if (!constraints.assertEqualTypes(typeProjection1.getType(), typeProjection2.getType(), this)) {
                return false;
            }
        }
        return true;
    }

    private boolean capture(
            @NotNull TypeProjection subtypeArgumentProjection,
            @NotNull TypeProjection supertypeArgumentProjection,
            @NotNull TypeParameterDescriptor parameter
    ) {
        // Capturing makes sense only for invariant classes
        if (parameter.getVariance() != Variance.INVARIANT) return false;

        // Now, both subtype and supertype relations transform to equality constraints on type arguments:
        // Array<out Int> is a subtype or equal to Array<T> then T captures a type that extends Int: 'Captured(out Int)'
        // Array<in Int> is a subtype or equal to Array<T> then T captures a type that extends Int: 'Captured(in Int)'

        if (subtypeArgumentProjection.getProjectionKind() != Variance.INVARIANT && supertypeArgumentProjection.getProjectionKind() == Variance. INVARIANT) {
            return constraints.capture(supertypeArgumentProjection.getType(), subtypeArgumentProjection);
        }
        return false;
    }

    private boolean isSubtypeOfForRepresentatives(CangJieType subtype, CangJieType supertype) {
        if (CangJieTypeKt.isError(subtype) || CangJieTypeKt.isError(supertype)) {
            return true;
        }

        if (!supertype.isMarkedOption() && subtype.isMarkedOption()) {
            return false;
        }

        if (CangJieBuiltIns.isNothing(subtype)) {
            return true;
        }

        @Nullable CangJieType closestSupertype = findCorrespondingSupertype(subtype, supertype, constraints);
        if (closestSupertype == null) {
            return constraints.noCorrespondingSupertype(subtype, supertype); // if this returns true, there still isn't any supertype to continue with
        }

        if (!supertype.isMarkedOption() && closestSupertype.isMarkedOption()) {
            return false;
        }

        return checkSubtypeForTheSameConstructor(closestSupertype, supertype);
    }

    private boolean checkSubtypeForTheSameConstructor(@NotNull CangJieType subtype, @NotNull CangJieType supertype) {
        TypeConstructor constructor = subtype.getConstructor();

        // this assert was moved to checker/utils.cj
        //assert constraints.assertEqualTypeConstructors(constructor, supertype.getConstructor()) : constructor + " is not " + supertype.getConstructor();

        List<TypeProjection> subArguments = subtype.getArguments();
        List<TypeProjection> superArguments = supertype.getArguments();
        if (subArguments.size() != superArguments.size()) return false;

        List<TypeParameterDescriptor> parameters = constructor.getParameters();
        for (int i = 0; i < parameters.size(); i++) {
            TypeParameterDescriptor parameter = parameters.get(i);

            TypeProjection superArgument = superArguments.get(i);
            TypeProjection subArgument = subArguments.get(i);



            if (capture(subArgument, superArgument, parameter)) continue;

            boolean argumentIsErrorType = CangJieTypeKt.isError(subArgument.getType()) || CangJieTypeKt.isError(superArgument.getType());
            if (!argumentIsErrorType && parameter.getVariance() == Variance.INVARIANT &&
                    subArgument.getProjectionKind() == Variance.INVARIANT && superArgument.getProjectionKind() == Variance.INVARIANT) {
                if (!constraints.assertEqualTypes(subArgument.getType(), superArgument.getType(), this)) return false;
                continue;
            }

            CangJieType superOut = getOutType(parameter, superArgument);
            CangJieType subOut = getOutType(parameter, subArgument);
            if (!constraints.assertSubtype(subOut, superOut, this)) return false;



        }
        return true;
    }

}
