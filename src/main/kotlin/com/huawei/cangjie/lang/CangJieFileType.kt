package com.huawei.cangjie.lang

import com.huawei.cangjie.icon.CangJieIcons
import com.intellij.openapi.fileTypes.LanguageFileType
import javax.swing.Icon

//object CangJieFileType.INSTANCE : LanguageFileType(CangJieLanguage) {
//    override fun getName(): String = CangJieLanguage.displayName
//
//    override fun getDescription(): String = name
//
//
//    const val EXTENSION: String = "cj"
//
//
//    override fun getDefaultExtension(): String = EXTENSION
//
//    override fun getIcon(): Icon = CangJieIcons.CANGJIE_FILE
//
//
//}

open class CangJieFileType : LanguageFileType(CangJieLanguage) {
    override fun getName(): String = CangJieLanguage.displayName

    override fun getDescription(): String = name


    override fun getDefaultExtension(): String = EXTENSION

    override fun getIcon(): Icon = CangJieIcons.CANGJIE_FILE

    companion object {
        val EXTENSION: String = "cj"

        val DOT_DEFAULT_EXTENSION: String = "." + EXTENSION
        val INSTANCE = CangJieFileType()
    }

}
