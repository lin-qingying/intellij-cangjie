package com.huawei.cangjie.lang.declarations

import com.huawei.cangjie.icon.CangJieIcons
import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.vfs.VirtualFile
import javax.swing.Icon

object CangJieBuiltInFileType : FileType {
    const val EXTENSION: String = "cangjie_declarations"


    override fun getName() = EXTENSION

    override fun getDescription(): String = DEFAULT_DESCRIPTION

    override fun getDefaultExtension() = EXTENSION

    override fun getIcon(): Icon = CangJieIcons.CANGJIE_FILE

    override fun isBinary() = false

    override fun isReadOnly() = true

    override fun getCharset(file: VirtualFile, content: ByteArray): String? = null

    private const val DEFAULT_DESCRIPTION = "CangJie built-in declarations"
}
