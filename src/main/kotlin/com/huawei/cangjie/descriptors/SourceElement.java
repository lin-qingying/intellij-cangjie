package com.huawei.cangjie.descriptors;

import org.jetbrains.annotations.NotNull;

public interface SourceElement {
    SourceElement NO_SOURCE = new SourceElement() {
        @Override
        public String toString() {
            return "NO_SOURCE";
        }

        @NotNull
        @Override
        public SourceFile getContainingFile() {
            return SourceFile.NO_SOURCE_FILE;
        }
    };

    @NotNull
    SourceFile getContainingFile();
}
