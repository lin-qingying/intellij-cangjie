package com.huawei.cangjie.resolve;

import com.huawei.cangjie.types.CangJieType;
import com.huawei.cangjie.types.TypeConstructor;
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
    private final boolean nullable;

    private PossiblyBareType(@Nullable CangJieType actualType, @Nullable TypeConstructor bareTypeConstructor, boolean nullable) {
        this.actualType = actualType;
        this.bareTypeConstructor = bareTypeConstructor;
        this.nullable = nullable;
    }

    @NotNull
    public static PossiblyBareType bare(@NotNull TypeConstructor bareTypeConstructor, boolean nullable) {
        return new PossiblyBareType(null, bareTypeConstructor, nullable);
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
