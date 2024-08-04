package com.linqingying.cangjie

import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.vfs.VirtualFile
import javax.swing.Icon

//
//object CangJieBuiltInFileType : FileType {
//    override fun getName() = "cangjie_builtins"
//
//    override fun getDescription(): String =
//       CangJieLabelProviderService.getService()?.getLabelForBuiltInFileType()
//            ?: DEFAULT_DESCRIPTION
//
//    override fun getDefaultExtension() = BuiltInSerializerProtocol.BUILTINS_FILE_EXTENSION
//
//    override fun getIcon(): Icon = CangJieIconProviderService.getInstance().builtInFileIcon
//
//    override fun isBinary() = true
//
//    override fun isReadOnly() = true
//
//    override fun getCharset(file: VirtualFile, content: ByteArray): String? = null
//
//    private const val DEFAULT_DESCRIPTION = "CangJie built-in declarations"
//}
