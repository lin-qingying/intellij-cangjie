package cn.cangnova.cangjie.buildsystem.impl.cjpm.project.workspace
import java.nio.file.Path


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
        val version: String,


        val origin: PackageOrigin,


//        val enabledFeatures: Set<FeatureName>,

//        val env: Map<String, String>,
//        val outDirUrl: String?,

        )

//
}
