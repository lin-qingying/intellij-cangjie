package com.huawei.cangjie.icon

import com.intellij.openapi.util.IconLoader
import com.intellij.ui.IconManager
import javax.swing.Icon

object CangJieBaseResourcesIcons {
    private fun load(path: String, cacheKey: Int, flags: Int): Icon {
        return IconManager.getInstance().loadRasterizedIcon(
            path,
           CangJieBaseResourcesIcons::class.java.getClassLoader(), cacheKey, flags
        )
    }
    private fun load(path: String): Icon = IconLoader.getIcon(path, CangJieBaseResourcesIcons::class.java)

    /** 16x16  */

    val Lambda: Icon =  load("/icons/lambda.svg" )

}
