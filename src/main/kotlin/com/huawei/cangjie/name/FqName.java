package com.huawei.cangjie.name;


import com.huawei.cangjie.utils.StringsKt;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public final class FqName {

    public static final FqName ROOT = new FqName("");
    @NotNull
    private final FqNameUnsafe fqName;
    private transient FqName parent;


    public FqName(@NotNull String fqName) {
        this.fqName = new FqNameUnsafe(fqName, this);
    }

    public FqName(@NotNull FqNameUnsafe fqName) {
        this.fqName = fqName;
    }

    private FqName(@NotNull FqNameUnsafe fqName, FqName parent) {
        this.fqName = fqName;
        this.parent = parent;
    }

    @NotNull
    public static FqName fromSegments(@NotNull List<String> names) {
        return new FqName(StringsKt.join(names, "."));
    }

    @NotNull
    public static FqName topLevel(@NotNull Name shortName) {
        return new FqName(FqNameUnsafe.topLevel(shortName));
    }

    @NotNull
    public String asString() {
        return fqName.asString();
    }

    @NotNull
    public FqNameUnsafe toUnsafe() {
        return fqName;
    }

    public boolean isRoot() {
        return fqName.isRoot();
//        return parent == null;
    }

    @NotNull
    public FqName parent() {
        if (parent != null) {
            return parent;
        }

        if (isRoot()) {
            throw new IllegalStateException("root");
        }

        parent = new FqName(fqName.parent());

        return parent;
    }

    public boolean isModuleName() {
        return parent.isRoot();
    }

    public Name getModuleName() {

        FqName _this =  this;
        while (true) {
            if (_this.parent().isRoot())
                return _this.shortName();
            _this = _this.parent;
        }

    }

    @NotNull
    public FqName child(@NotNull Name name) {
        return new FqName(fqName.child(name), this);
    }

    @NotNull
    public FqName child(@NotNull FqName name) {
        return new FqName(fqName.child(name), this);
    }

    @NotNull
    public Name shortName() {
        return fqName.shortName();
    }

    @NotNull
    public Name shortNameOrSpecial() {
        return fqName.shortNameOrSpecial();
    }

    @NotNull
    public List<Name> pathSegments() {
        return fqName.pathSegments();
    }

    public boolean startsWith(@NotNull Name segment) {
        return fqName.startsWith(segment);
    }

    public boolean startsWith(@NotNull FqName other) {
        return fqName.startsWith(other.fqName);
    }

    @Override
    public String toString() {
        return fqName.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof FqName otherFqName)) return false;

        return fqName.equals(otherFqName.fqName);
    }

    @Override
    public int hashCode() {
        return fqName.hashCode();
    }
}
