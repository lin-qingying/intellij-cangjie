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

import java.nio.file.Path as NioPath

/**
 * 包的唯一标识
 */
data class PackageId(
    val name: String,
    val version: CjVersion,
    val sourceId: SourceId
) : Comparable<PackageId> {
    override fun toString(): String = "$name:$version@${sourceId.shortName}"

    override fun compareTo(other: PackageId): Int {
        return compareValuesBy(this, other, { it.name }, { it.version.versionString }, { it.sourceId.shortName })
    }
}

/**
 * 包来源标识
 */
sealed class SourceId {
    /**
     * 本地工作区
     */
    data class Local(val path: NioPath) : SourceId()

    /**
     * 远程注册表
     */
    data class Registry(val url: String) : SourceId()

    /**
     * Git 仓库
     */
    data class Git(val url: String, val rev: String) : SourceId()

    /**
     * 本地路径
     */
    data class Path(val path: NioPath) : SourceId()

    data object Stdlib : SourceId()

    /**
     * 简短名称
     */
    val shortName: String
        get() = when (this) {
            is Local -> "local"
            is Registry -> "registry"
            is Git -> "git"
            is Path -> "path"
            is Stdlib -> "stdlib"
        }
}

/**
 * 特性定义
 */
data class Feature(
    val name: String,
    val description: String? = null,
    val enables: List<FeatureEnable> = emptyList()
)

/**
 * 特性启用项
 */
sealed class FeatureEnable {
    /**
     * 启用另一个特性
     * 例如：default = ["std", "logging"]
     */
    data class Feature(val name: String) : FeatureEnable()

    /**
     * 启用可选依赖
     * 例如：logging = ["log"]
     */
    data class OptionalDependency(val name: String) : FeatureEnable()

    /**
     * 传播到依赖的特性
     * 例如：full = ["tokio/full", "serde/derive"]
     */
    data class DependencyFeature(
        val dependency: String,
        val feature: String
    ) : FeatureEnable()
}
