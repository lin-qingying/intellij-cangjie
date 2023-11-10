package com.huawei.cangjie.idea.icons

import com.intellij.openapi.util.IconLoader
import com.intellij.ui.IconManager
import javax.swing.Icon

object CangJieBaseResourcesIcons {
    /** 16x16  */
    val Kotlin: Icon = load("/icons/kotlin.svg")
    private fun load(path: String, cacheKey: Int, flags: Int): Icon {
        return IconManager.getInstance().loadRasterizedIcon(
            path,
            CangJieBaseResourcesIcons::class.java.getClassLoader(), cacheKey, flags
        )
    }

    private fun load(path: String): Icon = IconLoader.getIcon(path, CangJieBaseResourcesIcons::class.java)
}
