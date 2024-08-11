package com.huawei.cangjie.types;

import com.huawei.cangjie.descriptors.TypeParameterDescriptor;
import com.huawei.cangjie.types.error.ErrorTypeKind;
import com.huawei.cangjie.types.model.TypeSubstitutorMarker;
import com.huawei.cangjie.types.typesApproximation.CapturedTypeApproximationKt;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

public class TypeSubstitutor implements TypeSubstitutorMarker {

    private final @NotNull TypeSubstitution substitution;
    public static final TypeSubstitutor EMPTY = create(TypeSubstitution.EMPTY);

    @NotNull
    public static TypeSubstitutor createChainedSubstitutor(@NotNull TypeSubstitution first, @NotNull TypeSubstitution second) {
        return create(DisjointKeysUnionTypeSubstitution.create(first, second));
    }

    @NotNull
    public static TypeSubstitutor create(@NotNull Map<TypeConstructor, TypeProjection> substitutionContext) {
        return create(TypeConstructorSubstitution.createByConstructorsMap(substitutionContext));
    }

    @NotNull
    public static Variance combine(@NotNull Variance typeParameterVariance, @NotNull Variance projectionKind) {
        if (typeParameterVariance == Variance.INVARIANT) return projectionKind;
        if (projectionKind == Variance.INVARIANT) return typeParameterVariance;
        if (typeParameterVariance == projectionKind) return projectionKind;
        throw new AssertionError("Variance conflict: type parameter variance '" + typeParameterVariance + "' and " +
                "projection kind '" + projectionKind + "' cannot be combined");
    }
    @NotNull
    public static Variance combine(@NotNull Variance typeParameterVariance, @NotNull TypeProjection typeProjection) {
        if (typeProjection.isStarProjection()) return Variance.OUT_VARIANCE;

        return combine(typeParameterVariance, typeProjection.getProjectionKind());
    }
    @Nullable
    public TypeProjection substituteWithoutApproximation(@NotNull TypeProjection typeProjection) {
        if (isEmpty()) {
            return typeProjection;
        }

        try {
            return unsafeSubstitute(typeProjection, null, 0);
        } catch (SubstitutionException e) {
            return null;
        }
    }
    @Nullable
    public TypeProjection substitute(@NotNull TypeProjection typeProjection) {
        TypeProjection substitutedTypeProjection = substituteWithoutApproximation(typeProjection);
        if (!substitution.approximateCapturedTypes() && !substitution.approximateContravariantCapturedTypes()) {
            return substitutedTypeProjection;
        }
        return CapturedTypeApproximationKt.approximateCapturedTypesIfNecessary(
                substitutedTypeProjection, substitution.approximateContravariantCapturedTypes());
    }

    @Nullable
    public CangJieType substitute(@NotNull CangJieType type, @NotNull Variance howThisTypeIsUsed) {
        TypeProjection projection =
                substitute(new TypeProjectionImpl(howThisTypeIsUsed, getSubstitution().prepareTopLevelType(type, howThisTypeIsUsed)));
        return projection == null ? null : projection.getType();
    }

    @NotNull
    public static TypeSubstitutor create(@NotNull CangJieType context) {
        return create(TypeConstructorSubstitution.create(context.getConstructor(), context.getArguments()));
    }
    @NotNull
    public static TypeSubstitutor create(@NotNull TypeSubstitution substitution) {
        return new TypeSubstitutor(substitution);
    }
    private static final class SubstitutionException extends Exception {
        public SubstitutionException(String message) {
            super(message);
        }
    }
    @NotNull
    private TypeProjection unsafeSubstitute(
            @NotNull TypeProjection originalProjection,
            @Nullable TypeParameterDescriptor typeParameter,
            int recursionDepth
    ) throws SubstitutionException{
        throw new UnsupportedOperationException("not implemented"); //To change body of created functions use File | Settings | File Templates.
    }
    @NotNull
    public CangJieType safeSubstitute(@NotNull CangJieType type, @NotNull Variance howThisTypeIsUsed) {
        if (isEmpty()) {
            return type;
        }

        try {
            return unsafeSubstitute(new TypeProjectionImpl(howThisTypeIsUsed, type), null, 0).getType();
        } catch (SubstitutionException e) {
            return ErrorUtils.createErrorType(ErrorTypeKind.UNABLE_TO_SUBSTITUTE_TYPE, e.getMessage());
        }
    }
    @NotNull
    public TypeSubstitution getSubstitution() {
        return substitution;
    }
    public boolean isEmpty() {
        return substitution.isEmpty();
    }
    protected TypeSubstitutor(@NotNull TypeSubstitution substitution) {
        this.substitution = substitution;
    }
}
