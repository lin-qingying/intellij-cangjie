package com.huawei.cangjie.idea.icons


import com.intellij.openapi.util.IconLoader
import javax.swing.Icon

object CangJieIcons {


    val CANGJIE_FILE = load("/icons/cangjieFile.svg")

    private fun load(path: String): Icon = IconLoader.getIcon(path, CangJieIcons::class.java)
}
