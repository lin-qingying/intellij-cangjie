package com.linqingying.cangjie.lang.declarations

import com.intellij.openapi.fileTypes.FileType
import com.linqingying.cangjie.icon.CangJieIcons
import com.linqingying.cangjie.lang.CangJieFileType
import com.intellij.openapi.vfs.VirtualFile
import com.linqingying.cangjie.ide.CangJieIconProviderService
import com.linqingying.cangjie.serialization.deserialization.BuiltInSerializerProtocol
import javax.swing.Icon

object CangJieBuiltInFileType : FileType {
    override fun getName() = "cangjie_builtins"

    override fun getDescription(): String = ""

    override fun getDefaultExtension() = BuiltInSerializerProtocol.BUILTINS_FILE_EXTENSION

    override fun getIcon(): Icon? = CangJieIconProviderService.instance.builtInFileIcon

    override fun isBinary() = true

    override fun isReadOnly() = true

    override fun getCharset(file: VirtualFile, content: ByteArray): String? = null

    private const val DEFAULT_DESCRIPTION = "CangJie built-in declarations"
}

object CangJieDeclarationsFileType : CangJieFileType() {
      val EXTENSION: String = "cjd"

    override fun getDisplayName(): String {
        return EXTENSION
    }

    override fun getName() = EXTENSION

    override fun getDescription(): String = DEFAULT_DESCRIPTION

    override fun getDefaultExtension() = "cjd"

    override fun getIcon(): Icon = CangJieIcons.CANGJIE_FILE

//    override fun isBinary() = false

    override fun isReadOnly() = true

    override fun getCharset(file: VirtualFile, content: ByteArray): String? = null

    private const val DEFAULT_DESCRIPTION = "CangJie built-in declarations"
}
