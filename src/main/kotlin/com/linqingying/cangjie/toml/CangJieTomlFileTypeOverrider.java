package com.linqingying.cangjie.toml;

import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.fileTypes.impl.FileTypeOverrider;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class CangJieTomlFileTypeOverrider implements FileTypeOverrider {
    @Override
    public @Nullable FileType getOverriddenFileType(@NotNull VirtualFile virtualFile) {
        return null;
    }
}
