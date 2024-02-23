package com.huawei.cangjie.cjpm.toolchain.impl

import CjpmWorkspaceData
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import com.huawei.cangjie.cjpm.CjpmConstants
import com.huawei.cangjie.cjpm.project.workspace.PackageOrigin
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.util.PathUtil
import com.intellij.util.io.systemIndependentPath
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.exists

class CjpmMetadataException(message: String) : IllegalStateException(message)

typealias PathConverter = (String) -> String

//
object CjpmMetadata {
    private fun Package.clean(
        fs: LocalFileSystem,
        isWorkspaceMember: Boolean,


        ): CjpmWorkspaceData.Package {
        val rootPath = manifest_path.let { PathUtil.getParentPath(it) }

        val root = fs.refreshAndFindFileByPath(rootPath)
            ?.let { if (isWorkspaceMember) it else it.canonicalFile }
            ?: throw CjpmMetadataException("`cjpm metadata` reported a package which does not exist at `$manifest_path`")

        return CjpmWorkspaceData.Package(
            root.url, name, version ?: "",
            origin = if (isWorkspaceMember) PackageOrigin.WORKSPACE else PackageOrigin.DEPENDENCY,

        )

    }

    fun clean(
        project: Project,

        ): CjpmWorkspaceData {
        val fs = LocalFileSystem.getInstance()
        val workspaceRoot = project.workspace_root?.let { fs.refreshAndFindFileByPath(it) }
        requireNotNull(workspaceRoot) { "`cjpm metadata` reported a workspace path which does not exist at `${project.workspace_root}`" }
        val packages = project.packages.map { pkg ->
            pkg.clean(fs,false)

        }
        return CjpmWorkspaceData(
            packages,


            workspaceRoot.url
        )

    }

    data class Project(
        @JsonProperty("requires")
        @JsonDeserialize(using = PackageListDeserializer::class)
        val packages: List<Package>,

        val version: Int,


        val workspace_root: String? = null

    ) {
        fun convertPaths(workspace_root: Path, converter: PathConverter): Project = copy(
//            packages = packages.map { it.convertPaths(converter) },
            workspace_root = converter(workspace_root.systemIndependentPath)
        )


    }


    data class Package(
        val name: String,

//        可选的
        val organization: String? = null,

//        可选的
        val version: String? = null,

        //        本地目录
        val path: String? = null,
//        git 标签
        val tag: String? = null,
//        git 提交hash值
        val commitId: String? = null,
//远程仓库
        val git: String? = null,
//        git 分支
        val branch: String? = null,


        ) {

        val manifest_path: String = if (path != null) {
            Paths.get(path).resolve(CjpmConstants.MANIFEST_FILE).systemIndependentPath
        } else if (git != null && commitId != null) {
            val localPath = Paths.get(System.getProperty("user.home")).resolve("AppData").resolve("Local")
            val cjpmPath = localPath.resolve(".cjpm")
         var cjpmPathString = ""
            if (cjpmPath.exists()) {
                val gitPath = cjpmPath.resolve("git")
                cjpmPathString =  gitPath.resolve(name).resolve(commitId).resolve(CjpmConstants.MANIFEST_FILE).systemIndependentPath
            }
            cjpmPathString
        } else {
            ""
        }

//        fun convertPaths(converter: PathConverter): Package = copy(
//            manifest_path = converter(manifest_path),
//
//        )
    }


    private class PackageListDeserializer : StdDeserializer<List<Package>>(List::class.java) {
        override fun deserialize(p: JsonParser?, ctxt: DeserializationContext?): List<Package> {
            val packages = mutableListOf<Package>()
            val node: JsonNode = p!!.codec.readTree(p)
            node.fields().forEach { (key, value) ->
                val packageNode = value as JsonNode
                val packageObject = Package(
                    key,
                    organization = packageNode.get("organization")?.asText(),

                    version = packageNode.get("version")?.asText(),
                    git = packageNode.get("git")?.asText(),
                    commitId = packageNode.get("commitId")?.asText(),
                    branch = packageNode.get("branch")?.asText(),
                    path = packageNode.get("path")?.asText(),
                    tag = packageNode.get("tag")?.asText()

                )
                packages.add(packageObject)
            }
            return packages
        }
    }
//        {
//            fun convertPaths(converter: PathConverter): Package = copy(
//                manifest_path = converter(manifest_path),
//                targets = targets.map { it.convertPaths(converter) }
//            )
//        }
//
//        data class RawDependency(
//
//            val name: String,
//
//            val rename: String?,
//            val kind: String?,
//            val target: String?,
//            val optional: Boolean,
//            val uses_default_features: Boolean,
//            val features: List<String>
//        )
//
//
//        data class Target(
//
//            val kind: List<String>,
//
//
//            val name: String,
//
//
//            val src_path: String,
//
//
//            val crate_types: List<String>,
//
//
//            val edition: String?,
//
//
//            val doctest: Boolean?,
//
//            @JsonProperty("required-features")
//            val required_features: List<String>?
//        ) {
//            val cleanKind: TargetKind
//                get() = when (kind.singleOrNull()) {
//                    "bin" -> TargetKind.BIN
//                    "example" -> TargetKind.EXAMPLE
//                    "test" -> TargetKind.TEST
//                    "bench" -> TargetKind.BENCH
//                    "proc-macro" -> TargetKind.LIB
//                    "custom-build" -> TargetKind.CUSTOM_BUILD
//                    else ->
//                        if (kind.any { it.endsWith("lib") })
//                            TargetKind.LIB
//                        else
//                            TargetKind.UNKNOWN
//                }
//
//            val cleanCrateTypes: List<CrateType>
//                get() = crate_types.map {
//                    when (it) {
//                        "bin" -> CrateType.BIN
//                        "lib" -> CrateType.LIB
//                        "dylib" -> CrateType.DYLIB
//                        "staticlib" -> CrateType.STATICLIB
//                        "cdylib" -> CrateType.CDYLIB
//                        "rlib" -> CrateType.RLIB
//                        "proc-macro" -> CrateType.PROC_MACRO
//                        else -> CrateType.UNKNOWN
//                    }
//                }
//
//            fun convertPaths(converter: PathConverter): Target = copy(
//                src_path = converter(src_path)
//            )
//        }
//
//        enum class TargetKind {
//            LIB, BIN, TEST, EXAMPLE, BENCH, CUSTOM_BUILD, UNKNOWN
//        }
//
//
//        enum class CrateType {
//            BIN, LIB, DYLIB, STATICLIB, CDYLIB, RLIB, PROC_MACRO, UNKNOWN
//        }
//
//        data class Resolve(
//            val nodes: List<ResolveNode>
//        )
//
//
//        data class ResolveNode(
//            val id: PackageId,
//
//
//            val dependencies: List<PackageId>,
//
//
//            val deps: List<Dep>?,
//
//            val features: List<String>?
//        )
//
//        data class Dep(
//
//            val pkg: PackageId,
//
//
//            val name: String?,
//
//
//            @Suppress("KDocUnresolvedReference")
//            val dep_kinds: List<DepKindInfo>?
//        )
//
//        data class DepKindInfo(
//            val kind: String?,
//            val target: String?
//        )
//
//
//        private val DYNAMIC_LIBRARY_EXTENSIONS: List<String> = listOf(".dll", ".so", ".dylib")
//
//
//        fun Project.replacePaths(replacer: (String) -> String): Project =
//            copy(
//                packages = packages.map { it.replacePaths(replacer) },
//                workspace_root = replacer(workspace_root)
//            )
//
//        private fun Package.replacePaths(replacer: (String) -> String): Package =
//            copy(
//                manifest_path = replacer(manifest_path),
//                targets = targets.map { it.replacePaths(replacer) }
//            )
//
//        private fun Target.replacePaths(replacer: (String) -> String): Target =
//            copy(src_path = replacer(src_path))
}