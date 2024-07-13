package com.huawei.cangjie.types;

import com.huawei.cangjie.types.checker.CangJieTypeRefiner;
import org.jetbrains.annotations.NotNull;

public class TypeProjectionImpl extends TypeProjectionBase{
    private final Variance projection;
    private final CangJieType type;

    public TypeProjectionImpl(@NotNull Variance projection, @NotNull CangJieType type) {
        this.projection = projection;
        this.type = type;
    }

    public TypeProjectionImpl(@NotNull CangJieType type) {
        this(Variance.INVARIANT, type);
    }
    @Override
    public @NotNull Variance getProjectionKind() {
        return projection;

    }

    @Override
    public @NotNull CangJieType getType() {
        return type;
    }

    @Override
    public boolean isStarProjection() {
        return false;
    }

    @NotNull
    @Override
    @TypeRefinement
    public  TypeProjection refine(@NotNull CangJieTypeRefiner cangjieTypeRefiner) {
        return new TypeProjectionImpl(projection, cangjieTypeRefiner.refineType(type));

    }
    @NotNull
    @Override
    public  TypeProjection replaceType(@NotNull CangJieType type) {
        return new TypeProjectionImpl(this.projection, type);

    }
}
