package com.linqingying.cangjie.types;

import com.linqingying.cangjie.types.checker.CangJieTypeRefiner;
import com.linqingying.cangjie.types.model.TypeArgumentMarker;
import org.jetbrains.annotations.NotNull;

public interface TypeProjection extends TypeArgumentMarker {
    @NotNull
    Variance getProjectionKind();

    @NotNull
   CangJieType getType();

//    boolean isStarProjection();

    @NotNull
    @TypeRefinement
    TypeProjection refine(@NotNull CangJieTypeRefiner cangjieTypeRefiner);

    @NotNull
    TypeProjection replaceType(@NotNull CangJieType type);
}
