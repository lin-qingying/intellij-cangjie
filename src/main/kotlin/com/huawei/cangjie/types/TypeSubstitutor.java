package com.huawei.cangjie.types;

import com.huawei.cangjie.types.model.TypeSubstitutorMarker;
import org.jetbrains.annotations.NotNull;

public class TypeSubstitutor implements TypeSubstitutorMarker {

    private final @NotNull TypeSubstitution substitution;

    @NotNull
    public static TypeSubstitutor create(@NotNull TypeSubstitution substitution) {
        return new TypeSubstitutor(substitution);
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