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

package org.cangnova.cangjie.cjpm.dependency

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFileManager
import org.cangnova.cangjie.cjpm.config.lock.CjpmLockFile
import org.cangnova.cangjie.cjpm.config.lock.CjpmLockParser
import org.cangnova.cangjie.cjpm.config.lock.LockedDependency
import org.cangnova.cangjie.cjpm.config.toml.CjpmTomlParser
import org.cangnova.cangjie.cjpm.project.CjpmBuildSystemId
import org.cangnova.cangjie.cjpm.project.model.toml.CjpmTomlConfig
import org.cangnova.cangjie.cjpm.project.model.toml.DependencyConfig
import org.cangnova.cangjie.extension.CjDependencyResolver
import org.cangnova.cangjie.project.extension.ProjectBuildSystemId
import org.cangnova.cangjie.project.model.*
import org.cangnova.cangjie.toolchain.api.CjProjectSdkConfig
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.exists
import kotlin.io.path.isDirectory

/**
 * CJPM 依赖解析器实现
 *
 * 基于 cjpm.lock 文件解析依赖，确保使用精确的已锁定版本和路径
 */
class CjpmDependencyResolver : CjDependencyResolver {
    override fun getBuildSystemId(): ProjectBuildSystemId = CjpmBuildSystemId

    override val resolverName: String = "CJPM Dependency Resolver"

    /**
     * 提取依赖替换规则（replace）
     *
     * 从根模块的 cjpm.toml 配置文件中读取 [replace] 字段
     */
    override fun extractReplaceRules(rootPackage: CjPackage, project: Project): Map<String, CjDependency> {
        // 只有 LocalModule 才是项目根
        if (rootPackage !is CjPackage.LocalModule) {
            LOG.debug("Root package is not a LocalModule, no replace rules")
            return emptyMap()
        }

        try {
            // 查找 cjpm.toml 配置文件
            val configFile = rootPackage.module.configFile ?: run {
                LOG.debug("No config file found for module: ${rootPackage.module.name}")
                return emptyMap()
            }

            // 解析配置文件
            val config = CjpmTomlParser.parse(configFile) ?: run {
                LOG.warn("Failed to parse config file: ${configFile.path}")
                return emptyMap()
            }

            // 提取 replace 配置
            val replaceConfigs = config.replace
            if (replaceConfigs.isEmpty()) {
                return emptyMap()
            }

            // baseDir 是模块的根目录
            val baseDir = rootPackage.module.rootDir.toNioPath()

            // 转换为 CjDependency 映射
            val rules = mutableMapOf<String, CjDependency>()
            replaceConfigs.forEach { (name, depConfig) ->
                val dependency = createDependencyFromConfig(
                    name = name,
                    config = depConfig,
                    scope = CjDependencyScope.COMPILE,
                    sourceModule = rootPackage.module,
                    baseDir = baseDir
                )
                rules[name] = dependency
                LOG.info("Loaded replace rule: $name -> ${dependency.toDependencySource()}")
            }

            return rules
        } catch (e: Exception) {
            LOG.error("Failed to extract replace rules from root package", e)
            return emptyMap()
        }
    }

    companion object {
        private val LOG: Logger = logger<CjpmDependencyResolver>()

        /**
         * CJPM 缓存目录
         * Windows: %USERPROFILE%\.cjpm
         * Unix: ~/.cjpm
         */
        private val CJPM_CACHE_DIR: Path by lazy {
            val userHome = System.getProperty("user.home")
            Paths.get(userHome, ".cjpm")
        }
    }

    override fun canResolve(dependency: CjDependency): Boolean {
        // CJPM 可以解析所有类型的依赖
        return true
    }

    override fun resolve(dependency: CjDependency, project: Project): CjPackage? {
        LOG.info("Resolving dependency: ${dependency.name} (${dependency::class.simpleName})")

        // 处理标准库依赖
        if (dependency is CjDependency.Stdlib) {
            return resolveStdlibDependency(dependency, project)
        }

        // 处理二进制依赖
        if (dependency is CjDependency.Binary) {
            return resolveBinaryDependency(dependency, project)
        }

        // 处理其他类型的依赖（Git/Path/Library）
        return try {
            // 首先尝试从 lock 文件解析
            val lockFile = findLockFile(project)
            val result = lockFile?.let { resolveFromLockFile(dependency, it, project) }

            // 从 lock 文件成功解析
            result
                ?: // 没有 lock 文件，或者在 lock 文件中没找到依赖
                // 对于 Path 依赖，尝试直接解析
                when (dependency) {
                    is CjDependency.Path -> {
                        val reason = if (lockFile != null) {
                            "not found in cjpm.lock"
                        } else {
                            "no cjpm.lock found"
                        }
                        LOG.info("Path dependency ${dependency.name} $reason, attempting direct resolution")
                        resolvePathDependencyDirectly(dependency, project)
                    }

                    is CjDependency.Library -> {
                        // 对于 Library（中心仓）依赖，尝试直接从缓存解析
                        // 当前 CJPM 不会将中心仓依赖写入 lock 文件，所以直接解析
                        val reason = if (lockFile != null) {
                            "not found in cjpm.lock (central repository dependencies are not locked)"
                        } else {
                            "no cjpm.lock found"
                        }
                        LOG.info("Library dependency ${dependency.name} $reason, attempting direct resolution from cache")
                        resolveLibraryDependencyDirectly(dependency, project)
                    }

                    else -> {
                        // 其他类型（Git）需要 lock 文件
                        val errorMsg = if (lockFile != null) {
                            "Dependency not found in cjpm.lock. Please run 'cjpm update'."
                        } else {
                            "No cjpm.lock found. Please run 'cjpm update' to generate lock file."
                        }
                        LOG.warn("Cannot resolveName ${dependency::class.simpleName} dependency: ${dependency.name} - $errorMsg")
                        return CjPackage.Failed(
                            PackageId(
                                dependency.name,
                                CjVersion.EMPTY,
                                dependency.sourceId()
                            ), errorMsg
                        )
                    }
                }
        } catch (e: Exception) {
            LOG.warn("Failed to resolveName dependency: ${dependency.name}", e)
            CjPackage.Failed(
                PackageId(
                    dependency.name,
                    CjVersion.EMPTY,
                    dependency.sourceId()
                ), e.message ?: "Unknown error"
            )
        }
    }

    /**
     * 查找项目的 cjpm.lock 文件
     */
    private fun findLockFile(project: Project): CjpmLockFile? {
        try {
            val cjProject = project.cjProject
            val projectRoot = cjProject.rootDir.toNioPath()
            val lockPath = projectRoot.resolve("cjpm.lock")

            if (!lockPath.exists()) {
                LOG.debug("cjpm.lock not found at: $lockPath")
                return null
            }

            val vfsManager = VirtualFileManager.getInstance()
            val lockVFile = vfsManager.findFileByNioPath(lockPath)
            if (lockVFile == null) {
                LOG.warn("Cannot access cjpm.lock at: $lockPath")
                return null
            }

            return CjpmLockParser.parse(lockVFile)
        } catch (e: Exception) {
            LOG.error("Failed to load cjpm.lock", e)
            return null
        }
    }

    /**
     * 基于 lock 文件解析依赖
     */
    private fun resolveFromLockFile(
        dependency: CjDependency,
        lockFile: CjpmLockFile,
        project: Project
    ): CjPackage? {
        val lockedDep = lockFile.requires[dependency.name] ?: return null

        return when (lockedDep) {
            is LockedDependency.Git -> resolveLockedGitDependency(dependency, lockedDep, project)
            is LockedDependency.Registry -> resolveLockedRegistryDependency(dependency, lockedDep, project)
            is LockedDependency.Path -> resolveLockedPathDependency(dependency, lockedDep, project)
        }
    }

    /**
     * 解析锁定的 Git 依赖
     *
     * 对于 Git 依赖，commitId 对应 ~/.cjpm/git/<依赖名>/<commitId> 目录
     */
    private fun resolveLockedGitDependency(
        dependency: CjDependency,
        locked: LockedDependency.Git,
        project: Project
    ): CjPackage {
        LOG.info("Resolving locked git dependency: ${locked.name} @ ${locked.commitId}")

        // Git 依赖路径: ~/.cjpm/git/<依赖名>/<commitId>
        val gitCacheDir = CJPM_CACHE_DIR.resolve("git").resolve(locked.name).resolve(locked.commitId)

        if (!gitCacheDir.exists() || !gitCacheDir.isDirectory()) {
            return CjPackage.Failed(
                PackageId(dependency.name, CjVersion.EMPTY, dependency.sourceId()),
                "Git dependency not found at: $gitCacheDir. Please run 'cjpm update'."
            )
        }

        // 解析包信息
        val manifestPath = gitCacheDir.resolve("cjpm.toml")
        if (!manifestPath.exists()) {
            return CjPackage.Failed(
                PackageId(dependency.name, CjVersion.EMPTY, dependency.sourceId()),
                "No cjpm.toml found in git dependency: ${locked.name}"
            )
        }

        val vfsManager = VirtualFileManager.getInstance()
        val manifestVFile = vfsManager.findFileByNioPath(manifestPath)
        if (manifestVFile == null) {
            return CjPackage.Failed(
                PackageId(dependency.name, CjVersion.EMPTY, dependency.sourceId()),
                "Cannot access cjpm.toml in git dependency"
            )
        }

        val config = CjpmTomlParser.parse(manifestVFile)
        if (config?.`package` == null) {
            return CjPackage.Failed(
                PackageId(dependency.name, CjVersion.EMPTY, dependency.sourceId()),
                "Invalid cjpm.toml in git dependency"
            )
        }

        val pkg = CjpmPackageMetadata(
            name = config.`package`.name,
            version = CjVersion(config.`package`.version),
            description = config.`package`.description,
            repositoryUrl = locked.git,
            localPath = gitCacheDir
        )

        //  解析包的依赖声明 - 传递依赖相对路径应相对于此包的根目录
        val dependencies = parseDependenciesFromConfig(
            config = config,
            sourceModule = dependency,
            baseDir = gitCacheDir
        )

        LOG.info("Resolved git dependency: ${locked.name} with ${dependencies.size} dependencies")

        return CjPackage.Git(
            PackageId(
                name = dependency.name,
                version = CjVersion(config.`package`.version),
                sourceId = dependency.sourceId()
            ),
            pkg,
            dependencies,  // 返回解析后的依赖列表
            emptyMap(),    // TODO: 从配置读取 features
            locked.git,
            locked.commitId,
            branch = locked.branch,
            tag = locked.tag,
            checkoutPath = gitCacheDir,
        )


    }

    /**
     * 解析锁定的注册表依赖
     *
     * 从 lock 文件中获取中心仓依赖信息，然后从缓存读取
     * 路径: ~/.cjpm/repository/<name>-<version>/
     */
    private fun resolveLockedRegistryDependency(
        dependency: CjDependency,
        locked: LockedDependency.Registry,
        project: Project
    ): CjPackage {
        LOG.info("Resolving locked registry dependency: ${locked.name} @ ${locked.version}")

        // 中心仓缓存路径: ~/.cjpm/repository/<name>-<version>
        val packageDir = CJPM_CACHE_DIR.resolve("repository").resolve("${locked.name}-${locked.version}")

        return resolveRegistryPackage(dependency, packageDir, locked.name, CjVersion(locked.version))
    }

    /**
     * 解析锁定的路径依赖
     */
    private fun resolveLockedPathDependency(
        dependency: CjDependency,
        locked: LockedDependency.Path,
        project: Project
    ): CjPackage {
        LOG.info("Resolving locked path dependency: ${locked.name} at ${locked.path}")

        val cjProject = project.cjProject
        val projectRoot = cjProject.rootDir.toNioPath()
        val dependencyPath = projectRoot.resolve(locked.path)

        if (!dependencyPath.exists() || !dependencyPath.isDirectory()) {
            return CjPackage.Failed(
                PackageId(
                    name = dependency.name,
                    version = CjVersion.EMPTY,
                    sourceId = dependency.sourceId()
                ),
                "Path dependency does not exist: ${locked.path}"
            )
        }

        val manifestPath = dependencyPath.resolve("cjpm.toml")
        if (!manifestPath.exists()) {
            return CjPackage.Failed(
                PackageId(
                    name = dependency.name,
                    version = CjVersion.EMPTY,
                    sourceId = dependency.sourceId()
                ),
                "No cjpm.toml found at: ${locked.path}"
            )
        }

        val vfsManager = VirtualFileManager.getInstance()
        val manifestVFile = vfsManager.findFileByNioPath(manifestPath)
        if (manifestVFile == null) {
            return CjPackage.Failed(
                PackageId(
                    name = dependency.name,
                    version = CjVersion.EMPTY,
                    sourceId = dependency.sourceId()
                ),
                "Cannot access cjpm.toml at: ${locked.path}"
            )
        }

        val config = CjpmTomlParser.parse(manifestVFile)
        if (config?.`package` == null) {
            return CjPackage.Failed(
                PackageId(
                    name = dependency.name,
                    version = CjVersion.EMPTY,
                    sourceId = dependency.sourceId()
                ),
                "Invalid cjpm.toml: no package section found"
            )
        }

        val pkg = CjpmPackageMetadata(
            name = config.`package`.name,
            version = CjVersion(config.`package`.version),
            description = config.`package`.description,
            localPath = dependencyPath
        )

        //  解析包的依赖声明 - 传递依赖相对路径应相对于此包的根目录
        val dependencies = parseDependenciesFromConfig(
            config = config,
            sourceModule = dependency,
            baseDir = dependencyPath
        )

        LOG.info("Resolved path dependency: ${locked.name} with ${dependencies.size} dependencies")

        // 检查该路径依赖是否指向工作空间内的模块
        val targetModule = cjProject.findModule(dependency.name)
        if (targetModule != null) {
            // 验证路径是否匹配
            val targetModulePath = targetModule.rootDir.toNioPath().normalize()
            if (targetModulePath == dependencyPath.normalize()) {
                LOG.debug("Locked path dependency ${dependency.name} resolves to workspace module, returning LocalModule")
             return CjPackage.fromModule(targetModule)
            }
        }

        // 外部路径依赖，返回 Path 包
        return CjPackage.Path(
            PackageId(
                name = config.`package`.name,
                version = CjVersion(config.`package`.version),
                sourceId = dependency.sourceId()
            ),
            pkg,
            dependencies,  // 返回解析后的依赖列表
            emptyMap(),    // TODO: 从配置读取 features
            dependencyPath
        )
    }

    /**
     * 直接解析路径依赖（不使用 lock 文件）
     *
     * 用于在没有 cjpm.lock 文件时解析本地路径依赖
     * 直接从依赖声明中的路径读取 cjpm.toml 并解析
     */
    private fun resolvePathDependencyDirectly(
        dependency: CjDependency.Path,
        project: Project
    ): CjPackage {

        LOG.info("Directly resolving path dependency: ${dependency.name} at ${dependency.path}")

        val cjProject = project.cjProject

        // 使用依赖的 sourceModule 来确定基准目录
        val baseDir = when (val source = dependency.sourceModule) {
            is CjModule -> {
                // 如果源是模块，使用模块的根目录
                source.rootDir.toNioPath()
            }
            is CjDependency -> {
                // 如果源是另一个依赖（传递依赖），需要先解析该依赖获取其路径
                // 这种情况较复杂，暂时回退到项目根目录
                LOG.warn("Source of dependency ${dependency.name} is another dependency, using project root")
                cjProject.rootDir.toNioPath()
            }
            else -> {
                // 未知类型，使用项目根目录
                LOG.warn("Unknown source type for dependency ${dependency.name}, using project root")
                cjProject.rootDir.toNioPath()
            }
        }

        // 处理相对路径和绝对路径
        val dependencyPath = if (dependency.path.isAbsolute) {
            dependency.path
        } else {
            baseDir.resolve(dependency.path).normalize()
        }

        if (!dependencyPath.exists() || !dependencyPath.isDirectory()) {
            return CjPackage.Failed(
                PackageId(
                    name = dependency.name,
                    version = CjVersion.EMPTY,
                    sourceId = dependency.sourceId()
                ),
                "Path dependency does not exist: ${dependency.path} (resolved to: $dependencyPath, base: $baseDir)"
            )
        }

        val manifestPath = dependencyPath.resolve("cjpm.toml")
        if (!manifestPath.exists()) {
            return CjPackage.Failed(
                PackageId(
                    name = dependency.name,
                    version = CjVersion.EMPTY,
                    sourceId = dependency.sourceId()
                ),
                "No cjpm.toml found at: ${dependency.path}"
            )
        }

        val vfsManager = VirtualFileManager.getInstance()
        val manifestVFile = vfsManager.findFileByNioPath(manifestPath)
        if (manifestVFile == null) {
            return CjPackage.Failed(
                PackageId(
                    name = dependency.name,
                    version = CjVersion.EMPTY,
                    sourceId = dependency.sourceId()
                ),
                "Cannot access cjpm.toml at: ${dependency.path}"
            )
        }

        val config = CjpmTomlParser.parse(manifestVFile)
        if (config?.`package` == null) {
            return CjPackage.Failed(
                PackageId(
                    name = dependency.name,
                    version = CjVersion.EMPTY,
                    sourceId = dependency.sourceId()
                ),
                "Invalid cjpm.toml: no package section found"
            )
        }

        val pkg = CjpmPackageMetadata(
            name = config.`package`.name,
            version = CjVersion(config.`package`.version),
            description = config.`package`.description,
            localPath = dependencyPath
        )

        // 解析包的依赖声明 - 传递依赖相对路径应相对于此包的根目录
        val dependencies = parseDependenciesFromConfig(
            config = config,
            sourceModule = dependency,
            baseDir = dependencyPath
        )

        LOG.info("Resolved path dependency directly: ${dependency.name} with ${dependencies.size} dependencies")

        // 检查该路径依赖是否指向工作空间内的模块
        val targetModule = cjProject.findModule(dependency.name)
        if (targetModule != null) {
            // 验证路径是否匹配
            val targetModulePath = targetModule.rootDir.toNioPath().normalize()
            if (targetModulePath == dependencyPath) {
                LOG.debug("Path dependency ${dependency.name} resolves to workspace module, returning LocalModule")

                return CjPackage.fromModule(targetModule)

            }
        }

        // 外部路径依赖，返回 Path 包
        return CjPackage.Path(
            PackageId(
                name = config.`package`.name,
                version = CjVersion(config.`package`.version),
                sourceId = dependency.sourceId()
            ),
            pkg,
            dependencies,  // 返回解析后的依赖列表
            emptyMap(),    // TODO: 从配置读取 features
            dependencyPath
        )
    }


    /**
     * 直接解析中心仓依赖（不使用 lock 文件）
     *
     * 用于在没有 cjpm.lock 文件或 lock 文件中没有该依赖时解析中心仓依赖
     * 直接从 ~/.cjpm/repository/<name>-<version>/ 读取 cjpm.toml 并解析
     *
     * @param dependency 中心仓依赖
     * @param project IntelliJ 项目
     * @return 解析后的包对象
     */
    private fun resolveLibraryDependencyDirectly(
        dependency: CjDependency.Library,
        project: Project
    ): CjPackage {
        LOG.info("Directly resolving library dependency: ${dependency.name} @ ${dependency.versionReq}")

        // 获取版本号（从 versionReq 中提取具体版本）
        val version = when (val req = dependency.versionReq) {
            is VersionRequirement.Exact -> req.version
           else -> {
                // 对于范围版本，暂时不支持，返回错误
                return CjPackage.Failed(
                    PackageId(
                        name = dependency.name,
                        version = CjVersion.EMPTY,
                        sourceId = dependency.sourceId()
                    ),
                    "Version range resolution not yet supported for library dependencies. Please specify exact version."
                )
            }
        }

        // 中心仓缓存路径: ~/.cjpm/repository/<name>-<version>
        val packageDir = CJPM_CACHE_DIR.resolve("repository").resolve("${dependency.name}-${version}")

        return resolveRegistryPackage(dependency, packageDir, dependency.name, version)
    }

    /**
     * 从中心仓缓存解析包
     *
     * 通用方法，用于从 ~/.cjpm/repository/<name>-<version>/ 解析包信息
     * 被 resolveLockedRegistryDependency 和 resolveLibraryDependencyDirectly 共同使用
     *
     * @param dependency 依赖声明
     * @param packageDir 包缓存目录
     * @param name 包名
     * @param version 包版本
     * @return 解析后的包对象
     */
    private fun resolveRegistryPackage(
        dependency: CjDependency,
        packageDir: Path,
        name: String,
        version: CjVersion
    ): CjPackage {
        // 检查缓存目录是否存在
        if (!packageDir.exists() || !packageDir.isDirectory()) {
            return CjPackage.Failed(
                PackageId(
                    name = name,
                    version = CjVersion.EMPTY,
                    sourceId = dependency.sourceId()
                ),
                "Registry package not found at: $packageDir. Please run 'cjpm update' to download dependencies."
            )
        }

        // 解析包的 cjpm.toml
        val manifestPath = packageDir.resolve("cjpm.toml")
        if (!manifestPath.exists()) {
            return CjPackage.Failed(
                PackageId(
                    name = name,
                    version = CjVersion.EMPTY,
                    sourceId = dependency.sourceId()
                ),
                "No cjpm.toml found in registry package: $name at $packageDir"
            )
        }

        val vfsManager = VirtualFileManager.getInstance()
        val manifestVFile = vfsManager.findFileByNioPath(manifestPath)
        if (manifestVFile == null) {
            return CjPackage.Failed(
                PackageId(
                    name = name,
                    version = CjVersion.EMPTY,
                    sourceId = dependency.sourceId()
                ),
                "Cannot access cjpm.toml in registry package: $name"
            )
        }

        val config = CjpmTomlParser.parse(manifestVFile)
        if (config?.`package` == null) {
            return CjPackage.Failed(
                PackageId(
                    name = name,
                    version = CjVersion.EMPTY,
                    sourceId = dependency.sourceId()
                ),
                "Invalid cjpm.toml in registry package: $name"
            )
        }

        // 创建包元数据
        val pkg = CjpmPackageMetadata(
            name = config.`package`.name,
            version = CjVersion(config.`package`.version),
            description = config.`package`.description,
            localPath = packageDir
        )

        // 解析包的依赖声明 - 传递依赖相对路径应相对于此包的根目录
        val dependencies = parseDependenciesFromConfig(
            config = config,
            sourceModule = dependency,
            baseDir = packageDir
        )

        LOG.info("Resolved registry package: $name @ $version with ${dependencies.size} dependencies")

        // 获取 registry URL（如果依赖声明中有指定）
        val registryUrl = if (dependency is CjDependency.Library) {
            dependency.registry
        } else {
            null
        }

        // 返回 Library 类型的包
        return CjPackage.Library(
            id = PackageId(
                name = config.`package`.name,
                version = CjVersion(config.`package`.version),
                sourceId = dependency.sourceId()
            ),
            metadata = pkg,
            dependencies = dependencies,
            features = emptyMap(),  // TODO: 从配置读取 features
            registry = registryUrl,
            downloadPath = packageDir,  // 缓存目录即为下载路径
            checksum = null  // TODO: 支持 checksum 验证
        )
    }

    /**
     * 从 CjpmTomlConfig 解析依赖列表
     *
     * @param config CJPM 配置对象
     * @param sourceModule 声明这些依赖的源（模块或依赖）
     * @param baseDir 配置文件所在的目录，用于解析相对路径
     * @return 依赖声明列表
     */
    private fun parseDependenciesFromConfig(
        config: CjpmTomlConfig,
        sourceModule: CjDependencyDeclarant,
        baseDir: Path
    ): List<CjDependency> {
        val result = mutableListOf<CjDependency>()

        // 解析编译时依赖
        config.dependencies.forEach { (name, depConfig) ->
            result.add(createDependencyFromConfig(name, depConfig, CjDependencyScope.COMPILE, sourceModule, baseDir))
        }

        // 解析测试时依赖
        config.testDependencies.forEach { (name, depConfig) ->
            result.add(createDependencyFromConfig(name, depConfig, CjDependencyScope.TEST, sourceModule, baseDir))
        }

        return result
    }

    /**
     * 从 TargetConfig 解析二进制依赖列表
     *
     * 处理 targetPlatform.xxx.bin-dependencies 配置，支持两种方式：
     * 1. path-option: 自动扫描目录下的所有 .cjo 文件
     * 2. package-option: 明确指定包名和 .cjo 文件路径
     *
     * @param config CJPM 配置对象
     * @param targetPlatform 目标平台（如 x86_64-unknown-linux-gnu）
     * @param projectRoot 项目根目录
     * @param sourceModule 声明这些依赖的源模块
     * @return 二进制依赖列表
     */
    fun parseBinaryDependenciesFromConfig(
        config: CjpmTomlConfig,
        targetPlatform: String,
        projectRoot: Path,
        sourceModule: CjDependencyDeclarant
    ): List<CjDependency.Binary> {
        val result = mutableListOf<CjDependency.Binary>()

        // 获取指定平台的 TargetConfig
        val targetConfig = config.target[targetPlatform] ?: return emptyList()
        val binDepsConfig = targetConfig.binDependencies ?: return emptyList()

        LOG.info("Parsing binary dependencies for targetPlatform: $targetPlatform")

        // 处理 package-option（优先级更高）
        binDepsConfig.packageOption?.forEach { (packageName, cjoPath) ->
            val resolvedCjoPath = projectRoot.resolve(cjoPath)
            result.add(
                CjDependency.Binary(
                    name = packageName,
                    cjoPath = resolvedCjoPath,
                    libPath = null,  // 自动查找
                    target = targetPlatform,
                    scope = CjDependencyScope.COMPILE,
                    sourceModule = sourceModule
                )
            )
            LOG.debug("Added binary dependency from package-option: $packageName -> $cjoPath")
        }

        // 处理 path-option（自动扫描）
        binDepsConfig.pathOption?.forEach { pathStr ->
            val scanDir = projectRoot.resolve(pathStr)
            if (scanDir.exists() && scanDir.isDirectory()) {
                val scannedDeps = scanDirectoryForBinaryDependencies(scanDir, targetPlatform, sourceModule)
                result.addAll(scannedDeps)
                LOG.debug("Scanned ${scannedDeps.size} binary dependencies from path-option: $pathStr")
            } else {
                LOG.warn("path-option directory does not exist: $pathStr")
            }
        }

        LOG.info("Parsed ${result.size} binary dependencies for targetPlatform $targetPlatform")
        return result
    }

    /**
     * 扫描目录查找所有符合规则的二进制依赖
     *
     * 规则：查找所有 .cjo 文件，包名从文件名提取（去除 .cjo 后缀）
     * 对应的库文件必须为 lib<包名>.so 或 lib<包名>.a
     *
     * @param directory 要扫描的目录
     * @param targetPlatform 目标平台
     * @param sourceModule 声明这些依赖的源模块
     * @return 扫描到的二进制依赖列表
     */
    private fun scanDirectoryForBinaryDependencies(
        directory: Path,
        targetPlatform: String,
        sourceModule: CjDependencyDeclarant
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
                                    scope = CjDependencyScope.COMPILE,
                                    sourceModule = sourceModule
                                )
                            )
                            LOG.debug("Found binary dependency: $packageName (cjo=$cjoPath, lib=$libPath)")
                        } else {
                            LOG.warn("Found .cjo file but no matching library file: $cjoPath")
                        }
                    }
            }
        } catch (e: Exception) {
            LOG.error("Failed to scan directory for binary dependencies: $directory", e)
        }

        return result
    }

    /**
     * 从 DependencyConfig 创建 CjDependency 对象
     *
     * @param name 依赖名称
     * @param config 依赖配置
     * @param scope 依赖作用域
     * @param sourceModule 声明此依赖的源（模块或依赖）
     * @param baseDir 配置文件所在的目录，用于解析相对路径
     * @return CjDependency 对象
     */
    private fun createDependencyFromConfig(
        name: String,
        config: DependencyConfig,
        scope: CjDependencyScope,
        sourceModule: CjDependencyDeclarant,
        baseDir: Path
    ): CjDependency {
        val version = CjVersion(config.version )
        val versionReq = VersionRequirement.Exact(version)

        return when {
            // 路径依赖 - 立即解析相对路径为绝对路径
            config.path != null -> {
                val rawPath = Paths.get(config.path)

                // 将相对路径解析为绝对路径
                val resolvedPath = if (rawPath.isAbsolute) {
                    rawPath
                } else {
                    baseDir.resolve(rawPath).normalize()
                }

                LOG.debug("Resolved path dependency: $name from ${config.path} to $resolvedPath (base: $baseDir)")

                CjDependency.Path(
                    name = name,
                    path = resolvedPath,  // 存储绝对路径
                    versionReq = versionReq,
                    scope = scope,
                    sourceModule = sourceModule
                )
            }

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
                    scope = scope,
                    sourceModule = sourceModule
                )
            }

            // 库依赖（默认）
            else -> CjDependency.Library(
                name = name,
                versionReq = versionReq,
                scope = scope,
                sourceModule = sourceModule
            )
        }
    }

    /**
     * 解析标准库依赖
     *
     * 标准库由 SDK 提供，位于 {sdkHome}/modules 目录
     */
    private fun resolveStdlibDependency(
        dependency: CjDependency.Stdlib,
        project: Project
    ): CjPackage {
        LOG.info("Resolving stdlib dependency: ${dependency.name}")

        // 获取项目关联的 SDK
        val sdkConfig = CjProjectSdkConfig.getInstance(project)
        val sdk = sdkConfig.getProjectSdk()

        if (sdk == null) {
            return CjPackage.Failed(
                PackageId(
                    dependency.name,
                    CjVersion(),
                    SourceId.Stdlib
                ),
                "No SDK configured for project. Please configure a CangJie SDK."
            )
        }

        // 标准库路径: {sdkHome}/modules
        val stdlibPath = sdk.stdlibPath

        if (!stdlibPath.exists() || !stdlibPath.isDirectory()) {
            return CjPackage.Failed(
                PackageId(
                    dependency.name,
                    CjVersion(),
                    SourceId.Stdlib
                ),
                "Standard library not found at: $stdlibPath. SDK may be corrupted."
            )
        }


        LOG.info("Resolved stdlib from SDK: ${sdk.name} at $stdlibPath")

        // 标准库没有传递依赖
        return CjPackage.Stdlib(
            PackageId(
                dependency.name,
                CjVersion(sdk.version?.semver?.parsedVersion),
                SourceId.Stdlib
            ),
            stdlibPath


        )
    }

    /**
     * 解析二进制依赖
     *
     * 二进制依赖包含编译好的 .cjo 文件和对应的库文件 (.so 或 .a)
     * 直接导入使用，不处理传递依赖
     */
    private fun resolveBinaryDependency(
        dependency: CjDependency.Binary,
        project: Project
    ): CjPackage {
        LOG.info("Resolving binary dependency: ${dependency.name}")

        val cjProject = project.cjProject
        val projectRoot = cjProject.rootDir.toNioPath()

        // 解析 cjo 文件路径（相对于项目根目录）
        val cjoPath = if (dependency.cjoPath.isAbsolute) {
            dependency.cjoPath
        } else {
            projectRoot.resolve(dependency.cjoPath)
        }

        // 检查 cjo 文件是否存在
        if (!cjoPath.exists()) {
            return CjPackage.Failed(
                PackageId(dependency.name, CjVersion.EMPTY, dependency.sourceId()),
                "Binary dependency .cjo file not found: ${dependency.cjoPath}"
            )
        }

        // 查找对应的库文件
        val libPath = dependency.libPath ?: findLibraryFile(cjoPath)

        if (libPath == null) {
            LOG.warn("No library file (.so/.a) found for binary dependency: ${dependency.name}")
        } else if (!libPath.exists()) {
            return CjPackage.Failed(
                PackageId(dependency.name, CjVersion.EMPTY, dependency.sourceId()),
                "Binary dependency library file not found: $libPath"
            )
        }

        LOG.info("Resolved binary dependency: ${dependency.name} (cjo=$cjoPath, lib=$libPath)")

        return CjPackage.Binary(
            id = PackageId(
                name = dependency.name,
                version = CjVersion.EMPTY,
                sourceId = dependency.sourceId()
            ),
            cjoPath = cjoPath,
            libPath = libPath,
            target = dependency.target
        )
    }

    /**
     * 查找与 .cjo 文件对应的库文件
     *
     * 根据文档，库文件名称规则为：lib<完整包名>.so 或 lib<完整包名>.a
     * 例如：pro0.xoo.cjo 对应 libpro0.xoo.so 或 libpro0.xoo.a
     *
     * @param cjoPath .cjo 文件路径
     * @return 库文件路径，如果找不到返回 null
     */
    private fun findLibraryFile(cjoPath: Path): Path? {
        val cjoFileName = cjoPath.fileName.toString()
        if (!cjoFileName.endsWith(".cjo")) {
            return null
        }

        // 提取包名：pro0.xoo.cjo -> pro0.xoo
        val packageName = cjoFileName.substringBeforeLast(".cjo")

        // 同目录查找 .so 或 .a 文件
        val cjoDir = cjoPath.parent ?: return null

        // 优先查找 .so 文件
        val soPath = cjoDir.resolve("lib$packageName.so")
        if (soPath.exists()) {
            return soPath
        }

        // 然后查找 .a 文件
        val aPath = cjoDir.resolve("lib$packageName.a")
        if (aPath.exists()) {
            return aPath
        }

        return null
    }


}