package com.huawei.cangjie.lang.core.create.impl

import com.huawei.cangjie.cjpm.CfgOptions
import com.huawei.cangjie.cjpm.project.workspace.FeatureState
import com.huawei.cangjie.cjpm.project.workspace.PackageOrigin
import com.huawei.cangjie.lang.core.create.Crate
import com.huawei.cangjie.lang.core.create.CratePersistentId
import com.huawei.cangjie.lang.core.psi.CjFile
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.UserDataHolderBase
import com.intellij.openapi.vfs.VirtualFile


class FakeDetachedCrate(
    override val rootMod: CjFile,
    override val id: CratePersistentId,
    override val dependencies: Collection<Crate.Dependency>,
) : FakeCrate() {
    override val flatDependencies: LinkedHashSet<Crate> = dependencies.flattenTopSortedDeps()

    override val rootModFile: VirtualFile? get() = rootMod.virtualFile
    override val presentableName: String get() = "Fake for ${rootModFile?.path}"
    override val project: Project get() = rootMod.project
}


class FakeInvalidCrate(override val project: Project) : FakeCrate() {
    override val id: CratePersistentId? get() = null
    override val dependencies: Collection<Crate.Dependency> get() = emptyList()
    override val flatDependencies: LinkedHashSet<Crate> get() = linkedSetOf()
    override val rootModFile: VirtualFile? get() = null
    override val rootMod: CjFile? get() = null
    override val presentableName: String get() = "Fake"
}
abstract class FakeCrate : UserDataHolderBase(), Crate {

    override val reverseDependencies: List<Crate> get() = emptyList()



    override val cfgOptions: CfgOptions get() = CfgOptions.EMPTY
    override val features: Map<String, FeatureState> get() = emptyMap()
    override val evaluateUnknownCfgToFalse: Boolean get() = true
    override val env: Map<String, String> get() = emptyMap()
    override val outDir: VirtualFile? get() = null

    override val origin: PackageOrigin get() = PackageOrigin.WORKSPACE

    override val areDoctestsEnabled: Boolean get() = false
    override val normName: String get() = "__fake__"


    override fun toString(): String = presentableName
}
