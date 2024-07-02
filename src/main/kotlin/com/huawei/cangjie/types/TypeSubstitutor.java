package com.huawei.cangjie.types;

import com.huawei.cangjie.descriptors.TypeParameterDescriptor;
import com.huawei.cangjie.types.error.ErrorTypeKind;
import com.huawei.cangjie.types.model.TypeSubstitutorMarker;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class TypeSubstitutor implements TypeSubstitutorMarker {

    private final @NotNull TypeSubstitution substitution;
    public static final TypeSubstitutor EMPTY = create(TypeSubstitution.EMPTY);

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