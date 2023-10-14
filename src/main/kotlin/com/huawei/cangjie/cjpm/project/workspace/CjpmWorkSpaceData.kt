package com.huawei.cangjie.cjpm.project.workspace

import com.huawei.cangjie.cjpm.CfgOptions
import com.huawei.cangjie.stdext.HashCode
import java.nio.file.Path


typealias PackageId = String

 typealias PackageRoot = Path

 typealias FeatureName = String

typealias FeatureDep = String
data class CjpmWorkspaceData(
    val packages: List<Package>,
      val dependencies: Map<PackageId, Set<Dependency>>,

    val workspaceRootUrl: String? = null
) {

    data class Dependency(
        val id: PackageId,
        val name: String? = null,
        val depKinds: List<CjpmWorkspace.DepKindInfo> = listOf(CjpmWorkspace.DepKindInfo(CjpmWorkspace.DepKind.Unclassified))
    )
    data class ProcMacroArtifact(
        val path: Path,
        val hash: HashCode
    )
    data class Target(
        val crateRootUrl: String,
        val name: String,
        val kind: CjpmWorkspace.TargetKind,
        val edition: CjpmWorkspace.Edition,
        val doctest: Boolean,
        val requiredFeatures: List<FeatureName>
    )
    data class Package(
        val id: PackageId,
        val contentRootUrl: String,
        val name: String,
        val version: String,
        val targets: Collection<Target>,
        val source: String?,
        val origin: PackageOrigin,
        val edition: CjpmWorkspace.Edition,
         val features: Map<FeatureName, List<FeatureDep>>,
         val enabledFeatures: Set<FeatureName>,
        val cfgOptions: CfgOptions?,
        val env: Map<String, String>,
        val outDirUrl: String?,

    )

}
