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

import com.intellij.openapi.vfs.VirtualFile


/**
 * 模块抽象
 *
 * 代表项目中的一个模块/包，每个CjModule应对应一个实际的IntelliJ Module
 */
interface CjModule : CjDependencyDeclarant {
    /**
     * 模块名称
     */
    override val name: String

    /**
     * 模块根目录
     */
    val rootDir: VirtualFile

    /**
     * 所属项目
     */
    val project: CjProject

    /**
     * 模块配置文件
     */
    val configFile: VirtualFile?

    /**
     * 源码集列表
     */
    val sourceSets: List<CjSourceSet>

    /**
     * 所有依赖列表（推荐使用）
     *
     * 返回此模块的所有依赖，包括：
     * - 外部库依赖 (CjDependency.Library)
     * - 路径依赖/模块依赖 (CjDependency.Path)
     * - Git 依赖 (CjDependency.Git)
     * - 系统依赖 (CjDependency.Stdlib)
     *
     * 默认返回空列表，具体实现需要根据项目配置(如 cjpm.toml)解析依赖关系。
     */
    val dependencies: List<CjDependency>
        get() = emptyList()


    val buildDependencies: List<CjDependency>
        get() = emptyList()

    val testDependencies: List<CjDependency>
        get() = emptyList()

    /**
     * 模块元数据
     *
     * 包含版本、描述、作者等信息，通常从配置文件（如 cjpm.toml）解析。
     * 提供统一的包元数据接口，便于在依赖解析和包管理中使用。
     */
    val metadata: CjPackageMetadata

}


/**
 * 便捷扩展属性：获取所有库依赖
 */
val CjModule.libraryDependencies: List<CjDependency.Library>
    get() = dependencies.filterIsInstance<CjDependency.Library>()

/**
 * 便捷扩展属性：获取所有路径依赖（模块依赖）
 */
val CjModule.pathDependencies: List<CjDependency.Path>
    get() = dependencies.filterIsInstance<CjDependency.Path>()

/**
 * 便捷扩展属性：获取所有 Git 依赖
 */
val CjModule.gitDependencies: List<CjDependency.Git>
    get() = dependencies.filterIsInstance<CjDependency.Git>()



