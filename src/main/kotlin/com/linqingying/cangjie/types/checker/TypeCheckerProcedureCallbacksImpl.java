package com.linqingying.cangjie.types.checker;

import com.linqingying.cangjie.types.CangJieType;
import com.linqingying.cangjie.types.TypeConstructor;
import com.linqingying.cangjie.types.TypeProjection;
import org.jetbrains.annotations.NotNull;

class TypeCheckerProcedureCallbacksImpl implements TypeCheckingProcedureCallbacks {
    @Override
    public boolean assertEqualTypes(@NotNull CangJieType a, @NotNull CangJieType b, @NotNull TypeCheckingProcedure typeCheckingProcedure) {
        return typeCheckingProcedure.equalTypes(a, b);
    }

    @Override
    public boolean assertEqualTypeConstructors(@NotNull TypeConstructor a, @NotNull TypeConstructor b) {
        return a.equals(b);
    }

    @Override
    public boolean assertSubtype(@NotNull CangJieType subtype, @NotNull CangJieType supertype, @NotNull TypeCheckingProcedure typeCheckingProcedure) {
        return typeCheckingProcedure.isSubtypeOf(subtype, supertype);
    }

    @Override
    public boolean capture(@NotNull CangJieType type, @NotNull TypeProjection typeProjection) {
        return false;
    }

    @Override
    public boolean noCorrespondingSupertype(@NotNull CangJieType subtype, @NotNull CangJieType supertype) {
        return false; // type checking fails
    }
}
