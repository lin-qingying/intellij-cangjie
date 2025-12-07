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

package org.cangnova.cangjie.project.model

/**
 * 依赖声明者接口
 *
 * 用于标识声明了某个依赖的实体，可以是模块 ([CjModule]) 或另一个依赖 ([CjDependency])
 * 这允许我们跟踪依赖的声明来源，以便正确解析相对路径依赖
 */
interface CjDependencyDeclarant {
    /**
     * 声明者的名称
     */
    val name: String
}

/**
 * 依赖范围
 */
enum class CjDependencyScope {
    /**
     * 编译时依赖
     */
    COMPILE,

    /**
     * 运行时依赖
     */
    RUNTIME,

    /**
     * 测试依赖
     */
    TEST,

    /**
     * 已提供依赖 (编译时需要，运行时由环境提供)
     */
    PROVIDED
}

/**
 * 依赖排除规则
 *
 * 用于排除传递依赖中的特定依赖项
 *
 * @property group 依赖组 (可选)
 * @property name 依赖名称
 */
data class DependencyExclusion(
    val group: String?,
    val name: String
) {
    /**
     * 检查给定的依赖是否匹配此排除规则
     */
    fun matches(dependency: CjDependency): Boolean {
        if (dependency.name != name) return false
        if (group != null && dependency.group != group) return false
        return true
    }

    override fun toString(): String {
        return if (group != null) "$group:$name" else name
    }
}

/**
 * 增强的依赖声明
 *
 * - 使用 [VersionRequirement] 代替简单的版本号
 * - 支持特性系统（features）
 * - 支持依赖重命名（rename）
 * - 支持条件编译（targetPlatform）
 *
 */
sealed class CjDependency : CjDependencyDeclarant {
    /**
     * 依赖名称
     */
    abstract override val name: String

    /**
     * 依赖组（可选，类似 Maven 的 groupId）
     */
    abstract val group: String?

    /**
     * 版本要求
     *
     * 注意，这里是配置文件中的版本，包解析后是一个确定的版本，不要搞混
     * @see [VersionRequirement]
     * @see [CjVersion]
     */
    abstract val versionReq: VersionRequirement

    /**
     * 依赖范围
     */
    abstract val scope: CjDependencyScope

    /**
     * 要启用的特性列表
     */
    abstract val features: List<String>

    /**
     * 是否启用默认特性
     */
    abstract val defaultFeatures: Boolean

    /**
     * 是否为可选依赖
     */
    abstract val optional: Boolean

    /**
     * 是否包含传递依赖
     */
    abstract val transitive: Boolean

    /**
     * 依赖重命名（用于解决命名冲突）
     */
    abstract val rename: String?

    /**
     * 条件编译目标（如 cfg(unix), cfg(windows)）
     */
    abstract val target: String?

    /**
     * 排除的传递依赖列表
     */
    abstract val excludes: List<DependencyExclusion>

    /**
     * 声明此依赖的源模块或依赖
     *
     * 用于解析相对路径依赖时确定基准目录
     * 可以是模块（直接依赖）或另一个依赖（传递依赖）
     * 对于 Path 依赖，相对路径应相对于声明它的模块或依赖的根目录
     */
    abstract val sourceModule: CjDependencyDeclarant

    /**
     * 依赖的唯一标识符
     */
    val id: String
        get() = "${if (group != null) "$group:" else ""}${rename ?: name}"


    /**
     * 外部库依赖
     *
     * 从远程仓库（如包管理器仓库）下载的依赖
     */
    data class Library(
        override val name: String,
        override val group: String? = null,
        override val versionReq: VersionRequirement,
        override val scope: CjDependencyScope = CjDependencyScope.COMPILE,
        val registry: String? = null,
        override val features: List<String> = emptyList(),
        override val defaultFeatures: Boolean = true,
        override val optional: Boolean = false,
        override val transitive: Boolean = true,
        override val rename: String? = null,
        override val target: String? = null,
        override val excludes: List<DependencyExclusion> = emptyList(),
        override val sourceModule: CjDependencyDeclarant
    ) : CjDependency() {
        override fun toString(): String {
            val parts = mutableListOf<String>()
            parts.add("${group?.let { "$it:" } ?: ""}$name")
            parts.add(versionReq.toString())
            if (features.isNotEmpty()) parts.add("features=[${features.joinToString(", ")}]")
            if (rename != null) parts.add("rename=$rename")
            if (target != null) parts.add("targetPlatform=$target")
            if (optional) parts.add("optional")
            return parts.joinToString(" ")
        }
    }

    /**
     * 本地路径依赖
     */
    data class Path(
        override val name: String,
        val path: java.nio.file.Path,
        override val versionReq: VersionRequirement = VersionRequirement.Exact(CjVersion()),
        override val scope: CjDependencyScope = CjDependencyScope.COMPILE,
        override val features: List<String> = emptyList(),
        override val defaultFeatures: Boolean = true,
        override val optional: Boolean = false,
        override val transitive: Boolean = false,
        override val rename: String? = null,
        override val target: String? = null,
        override val excludes: List<DependencyExclusion> = emptyList(),
        override val sourceModule: CjDependencyDeclarant
    ) : CjDependency() {
        override val group: String? = null

        override fun toString(): String {
            return "$name (path=$path)"
        }
    }

    /**
     * Git 仓库依赖
     */
    data class Git(
        override val name: String,
        val url: String,
        val ref: GitRef,
        override val versionReq: VersionRequirement,
        override val scope: CjDependencyScope = CjDependencyScope.COMPILE,
        override val features: List<String> = emptyList(),
        override val defaultFeatures: Boolean = true,
        override val optional: Boolean = false,
        override val transitive: Boolean = true,
        override val rename: String? = null,
        override val target: String? = null,
        override val excludes: List<DependencyExclusion> = emptyList(),
        override val sourceModule: CjDependencyDeclarant
    ) : CjDependency() {
        override val group: String? = null

        override fun toString(): String {
            return "$name (git=$url, ref=$ref)"
        }
    }

    /**
     * 系统依赖
     */
    data class Stdlib(
        override val name: String,
        override val versionReq: VersionRequirement,
        override val scope: CjDependencyScope = CjDependencyScope.PROVIDED,
        override val sourceModule: CjDependencyDeclarant
    ) : CjDependency() {
        override val group: String? = null
        override val features: List<String> = emptyList()
        override val defaultFeatures: Boolean = false
        override val optional: Boolean = false
        override val transitive: Boolean = false
        override val rename: String? = null
        override val target: String? = null
        override val excludes: List<DependencyExclusion> = emptyList()

        override fun toString(): String {
            return "$name:$versionReq"
        }
    }

    /**
     * 二进制依赖
     *
     * 用于导入已编译好的、适用于指定 targetPlatform 的仓颉库产物文件。
     * 包含前端文件 (.cjo) 和对应的库文件 (.so 或 .a)
     *
     * 示例：
     * ```toml
     * [targetPlatform.x86_64-unknown-linux-gnu.bin-dependencies]
     *   path-option = ["./test/pro0"]
     *
     * [targetPlatform.x86_64-unknown-linux-gnu.bin-dependencies.package-option]
     *   "pro0.xoo" = "./test/pro0/pro0.xoo.cjo"
     * ```
     */
    data class Binary(
        override val name: String,
        /**
         * 前端接口文件路径 (.cjo)
         */
        val cjoPath: java.nio.file.Path,

        /**
         * 库文件路径 (.so 或 .a)，可选
         * 如果为 null，则从 cjoPath 同目录查找对应的库文件
         */
        val libPath: java.nio.file.Path? = null,

        /**
         * 目标平台（如 x86_64-unknown-linux-gnu）
         */
        override val target: String,

        override val scope: CjDependencyScope = CjDependencyScope.COMPILE,
        override val sourceModule: CjDependencyDeclarant
    ) : CjDependency() {
        override val group: String? = null
        override val versionReq: VersionRequirement = VersionRequirement.Exact(CjVersion())
        override val features: List<String> = emptyList()
        override val defaultFeatures: Boolean = false
        override val optional: Boolean = false
        override val transitive: Boolean = false  // 二进制依赖不处理传递依赖
        override val rename: String? = null
        override val excludes: List<DependencyExclusion> = emptyList()

        override fun toString(): String {
            return "$name (binary=${cjoPath.fileName})"
        }
    }


}

/**
 * Git 引用
 */
sealed class GitRef {
    /**
     * 分支引用
     */
    data class Branch(val name: String) : GitRef() {
        override fun toString(): String = "branch=$name"
    }

    /**
     * 标签引用
     */
    data class Tag(val name: String) : GitRef() {
        override fun toString(): String = "tag=$name"
    }

    /**
     * 提交哈希引用
     */
    data class Rev(val commitHash: String) : GitRef() {
        override fun toString(): String = "rev=${commitHash.take(7)}"
    }
}

/**
 * 依赖来源
 */
sealed class DependencySource {
    /**
     * Registry 仓库
     */
    data class Registry(val url: String?) : DependencySource()

    /**
     * Git 仓库
     */
    data class Git(val url: String, val ref: GitRef) : DependencySource()

    /**
     * 本地路径
     */
    data class Path(val path: java.nio.file.Path) : DependencySource()

    /**
     * 系统提供
     */
    object Stdlib : DependencySource()

    /**
     * 二进制文件
     */
    data class Binary(val cjoPath: java.nio.file.Path) : DependencySource()
}

/**
 * 扩展方法：从 CjDependency 提取依赖来源
 */
fun CjDependency.toDependencySource(): DependencySource {
    return when (this) {
        is CjDependency.Library -> DependencySource.Registry(registry)
        is CjDependency.Git -> DependencySource.Git(url, ref)
        is CjDependency.Path -> DependencySource.Path(path)
        is CjDependency.Stdlib -> DependencySource.Stdlib
        is CjDependency.Binary -> DependencySource.Binary(cjoPath)
    }
}

fun CjDependency.sourceId(): SourceId = when (this) {
    is CjDependency.Git -> SourceId.Git(this.url, this.ref.toString())
    is CjDependency.Library -> SourceId.Registry(this.registry ?: "https://repo.cangnova.org/repository/maven-public")
    is CjDependency.Path -> SourceId.Path(this.path)
    is CjDependency.Stdlib -> SourceId.Stdlib
    is CjDependency.Binary -> SourceId.Path(this.cjoPath)  // 二进制依赖使用本地路径标识
}