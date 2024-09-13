package com.huawei.cangjie.icon

import com.intellij.ui.IconManager

import javax.swing.Icon

object CangJieResourcesIcons {
    private fun load(path: String, cacheKey: Int, flags: Int): Icon {
        return IconManager.  getInstance().loadRasterizedIcon(
            path,
            CangJieResourcesIcons::class.java.classLoader, cacheKey, flags
        )
    }

    /** 16x16  */
    val CangJie_file: Icon = load("/icons/cangjie_file.svg", 486618922, 0)
}
