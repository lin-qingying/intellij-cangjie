package com.huawei.cangjie.idea.icons


import com.intellij.openapi.util.IconLoader
import com.intellij.ui.IconManager
import javax.swing.Icon

object CangJieIcons {
    /** 16x16  */
//    val FILE: Icon = CangJieResourcesIcons.CangJie_file

    val CANGJIE_FILE = load("/icons/cangjie_file.svg")
    /** 16x16  */
    val SMALL_LOGO: Icon = load("/icons/kotlin.svg")


    /** 16x16  */

    /** 16x16  */
    val Class: Icon =
        load("/icons/class.svg" )

    private fun load(path: String): Icon = IconLoader.getIcon(path, CangJieIcons::class.java)

    private fun load(path: String, cacheKey: Int, flags: Int): Icon {
        return IconManager.getInstance()
            .loadRasterizedIcon(path, CangJieIcons::class.java.getClassLoader(), cacheKey, flags)
    }

}
