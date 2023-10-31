package com.huawei.cangjie.idea.icons

import com.intellij.ui.IconManager
import javax.swing.Icon

object CangJieBaseResourcesIcons {

    private fun load(path: String, cacheKey: Int, flags: Int): Icon {
        return IconManager.getInstance().loadRasterizedIcon(
            path,
            CangJieBaseResourcesIcons::class.java.getClassLoader(), cacheKey, flags
        )
    }
}
