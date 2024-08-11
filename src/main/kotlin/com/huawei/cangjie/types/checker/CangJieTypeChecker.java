package com.huawei.cangjie.types.checker;

import com.huawei.cangjie.types.CangJieType;
import com.huawei.cangjie.types.TypeConstructor;
import org.jetbrains.annotations.NotNull;


public interface CangJieTypeChecker {

    interface TypeConstructorEquality {
        boolean equals(@NotNull TypeConstructor a, @NotNull TypeConstructor b);
    }

    CangJieTypeChecker DEFAULT = NewCangJieTypeChecker.Companion.getDefault();

    boolean isSubtypeOf(@NotNull CangJieType subtype, @NotNull CangJieType supertype);
    boolean equalTypes(@NotNull CangJieType a, @NotNull CangJieType b);
}
