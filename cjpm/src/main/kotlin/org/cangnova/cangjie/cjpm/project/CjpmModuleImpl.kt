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

package org.cangnova.cangjie.cjpm.project

import com.intellij.openapi.vfs.VirtualFile
import org.cangnova.cangjie.cjpm.config.toml.CjpmTomlParser
import org.cangnova.cangjie.cjpm.project.model.toml.DependencyConfig
import org.cangnova.cangjie.cjpm.project.model.toml.PackageConfig

import org.cangnova.cangjie.project.model.*
import kotlin.io.path.exists
import kotlin.io.path.isDirectory

/**
 * CJPM 模块实现
 */
class CjpmModuleImpl(
    override val name: String,
    override val rootDir: VirtualFile,
    override val project: CjProject,

    val packageConfig: PackageConfig
) : CjModule {


    override val configFile: VirtualFile?
        get() = rootDir.findChild("cjpm.toml")

    override val sourceSets: List<CjSourceSet> by lazy {
        buildSourceSets()
    }

    /**
     * 编译时依赖（源码依赖和二进制依赖）
     *
     * 从 cjpm.toml 中解析编译时依赖，包括：
     * - 外部库依赖 (Library)
     * - 路径依赖/模块依赖 (Path)
     * - Git 依赖 (Git)
     * - 二进制依赖 (Binary)
     * - 标准库依赖 (Stdlib)
     */
    override val dependencies: List<CjDependency> by lazy {
        buildCompileDependencies()
    }

    /**
     * 测试依赖
     *
     * 从 cjpm.toml 的 [test-dependencies] 中解析测试依赖
     */
    override val testDependencies: List<CjDependency> by lazy {
        buildTestDependencies()
    }

    /**
     * 模块元数据
     *
     * 从 cjpm.toml 的 [package] 配置中读取元数据信息
     */
    override val metadata: CjPackageMetadata by lazy {
        object : CjPackageMetadata {
            override val name: String = this@CjpmModuleImpl.name
            override val group: String? = null
            override val version: CjVersion = CjVersion(packageConfig.version)
            override val description: String? = packageConfig.description
            override val authors: List<String> = packageConfig.authors ?: emptyList()
            override val license: String? = packageConfig.license
            override val repositoryUrl: String? = packageConfig.repositoryUrl
            override val dependencies: List<CjDependency> = this@CjpmModuleImpl.dependencies
            override val localPath: java.nio.file.Path = rootDir.toNioPath()
        }
    }


    /**
     * 构建编译时依赖（源码依赖和二进制依赖）
     *
     * 从 cjpm.toml 的 [dependencies] 和当前目标平台的 [targetPlatform.<platform>.dependencies] 及 [targetPlatform.<platform>.bin-dependencies] 中解析编译时依赖
     */
    private fun buildCompileDependencies(): List<CjDependency> {
        val manifestFile = configFile ?: return emptyList()
        val config = CjpmTomlParser.parse(manifestFile) ?: return emptyList()

        val result = mutableListOf<CjDependency>()

        // 1. 解析全局编译时依赖
        config.dependencies.forEach { (name, depConfig) ->
            result.add(createDependencyFromConfig(name, depConfig, CjDependencyScope.COMPILE))
        }

        // 2. 获取当前目标平台，解析平台特定的依赖
        val sdk = project.intellijProject.cjSdk
        val targetPlatform = sdk?.version?.targetPlatform

        if (targetPlatform != null) {
            val targetConfig = config.target[targetPlatform]
            if (targetConfig != null) {
                // 解析平台特定的源码依赖
                targetConfig.dependencies?.forEach { (name, depConfig) ->
                    result.add(createDependencyFromConfig(name, depConfig, CjDependencyScope.COMPILE))
                }

                // 解析平台特定的二进制依赖
                targetConfig.binDependencies?.let { binDepsConfig ->
                    // 处理 package-option（明确指定的二进制依赖）
                    binDepsConfig.packageOption?.forEach { (packageName, cjoPath) ->
                        val resolvedCjoPath = rootDir.toNioPath().resolve(cjoPath)
                        result.add(
                            CjDependency.Binary(
                                name = packageName,
                                cjoPath = resolvedCjoPath,
                                libPath = null,  // 自动查找
                                target = targetPlatform,
                                scope = CjDependencyScope.COMPILE
                            )
                        )
                    }

                    // 处理 path-option（自动扫描的二进制依赖）
                    binDepsConfig.pathOption?.forEach { pathStr ->
                        val scanDir = rootDir.toNioPath().resolve(pathStr)
                        if (scanDir.exists() && scanDir.isDirectory()) {
                            val scannedDeps = scanDirectoryForBinaryDependencies(scanDir, targetPlatform)
                            result.addAll(scannedDeps)
                        }
                    }
                }
            }
        }

        // 3. 增加 stdlib
        sdk?.let {
            result.add(
                CjDependency.Stdlib(
                    name = "stdlib",
                    versionReq = VersionRequirement.Exact(CjVersion(it.version.toString())),
                    scope = CjDependencyScope.COMPILE
                )
            )
        }

        return result
    }

    /**
     * 构建测试依赖
     *
     * 从 cjpm.toml 的 [test-dependencies] 和当前目标平台的 [targetPlatform.<platform>.test-dependencies] 中解析测试依赖
     */
    private fun buildTestDependencies(): List<CjDependency> {
        val manifestFile = configFile ?: return emptyList()
        val config = CjpmTomlParser.parse(manifestFile) ?: return emptyList()

        val result = mutableListOf<CjDependency>()

        // 1. 解析全局测试依赖
        config.testDependencies.forEach { (name, depConfig) ->
            result.add(createDependencyFromConfig(name, depConfig, CjDependencyScope.TEST))
        }

        // 2. 获取当前目标平台，解析平台特定的测试依赖
        val sdk = project.intellijProject.cjSdk
        val targetPlatform = sdk?.version?.targetPlatform

        if (targetPlatform != null) {
            val targetConfig = config.target[targetPlatform]
            targetConfig?.testDependencies?.forEach { (name, depConfig) ->
                result.add(createDependencyFromConfig(name, depConfig, CjDependencyScope.TEST))
            }
        }

        return result
    }

    /**
     * 扫描目录查找所有符合规则的二进制依赖
     *
     * 规则：查找所有 .cjo 文件，包名从文件名提取（去除 .cjo 后缀）
     * 对应的库文件必须为 lib<包名>.so 或 lib<包名>.a
     */
    private fun scanDirectoryForBinaryDependencies(
        directory: java.nio.file.Path,
        targetPlatform: String
    ): List<CjDependency.Binary> {
        val result = mutableListOf<CjDependency.Binary>()

        try {
            java.nio.file.Files.walk(directory, 1).use { paths ->
                paths.filter { it.toString().endsWith(".cjo") }
                    .forEach { cjoPath ->
                        val cjoFileName = cjoPath.fileName.toString()
                        val packageName = cjoFileName.substringBeforeLast(".cjo")

                        // 检查是否存在对应的库文件
                        val libPath = findLibraryFile(cjoPath)
                        if (libPath != null) {
                            result.add(
                                CjDependency.Binary(
                                    name = packageName,
                                    cjoPath = cjoPath,
                                    libPath = libPath,
                                    target = targetPlatform,
                                    scope = CjDependencyScope.COMPILE
                                )
                            )
                        }
                    }
            }
        } catch (e: Exception) {
            // 忽略扫描错误
        }

        return result
    }

    /**
     * 查找与 .cjo 文件对应的库文件
     *
     * 根据文档，库文件名称规则为：lib<完整包名>.so 或 lib<完整包名>.a
     */
    private fun findLibraryFile(cjoPath: java.nio.file.Path): java.nio.file.Path? {
        val cjoFileName = cjoPath.fileName.toString()
        if (!cjoFileName.endsWith(".cjo")) {
            return null
        }

        val packageName = cjoFileName.substringBeforeLast(".cjo")
        val cjoDir = cjoPath.parent ?: return null

        // 优先查找 .so 文件
        val soPath = cjoDir.resolve("lib$packageName.so")
        if (java.nio.file.Files.exists(soPath)) {
            return soPath
        }

        // 然后查找 .a 文件
        val aPath = cjoDir.resolve("lib$packageName.a")
        if (java.nio.file.Files.exists(aPath)) {
            return aPath
        }

        return null
    }

    /**
     * 从配置创建依赖对象（新实现，使用 sealed class）
     */
    private fun createDependencyFromConfig(
        name: String,
        config: DependencyConfig,
        scope: CjDependencyScope
    ): CjDependency {
        val version = CjVersion(config.version)
        val versionReq = VersionRequirement.Exact(version)

        return when {
            // 路径依赖
            config.path != null -> CjDependency.Path(
                name = name,
                path = java.nio.file.Paths.get(config.path),
                versionReq = versionReq,
                scope = scope
            )

            // Git 依赖
            config.git != null -> {
                val ref = when {
                    config.branch != null -> GitRef.Branch(config.branch)
                    config.tag != null -> GitRef.Tag(config.tag)
                    config.rev != null -> GitRef.Rev(config.rev)
                    else -> GitRef.Branch("main")  // 默认分支
                }

                CjDependency.Git(
                    name = name,
                    url = config.git,
                    ref = ref,
                    versionReq = versionReq,
                    scope = scope
                )
            }

            // 库依赖（默认）
            else -> CjDependency.Library(
                name = name,
                versionReq = versionReq,
                scope = scope
            )
        }
    }


    private fun buildSourceSets(): List<CjSourceSet> {
        val srcDir = packageConfig.srcDir
        val targetDir = packageConfig.targetDir
        val srcFile = rootDir.findFileByRelativePath(srcDir)

        return if (srcFile != null) {
            listOf(
                CjpmSourceSetImpl(
                    name = "main",
                    rootDir = rootDir,
                    srcDir = srcDir,
                    targetDir = targetDir,
                    isTest = false
                )
            )
        } else {
            emptyList()
        }
    }


}

/**
 * CJPM 源码集实现
 */
class CjpmSourceSetImpl(
    override val name: String,
    private val rootDir: VirtualFile,
    private val srcDir: String,
    private val targetDir: String,
    override val isTest: Boolean
) : CjSourceSet {

    override val sourceRoots: List<VirtualFile>
        get() {
            if (!rootDir.isValid) return emptyList()
            val srcFile = rootDir.findFileByRelativePath(srcDir)
            return if (srcFile != null && srcFile.isValid && srcFile.isDirectory) {
                listOf(srcFile)
            } else {
                emptyList()
            }
        }

    override val resourceRoots: List<VirtualFile>
        get() {
            if (!rootDir.isValid) return emptyList()
            val resourcesDir = rootDir.findFileByRelativePath("resources")
            return if (resourcesDir != null && resourcesDir.isValid && resourcesDir.isDirectory) {
                listOf(resourcesDir)
            } else {
                emptyList()
            }
        }

    override val outputDirectory: List<VirtualFile>
        get() {
            if (!rootDir.isValid) return emptyList()
            val targetDirFile = rootDir.findFileByRelativePath(targetDir)
            return if (targetDirFile != null && targetDirFile.isValid && targetDirFile.isDirectory) {
                listOf(targetDirFile)
            } else {
                emptyList()
            }
        }
}

 