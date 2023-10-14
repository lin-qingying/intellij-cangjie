package com.huawei.cangjie.lang.core.create

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile

interface CrateGraphService {

    val topSortedCrates: List<Crate>


    fun findCrateById(id: CratePersistentId): Crate?

    fun findCrateByRootMod(rootModFile: VirtualFile): Crate?
}

val Project.crateGraph: CrateGraphService
    get() = service()
