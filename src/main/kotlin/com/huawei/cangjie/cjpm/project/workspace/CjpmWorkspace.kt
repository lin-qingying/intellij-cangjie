package com.huawei.cangjie.cjpm.project.workspace

import com.huawei.cangjie.cjpm.CfgOptions
import com.huawei.cangjie.cjpm.CjpmConfig
import com.intellij.openapi.util.UserDataHolderEx
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.ThreeState
import org.jetbrains.annotations.TestOnly
import java.nio.file.Path
import java.util.*
import com.huawei.cangjie.cjpm.project.workspace.PackageOrigin.*
import com.huawei.cangjie.openapiext.CachedVirtualFile
import com.huawei.cangjie.stdext.applyWithSymlink
import com.intellij.openapi.util.UserDataHolderBase
import com.intellij.openapi.vfs.VirtualFileManager
import java.nio.file.Paths

interface CjpmWorkspace {
    val manifestPath: Path
    val contentRoot: Path get() = manifestPath.parent

    val workspaceRoot: VirtualFile?

    val cfgOptions: CfgOptions
    val cargoConfig: CjpmConfig


    val packages: Collection<Package>



    fun findPackageById(id: PackageId): Package? = packages.find { it.id == id }
    fun findPackageByName(name: String, isStd: ThreeState = ThreeState.UNSURE): Package? = packages.find {
        if (it.name != name && it.normName != name) return@find false
        when (isStd) {
            ThreeState.YES -> it.origin == STDLIB
            ThreeState.NO -> it.origin == WORKSPACE || it.origin == DEPENDENCY
            ThreeState.UNSURE -> true
        }
    }

    fun findTargetByCrateRoot(root: VirtualFile): Target?
    fun isCrateRoot(root: VirtualFile) = findTargetByCrateRoot(root) != null


    val hasStandardLibrary: Boolean get() = packages.any { it.origin == STDLIB }



    interface Package : UserDataHolderEx {
        val contentRoot: VirtualFile?
        val rootDirectory: Path

        val id: String
        val name: String
        val normName: String get() = name.replace('-', '_')

        val version: String

        val source: String?
        val origin: PackageOrigin

        val targets: Collection<Target>
        val libTarget: Target? get() = targets.find { it.kind.isLib }
        val customBuildTarget: Target? get() = targets.find { it.kind == TargetKind.CustomBuild }
        val hasCustomBuildScript: Boolean get() = customBuildTarget != null

        val dependencies: Collection<Dependency>


        val cfgOptions: CfgOptions?


        val workspace: CjpmWorkspace

        val edition: Edition

        val env: Map<String, String>

        val outDir: VirtualFile?

        val featureState: Map<FeatureName, FeatureState>


        fun findDependency(normName: String): Target? =
            if (this.normName == normName) libTarget else dependencies.find { it.name == normName }?.pkg?.libTarget
    }


    interface Target {
        val name: String

       val normName: String get() = name.replace('-', '_')

        val kind: TargetKind

        val crateRoot: VirtualFile?

        val pkg: Package

        val edition: Edition

        val doctest: Boolean

        val requiredFeatures: List<String>

        val cfgOptions: CfgOptions
    }

    interface Dependency {
        val pkg: Package
        val name: String
        val cargoFeatureDependencyPackageName: String
        val depKinds: List<DepKindInfo>


        val requiredFeatures: Set<String>
    }

    data class DepKindInfo(
        val kind: DepKind,
        val target: String? = null
    )

    enum class DepKind(val cargoName: String?) {

        Unclassified(null),

        Stdlib("stdlib?"),


        Normal(null),


        Development("dev"),


        Build("build")
    }

    sealed class TargetKind(val name: String) {
        class Lib(val kinds: EnumSet<LibKind>) : TargetKind("lib") {
            constructor(vararg kinds: LibKind) : this(EnumSet.copyOf(kinds.asList()))
        }

        object Bin : TargetKind("bin")
        object Test : TargetKind("test")
        object ExampleBin : TargetKind("example")
        class ExampleLib(val kinds: EnumSet<LibKind>) : TargetKind("example")
        object Bench : TargetKind("bench")
        object CustomBuild : TargetKind("custom-build")
        object Unknown : TargetKind("unknown")

        val isLib: Boolean get() = this is Lib
        val isBin: Boolean get() = this == Bin
        val isExampleBin: Boolean get() = this == ExampleBin
        val isCustomBuild: Boolean get() = this == CustomBuild
        val isProcMacro: Boolean
            get() = this is Lib && this.kinds.contains(LibKind.PROC_MACRO)
        val canHaveMainFunction: Boolean
            get() = isBin || isExampleBin || isCustomBuild
    }

    enum class LibKind {
        LIB, DYLIB, STATICLIB, CDYLIB, RLIB, PROC_MACRO, UNKNOWN
    }

    enum class Edition(val presentation: String) {
        EDITION_2015("2015"),
        EDITION_2018("2018"),
        EDITION_2021("2021");

        companion object {
            val DEFAULT: Edition = EDITION_2018
        }
    }

    companion object {
        fun deserialize(
            manifestPath: Path,
            data: CjpmWorkspaceData,
            cfgOptions: CfgOptions = CfgOptions.DEFAULT,
            cargoConfig: CjpmConfig = CjpmConfig.DEFAULT,
        ): CjpmWorkspace =
            WorkspaceImpl.deserialize(manifestPath, data, cfgOptions, cargoConfig)
    }
}

private class WorkspaceImpl(
    override val manifestPath: Path,
    val workspaceRootUrl: String?,
    packagesData: Collection<CjpmWorkspaceData.Package>,
    override val cfgOptions: CfgOptions,
    override val cargoConfig: CjpmConfig,
    val featuresState: Map<PackageRoot, Map<FeatureName, FeatureState>>
) : CjpmWorkspace {
    companion object {
        fun deserialize(
            manifestPath: Path,
            data: CjpmWorkspaceData,
            cfgOptions: CfgOptions,
            cargoConfig: CjpmConfig,
        ): WorkspaceImpl {

            val result = WorkspaceImpl(
                manifestPath,
                data.workspaceRootUrl,
                data.packages,
                cfgOptions,
                cargoConfig,
                emptyMap()
            )

            run {
                val idToPackage = result.packages.associateBy { it.id }
                idToPackage.forEach { (_, pkg) -> pkg.addDependencies(data, idToPackage) }
            }

            return result
        }
    }

    override val workspaceRoot: VirtualFile? by CachedVirtualFile(workspaceRootUrl)
    override val packages: List<PackageImpl> = packagesData.map { pkg ->
        PackageImpl(
            this,
            pkg.id,
            pkg.contentRootUrl,
            pkg.name,
            pkg.version,
            pkg.targets,
            pkg.source,
            pkg.origin,
            pkg.edition,
            pkg.cfgOptions,
            pkg.features,
            pkg.enabledFeatures,
            pkg.env,
            pkg.outDirUrl,

            )
    }

    val targetByCrateRootUrl = packages.flatMap { it.targets }.associateBy { it.crateRootUrl }

    override fun findTargetByCrateRoot(root: VirtualFile): CjpmWorkspace.Target? =
        root.applyWithSymlink { targetByCrateRootUrl[it.url] }


}

private fun PackageImpl.addDependencies(workspaceData: CjpmWorkspaceData, packagesMap: Map<PackageId, PackageImpl>) {
    val pkgDeps = workspaceData.dependencies[id].orEmpty()

    dependencies += pkgDeps.mapNotNull { dep ->
        val dependencyPackage = packagesMap[dep.id] ?: return@mapNotNull null

        val depTargetName = dependencyPackage.libTarget?.normName ?: dependencyPackage.normName
        val depName = dep.name ?: depTargetName
        val rename = if (depName != depTargetName) depName else null



        DependencyImpl(
            dependencyPackage,
            depName,
            dep.depKinds,


        )
    }
}

private class PackageImpl(
    override val workspace: WorkspaceImpl,
    override val id: PackageId,

    val contentRootUrl: String,
    override val name: String,
    override val version: String,
    targetsData: Collection<CjpmWorkspaceData.Target>,
    override val source: String?,
    override var origin: PackageOrigin,
    override val edition: CjpmWorkspace.Edition,
    override val cfgOptions: CfgOptions?,
     val rawFeatures: Map<FeatureName, List<FeatureDep>>,
    val cargoEnabledFeatures: Set<FeatureName>,
    override val env: Map<String, String>,
    val outDirUrl: String?,

    ) : UserDataHolderBase(), CjpmWorkspace.Package {
    override val contentRoot: VirtualFile? by CachedVirtualFile(contentRootUrl)

    override val rootDirectory: Path
        get() = Paths.get(VirtualFileManager.extractPath(contentRootUrl))

    override val targets = targetsData.map {
        TargetImpl(
            this,
            crateRootUrl = it.crateRootUrl,
            name = it.name,
            kind = it.kind,
            edition = it.edition,
            doctest = it.doctest,
            requiredFeatures = it.requiredFeatures
        )
    }

    override val dependencies: MutableList<DependencyImpl> = ArrayList()

    override val outDir: VirtualFile? by CachedVirtualFile(outDirUrl)
    override val featureState: Map<FeatureName, FeatureState>
        get() = workspace.featuresState[rootDirectory] ?: emptyMap()

}
private class TargetImpl(
    override val pkg: PackageImpl,
    val crateRootUrl: String,
    override val name: String,
    override val kind: CjpmWorkspace.TargetKind,
    override val edition: CjpmWorkspace.Edition,
    override val doctest: Boolean,
    override val requiredFeatures: List<FeatureName>
) : CjpmWorkspace.Target {

    override val crateRoot: VirtualFile? by CachedVirtualFile(crateRootUrl)

    override val cfgOptions: CfgOptions = pkg.workspace.cfgOptions + (pkg.cfgOptions ?: CfgOptions.EMPTY) +
             if (kind.isProcMacro) CfgOptions(emptyMap(), setOf("proc_macro")) else CfgOptions.EMPTY

    override fun toString(): String = "Target(name='$name', kind=$kind, crateRootUrl='$crateRootUrl')"
}
private class DependencyImpl(
    override val pkg: PackageImpl,
    override val name: String = pkg.libTarget?.normName ?: pkg.normName,
    override val depKinds: List<CjpmWorkspace.DepKindInfo>,
    val isOptional: Boolean = false,
    val areDefaultFeaturesEnabled: Boolean = true,
    override val requiredFeatures: Set<String> = emptySet(),
    override val cargoFeatureDependencyPackageName: String = if (name == pkg.libTarget?.normName) pkg.name else name
) : CjpmWorkspace.Dependency {

    fun withPackage(newPkg: PackageImpl): DependencyImpl =
        DependencyImpl(
            newPkg,
            name,
            depKinds,
            isOptional,
            areDefaultFeaturesEnabled,
            requiredFeatures,
            cargoFeatureDependencyPackageName
        )

    override fun toString(): String = name
}
