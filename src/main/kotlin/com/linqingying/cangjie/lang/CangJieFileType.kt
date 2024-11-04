package com.linqingying.cangjie.lang

import com.linqingying.cangjie.icon.CangJieIcons
import com.intellij.openapi.fileTypes.LanguageFileType
import javax.swing.Icon


open class CangJieFileType : LanguageFileType(CangJieLanguage) {
    override fun getName(): String = CangJieLanguage.displayName

    override fun getDescription(): String = name


    override fun getDefaultExtension(): String = EXTENSION

    override fun getIcon(): Icon = CangJieIcons.CANGJIE_FILE

    companion object {
        val EXTENSION: String = "cj"

        val DOT_DEFAULT_EXTENSION: String = ".$EXTENSION"
        val INSTANCE = CangJieFileType()
    }

}
