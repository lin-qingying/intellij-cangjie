package com.huawei.cangjie.lang


import com.huawei.cangjie.CjBundle
import com.huawei.cangjie.ide.icons.CjIcons
import com.intellij.openapi.fileTypes.LanguageFileType
import com.intellij.openapi.vfs.VirtualFile
import javax.swing.Icon


object CjFileType : LanguageFileType(CjLanguage) {

    override fun getName(): String = "CangJie"

    override fun getIcon(): Icon = CjIcons.CANGJIE_FILE

    override fun getDefaultExtension(): String = "cj"

    override fun getCharset(file: VirtualFile, content: ByteArray): String = "UTF-8"

    override fun getDescription(): String = CjBundle.message("label.CangJie.files")
}
