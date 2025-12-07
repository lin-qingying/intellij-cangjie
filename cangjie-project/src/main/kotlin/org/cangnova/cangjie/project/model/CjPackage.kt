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

import com.intellij.openapi.project.Project
import java.nio.file.Path as JPath

/**
 * 统一的包抽象
 *
 * 表示本地模块和外部依赖的统一接口，提供完整的包管理能力。
 * CjDependency 解析后会转换为该类的具体实现。
 */
sealed class CjPackage {
    /**
     * 包的唯一标识
     */
    abstract val id: PackageId

    /**
     * 包元数据
     */
    abstract val metadata: CjPackageMetadata

    /**
     * 包依赖列表
     */
    abstract val dependencies: List<CjDependency>

    /**
     * 包特性（features）配置
     */
    abstract val features: Map<String, Feature>

    /**
     * 解析失败的包
     *
     * @property id 包标识
     * @property errorMessage 错误信息
     */
    data class Failed(
        override val id: PackageId,
        val errorMessage: String
    ) : CjPackage() {
        override val metadata: CjPackageMetadata = CjPackageMetadata.EMPTY
        override val dependencies: List<CjDependency> = emptyList()
        override val features: Map<String, Feature> = emptyMap()
    }

    /**
     * 本地模块包
     *
     * 表示工作区中的本地模块（对应 CjModule）
     *
     * @property module 关联的 CjModule
     * @property sourceSets 源码集合
     * @property intellijProject IntelliJ 项目实例
     */
    data class LocalModule(
        override val id: PackageId,
        override val metadata: CjPackageMetadata,
        override val dependencies: List<CjDependency>,
        override val features: Map<String, Feature>,
        val module: CjModule,
        val sourceSets: List<CjSourceSet>,
        val intellijProject: Project?
    ) : CjPackage()

    /**
     * Registry 库包
     *
     * 从包注册表下载的外部依赖
     *
     * @property registry 注册表 URL
     * @property downloadPath 下载后的本地路径
     * @property checksum 校验和
     */
    data class Library(
        override val id: PackageId,
        override val metadata: CjPackageMetadata,
        override val dependencies: List<CjDependency>,
        override val features: Map<String, Feature>,
        val registry: String?,
        val downloadPath: JPath?,
        val checksum: String?
    ) : CjPackage()

    /**
     * Git 仓库包
     *
     * 从 Git 仓库获取的依赖
     *
     * @property url Git 仓库 URL
     * @property rev Git 提交哈希或引用
     * @property branch Git 分支
     * @property tag Git 标签
     * @property checkoutPath 检出后的本地路径
     */
    data class Git(
        override val id: PackageId,
        override val metadata: CjPackageMetadata,
        override val dependencies: List<CjDependency>,
        override val features: Map<String, Feature>,
        val url: String,
        val rev: String,
        val branch: String?,
        val tag: String?,
        val checkoutPath: JPath
    ) : CjPackage()

    /**
     * 本地路径包
     *
     * 通过本地文件系统路径引用的依赖
     *
     * @property path 包的本地路径
     */
    data class Path(
        override val id: PackageId,
        override val metadata: CjPackageMetadata,
        override val dependencies: List<CjDependency>,
        override val features: Map<String, Feature>,
        val path: JPath
    ) : CjPackage()

    /**
     * 标准库包
     *
     * 表示单包标准库，例如 std.core、std.ast
     *
     * @property path 标准库的本地路径
     */
    data class Stdlib(
        override val id: PackageId, val path: JPath,
        override val metadata: CjPackageMetadata = CjPackageMetadata.EMPTY,
        override val dependencies: List<CjDependency> = emptyList(),
        override val features: Map<String, Feature> = emptyMap(),

        ) : CjPackage()

    /**
     * 二进制库包
     *
     * 表示已编译的仓颉库产物文件，包含前端文件 (.cjo) 和库文件 (.so/.a)
     *
     * @property cjoPath 前端接口文件路径
     * @property libPath 库文件路径
     * @property target 目标平台标识
     */
    data class Binary(
        override val id: PackageId,
        override val metadata: CjPackageMetadata = CjPackageMetadata.EMPTY,
        val cjoPath: JPath,
        val libPath: JPath?,
        val target: String
    ) : CjPackage() {
        // 二进制依赖不处理传递依赖
        override val dependencies: List<CjDependency> = emptyList()
        override val features: Map<String, Feature> = emptyMap()
    }

    companion object {
        /**
         * 从现有 CjModule 转换为 CjPackage.LocalModule
         *
         * @param module 要转换的模块
         * @return 转换后的 LocalModule 实例
         */
        fun fromModule(module: CjModule): LocalModule {
            return LocalModule(
                id = PackageId(
                    name = module.name,
                    version = module.metadata.version,
                    sourceId = SourceId.Local(module.rootDir.toNioPath())
                ),
                metadata = module.metadata,
                dependencies = module.dependencies,
                features = emptyMap(), // TODO: 从配置读取 features
                module = module,
                sourceSets = module.sourceSets,
                intellijProject = module.project.intellijProject
            )
        }
    }
}

/**
 * CjModule 扩展方法：转换为 CjPackage
 */
fun CjModule.toPackage(): CjPackage.LocalModule = CjPackage.fromModule(this)

/**
 * 编译输出产物类型
 */
enum class CjOutputType {
    /** 可执行程序 */
    EXECUTABLE,

    /** 静态库 */
    STATIC,

    /** 动态库 */
    DYNAMIC
}

/**
 * 包元数据接口
 *
 * 表示包的元数据信息，包括名称、版本、依赖等
 */
interface CjPackageMetadata {
    /** 包名称 */
    val name: String

    /** 包组（可选） */
    val group: String?

    /** 包版本 */
    val version: CjVersion

    /** 包描述 */
    val description: String?

    /** 包作者列表 */
    val authors: List<String>

    /** 包许可证 */
    val license: String?

    /** 包仓库 URL */
    val repositoryUrl: String?

    /** 包依赖列表 */
    val dependencies: List<CjDependency>

    /** 包的本地路径（如果已下载） */
    val localPath: JPath?

    /** 包的输出类型 */
    val outputType: CjOutputType?

    companion object {
        /**
         * 空元数据实例，用于占位或初始化
         */
        val EMPTY: CjPackageMetadata = object : CjPackageMetadata {
            override val name: String = ""
            override val group: String? = null
            override val version: CjVersion = CjVersion.EMPTY
            override val description: String? = null
            override val authors: List<String> = emptyList()
            override val license: String? = null
            override val repositoryUrl: String? = null
            override val dependencies: List<CjDependency> = emptyList()
            override val localPath: JPath? = null
            override val outputType: CjOutputType? = null
        }
    }
}