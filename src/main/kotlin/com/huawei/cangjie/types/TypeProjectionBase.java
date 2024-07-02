package com.huawei.cangjie.types;

public abstract class TypeProjectionBase implements TypeProjection {
    @Override
    public String toString() {
        if (isStarProjection()) {
            return "*";
        }
        if (getProjectionKind() == Variance.INVARIANT) {
            return getType().toString();
        }
        return getProjectionKind() + " " + getType();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TypeProjection)) return false;

        TypeProjection that = (TypeProjection) o;

        if (isStarProjection() != that.isStarProjection()) return false;
        if (getProjectionKind() != that.getProjectionKind()) return false;
        if (!getType().equals(that.getType())) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = getProjectionKind().hashCode();
        if (TypeUtils.noExpectedType(getType())) {
            result = 31 * result +19;
        } else {
            result = 31 * result + (isStarProjection() ? 17 : getType().hashCode());
        }
        return result;
    }
}