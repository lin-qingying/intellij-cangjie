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

import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFileManager
import org.cangnova.cangjie.cjpm.config.lock.CjpmLockFile
import org.cangnova.cangjie.cjpm.config.lock.CjpmLockParser
import org.cangnova.cangjie.cjpm.config.lock.LockedDependency
import org.cangnova.cangjie.cjpm.project.CjpmBuildSystemId
import org.cangnova.cangjie.cjpm.project.model.toml.CjpmTomlConfig
import org.cangnova.cangjie.cjpm.project.model.toml.CjpmTomlParser
import org.cangnova.cangjie.cjpm.project.model.toml.DependencyConfig
import org.cangnova.cangjie.extension.CjDependencyResolver
import org.cangnova.cangjie.model.*
import org.cangnova.cangjie.project.extension.ProjectBuildSystemId
import org.cangnova.cangjie.project.model.cjProject
import java.nio.file.Files
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

    companion object {
        private val LOG = logger<CjpmDependencyResolver>()

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

    override fun resolve(dependency: CjDependency, project: Project): CjResolvedDependency? {
        LOG.info("Resolving dependency: ${dependency.name} (${dependency::class.simpleName})")
        if (dependency is CjDependency.Stdlib) {
            return resolveStdlibDependency(dependency, project)
        }
        return try {
            // 首先尝试从 lock 文件解析
            val lockFile = findLockFile(project)
            if (lockFile != null) {
                resolveFromLockFile(dependency, lockFile, project)
            } else {
                // 如果没有 lock 文件，回退到传统方式
                LOG.warn("No cjpm.lock found, falling back to traditional resolution for: ${dependency.name}")
                resolveWithoutLock(dependency, project)
            }
        } catch (e: Exception) {
            LOG.warn("Failed to resolve dependency: ${dependency.name}", e)
            CjpmResolvedDependency.failed(dependency, e.message ?: "Unknown error")
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
    ): CjResolvedDependency? {
        val lockedDep = lockFile.requires[dependency.name]
        if (lockedDep == null) {
            LOG.warn("Dependency ${dependency.name} not found in cjpm.lock")
            return CjpmResolvedDependency.failed(
                dependency,
                "Dependency not found in cjpm.lock. Please run 'cjpm update'."
            )
        }

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
    ): CjResolvedDependency {
        LOG.info("Resolving locked git dependency: ${locked.name} @ ${locked.commitId}")

        // Git 依赖路径: ~/.cjpm/git/<依赖名>/<commitId>
        val gitCacheDir = CJPM_CACHE_DIR.resolve("git").resolve(locked.name).resolve(locked.commitId)

        if (!gitCacheDir.exists() || !gitCacheDir.isDirectory()) {
            return CjpmResolvedDependency.failed(
                dependency,
                "Git dependency not found at: $gitCacheDir. Please run 'cjpm update'."
            )
        }

        // 解析包信息
        val manifestPath = gitCacheDir.resolve("cjpm.toml")
        if (!manifestPath.exists()) {
            return CjpmResolvedDependency.failed(
                dependency,
                "No cjpm.toml found in git dependency: ${locked.name}"
            )
        }

        val vfsManager = VirtualFileManager.getInstance()
        val manifestVFile = vfsManager.findFileByNioPath(manifestPath)
        if (manifestVFile == null) {
            return CjpmResolvedDependency.failed(
                dependency,
                "Cannot access cjpm.toml in git dependency"
            )
        }

        val config = CjpmTomlParser.parse(manifestVFile)
        if (config?.`package` == null) {
            return CjpmResolvedDependency.failed(
                dependency,
                "Invalid cjpm.toml in git dependency"
            )
        }

        val pkg = CjpmPackage(
            name = config.`package`.name,
            version = CjVersion(config.`package`.version),
            description = config.`package`.description,
            repositoryUrl = locked.git,
            localPath = gitCacheDir
        )

        val transitiveDeps = if (dependency.transitive) {
            parseTransitiveDependencies(config, dependency.scope)
        } else {
            emptyList()
        }

        return CjpmResolvedDependency.resolvedAsPackage(dependency, pkg, transitiveDeps)
    }

    /**
     * 解析锁定的注册表依赖
     */
    private fun resolveLockedRegistryDependency(
        dependency: CjDependency,
        locked: LockedDependency.Registry,
        project: Project
    ): CjResolvedDependency {
        LOG.info("Resolving locked registry dependency: ${locked.name}:${locked.version}")

        // 注册表依赖路径: ~/.cjpm/registry/<name>-<version>
        val registryCacheDir = CJPM_CACHE_DIR.resolve("registry")
        val dependencyDir = registryCacheDir.resolve("${locked.name}-${locked.version}")

        if (!dependencyDir.exists() || !dependencyDir.isDirectory()) {
            return CjpmResolvedDependency.failed(
                dependency,
                "Registry dependency not found at: $dependencyDir. Please run 'cjpm update'."
            )
        }

        // 查找编译产物
        val libraryFiles = findLibraryFiles(dependencyDir)
        if (libraryFiles.isEmpty()) {
            return CjpmResolvedDependency.failed(
                dependency,
                "No library files found for: ${locked.name}"
            )
        }

        val library = CjpmLibrary(
            name = locked.name,
            version = CjVersion(locked.version),
            libraryPath = libraryFiles.first(),
            libraryType = CjLibraryType.COMPILED,
            sourcePath = dependencyDir.resolve("src").takeIf { it.exists() }
        )

        // 解析传递依赖
        val transitiveDeps = if (dependency.transitive) {
            val manifestPath = dependencyDir.resolve("cjpm.toml")
            if (manifestPath.exists()) {
                val vfsManager = VirtualFileManager.getInstance()
                val manifestVFile = vfsManager.findFileByNioPath(manifestPath)
                manifestVFile?.let { vf ->
                    val config = CjpmTomlParser.parse(vf)
                    config?.let { c ->
                        parseTransitiveDependencies(c, dependency.scope)
                    }
                } ?: emptyList()
            } else {
                emptyList()
            }
        } else {
            emptyList()
        }

        return CjpmResolvedDependency.resolvedAsLibrary(dependency, library, transitiveDeps)
    }

    /**
     * 解析锁定的路径依赖
     */
    private fun resolveLockedPathDependency(
        dependency: CjDependency,
        locked: LockedDependency.Path,
        project: Project
    ): CjResolvedDependency {
        LOG.info("Resolving locked path dependency: ${locked.name} at ${locked.path}")

        val cjProject = project.cjProject
        val projectRoot = cjProject.rootDir.toNioPath()
        val dependencyPath = projectRoot.resolve(locked.path)

        if (!dependencyPath.exists() || !dependencyPath.isDirectory()) {
            return CjpmResolvedDependency.failed(
                dependency,
                "Path dependency does not exist: ${locked.path}"
            )
        }

        val manifestPath = dependencyPath.resolve("cjpm.toml")
        if (!manifestPath.exists()) {
            return CjpmResolvedDependency.failed(
                dependency,
                "No cjpm.toml found at: ${locked.path}"
            )
        }

        val vfsManager = VirtualFileManager.getInstance()
        val manifestVFile = vfsManager.findFileByNioPath(manifestPath)
        if (manifestVFile == null) {
            return CjpmResolvedDependency.failed(
                dependency,
                "Cannot access cjpm.toml at: ${locked.path}"
            )
        }

        val config = CjpmTomlParser.parse(manifestVFile)
        if (config?.`package` == null) {
            return CjpmResolvedDependency.failed(
                dependency,
                "Invalid cjpm.toml: no package section found"
            )
        }

        val pkg = CjpmPackage(
            name = config.`package`.name,
            version = CjVersion(config.`package`.version),
            description = config.`package`.description,
            localPath = dependencyPath
        )

        val transitiveDeps = if (dependency.transitive) {
            parseTransitiveDependencies(config, dependency.scope)
        } else {
            emptyList()
        }

        return CjpmResolvedDependency.resolvedAsPackage(dependency, pkg, transitiveDeps)
    }

    /**
     * 回退方法：在没有 lock 文件时使用
     */
    private fun resolveWithoutLock(dependency: CjDependency, project: Project): CjResolvedDependency? {
        return when (dependency) {
            is CjDependency.Path -> resolvePathDependency(dependency, project)
            is CjDependency.Git -> resolveGitDependency(dependency, project)
            is CjDependency.Library -> resolveLibraryDependency(dependency, project)
            is CjDependency.System -> resolveSystemDependency(dependency, project)
            is CjDependency.Stdlib -> resolveStdlibDependency(dependency, project)
        }
    }

    /**
     * 解析路径依赖
     *
     * 路径依赖通常指向工作空间内的其他模块
     */
    private fun resolvePathDependency(
        dependency: CjDependency.Path,
        project: Project
    ): CjResolvedDependency {
        LOG.info("Resolving path dependency: ${dependency.name} at ${dependency.path}")

        val cjProject = project.cjProject
        val projectRoot = cjProject.rootDir.toNioPath()
        val dependencyPath = projectRoot.resolve(dependency.path)

        // 检查路径是否存在
        if (!dependencyPath.exists() || !dependencyPath.isDirectory()) {
            return CjpmResolvedDependency.failed(
                dependency,
                "Dependency path does not exist: ${dependency.path}"
            )
        }

        // 查找 cjpm.toml
        val manifestPath = dependencyPath.resolve("cjpm.toml")
        if (!manifestPath.exists()) {
            return CjpmResolvedDependency.failed(
                dependency,
                "No cjpm.toml found at: ${dependency.path}"
            )
        }

        // 解析 cjpm.toml
        val vfsManager = VirtualFileManager.getInstance()
        val manifestVFile = vfsManager.findFileByNioPath(manifestPath)
        if (manifestVFile == null) {
            return CjpmResolvedDependency.failed(
                dependency,
                "Cannot access cjpm.toml at: ${dependency.path}"
            )
        }

        val config = CjpmTomlParser.parse(manifestVFile)

        if (config?.`package` == null) {
            return CjpmResolvedDependency.failed(
                dependency,
                "Invalid cjpm.toml: no package section found"
            )
        }

        // 创建包对象
        val pkg = CjpmPackage(
            name = config.`package`.name,
            version = CjVersion(config.`package`.version),
            description = config.`package`.description,
            localPath = dependencyPath
        )

        // 解析传递依赖（如果需要）
        val transitiveDeps = if (dependency.transitive) {
            parseTransitiveDependencies(config, dependency.scope)
        } else {
            emptyList()
        }

        return CjpmResolvedDependency.resolvedAsPackage(dependency, pkg, transitiveDeps)
    }

    /**
     * 解析 Git 依赖
     *
     * Git 依赖会被 cjpm 克隆到缓存目录
     */
    private fun resolveGitDependency(
        dependency: CjDependency.Git,
        project: Project
    ): CjResolvedDependency {
        LOG.info("Resolving git dependency: ${dependency.name} from ${dependency.url}")

        // Git 依赖通常被缓存在 ~/.cjpm/git/<repo-hash>/<ref>
        // 简化实现：假设 cjpm update 已经下载了依赖
        val gitCacheDir = CJPM_CACHE_DIR.resolve("git")

        // 尝试查找已克隆的依赖
        // 简化版本：基于名称查找
        val possiblePaths = listOfNotNull(
            // 直接使用依赖名称
            gitCacheDir.resolve(dependency.name),
            // 使用 URL 的最后一部分（去掉 .git）
            gitCacheDir.resolve(dependency.url.substringAfterLast('/').removeSuffix(".git"))
        )

        val dependencyPath = possiblePaths.firstOrNull { it.exists() && it.isDirectory() }

        if (dependencyPath == null) {
            // Git 依赖未找到，返回未解析状态但不报错（可能需要 cjpm update）
            LOG.warn("Git dependency not found in cache: ${dependency.name}. Run 'cjpm update' first.")
            return CjpmResolvedDependency.failed(
                dependency,
                "Git dependency not found. Please run 'cjpm update' to download dependencies."
            )
        }

        // 解析 cjpm.toml
        val manifestPath = dependencyPath.resolve("cjpm.toml")
        if (!manifestPath.exists()) {
            return CjpmResolvedDependency.failed(
                dependency,
                "No cjpm.toml found in git dependency: ${dependency.name}"
            )
        }

        val vfsManager = VirtualFileManager.getInstance()
        val manifestVFile = vfsManager.findFileByNioPath(manifestPath)
        if (manifestVFile == null) {
            return CjpmResolvedDependency.failed(
                dependency,
                "Cannot access cjpm.toml in git dependency"
            )
        }

        val config = CjpmTomlParser.parse(manifestVFile)

        if (config?.`package` == null) {
            return CjpmResolvedDependency.failed(
                dependency,
                "Invalid cjpm.toml in git dependency"
            )
        }

        val pkg = CjpmPackage(
            name = config.`package`.name,
            version = CjVersion(config.`package`.version),
            description = config.`package`.description,
            repositoryUrl = dependency.url,
            localPath = dependencyPath
        )

        val transitiveDeps = if (dependency.transitive) {
            parseTransitiveDependencies(config, dependency.scope)
        } else {
            emptyList()
        }

        return CjpmResolvedDependency.resolvedAsPackage(dependency, pkg, transitiveDeps)
    }

    /**
     * 解析库依赖
     *
     * 库依赖会从远程仓库下载到缓存目录
     */
    private fun resolveLibraryDependency(
        dependency: CjDependency.Library,
        project: Project
    ): CjResolvedDependency {
        LOG.info("Resolving library dependency: ${dependency.name}:${dependency.version}")

        // 库依赖通常被缓存在 ~/.cjpm/registry/<name>-<version>
        val libCacheDir = CJPM_CACHE_DIR.resolve("registry")
        val dependencyDir = libCacheDir.resolve("${dependency.name}-${dependency.version}")

        if (!dependencyDir.exists() || !dependencyDir.isDirectory()) {
            // 库未找到，返回未解析状态
            LOG.warn("Library dependency not found in cache: ${dependency.name}. Run 'cjpm update' first.")
            return CjpmResolvedDependency.failed(
                dependency,
                "Library dependency not found. Please run 'cjpm update' to download dependencies."
            )
        }

        // 查找编译产物（.cjo 文件或其他库文件）
        val libraryFiles = findLibraryFiles(dependencyDir)
        if (libraryFiles.isEmpty()) {
            return CjpmResolvedDependency.failed(
                dependency,
                "No library files found for: ${dependency.name}"
            )
        }

        val library = CjpmLibrary(
            name = dependency.name,
            version = dependency.version,
            libraryPath = libraryFiles.first(), // 主库文件
            libraryType = CjLibraryType.COMPILED,
            sourcePath = dependencyDir.resolve("src").takeIf { it.exists() }
        )

        // 尝试解析传递依赖
        val transitiveDeps = if (dependency.transitive) {
            val manifestPath = dependencyDir.resolve("cjpm.toml")
            if (manifestPath.exists()) {
                val vfsManager = VirtualFileManager.getInstance()
                val manifestVFile = vfsManager.findFileByNioPath(manifestPath)
                manifestVFile?.let { vf ->
                    val config = CjpmTomlParser.parse(vf)
                    config?.let { c ->
                        parseTransitiveDependencies(c, dependency.scope)
                    }
                } ?: emptyList()
            } else {
                emptyList()
            }
        } else {
            emptyList()
        }

        return CjpmResolvedDependency.resolvedAsLibrary(dependency, library, transitiveDeps)
    }

    /**
     * 解析系统依赖
     *
     * 系统依赖由工具链提供，不需要下载
     */
    private fun resolveSystemDependency(
        dependency: CjDependency.System,
        project: Project
    ): CjResolvedDependency {
        LOG.info("Resolving system dependency: ${dependency.name}")

        // 系统依赖不需要解析，直接返回成功
        // 实际路径由工具链提供
        val library = CjpmLibrary(
            name = dependency.name,
            version = dependency.version,
            libraryPath = Paths.get(""), // 占位符，实际由工具链处理
            libraryType = CjLibraryType.STATIC
        )

        return CjpmResolvedDependency.resolvedAsLibrary(dependency, library, emptyList())
    }

    /**
     * 解析标准库依赖
     *
     * 标准库由 SDK 提供，位于 {sdkHome}/modules 目录
     */
    private fun resolveStdlibDependency(
        dependency: CjDependency.Stdlib,
        project: Project
    ): CjResolvedDependency {
        LOG.info("Resolving stdlib dependency: ${dependency.name}")

        // 获取项目关联的 SDK
        val sdkConfig = org.cangnova.cangjie.toolchain.api.CjProjectSdkConfig.getInstance(project)
        val sdk = sdkConfig.getProjectSdk()

        if (sdk == null) {
            return CjpmResolvedDependency.failed(
                dependency,
                "No SDK configured for project. Please configure a CangJie SDK."
            )
        }

        // 标准库路径: {sdkHome}/modules
        val stdlibPath = sdk.stdlibPath

        if (!stdlibPath.exists() || !stdlibPath.isDirectory()) {
            return CjpmResolvedDependency.failed(
                dependency,
                "Standard library not found at: $stdlibPath. SDK may be corrupted."
            )
        }

        // 创建标准库包对象
        val library = CjpmLibrary(
            name = "stdlib",
            version = sdk.version?.let { CjVersion(it.toString()) } ?: CjVersion("unknown"),
            libraryPath = stdlibPath,

            )

        LOG.info("Resolved stdlib from SDK: ${sdk.name} at $stdlibPath")

        // 标准库没有传递依赖
        return CjpmResolvedDependency.resolvedAsLibrary(dependency, library, emptyList())
    }

    /**
     * 查找库文件（.cjo, .a, .so, .dll 等）
     */
    private fun findLibraryFiles(directory: Path): List<Path> {
        if (!directory.exists() || !directory.isDirectory()) {
            return emptyList()
        }

        return try {
            Files.walk(directory, 3)
                .filter { Files.isRegularFile(it) }
                .filter { path ->
                    val name = path.fileName.toString()
                    name.endsWith(".cjo") ||
                            name.endsWith(".a") ||
                            name.endsWith(".so") ||
                            name.endsWith(".dll") ||
                            name.endsWith(".dylib")
                }
                .toList()
        } catch (e: Exception) {
            LOG.warn("Failed to scan for library files in $directory", e)
            emptyList()
        }
    }

    /**
     * 从配置中解析传递依赖
     */
    private fun parseTransitiveDependencies(
        config: CjpmTomlConfig,
        scope: CjDependencyScope
    ): List<CjDependency> {
        val result = mutableListOf<CjDependency>()

        // 只解析编译时依赖的传递依赖
        if (scope == CjDependencyScope.COMPILE || scope == CjDependencyScope.RUNTIME) {
            config.dependencies.forEach { (name, depConfig) ->
                val dep = createDependencyFromConfig(name, depConfig, CjDependencyScope.COMPILE)
                result.add(dep)
            }
        }

        return result
    }

    /**
     * 从配置创建依赖对象
     */
    private fun createDependencyFromConfig(
        name: String,
        config: DependencyConfig,
        scope: CjDependencyScope
    ): CjDependency {
        val version = CjVersion(config.version)

        return when {
            config.path != null -> CjDependency.Path(
                name = name,
                path = config.path,
                version = version,
                scope = scope
            )

            config.git != null -> CjDependency.Git(
                name = name,
                url = config.git,
                branch = config.branch,
                tag = config.tag,
                rev = config.rev,
                version = version,
                scope = scope
            )

            else -> CjDependency.Library(
                name = name,
                version = version,
                scope = scope
            )
        }
    }

    override fun resolveTransitive(dependency: CjDependency, project: Project): List<CjDependency> {
        LOG.info("Resolving transitive dependencies for: ${dependency.name}")

        // 首先解析当前依赖
        val resolved = resolve(dependency, project) ?: return emptyList()

        // 返回传递依赖列表
        return resolved.transitiveDependencies
    }
}