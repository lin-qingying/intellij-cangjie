package com.huawei.cangjie.idea.icons;

import com.intellij.ui.IconManager;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

public final class CangJieResourcesIcons {
    private static @NotNull Icon load(@NotNull String path, int cacheKey, int flags) {
        return IconManager.getInstance().loadRasterizedIcon(path, CangJieResourcesIcons.class.getClassLoader(), cacheKey, flags);
    }
    /** 16x16 */ public static final @NotNull Icon CangJie_file = load("/icons/cangjie_file.svg", 486618922, 0);

}
