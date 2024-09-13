package com.huawei.cangjie.name;

import kotlin.collections.ArraysKt;
import kotlin.jvm.functions.Function1;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

public final class FqNameUnsafe {
    private static final Name ROOT_NAME = Name.special("<root>");
    private static final Pattern SPLIT_BY_DOTS = Pattern.compile("\\.");

    private static final Function1<String, Name> STRING_TO_NAME = Name::guessByFirstCharacter;

    @NotNull
    private final String fqName;


    private transient FqName safe;
    private transient FqNameUnsafe parent;
    private transient Name shortName;

    FqNameUnsafe(@NotNull String fqName, @NotNull FqName safe) {
        this.fqName = fqName;
        this.safe = safe;
    }

    public FqNameUnsafe(@NotNull String fqName) {
        this.fqName = fqName;
    }

    private FqNameUnsafe(@NotNull String fqName, FqNameUnsafe parent, Name shortName) {
        this.fqName = fqName;
        this.parent = parent;
        this.shortName = shortName;
    }

    public static boolean isValid(@Nullable String qualifiedName) {
        // TODO: ���ڴ���ת���ַ�����Ч����''
        return qualifiedName != null && qualifiedName.indexOf('/') < 0 && qualifiedName.indexOf('*') < 0;
    }

    @NotNull
    public static FqNameUnsafe topLevel(@NotNull Name shortName) {
        return new FqNameUnsafe(shortName.asString(), FqName.ROOT.toUnsafe(), shortName);
    }

    /**
     * 返回 shortName 的集合，按指定顺序排列。
     *
     * @param ascending 如果为 true，则返回正序，否则返回倒序。
     * @return shortName 的集合。
     */
    @NotNull
    public List<Name> getShortNames(boolean ascending) {
        List<Name> shortNames = new ArrayList<>();

        // 获取路径段
        List<Name> segments = pathSegments();
        for (Name segment : segments) {
            shortNames.add(segment);
        }

        // 根据 ascending 参数决定排序顺序
        if (!ascending) {
            Collections.reverse(shortNames);
        }

        return shortNames;
    }

    private void compute() {
        int lastDot = fqName.lastIndexOf('.');
        if (lastDot >= 0) {
            shortName = Name.guessByFirstCharacter(fqName.substring(lastDot + 1));
            parent = new FqNameUnsafe(fqName.substring(0, lastDot));
        } else {
            shortName = Name.guessByFirstCharacter(fqName);
            parent = FqName.ROOT.toUnsafe();
        }
    }

    @NotNull
    public String asString() {
        return fqName;
    }

    public boolean isSafe() {
        return safe != null || asString().indexOf('<') < 0;
    }

    @NotNull
    public FqName toSafe() {
        if (safe != null) {
            return safe;
        }
        safe = new FqName(this);
        return safe;
    }

    public boolean isRoot() {
        return fqName.isEmpty();
//        return parent == null;
    }

    @NotNull
    public FqNameUnsafe parent() {
        if (parent != null) {
            return parent;
        }

        if (isRoot()) {
            throw new IllegalStateException("root");
        }

        compute();

        return parent;
    }

    @NotNull
    public FqNameUnsafe child(@NotNull Name name) {
        String childFqName;
        if (isRoot()) {
            childFqName = name.asString();
        } else {
            childFqName = fqName + "." + name.asString();
        }
        return new FqNameUnsafe(childFqName, this, name);
    }

    @NotNull
    public FqNameUnsafe child(@NotNull FqName fqname) {

        FqNameUnsafe _this = this;
        for (Name name : fqname.pathSegments()) {
            _this = _this.child(name);
        }
        return _this;


    }

    @NotNull
    public Name shortName() {
        if (shortName != null) {
            return shortName;
        }

        if (isRoot()) {
            throw new IllegalStateException("root");
        }

        compute();

        return shortName;
    }

    @NotNull
    public Name shortNameOrSpecial() {
        if (isRoot()) {
            return ROOT_NAME;
        } else {
            return shortName();
        }
    }

    @NotNull
    public List<Name> pathSegments() {
        return isRoot() ? Collections.emptyList() : ArraysKt.map(SPLIT_BY_DOTS.split(fqName), STRING_TO_NAME);
    }

    public boolean startsWith(@NotNull Name segment) {
        if (isRoot())
            return false;

        int firstDot = fqName.indexOf('.');
        String segmentAsString = segment.asString();
        return fqName.regionMatches(0, segmentAsString, 0, firstDot == -1 ? Math.max(fqName.length(), segmentAsString.length()) : firstDot);
    }

    public boolean startsWith(@NotNull FqNameUnsafe other) {
        if (isRoot()) return false;

        int thisLength = fqName.length();
        int otherLength = other.fqName.length();
        if (thisLength < otherLength) return false;

        return (thisLength == otherLength || fqName.charAt(otherLength) == '.') &&
                fqName.regionMatches(0, other.fqName, 0, otherLength);
    }

    @Override
    @NotNull
    public String toString() {
        return isRoot() ? ROOT_NAME.asString() : fqName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof FqNameUnsafe that)) return false;

        return fqName.equals(that.fqName);
    }

    @Override
    public int hashCode() {
        return fqName.hashCode();
    }
}
