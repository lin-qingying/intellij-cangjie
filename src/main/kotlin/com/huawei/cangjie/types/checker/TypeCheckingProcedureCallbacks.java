package com.huawei.cangjie.types.checker;

import com.huawei.cangjie.types.CangJieType;
import com.huawei.cangjie.types.TypeConstructor;
import com.huawei.cangjie.types.TypeProjection;
import org.jetbrains.annotations.NotNull;


/**
 * Methods of this class return true to continue type checking and false to fail
 */
public interface TypeCheckingProcedureCallbacks {
    boolean assertEqualTypes(@NotNull CangJieType a, @NotNull CangJieType b, @NotNull TypeCheckingProcedure typeCheckingProcedure);

    boolean assertEqualTypeConstructors(@NotNull TypeConstructor a, @NotNull TypeConstructor b);

    boolean assertSubtype(@NotNull CangJieType subtype, @NotNull CangJieType supertype, @NotNull TypeCheckingProcedure typeCheckingProcedure);

    boolean capture(@NotNull CangJieType type, @NotNull TypeProjection typeProjection);

    boolean noCorrespondingSupertype(@NotNull CangJieType subtype, @NotNull CangJieType supertype);
}
