package com.huawei.cangjie.lang.core.create

import com.huawei.cangjie.cjpm.CfgOptions
import com.huawei.cangjie.cjpm.project.workspace.FeatureState
import com.huawei.cangjie.cjpm.project.workspace.PackageOrigin
import com.huawei.cangjie.lang.core.psi.CjFile
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.UserDataHolderEx
import com.intellij.openapi.vfs.VirtualFile


typealias CratePersistentId = Int

interface Crate: UserDataHolderEx {



    val id: CratePersistentId?





    val origin: PackageOrigin

    val cfgOptions: CfgOptions
    val features: Map<String, FeatureState>


    val evaluateUnknownCfgToFalse: Boolean

    val env: Map<String, String>

     val outDir: VirtualFile?

     val dependencies: Collection<Dependency>

   val flatDependencies: LinkedHashSet<Crate>

    val reverseDependencies: List<Crate>

    val dependenciesWithCyclic: Collection<Dependency>
        get() = dependencies

     val hasCyclicDevDependencies: Boolean
        get() = false

     val rootModFile: VirtualFile?
    val rootMod: CjFile?

    val areDoctestsEnabled: Boolean

    val presentableName: String


    val normName: String

    val project: Project




    data class Dependency(
        val normName: String,

        val crate: Crate
    )
}
