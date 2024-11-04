package com.linqingying.cangjie.types;

import com.linqingying.cangjie.types.util.TypeUtils;

public abstract class TypeProjectionBase implements TypeProjection {
    @Override
    public String toString() {
//        if (isStarProjection()) {
//            return "*";
//        }
        if (getProjectionKind() == Variance.INVARIANT) {
            return getType().toString();
        }
        return getProjectionKind() + " " + getType();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TypeProjection that)) return false;


        if (getProjectionKind() != that.getProjectionKind()) return false;
        return getType().equals(that.getType());
    }

    @Override
    public int hashCode() {
        int result = getProjectionKind().hashCode();
        if (TypeUtils.noExpectedType(getType())) {
            result = 31 * result +19;
        } else {
            result = 31 * result + ( getType().hashCode());
        }
        return result;
    }
}
