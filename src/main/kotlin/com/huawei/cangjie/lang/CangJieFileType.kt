package com.huawei.cangjie.lang

import com.huawei.cangjie.idea.icons.CangJieIcons
import com.intellij.openapi.fileTypes.LanguageFileType
import javax.swing.Icon


object CangJieFileType : LanguageFileType(CangJieLanguage){
    override fun getName(): String = CangJieLanguage.displayName

    override fun getDescription(): String = name


        const val EXTENSION: String = "cj"


    override fun getDefaultExtension(): String  = EXTENSION

    override fun getIcon(): Icon = CangJieIcons.CANGJIE_FILE



}
