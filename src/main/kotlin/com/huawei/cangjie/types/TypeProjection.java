package com.huawei.cangjie.types;

import com.huawei.cangjie.types.checker.CangJieTypeRefiner;
import org.jetbrains.annotations.NotNull;

public interface TypeProjection extends TypeArgumentMarker {
//    @NotNull
//    Variance getProjectionKind();

    @NotNull
   CangJieType getType();

    boolean isStarProjection();

    @NotNull
    @TypeRefinement
    TypeProjection refine(@NotNull CangJieTypeRefiner kotlinTypeRefiner);

    @NotNull
    TypeProjection replaceType(@NotNull CangJieType type);
}
