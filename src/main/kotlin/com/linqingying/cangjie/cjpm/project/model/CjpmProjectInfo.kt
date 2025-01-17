/*
 * Copyright 2024 LinQingYing. and contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * The use of this source code is governed by the Apache License 2.0,
 * which allows users to freely use, modify, and distribute the code,
 * provided they adhere to the terms of the license.
 *
 * The software is provided "as-is", and the authors are not responsible for
 * any damages or issues arising from its use.
 *
 */

package com.linqingying.cangjie.cjpm.project.model

//import com.akuleshov7.ktoml.file.TomlFileReader
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import com.fasterxml.jackson.databind.node.TextNode
import com.linqingying.cangjie.cjpm.project.model.toml.PackageConfigurationInfo
import com.linqingying.cangjie.cjpm.toolchain.tools.Cjpm
import com.moandjiezana.toml.Toml
import java.nio.file.Path
import java.util.*


/**
 * cjpm项目信息，该类存储module.json原始数据
 */
//@JsonDeserialize(using = CjpmProjectInfo.CjpmProjectInfoDeserializer::class)
//@JsonInclude(JsonInclude.Include.NON_NULL)
data class CjpmProjectInfo(

    @JsonProperty("cjc_version")
    private val cjc_version1: String? = null,

    @JsonProperty("cjc-version")
    private val cjc_version2: String? = null,
    @JsonProperty("organization")
    val organization: String? = null,
    @JsonProperty("name")
    val name: String,
    @JsonProperty("version")
    val version: String,
    @JsonProperty("description")
    val description: String? = null,
//    仓颉中心仓库依赖
    @JsonProperty("dependencies")
    @JsonDeserialize(using = Dependencie.ListDeserializer::class)
    private   var dependencies1: List<Dependencie>? = emptyList(),
    @JsonProperty("dev-dependencies")
    @JsonDeserialize(using = Require.ListDeserializer::class)
    private   var devDependencies1: List<Dependencie>? = emptyList(),

//     依赖模块信息配置项，非必须
    @JsonProperty("requires")
    @JsonDeserialize(using = Require.ListDeserializer::class)
    private val requires: List<Dependencie>? = emptyList(),


//    用于指定仅在开发过程中使用的依赖项，非必须
    @JsonProperty("dev_requires")
    @JsonDeserialize(using = Require.ListDeserializer::class)
    private val devRequires: List<Dependencie>? = emptyList(),


//    依赖的仓颉package， 非必须
    @JsonProperty("package_requires")
    @JsonDeserialize(using = Require.ListDeserializer::class)
    private val package_requires1: List<PackageRequires>? = emptyList(),
    @JsonProperty("package-requires")
    @JsonDeserialize(using = Require.ListDeserializer::class)
    private val package_requires2: List<PackageRequires>? = emptyList(),
//    外部调用 c 库的依赖项，非必须
    @JsonProperty("foreign_requires")
    @JsonDeserialize(using = ForeignRequires.ListDeserializer::class)
    private val foreign_requires1: List<ForeignRequires>? = emptyList(),
    @JsonProperty("ffi")
    @JsonDeserialize(using = ForeignRequires.ListDeserializer1::class)
    private val foreign_requires2: List<ForeignRequires>? = emptyList(),


//项目类型 二进制执行程序，静态库，动态库
    @JsonProperty("output_type")
    @JsonDeserialize(using = OutPutType.EnumDeserializer::class)
    private val output_type1: OutPutType? = null,
    @JsonProperty("output-type")
    @JsonDeserialize(using = OutPutType.EnumDeserializer::class)
    private val output_type2: OutPutType? = null,


//    额外编译命令选项，非必须
    @JsonProperty("command_option")
    private val command_option1: String? = null,
    @JsonProperty("command-option")
    private val command_option2: String? = null,

//    传给链接器的编译选项，可用于透传安全编译命令，如下所示。注意，这里配置的命令在编译时只会自动透传给动态库和可执行文件对应的包。
    @JsonProperty("link_option")
    private val link_option1: String? = null,
    @JsonProperty("link-option")
    private val link_option2: String? = null,

//    按照条件选项透传给 cjc 的命令
    @JsonProperty("condition_option")
    private   val condition_option1: Map<String, String>? = mapOf(),
    @JsonProperty("condition-option")
    private val condition_option2: Map<String, String>? = mapOf(),

//    单包配置选项，非必须
    @JsonProperty("package_configuration")
    @JsonDeserialize(using = PackageConfiguration.ListDeserializer::class)
    private val package_configuration1: List<PackageConfiguration>? = emptyList(),
    @JsonProperty("package-configuration")
    @JsonDeserialize(using = PackageConfiguration.ListDeserializer::class)
    private val package_configuration2: List<PackageConfiguration>? = emptyList(),


//    交叉编译到目标平台所需的配置项
    @JsonProperty("cross_compile_configuration")
    private val cross_compile_configuration1: Map<String, String>? = mapOf(),
    @JsonProperty("cross_compile-configuration")
    private val cross_compile_configuration2: Map<String, String>? = mapOf(),


//    指定编译产物的存放路径
    @JsonProperty("build_dir")
    val build_dir1: String? = null,
    @JsonProperty("build-dir")
    val build_dir2: String? = null,

//    指定源码的存放路径 不指定默认为src
    @JsonProperty("src_dir")
    val src_dir1: String? = null,
    @JsonProperty("src-dir")
    val src_dir2: String? = null,

//    配置脚本内容
    @JsonProperty("scripts")
    val script: Map<String, String>? = mapOf(),

//    使能且指定 LTO （Link Time Optimization 链接时优化）优化编译模式。指定该字段后，对该模块及依赖的所有上游模块均生效。
    @JsonProperty("lto")
    val lto: String? = null,


    ) {
    val cjcVersion: String

    var commandOption: String? = null
    var packageConfiguration: List<PackageConfiguration> = emptyList()
    val outputType: OutPutType
    var crossCompileConfiguration: Map<String, String> = mapOf()
    var dependencies: MutableList<Dependencie> = mutableListOf()
    var sourceDir: String
    var linkOption: String?
    var foreignRequires: List<ForeignRequires> = emptyList()
    var buildDir: String
    var packageRequires: List<PackageRequires> = emptyList()
    var conditionOption: Map<String, String> = mapOf()
    var devDependencies: List<Dependencie> = emptyList()

    init {
        cjcVersion = cjc_version1 ?: cjc_version2 ?: ""
        commandOption = command_option1 ?: command_option2

        if (package_requires1 != null) {
            packageRequires += package_requires1
        }
        if (package_requires2 != null) {
            packageRequires += package_requires2

        }
        //  packageConfiguration = package_configuration1 + package_configuration2
        outputType = output_type1 ?: output_type2 ?: OutPutType.EXECUTABLE

        if (package_configuration1 != null) {
            packageConfiguration += package_configuration1
        }
        if (package_configuration2 != null) {
            packageConfiguration += package_configuration2
        }
        if (cross_compile_configuration1 != null) {
            crossCompileConfiguration += cross_compile_configuration1
        }
//        crossCompileConfiguration = cross_compile_configuration1 + cross_compile_configuration2
        if (cross_compile_configuration2 != null) {
            crossCompileConfiguration += cross_compile_configuration2
        }
        sourceDir = src_dir1 ?: src_dir2 ?: "src"
        buildDir = build_dir1 ?: build_dir2 ?: "build"

        linkOption = link_option1 ?: link_option2

        if (package_requires1 != null) {
            packageRequires += package_requires1
        }
        if (package_requires2 != null) {
            packageRequires += package_requires2
        }


        // packageRequires = package_requires1?.plus(package_requires2) ?: emptyList<PackageRequires>()
        if (foreign_requires1 != null) {
            foreignRequires += foreign_requires1
        }
        if (foreign_requires2 != null) {
            foreignRequires += foreign_requires2

        }
        //    foreignRequires = foreign_requires1?.plus(foreign_requires2) ?: emptyList()

        if (condition_option1 != null) {
            conditionOption += condition_option1
        }
        if (condition_option2 != null) {
            conditionOption += condition_option2
        }

        if (dependencies1 != null) {
            dependencies.addAll(dependencies1!!)
        }
        if (requires != null) {
            dependencies .addAll(requires)
        }
        //  dependencies += requires
        // devDependencies = devDependencies?.plus(devRequires

        if (devDependencies1 != null) {
            devDependencies += devDependencies1!!
        }
        if (devRequires != null) {
            devDependencies += devRequires
        }

    }
//    class CjpmProjectInfoDeserializer : StdDeserializer<CjpmProjectInfo>(CjpmProjectInfo::class.java) {
//        override fun deserialize(p0: JsonParser?, p1: DeserializationContext?): CjpmProjectInfo {
//            val node: JsonNode = p0!!.codec.readTree(p0)
//
//
//            TODO()
//        }
//    }

    companion object {
        fun deserialize(filePath: Path): CjpmProjectInfo {

            val file = filePath.toFile()

            val data = file.readText()




            return if (filePath.toFile().name == "cjpm.toml") {


                val toml: Toml = Toml().read(data)
                val map = mutableMapOf<String, Any>()

                toml.toMap().forEach {
                    if (it.key == "package") {
                        val packageNode = it.value as Map<String, Any>

                        packageNode.forEach { key, value ->
                            map[key] = value
                        }
                    } else {
                        map[it.key] = it.value
                    }
                }


//                将package移动到顶层


                val jsonString = Cjpm.JSON_MAPPER.writeValueAsString(map)

                Cjpm.JSON_MAPPER.readValue(jsonString, CjpmProjectInfo::class.java)


            } else {
                Cjpm.JSON_MAPPER.readValue(data, CjpmProjectInfo::class.java)
            }


        }
    }
}
data class PackageConfigurationInfo(
    @JsonProperty("cjc-version")
    val cjcVersion: String?,
    @JsonProperty("name")
    val name: String?,
    @JsonProperty("description")
    val description: String?,
    @JsonProperty("version")
    val version: String?,
    @JsonProperty("compile-option")
    val compileOption: String?,
    @JsonProperty("override-compile-option")
    val overrideCompileOption: String?,
    @JsonProperty("link-option")
    val linkOption: String?,
    @JsonProperty("output-type")
    val outputType: String?,
    @JsonProperty("src-dir")
    val srcDir: String?,
    @JsonProperty("target-dir")
    val targetDir: String?,
)

data class PackageConfiguration(
    val packageName: String,
    val configuration: PackageConfigurationInfo
) {
    class ListDeserializer : StdDeserializer<List<PackageConfiguration>>(List::class.java) {
        override fun deserialize(p0: JsonParser?, p1: DeserializationContext?): List<PackageConfiguration> {
            val packageConfiguration = mutableListOf<PackageConfiguration>()
            val node: JsonNode = p0!!.codec.readTree(p0)
            node.fields().forEach { (key, value) ->
                val packageNode = value as JsonNode
                val packageObject = PackageConfiguration(
                    packageName = key,
                    configuration = Cjpm.JSON_MAPPER.convertValue(packageNode, PackageConfigurationInfo::class.java)

                )
                packageConfiguration.add(packageObject)
            }
            return packageConfiguration
        }
    }
}

enum class OutPutType {
    EXECUTABLE,
    STATIC,
    DYNAMIC;

    override fun toString(): String {
//         转换为小写
        return this.name.lowercase(Locale.getDefault())
    }

    class EnumDeserializer : StdDeserializer<OutPutType>(OutPutType::class.java) {
        override fun deserialize(p0: JsonParser?, p1: DeserializationContext?): OutPutType {
            val node: JsonNode = p0!!.codec.readTree(p0)
            return OutPutType.valueOf(node.asText().uppercase(Locale.getDefault()))
        }
    }
}


data class PackageRequires(
    @JsonProperty("package_option")
    @JsonDeserialize(using = PackageOption.ListDeserializer::class)
    val packageOption: List<PackageOption> = emptyList(),


    @JsonProperty("path_option")
    val pathOption: List<String> = emptyList(),
) {


    data class PackageOption(
        val name: String,
        val path: String
    ) {
        class ListDeserializer : StdDeserializer<List<PackageOption>>(List::class.java) {
            override fun deserialize(p0: JsonParser?, p1: DeserializationContext?): List<PackageOption> {
                val packageOption = mutableListOf<PackageOption>()
                val node: JsonNode = p0!!.codec.readTree(p0)
                node.fields().forEach { (key, value) ->
                    val packageNode = value as JsonNode
                    val packageObject = PackageOption(
                        key,
                        value.asText()
                    )
                    packageOption.add(packageObject)
                }
                return packageOption
            }

        }
    }
}


data class ForeignRequires(
    val name: String,
    val path: String,
    val export: List<String> = emptyList(),
) {

    class ListDeserializer1 : StdDeserializer<List<ForeignRequires>>(List::class.java) {
        override fun deserialize(p0: JsonParser?, p1: DeserializationContext?): List<ForeignRequires> {
            val foreignRequires = mutableListOf<ForeignRequires>()
            val node: JsonNode = p0!!.codec.readTree<JsonNode?>(p0).get("c")
            node.fields().forEach { (key, value) ->
                val packageNode = value as JsonNode
                val packageObject = ForeignRequires(
                    key,
                    path = packageNode.get("path")?.asText() ?: "",
                    export = packageNode.get("export")?.asText()?.split(",")?.map { it.trim() }?.toList() ?: emptyList()
                )
                foreignRequires.add(packageObject)
            }
            return foreignRequires
        }
    }

    class ListDeserializer : StdDeserializer<List<ForeignRequires>>(List::class.java) {
        override fun deserialize(p0: JsonParser?, p1: DeserializationContext?): List<ForeignRequires> {
            val foreignRequires = mutableListOf<ForeignRequires>()
            val node: JsonNode = p0!!.codec.readTree(p0)
            node.fields().forEach { (key, value) ->
                val packageNode = value as JsonNode
                val packageObject = ForeignRequires(
                    key,
                    path = packageNode.get("path").asText(),
                    export = packageNode.get("export").asText().split(",").map { it.trim() }.toList()
                )
                foreignRequires.add(packageObject)
            }
            return foreignRequires
        }
    }
}


///**
// * 依赖项 整合所有
// */
data class Dependencie(
    val name: String,
    val version: String? = null,

//        可选的
    val organization: String? = null,


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
    class ListDeserializer : StdDeserializer<List<Dependencie>>(List::class.java) {
        override fun deserialize(p0: JsonParser?, p1: DeserializationContext?): List<Dependencie> {
            val dependency = mutableListOf<Dependencie>()
            val node: JsonNode = p0!!.codec.readTree(p0)
            node.fields().forEach { (key, value) ->
                when (val packageNode = value as JsonNode) {
                    is TextNode -> {
                        dependency.add(
                            Dependencie(
                                key,
                                version = packageNode.asText(),
                            )
                        )
                    }

                    else -> {
                        val packageObject = Dependencie(
                            key,
                            organization = packageNode.get("organization")?.asText(),

                            version = packageNode.get("version")?.asText(),
                            git = packageNode.get("git")?.asText(),
                            commitId = packageNode.get("commitId")?.asText(),
                            branch = packageNode.get("branch")?.asText(),
                            path = packageNode.get("path")?.asText(),
                            tag = packageNode.get("tag")?.asText()

                        )
                        dependency.add(packageObject)
                    }
                }

//                val packageObject = Dependencie(
//                    key,
//                    version = packageNode.asText(),
//                )
//                dependency.add(packageObject)
            }
            return dependency
        }

    }

}

/**
 * 仓颉中心仓库依赖
 */

data class Dependency(
    val name: String,
    val version: String,

    ) {
    class ListDeserializer : StdDeserializer<List<Dependency>>(List::class.java) {
        override fun deserialize(p0: JsonParser?, p1: DeserializationContext?): List<Dependency> {
            val dependency = mutableListOf<Dependency>()
            val node: JsonNode = p0!!.codec.readTree(p0)
            node.fields().forEach { (key, value) ->
                val packageNode = value as JsonNode
                val packageObject = Dependency(
                    key,
                    version = packageNode.asText(),
                )
                dependency.add(packageObject)
            }
            return dependency
        }

    }

}


data class Require(
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


    class ListDeserializer : StdDeserializer<List<Require>>(List::class.java) {
        override fun deserialize(p: JsonParser?, ctxt: DeserializationContext?): List<Require> {
            val requires = mutableListOf<Require>()
            val node: JsonNode = p!!.codec.readTree(p)
            node.fields().forEach { (key, value) ->
                val packageNode = value as JsonNode
                val packageObject = Require(
                    key,
                    organization = packageNode.get("organization")?.asText(),

                    version = packageNode.get("version")?.asText(),
                    git = packageNode.get("git")?.asText(),
                    commitId = packageNode.get("commitId")?.asText(),
                    branch = packageNode.get("branch")?.asText(),
                    path = packageNode.get("path")?.asText(),
                    tag = packageNode.get("tag")?.asText()

                )
                requires.add(packageObject)
            }
            return requires
        }
    }
}
