package com.huawei.cangjie.icon

import com.intellij.openapi.util.IconLoader
import javax.swing.Icon

object CjpmIcons {
    val ICON = CangJieIcons.TOML
    val LOCK_ICON = CangJieIcons.TOML
    val MANIFEST_ICON = CangJieIcons.TOML
//    val ICON = load("/icons/cargo.svg")
//    val LOCK_ICON = load("/icons/cargoLock.svg")

    private fun load(path: String): Icon = IconLoader.getIcon(path, CjpmIcons::class.java)

}
