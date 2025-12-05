/*
 * Copyright 2025 LinQingYing. and contributors.
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

package org.cangnova.cangjie.cjpm.project.model.toml

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.core.JsonToken
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import kotlinx.serialization.Serializable

/**
 * CJPM TOML 配置文件的根配置类
 */
@Serializable
data class CjpmTomlConfig(
    /** 单模块配置字段，与 workspace 字段不能同时存在 */
    @field:JsonProperty("package") val `package`: PackageConfig? = null,

    /** 工作空间管理字段，与 package 字段不能同时存在 */
    @field:JsonDeserialize(using = EmptyStringToWorkspaceConfigDeserializer::class) val workspace: WorkspaceConfig? = null,

    /** 源码依赖配置项 */
    @field:JsonProperty("dependencies") val dependencies: Map<String, DependencyConfig> = mapOf(),

    /** 测试阶段的依赖配置项 */
    @field:JsonProperty("test-dependencies") val testDependencies: Map<String, DependencyConfig> = mapOf(),

    /** 构建脚本依赖配置项 */
    @field:JsonProperty("script-dependencies") val scriptDependencies: Map<String, DependencyConfig> = mapOf(),

    /**
     * 依赖替换配置
     *
     * 用于替换间接依赖（传递依赖）的同名模块。
     * 格式与 dependencies 字段完全相同。
     *
     * 注意：只有入口模块（根模块）的 replace 字段会生效，
     * 子依赖的 replace 配置会被忽略。
     *
     * 示例：
     * ```toml
     * [replace]
     *   bbb = { path = "new/path/to/bbb" }
     * ```
     */
    @field:JsonProperty("replace")
    val replace: Map<String, DependencyConfig> = mapOf(),

    /** FFI配置项 */
    @field:JsonDeserialize(using = EmptyStringToFfiConfigDeserializer::class) val ffi: FfiConfig? = null,

    /** 命令剖面配置项 */
    @field:JsonDeserialize(using = EmptyStringToProfileConfigDeserializer::class) val profile: ProfileConfig? = null,

    /** 后端和平台隔离配置项 */
    @field:JsonProperty("target") val target: Map<String, TargetConfig> = mapOf()
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
        p: JsonParser, ctxt: DeserializationContext
    ): FfiConfig? {
        return when (p.currentToken) {
            JsonToken.VALUE_STRING -> {
                if (p.valueAsString.isEmpty()) null else ctxt.readValue(p, FfiConfig::class.java)
            }

            JsonToken.START_OBJECT -> {
                ctxt.readValue(p, FfiConfig::class.java)
            }

            JsonToken.VALUE_NULL -> null
            else -> ctxt.readValue(p, FfiConfig::class.java)
        }
    }
}

/**
 * 自定义反序列化器，将空字符串转换为默认值 "src"
 */
class EmptyStringToSrcDirDeserializer : JsonDeserializer<String>() {
    override fun deserialize(p: JsonParser, ctxt: DeserializationContext): String {
        val value = p.valueAsString
        return if (value.isNullOrEmpty()) "src" else value
    }
}

/**
 * 自定义反序列化器，将空字符串转换为默认值 "targetPlatform"
 */
class EmptyStringToTargetDirDeserializer : JsonDeserializer<String>() {
    override fun deserialize(p: JsonParser, ctxt: DeserializationContext): String {
        val value = p.valueAsString
        return if (value.isNullOrEmpty()) "target" else value
    }
}

/**
 * 单模块配置
 */
@Serializable
data class PackageConfig(
    /** 所需 cjc 的最低版本要求 */
    @field:JsonProperty("cjc-version") val cjcVersion: String,

    /** 模块名及模块 root 包名 */
    val name: String,

    /** 描述信息 */
    val description: String? = null,

    /** 模块版本信息 */
    val version: String,

    /** 作者列表 */
    val authors: List<String>? = null,

    /** 许可证 */
    val license: String? = null,

    /** 仓库 URL */
    @field:JsonProperty("repository-url") val repositoryUrl: String? = null,

    /** 额外编译命令选项 */
    @field:JsonProperty("compile-option") val compileOption: String? = null,

    /** 额外全局编译命令选项 */
    @field:JsonProperty("override-compile-option") val overrideCompileOption: String? = null,

    /** 链接器透传选项 */
    @field:JsonProperty("link-option") val linkOption: String? = null,

    /** 编译输出产物类型 */
    @field:JsonProperty("output-type") val outputType: OutputType,

    /** 指定源码存放路径 */
    @field:JsonProperty("src-dir")
    @field:JsonDeserialize(using = EmptyStringToSrcDirDeserializer::class)
    val srcDir: String = "src",

    /** 指定产物存放路径 */
    @field:JsonProperty("targetPlatform-dir")
    @field:JsonDeserialize(using = EmptyStringToTargetDirDeserializer::class)
    val targetDir: String = "target",

    /** 单包配置选项 */
    @field:JsonProperty("package-configuration") val packageConfiguration: Map<String, PackageConfigurationInfo> = mapOf()
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
class EmptyStringToProfileConfigDeserializer : com.fasterxml.jackson.databind.JsonDeserializer<ProfileConfig?>() {
    override fun deserialize(
        p: JsonParser, ctxt: DeserializationContext
    ): ProfileConfig? {
        return when (p.currentToken) {
            JsonToken.VALUE_STRING -> {
                if (p.valueAsString.isEmpty()) null else ctxt.readValue(p, ProfileConfig::class.java)
            }

            JsonToken.START_OBJECT -> {
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
class EmptyStringToNullMapDeserializer : com.fasterxml.jackson.databind.JsonDeserializer<Map<String, PackageConfigurationInfo>?>() {
    override fun deserialize(
        p: JsonParser, ctxt: DeserializationContext
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
    @field:JsonProperty("build-members") val buildMembers: List<String>? = null,

    /** 工作空间测试模块列表，需要是编译模块列表的子集 */
    @field:JsonProperty("test-members") val testMembers: List<String>? = null,

    /** 应用于所有工作空间成员模块的额外编译命令选项 */
    @field:JsonProperty("compile-option") val compileOption: String? = null,

    /** 应用于所有工作空间成员模块的额外全局编译命令选项 */
    @field:JsonProperty("override-compile-option") val overrideCompileOption: String? = null,

    /** 应用于所有工作空间成员模块的链接器透传选项 */
    @field:JsonProperty("link-option") val linkOption: String? = null,

    /** 指定产物存放路径 */
    @field:JsonProperty("targetPlatform-dir") val targetDir: String? = null
)

/**
 * 自定义反序列化器，将空字符串转换为 null
 */
class EmptyStringToWorkspaceConfigDeserializer : com.fasterxml.jackson.databind.JsonDeserializer<WorkspaceConfig?>() {
    override fun deserialize(
        p: JsonParser, ctxt: DeserializationContext
    ): WorkspaceConfig? {
        return when (p.currentToken) {
            JsonToken.VALUE_STRING -> {
                if (p.valueAsString.isEmpty()) null else ctxt.readValue(p, WorkspaceConfig::class.java)
            }

            JsonToken.START_OBJECT -> {
                ctxt.readValue(p, WorkspaceConfig::class.java)
            }

            JsonToken.VALUE_NULL -> null
            else -> ctxt.readValue(p, WorkspaceConfig::class.java)
        }
    }
}

