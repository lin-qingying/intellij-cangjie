package com.linqingying.cangjie.ide.projectStructure

import com.linqingying.cangjie.lang.declarations.CangJieDeclarationsFileType
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.fileTypes.FileType


abstract class CangJieBinaryExtension(val fileType: FileType) {
    companion object {
        val EP_NAME: ExtensionPointName<CangJieBinaryExtension> = ExtensionPointName.create("com.linqingying.cangjie.binaryExtension")

        val cangjieBinaries: List<FileType> by lazy(LazyThreadSafetyMode.PUBLICATION) {
            EP_NAME.extensionList.map { it.fileType }
        }
    }
}


class CangJieBuiltInBinary : CangJieBinaryExtension(CangJieDeclarationsFileType)
//class CangJieModuleBinary : CangJieBinaryExtension(CangJieModuleFileType.INSTANCE)
//class CangJieJsMetaBinary : CangJieBinaryExtension(CangJieJavaScriptMetaFileType)
//class ClibMetaBinary : CangJieBinaryExtension(ClibMetaFileType)

val FileType.isCangJieBinary: Boolean
    get() = this in CangJieBinaryExtension.cangjieBinaries
