package com.huawei.cj.lang
import com.intellij.openapi.fileTypes.LanguageFileType
import com.intellij.openapi.vfs.VirtualFile


object RsFileType : LanguageFileType(CjLanguage) {

    override fun getName(): String = "CangJie"

    override fun getIcon(): Icon = RsIcons.RUST_FILE

    override fun getDefaultExtension(): String = "cj"

    override fun getCharset(file: VirtualFile, content: ByteArray): String = "UTF-8"

    override fun getDescription(): String = RsBundle.message("label.rust.files")
}
