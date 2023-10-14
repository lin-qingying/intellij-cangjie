package com.huawei.cangjie.lang.core.resolve

import com.huawei.cangjie.cjpm.project.model.cjpmProjects
import com.huawei.cangjie.lang.core.create.crateGraph
import com.huawei.cangjie.lang.core.psi.CjFile
import com.intellij.openapi.vfs.VirtualFileWithId


data class FileInclusionPoint(
    val defMap: CrateDefMap,
    val modData: ModData,

)
