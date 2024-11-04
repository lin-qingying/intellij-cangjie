package com.linqingying.cangjie.descriptors;

import org.jetbrains.annotations.Nullable;

public interface SourceFile {
    SourceFile NO_SOURCE_FILE = new SourceFile() {
        @Nullable
        @Override
        public String getName() {
            return null;
        }
    };

    @Nullable
    String getName();
}
