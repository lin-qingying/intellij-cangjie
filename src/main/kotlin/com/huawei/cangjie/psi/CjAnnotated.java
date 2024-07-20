package com.huawei.cangjie.psi;

import org.jetbrains.annotations.NotNull;

import java.util.List;

public interface CjAnnotated extends CjElement {
    @NotNull
    List<CjAnnotation> getAnnotations();

    @NotNull
    List<CjAnnotationEntry> getAnnotationEntries();
}
