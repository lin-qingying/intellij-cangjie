package com.linqingying.cangjie.resolve;

import com.linqingying.cangjie.types.CangJieType;
import com.linqingying.cangjie.types.CastDiagnosticsUtil;
import com.linqingying.cangjie.types.TypeConstructor;
import com.linqingying.cangjie.types.TypeReconstructionResult;
import com.linqingying.cangjie.types.util.TypeUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Bare types are somewhat like raw types, but in CangJie they are only allowed on the right-hand side of is/as.
 * For example:
 * <p>
 * fun foo(a: Any) {
 * if (a is List) {
 * // a is known to be List<*> here
 * }
 * }
 * <p>
 * Another example:
 * <p>
 * fun foo(a: Collection<String>) {
 * if (a is List) {
 * // a is known to be List<String> here
 * }
 * }
 * <p>
 * One can call reconstruct(supertype) to get an actual type from a bare type
 */
public class PossiblyBareType {
    private final CangJieType actualType;
    private final TypeConstructor bareTypeConstructor;
    private final boolean optional;

    private PossiblyBareType(@Nullable CangJieType actualType, @Nullable TypeConstructor bareTypeConstructor, boolean optional) {
        this.actualType = actualType;
        this.bareTypeConstructor = bareTypeConstructor;
        this.optional = optional;
    }
    private boolean isBareTypeNullable() {
        return optional;
    }
    public boolean isOptional() {
        if (isBare()) return isBareTypeNullable();
        return getActualType().isMarkedOption();
    }
    @NotNull
    public static PossiblyBareType bare(@NotNull TypeConstructor bareTypeConstructor, boolean optional) {
        return new PossiblyBareType(null, bareTypeConstructor, optional);
    }

    @NotNull
    public TypeReconstructionResult reconstruct(@NotNull CangJieType subjectType) {
        if (!isBare()) return new TypeReconstructionResult(getActualType(), true);

        TypeReconstructionResult reconstructionResult = CastDiagnosticsUtil.findStaticallyKnownSubtype(
                TypeUtils.makeNotNullable(subjectType),
                getBareTypeConstructor()
        );
        CangJieType type = reconstructionResult.getResultingType();
        // No need to make an absent type nullable
        if (type == null) return reconstructionResult;

        CangJieType resultingType = TypeUtils.makeOptionalAsSpecified(type, isBareTypeNullable());
        return new TypeReconstructionResult(resultingType, reconstructionResult.isAllArgumentsInferred());
    }

    public PossiblyBareType makeOptional() {
        if (isBare()) {
            return isBareTypeNullable() ? this : bare(getBareTypeConstructor(), true);
        }

        return type(TypeUtils.makeOptional(getActualType()));
    }

    @NotNull
    public TypeConstructor getBareTypeConstructor() {
        //noinspection ConstantConditions
        return bareTypeConstructor;
    }
    @NotNull
    public CangJieType getActualType() {
        //noinspection ConstantConditions
        return actualType;
    }
    public boolean isBare() {
        return actualType == null;
    }
    @NotNull
    public static PossiblyBareType type(@NotNull CangJieType actualType) {
        return new PossiblyBareType(actualType, null, false);
    }
}
