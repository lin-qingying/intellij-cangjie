package com.huawei.cangjie.cjpm.project.model

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import com.huawei.cangjie.cjpm.toolchain.tools.Cjpm
import java.util.*


/**
 * cjpm项目信息，该类存储module.json原始数据
 */
data class CjpmProjectInfo(
    @JsonProperty("cjc_version")
    val cjcVersion: String,
    val organization: String,
    val name: String,
    val version: String,
    val description: String,
//    仓颉中心仓库依赖
    @JsonDeserialize(using = Dependency.ListDeserializer::class)
    val dependencies: List<Dependency> = emptyList(),

//     依赖模块信息配置项，非必须
    @JsonDeserialize(using = Require.ListDeserializer::class)
    val requires: List<Require> = emptyList(),


//    用于指定仅在开发过程中使用的依赖项，非必须
    @JsonProperty("dev_requires")
    @JsonDeserialize(using = Require.ListDeserializer::class)
    val devRequires: List<Require> = emptyList(),


//    依赖的仓颉package， 非必须
    @JsonProperty("package_requires")
    @JsonDeserialize(using = Require.ListDeserializer::class)
    val packageRequires: List<PackageRequires> = emptyList(),

//    外部调用 c 库的依赖项，非必须
    @JsonProperty("foreign_requires")
    @JsonDeserialize(using = ForeignRequires.ListDeserializer::class)
    val foreignRequires: List<ForeignRequires> = emptyList(),

//项目类型 二进制执行程序，静态库，动态库
    @JsonProperty("output_type")
    @JsonDeserialize(using = OutPutType.EnumDeserializer::class)
    val outputType: OutPutType,


//    额外编译命令选项，非必须
    @JsonProperty("command_option")
    val compileOption: String? = null,

//    传给链接器的编译选项，可用于透传安全编译命令，如下所示。注意，这里配置的命令在编译时只会自动透传给动态库和可执行文件对应的包。
    @JsonProperty("link_option")
    val linkOption: String? = null,


//    按照条件选项透传给 cjc 的命令
    @JsonProperty("condition_option")
    val conditionOption: Map<String, String> = mapOf(),

//    单包配置选项，非必须
    @JsonProperty("package_configuration")
    @JsonDeserialize(using = PackageConfiguration.ListDeserializer::class)
    val packageConfiguration: List<PackageConfiguration> = emptyList(),


//    交叉编译到目标平台所需的配置项
    @JsonProperty("cross_compile_configuration")
    val crossCompileConfiguration: Map<String, String> = mapOf(),


//    指定编译产物的存放路径
    @JsonProperty("build_dir")
    val buildDir: String? = "build",


//    指定源码的存放路径 不指定默认为src
    @JsonProperty("src_dir")
    val sourceDir: String? = "src",


//    配置脚本内容
    @JsonProperty("scripts")
    val script: Map<String, String>? = mapOf(),

//    使能且指定 LTO （Link Time Optimization 链接时优化）优化编译模式。指定该字段后，对该模块及依赖的所有上游模块均生效。
    @JsonProperty("lto")
    val lto: String? = null,
)


data class PackageConfiguration(
    val packageName: String,
    val configuration: CjpmProjectInfo
) {
    class ListDeserializer : StdDeserializer<List<PackageConfiguration>>(List::class.java) {
        override fun deserialize(p0: JsonParser?, p1: DeserializationContext?): List<PackageConfiguration> {
            val packageConfiguration = mutableListOf<PackageConfiguration>()
            val node: JsonNode = p0!!.codec.readTree(p0)
            node.fields().forEach { (key, value) ->
                val packageNode = value as JsonNode
                val packageObject = PackageConfiguration(
                    packageName = key,
                    configuration = Cjpm.JSON_MAPPER.readValue(packageNode.asText(), CjpmProjectInfo::class.java)
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
            return OutPutType.valueOf(node.asText().toUpperCase(Locale.getDefault()))
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
                    version = packageNode.get("version").asText(),
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