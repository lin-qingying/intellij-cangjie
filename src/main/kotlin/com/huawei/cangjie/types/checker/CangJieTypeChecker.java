package com.huawei.cangjie.types.checker;

import com.huawei.cangjie.types.CangJieType;
import com.huawei.cangjie.types.TypeConstructor;
import org.jetbrains.annotations.NotNull;


public interface CangJieTypeChecker {

    interface TypeConstructorEquality {
        boolean equals(@NotNull TypeConstructor a, @NotNull TypeConstructor b);
    }

    CangJieTypeChecker DEFAULT = NewCangJieTypeChecker.Companion.getDefault();

    /**
     * 比较两个类型，但是不比较泛型
     */
    boolean equalsIgnoringGenerics(@NotNull CangJieType a, @NotNull CangJieType b);

    boolean isSubtypeOf(@NotNull CangJieType subtype, @NotNull CangJieType supertype);
    boolean equalTypes(@NotNull CangJieType a, @NotNull CangJieType b);
}
