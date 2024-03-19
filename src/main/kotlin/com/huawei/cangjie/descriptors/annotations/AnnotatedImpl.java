package com.huawei.cangjie.descriptors.annotations;


import org.jetbrains.annotations.NotNull;

public class AnnotatedImpl implements Annotated {
    private final Annotations annotations;

    public AnnotatedImpl(@NotNull Annotations annotations) {
        this.annotations = annotations;
    }

    @NotNull
    @Override
    public Annotations getAnnotations() {
        return annotations;
    }
}
