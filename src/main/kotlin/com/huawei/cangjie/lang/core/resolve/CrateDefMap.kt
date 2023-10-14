package com.huawei.cangjie.lang.core.resolve

import com.huawei.cangjie.lang.core.create.CratePersistentId
import com.huawei.cangjie.lang.core.psi.CjFile
import com.huawei.cangjie.stdext.HashCode
import com.intellij.openapi.editor.Document
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.FileViewProvider
import com.intellij.psi.PsiFile

class CrateDefMap(
    val crate: CratePersistentId,
    val root: ModData,



    val directDependenciesDefMaps: Map<String, CrateDefMap>,
    private val allDependenciesDefMaps: Map<CratePersistentId, CrateDefMap>,
    initialExternPrelude: Map<String, CrateDefMap>,


    val rootModMacroIndex: Int,



    val recursionLimitRaw: Int,

    val crateDescription: String,
) {
    val fileInfos: MutableMap<FileId, FileInfo> = hashMapOf()

}

class ModPath(
    val crate: CratePersistentId,
    val segments: Array<String>,

) {

}

typealias FileId = Int

class ModData(
    val parent: ModData?,
    val crate: CratePersistentId,
    val path: ModPath,


    val isDeeplyEnabledByCfgOuter: Boolean,
    val isEnabledByCfgInner: Boolean,

    val fileId: FileId?,


    val fileRelativePath: String,

    val ownedDirectoryId: FileId?,
    val hasPathAttribute: Boolean,
    val hasMacroUse: Boolean,
    val isEnum: Boolean = false,

    val isNormalCrate: Boolean = true,

    val context: ModData? = null,

    val isBlock: Boolean = false,

    val crateDescription: String,
) {

}
class FileInfo(

    val modificationStamp: Long,

    val modData: ModData,
    val hash: HashCode,

)
