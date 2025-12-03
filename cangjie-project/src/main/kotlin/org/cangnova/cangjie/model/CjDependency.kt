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

package org.cangnova.cangjie.model

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
 * 依赖抽象 - 使用 sealed class 表示不同类型的依赖
 *
 * 这个设计改进了原来的 interface 方式，使不同类型的依赖有明确的区分：
 * - Library: 外部库依赖（从仓库下载）
 * - Path: 本地路径依赖（通常是工作空间内的模块）
 * - Git: Git 仓库依赖
 * - System: 系统提供的依赖（如标准库）
 */
sealed class CjDependency {
    /**
     * 依赖名称
     */
    abstract val name: String

    /**
     * 依赖组 (可选，类似 Maven 的 groupId)
     */
    abstract val group: String?


    /**
     * 依赖版本
     */
    abstract val version: CjVersion

    /**
     * 依赖范围
     */
    abstract val scope: CjDependencyScope

    /**
     * 是否为可选依赖
     */
    abstract val optional: Boolean

    /**
     * 是否包含传递依赖
     */
    abstract val transitive: Boolean

    /**
     * 排除的传递依赖列表
     */
    abstract val excludes: List<DependencyExclusion>

    /**
     * 依赖的唯一标识符
     */
    val id: String
        get() = "${if (group != null) "$group:" else ""}$name:$version"

    /**
     * 外部库依赖
     *
     * 从远程仓库（如包管理器仓库）下载的依赖
     *
     * @property name 依赖名称
     * @property group 依赖组
     * @property version 依赖版本
     * @property scope 依赖范围
     * @property registry 仓库地址（可选，null 表示使用默认仓库）
     * @property optional 是否为可选依赖
     * @property transitive 是否包含传递依赖
     * @property excludes 排除的传递依赖列表
     */
    data class Library(
        override val name: String,
        override val group: String? = null,
        override val version: CjVersion,
        override val scope: CjDependencyScope = CjDependencyScope.COMPILE,
        val registry: String? = null,
        override val optional: Boolean = false,
        override val transitive: Boolean = true,
        override val excludes: List<DependencyExclusion> = emptyList()
    ) : CjDependency() {
        override fun toString(): String {
            val base = if (group != null) "$group:$name:$version" else "$name:$version"
            val extras = mutableListOf<String>()
            if (registry != null) extras.add("registry=$registry")
            if (optional) extras.add("optional")
            if (!transitive) extras.add("no-transitive")
            return if (extras.isEmpty()) base else "$base (${extras.joinToString(", ")})"
        }
    }

    /**
     * 本地路径依赖
     *
     * 指向本地文件系统中的依赖（通常是工作空间内的其他模块）
     *
     * @property name 依赖名称
     * @property path 相对或绝对路径
     * @property version 依赖版本（用于版本检查）
     * @property scope 依赖范围
     * @property optional 是否为可选依赖
     * @property transitive 是否包含传递依赖（路径依赖通常不包含）
     * @property excludes 排除的传递依赖列表
     */
    data class Path(
        override val name: String,
        val path: String,
        override val version: CjVersion = CjVersion(),
        override val scope: CjDependencyScope = CjDependencyScope.COMPILE,
        override val optional: Boolean = false,
        override val transitive: Boolean = false,
        override val excludes: List<DependencyExclusion> = emptyList()
    ) : CjDependency() {
        override val group: String? = null

        override fun toString(): String {
            return "$name (path=$path)"
        }
    }

    /**
     * Git 仓库依赖
     *
     * 从 Git 仓库克隆的依赖
     *
     * @property name 依赖名称
     * @property url Git 仓库 URL
     * @property branch Git 分支名（可选）
     * @property tag Git 标签名（可选）
     * @property rev Git commit ID（可选）
     * @property version 依赖版本（用于版本检查）
     * @property scope 依赖范围
     * @property optional 是否为可选依赖
     * @property transitive 是否包含传递依赖
     * @property excludes 排除的传递依赖列表
     *
     * 注意：branch、tag、rev 三者互斥，只能指定其中一个
     */
    data class Git(
        override val name: String,
        val url: String,
        val branch: String? = null,
        val tag: String? = null,
        val rev: String? = null,
        override val version: CjVersion,
        override val scope: CjDependencyScope = CjDependencyScope.COMPILE,
        override val optional: Boolean = false,
        override val transitive: Boolean = true,
        override val excludes: List<DependencyExclusion> = emptyList()
    ) : CjDependency() {
        override val group: String? = null

        init {
            // 验证只有一个 Git 引用被指定
            val refCount = listOfNotNull(branch, tag, rev).size
            require(refCount <= 1) {
                "Only one of branch, tag, or rev can be specified, but got: " +
                        "branch=$branch, tag=$tag, rev=$rev"
            }
        }

        /**
         * Git 引用（branch、tag 或 rev）
         */
        val gitRef: String?
            get() = branch ?: tag ?: rev

        override fun toString(): String {
            val ref = when {
                branch != null -> "branch=$branch"
                tag != null -> "tag=$tag"
                rev != null -> "rev=${rev.take(7)}"
                else -> "HEAD"
            }
            return "$name (git=$url, $ref)"
        }
    }

    /**
     * 系统依赖
     *
     * 由系统或工具链提供的依赖（如标准库）
     *
     * @property name 依赖名称
     * @property version 依赖版本
     * @property scope 依赖范围

     */
    data class System(
        override val name: String,
        override val version: CjVersion,
        override val scope: CjDependencyScope = CjDependencyScope.PROVIDED,
    ) : CjDependency() {
        override val group: String? = null
        override val optional: Boolean = false
        override val transitive: Boolean = false
        override val excludes: List<DependencyExclusion> = emptyList()

        override fun toString(): String {
            return "$name:$version"
        }
    }

    /**
     * 标准库
     */
    data class Stdlib(

        override val version: CjVersion,
        override val scope: CjDependencyScope = CjDependencyScope.PROVIDED,
    ) : CjDependency() {
        override val name: String = "stdlib"
        override val group: String? = null
        override val optional: Boolean = false
        override val transitive: Boolean = false
        override val excludes: List<DependencyExclusion> = emptyList()

        override fun toString(): String {
            return "$name:${version.versionString ?: ""}"
        }
    }
}



