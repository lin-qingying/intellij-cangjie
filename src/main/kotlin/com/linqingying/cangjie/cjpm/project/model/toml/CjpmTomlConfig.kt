package com.linqingying.cangjie.cjpm.project.model.toml

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.core.JsonToken
import com.fasterxml.jackson.databind.*
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.linqingying.cangjie.cjpm.project.model.toml.PackageConfigurationInfo
import kotlinx.serialization.Serializable

/**
 * CJPM TOML 配置文件的根配置类
 */
@Serializable
data class CjpmTomlConfig(
    /** 单模块配置字段，与 workspace 字段不能同时存在 */
    val `package`: PackageConfig? = null,

    /** 工作空间管理字段，与 package 字段不能同时存在 */
    @JsonDeserialize(using = EmptyStringToWorkspaceConfigDeserializer::class) val workspace: WorkspaceConfig? = null,

    /** 源码依赖配置项 */
    val dependencies: Map<String, DependencyConfig> = mapOf(),

    /** 测试阶段的依赖配置项 */
    @JsonProperty("test-dependencies") val testDependencies: Map<String, DependencyConfig> = mapOf(),

    /** 构建脚本依赖配置项 */
    @JsonProperty("script-dependencies") val scriptDependencies: Map<String, DependencyConfig> = mapOf(),

    /** FFI配置项 */
    @JsonDeserialize(using = EmptyStringToFfiConfigDeserializer::class) val ffi: FfiConfig? = null,

    /** 命令剖面配置项 */
    @JsonDeserialize(using = EmptyStringToProfileConfigDeserializer::class) val profile: ProfileConfig? = null,

    /** 后端和平台隔离配置项 */
    val target: Map<String, TargetConfig> = mapOf()
) {

    val srcDir get() = if (`package`?.srcDir?.isNullOrEmpty() != false) "src" else `package`.srcDir
    val targetDir
        get() = if (workspace != null) {
            workspace.targetDir ?: "target"
        } else {
            if (`package`?.targetDir?.isNullOrEmpty() != false) "target" else `package`.targetDir
        }
    val name get() = `package`?.name ?: workspace?.name
}

/**
 * 自定义反序列化器，将空字符串转换为 null (FFI配置)
 */
class EmptyStringToFfiConfigDeserializer : com.fasterxml.jackson.databind.JsonDeserializer<FfiConfig?>() {
    override fun deserialize(
        p: com.fasterxml.jackson.core.JsonParser, ctxt: com.fasterxml.jackson.databind.DeserializationContext
    ): FfiConfig? {
        return when (p.currentToken) {
            com.fasterxml.jackson.core.JsonToken.VALUE_STRING -> {
                if (p.valueAsString.isEmpty()) null else ctxt.readValue(p, FfiConfig::class.java)
            }

            com.fasterxml.jackson.core.JsonToken.START_OBJECT -> {
                ctxt.readValue(p, FfiConfig::class.java)
            }

            com.fasterxml.jackson.core.JsonToken.VALUE_NULL -> null
            else -> ctxt.readValue(p, FfiConfig::class.java)
        }
    }
}

/**
 * 单模块配置
 */
@Serializable
data class PackageConfig(
    /** 所需 cjc 的最低版本要求 */
    @JsonProperty("cjc-version") val cjcVersion: String,

    /** 模块名及模块 root 包名 */
    val name: String,

    /** 描述信息 */
    val description: String? = null,

    /** 模块版本信息 */
    val version: String,

    /** 额外编译命令选项 */
    @JsonProperty("compile-option") val compileOption: String? = null,

    /** 额外全局编译命令选项 */
    @JsonProperty("override-compile-option") val overrideCompileOption: String? = null,

    /** 链接器透传选项 */
    @JsonProperty("link-option") val linkOption: String? = null,

    /** 编译输出产物类型 */
    @JsonProperty("output-type") val outputType: OutputType,

    /** 指定源码存放路径 */
    @JsonProperty("src-dir") val srcDir: String = "src",

    /** 指定产物存放路径 */
    @JsonProperty("target-dir") val targetDir: String = "target",

    /** 单包配置选项 */
    @JsonProperty("package-configuration") @JsonDeserialize(using = EmptyStringToNullMapDeserializer::class) val packageConfiguration: Map<String, PackageConfigurationInfo>? = null
)

/**
 * 编译输出产物类型
 */
@Serializable
enum class OutputType {
    /** 可执行程序 */
    @JsonProperty("executable")
    EXECUTABLE,

    /** 静态库 */
    @JsonProperty("static")
    STATIC,

    /** 动态库 */
    @JsonProperty("dynamic")
    DYNAMIC
}

/**
 * 自定义反序列化器，将空字符串转换为 null (Profile配置)
 */
class EmptyStringToProfileConfigDeserializer : JsonDeserializer<ProfileConfig?>() {
    override fun deserialize(
        p: com.fasterxml.jackson.core.JsonParser, ctxt: DeserializationContext
    ): ProfileConfig? {
        return when (p.currentToken) {
            com.fasterxml.jackson.core.JsonToken.VALUE_STRING -> {
                if (p.valueAsString.isEmpty()) null else ctxt.readValue(p, ProfileConfig::class.java)
            }

            com.fasterxml.jackson.core.JsonToken.START_OBJECT -> {
                ctxt.readValue(p, ProfileConfig::class.java)
            }

            JsonToken.VALUE_NULL -> null
            else -> ctxt.readValue(p, ProfileConfig::class.java)
        }
    }
}

/**
 * 自定义反序列化器，将空字符串转换为 null
 */
class EmptyStringToNullMapDeserializer : JsonDeserializer<Map<String, PackageConfigurationInfo>?>() {
    override fun deserialize(
        p: JsonParser, ctxt: com.fasterxml.jackson.databind.DeserializationContext
    ): Map<String, PackageConfigurationInfo>? {
        val value = p.valueAsString
        return when {
            value.isNullOrEmpty() -> null
            else -> ctxt.readValue(
                p, ctxt.typeFactory.constructMapType(
                    LinkedHashMap::class.java, String::class.java, PackageConfigurationInfo::class.java
                )
            )
        }
    }
}

/**
 * 工作空间配置
 */
@Serializable
data class WorkspaceConfig(
    /** 工作空间成员模块列表 */
    val members: List<String>,

    val name: String? = null,
    /** 工作空间编译模块列表，需要是成员模块列表的子集 */
    @JsonProperty("build-members") val buildMembers: List<String>? = null,

    /** 工作空间测试模块列表，需要是编译模块列表的子集 */
    @JsonProperty("test-members") val testMembers: List<String>? = null,

    /** 应用于所有工作空间成员模块的额外编译命令选项 */
    @JsonProperty("compile-option") val compileOption: String? = null,

    /** 应用于所有工作空间成员模块的额外全局编译命令选项 */
    @JsonProperty("override-compile-option") val overrideCompileOption: String? = null,

    /** 应用于所有工作空间成员模块的链接器透传选项 */
    @JsonProperty("link-option") val linkOption: String? = null,

    /** 指定产物存放路径 */
    @JsonProperty("target-dir") val targetDir: String? = null
)

/**
 * 自定义反序列化器，将空字符串转换为 null
 */
class EmptyStringToWorkspaceConfigDeserializer : com.fasterxml.jackson.databind.JsonDeserializer<WorkspaceConfig?>() {
    override fun deserialize(
        p: com.fasterxml.jackson.core.JsonParser, ctxt: com.fasterxml.jackson.databind.DeserializationContext
    ): WorkspaceConfig? {
        return when (p.currentToken) {
            com.fasterxml.jackson.core.JsonToken.VALUE_STRING -> {
                if (p.valueAsString.isEmpty()) null else ctxt.readValue(p, WorkspaceConfig::class.java)
            }

            com.fasterxml.jackson.core.JsonToken.START_OBJECT -> {
                ctxt.readValue(p, WorkspaceConfig::class.java)
            }

            com.fasterxml.jackson.core.JsonToken.VALUE_NULL -> null
            else -> ctxt.readValue(p, WorkspaceConfig::class.java)
        }
    }
}

