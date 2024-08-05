import com.linqingying.cangjie.cjpm.project.workspace.PackageOrigin
import java.nio.file.Path

//package com.linqingying.cangjie.cjpm.project.workspace
//
//import com.linqingying.cangjie.cjpm.toolchain.impl.CjpmMetadata
//import java.nio.file.Path
//
//
typealias FeatureName = String
typealias PackageId = String
typealias PackageRoot = Path

data class CjpmWorkspaceData(
    val packages: List<Package>,

    val workspaceRootUrl: String? = null
) {
    //
//
//
    data class Package(
//        val id: PackageId,
        val contentRootUrl: String,
        val name: String,
        val version: String?,


        val origin: PackageOrigin,


//        val enabledFeatures: Set<FeatureName>,

//        val env: Map<String, String>,
//        val outDirUrl: String?,

        )

//
}
