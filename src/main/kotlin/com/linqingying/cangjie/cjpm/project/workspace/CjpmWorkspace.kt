package com.linqingying.cangjie.cjpm.project.workspace

import CjpmWorkspaceData
import com.fasterxml.jackson.core.JacksonException
import com.linqingying.cangjie.cjpm.CjpmConstants
import com.linqingying.cangjie.cjpm.project.model.CjpmProjectInfo
import com.linqingying.cangjie.cjpm.project.model.Require
import com.linqingying.cangjie.cjpm.project.model.impl.CachedVirtualFile
import com.linqingying.cangjie.cjpm.project.pathAsPath
import com.linqingying.cangjie.cjpm.resolve
import com.intellij.openapi.util.UserDataHolderBase
import com.intellij.openapi.util.UserDataHolderEx
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.exists


//class CachedVirtualFile(private val url: String?) {
//    private val cache = AtomicReference<VirtualFile>()
//
//    operator fun getValue(thisRef: Any?, property: KProperty<*>): VirtualFile? {
//        if (url == null) return null
//        val cached = cache.get()
//        if (cached != null && cached.isValid) return cached
//        val file = VirtualFileManager.getInstance().findFileByUrl(url)
//        cache.set(file)
//        return file
//    }
//}

interface CjpmWorkspace {
    val manifestPath: Path
    val contentRoot: Path get() = manifestPath.parent


    //
    val workspaceRoot: VirtualFile?
//
//    fun withDisabledFeatures(userDisabledFeatures: UserDisabledFeatures): CjpmWorkspace


    val packages: Collection<Package>
//    fun withStdlib(stdlib: StandardLibrary,  rustcInfo: CjcInfo? = null): CjpmWorkspace


    //    cjpm module.json数据
    val moduleData: CjpmProjectInfo?

    /**
     * 该接口表示一个cjpm包  一个项目会有N个cjpm包
     */
    interface Package : UserDataHolderEx {

        val contentRoot: VirtualFile?
        val rootDirectory: Path
        val name: String
//        val features: Set<PackageFeature>

        val workspace: CjpmWorkspace


        val moduleData: CjpmProjectInfo?
        val version: String
        val outDir: VirtualFile?
        val origin: PackageOrigin


//        fun getDependencies(): List<Package>

    }

    companion object {
        fun deserialize(
            manifestPath: Path,
            data: CjpmWorkspaceData,
        ): CjpmWorkspace =
            WorkspaceImpl.deserialize(manifestPath, data)
    }
}

class PackageImpl(
    override val workspace: WorkspaceImpl,
    val contentRootUrl: String,
    override val name: String,
    override val version: String,

    override var origin: PackageOrigin,
    val outDirUrl: String? = null,

    override var moduleData: CjpmProjectInfo? = null

) : UserDataHolderBase(), CjpmWorkspace.Package {
    override fun toString() = "Package(name='$name', contentRootUrl='$contentRootUrl', version='$version')"

    override val contentRoot: VirtualFile? by CachedVirtualFile(contentRootUrl)
    val manifestPath: Path? = contentRoot?.pathAsPath?.resolve(CjpmConstants.MANIFEST_FILE)

//TODO 为每个包的module.json编制数据 时间占用过于庞大，目前用不到，所以就先不读了
//    init {
//        moduleData = if (manifestPath?.exists() == true) {
//
//            val json = manifestPath.toFile().readText()
//            try {
//                Cjpm.JSON_MAPPER.readValue(json, CjpmProjectInfo::class.java)
//            } catch (e: JacksonException) {
//                throw e
//                println(e)
//                null
//            }
//        } else {
//            null
//        }
//    }

    fun Require.contentRoot(): VirtualFile? =
        if (path != null) {
            LocalFileSystem.getInstance().findFileByPath(path)
        } else {
            workspace.packages.find { `package` ->
                name == `package`.name
            }?.contentRoot
        }


    override val rootDirectory: Path
        get() = Paths.get(VirtualFileManager.extractPath(contentRootUrl))

    override val outDir: VirtualFile? by CachedVirtualFile(outDirUrl)


}

class WorkspaceImpl(
    override val manifestPath: Path,
    val workspaceRootUrl: String?,
    packagesData: Collection<CjpmWorkspaceData.Package>,
) : CjpmWorkspace {


    override val workspaceRoot: VirtualFile? by CachedVirtualFile(workspaceRootUrl)
//    override fun withDisabledFeatures(userDisabledFeatures: UserDisabledFeatures): CjpmWorkspace {
//        val featuresState = inferFeatureState(userDisabledFeatures).associateByPackageRoot()
//
//        return WorkspaceImpl(
//            manifestPath,
//            workspaceRootUrl,
//            packages.map { it.asPackageData() },
//
//
//            featuresState
//        )
//    }


    override val moduleData: CjpmProjectInfo? = if (manifestPath.exists()) {

        try {
//            Cjpm.JSON_MAPPER.readValue(json, CjpmProjectInfo::class.java)
            CjpmProjectInfo.deserialize(manifestPath )
        } catch (e: JacksonException) {
            throw e
            println(e)
            null
        }
    } else {
        null
    }


    override val packages: Collection<PackageImpl> = packagesData.map {

        PackageImpl(
            this,
            it.contentRootUrl,
            it.name,
            it.version,
            it.origin,
        )

    }.toMutableList().apply {
        add(
            PackageImpl(
                this@WorkspaceImpl,
                this@WorkspaceImpl.workspaceRootUrl ?: "",
                moduleData?.name ?: "",
                moduleData?.version ?: "",
                PackageOrigin.WORKSPACE,

                moduleData = moduleData

            )
        )
    }
//    override val moduleData: CjpmProjectInfo? = ApplicationManager.getApplication().executeOnPooledThread<CjpmProjectInfo> {
//
//        manifestPath
//        val json = manifestPath.toFile().readText()
//
//        null
//    }.get()


//    override fun withStdlib(stdlib: StandardLibrary, rustcInfo: CjcInfo?): CjpmWorkspace {
//
//        val (newPackagesData, @Suppress("NAME_SHADOWING") stdlib) = if (!stdlib.isPartOfCargoProject) {
//            Pair(
//                packages.map { it.asPackageData() } + stdlib.asPackageData(rustcInfo),
//                stdlib
//            )
//        } else {
//            // In the case of https://github.com/rust-lang/rust project, stdlib
//            // is already a part of the project, so no need to add extra packages.
//            val oldPackagesData = packages.map { it.asPackageData() }
//            val stdCratePackageRoots = stdlib.crates.mapToSet { it.contentRootUrl }
//            val (stdPackagesData, otherPackagesData) = oldPackagesData.partition { it.contentRootUrl in stdCratePackageRoots }
//            val stdPackagesByPackageRoot = stdPackagesData.associateBy { it.contentRootUrl }
//            val pkgIdMapping = stdlib.crates.associate {
//                it.id to (stdPackagesByPackageRoot[it.contentRootUrl]?.id ?: it.id)
//            }
//            val newStdlibCrates = stdlib.crates.map { it.copy(id = pkgIdMapping.getValue(it.id)) }
//            val newStdlibDependencies = stdlib.workspaceData.dependencies.map { (oldId, dependency) ->
//                val newDependencies = dependency.mapToSet { it.copy(id = pkgIdMapping.getValue(it.id)) }
//                pkgIdMapping.getValue(oldId) to newDependencies
//            }.toMap()
//
//            Pair(
//                otherPackagesData + stdPackagesData.map { it.copy(origin = STDLIB) },
//                stdlib.copy(
//                    workspaceData = stdlib.workspaceData.copy(
//                        packages = newStdlibCrates,
//                        dependencies = newStdlibDependencies
//                    )
//                )
//            )
//        }
//
//        val stdAll = stdlib.crates.associateBy { it.id }
//        val stdInternalDeps = stdlib.crates.filter { it.origin == STDLIB_DEPENDENCY }.mapToSet { it.id }
//
//
//        val result = WorkspaceImpl(
//            manifestPath,
//            workspaceRootUrl,
//            newPackagesData,
//
//        )
//
//        run {
//            val oldIdToPackage = packages.associateBy { it.id }
//            val newIdToPackage = result.packages.associateBy { it.id }
//            val stdlibDependencies = result.packages.filter { it.origin == STDLIB }
//                .map { DependencyImpl(it, depKinds = listOf(CargoWorkspace.DepKindInfo(CargoWorkspace.DepKind.Stdlib))) }
//            newIdToPackage.forEach { (id, pkg) ->
//                val stdCrate = stdAll[id]
//                if (stdCrate == null) {
//                    pkg.dependencies.addAll(oldIdToPackage[id]?.dependencies.orEmpty().mapNotNull { dep ->
//                        val dependencyPackage = newIdToPackage[dep.pkg.id] ?: return@mapNotNull null
//                        dep.withPackage(dependencyPackage)
//                    })
//                    val explicitDeps = pkg.dependencies.map { it.name }.toSet()
//                    pkg.dependencies.addAll(stdlibDependencies.filter { it.name !in explicitDeps && it.pkg.id !in stdInternalDeps })
//                } else {
//                    // `pkg` is a crate from stdlib
//                    pkg.addDependencies(stdlib.workspaceData, newIdToPackage)
//                }
//            }
//        }
//        return result
//    }


    companion object {
        fun deserialize(
            manifestPath: Path,
            data: CjpmWorkspaceData,

            ): WorkspaceImpl {
            val result = WorkspaceImpl(
                manifestPath,
                data.workspaceRootUrl,
                data.packages,


                )

//            run {
//                val idToPackage = result.packages.associateBy { it.id }
//                idToPackage.forEach { (_, pkg) -> pkg.addDependencies(data, idToPackage) }
//            }
            return result

        }
    }
}


private fun PackageImpl.asPackageData(): CjpmWorkspaceData.Package =
    CjpmWorkspaceData.Package(

        contentRootUrl = contentRootUrl,
        name = name,
        version = version,
        origin = origin,


        )

/**
 * A way to add additional (indexable) source roots for a package.
 * These hacks are needed for the stdlib that has a weird source structure.
 */
fun CjpmWorkspace.Package.additionalRoots(): List<VirtualFile> {
    return emptyList()
//    return if (origin == PackageOrigin.STDLIB) {
//        when (name) {
//            STD -> listOfNotNull(contentRoot?.parent?.findFileByRelativePath("backtrace"))
//            CORE -> contentRoot?.parent?.let {
//                listOfNotNull(
//                    it.findFileByRelativePath("stdarch/crates/core_arch"),
//                    it.findFileByRelativePath("stdarch/crates/std_detect"),
//                    it.findFileByRelativePath("portable-simd/crates/core_simd"),
//                    it.findFileByRelativePath("portable-simd/crates/std_float"),
//                )
//            } ?: emptyList()
//            else -> emptyList()
//        }
//    } else {
//        emptyList()
//    }
}
