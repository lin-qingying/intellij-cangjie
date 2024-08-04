package com.linqingying.cangjie.icon

import com.intellij.openapi.util.IconLoader
import javax.swing.Icon

object CjpmIcons {
    val ICON = CangJieIcons.CANGJIE_FILE
    val LOCK_ICON = CangJieIcons.CANGJIE_FILE
    val MANIFEST_ICON = CangJieIcons.CANGJIE_FILE
//    val ICON = load("/icons/cargo.svg")
//    val LOCK_ICON = load("/icons/cargoLock.svg")

    private fun load(path: String): Icon = IconLoader.getIcon(path, CjpmIcons::class.java)

}
