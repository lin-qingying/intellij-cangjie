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


import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import org.cangnova.cangjie.cjpm.config.CjpmConfigConverter
import org.cangnova.cangjie.cjpm.project.model.toml.CjpmTomlParser
import org.cangnova.cangjie.messages.CangJieBundle
import org.cangnova.cangjie.project.model.CjModule
import org.cangnova.cangjie.project.model.CjProject
import org.cangnova.cangjie.project.model.CjWorkspace
import org.cangnova.cangjie.project.model.roots
import org.cangnova.cangjie.project.service.CjProjectsService
import org.cangnova.cangjie.result.CjResult
import org.cangnova.cangjie.toolchain.api.CjProjectSdkConfig
import org.cangnova.cangjie.toolchain.command.ToolchainCommandLine
import org.cangnova.cangjie.utils.pathAsPath

/**
 * CJPM 更新异常
 *
 * 当 CJPM update 命令执行失败时抛出此异常
 */
class CjpmUpdateException(
    message: String,
    val details: String? = null,
    cause: Throwable? = null
) : Exception(message, cause)

/**
 * 可重置的 Lazy 委托，支持缓存重置
 */
class ResettableLazy<T>(private val initializer: () -> T) {
    @Volatile
    private var _value: Any? = UNINITIALIZED_VALUE
    private val lock = Any()

    val value: T
        get() {
            val _v1 = _value
            if (_v1 !== UNINITIALIZED_VALUE) {
                @Suppress("UNCHECKED_CAST")
                return _v1 as T
            }

            return synchronized(lock) {
                val _v2 = _value
                if (_v2 !== UNINITIALIZED_VALUE) {
                    @Suppress("UNCHECKED_CAST")
                    _v2 as T
                } else {
                    val typedValue = initializer()
                    _value = typedValue
                    typedValue
                }
            }
        }

    fun reset() {
        synchronized(lock) {
            _value = UNINITIALIZED_VALUE
        }
    }

    private companion object {
        val UNINITIALIZED_VALUE = Any()
    }
}

/**
 * 创建可重置的 Lazy
 */
fun <T> resettableLazy(initializer: () -> T): ResettableLazy<T> = ResettableLazy(initializer)

/**
 * CJPM 项目实现
 */
class CjpmProjectImpl(
    override val rootDir: VirtualFile,
    override val intellijProject: Project,
    private val manifestFile: VirtualFile
) : CjProject {

    companion object {
        private val LOG = logger<CjpmProjectImpl>()
    }

    private val config = resettableLazy {
        val fullConfig = CjpmTomlParser.parse(manifestFile)
        fullConfig?.let { CjpmConfigConverter.convertToSimpleConfig(it) }
    }

    override val name: String
        get() = intellijProject.name
    override val isWorkspace: Boolean
        get() = workspace != null
    override val module: CjModule?
        get() = modulesCache.value


    private val modulesCache = resettableLazy {
        buildModule()
    }


    private val workspaceCache = resettableLazy {
        buildWorkspace()
    }

    override val workspace: CjWorkspace?
        get() = workspaceCache.value


    override val isValid: Boolean
        get() = config.value != null

    private val indexableDirectoriesCache = resettableLazy {
        buildIndexableDirectories()
    }

    override val indexableDirectories: List<VirtualFile>
        get() = indexableDirectoriesCache.value

    @Throws(Exception::class)
    override fun refresh(onComplete: (() -> Unit)?) {
        LOG.info("Refreshing CJPM project: $name")


        // 清除配置缓存，强制重新解析 TOML
        config.reset()

        // 清除模块列表缓存，强制重新构建模块结构
        modulesCache.reset()

        // 清除工作空间缓存，强制重新构建工作空间信息
        workspaceCache.reset()


        // 清除索引目录缓存，强制重新计算可索引目录
        indexableDirectoriesCache.reset()


        // 检查模块目录情况,完成后执行 cjpm update
        checkAndCreateModuleDirectories {
            // 刷新 IntelliJ 文件系统
            rootDir.refresh(/* asynchronous = */ false, /* recursive = */ true)


            try {

                // 执行 cjpm update 命令来更新依赖
                executeCjpmUpdate()

                LOG.info("Successfully refreshed CJPM project: $name")

            } catch (e: Exception) {


                throw e
            } finally {
                //            // 清除工作空间缓存，强制重新构建工作空间信息
                workspaceCache.reset()

                // 调用完成回调
                onComplete?.invoke()
            }


        }

    }

    /**
     * 执行 cjpm update 命令
     * @throws CjpmUpdateException 当 CJPM 更新失败时抛出异常
     */
    private fun executeCjpmUpdate() {
        try {
            // 获取项目关联的 SDK
            val sdkConfig = CjProjectSdkConfig.getInstance(intellijProject)
            val sdk = sdkConfig.getProjectSdk()

            if (sdk == null || !sdk.isValid) {

                throw CjpmUpdateException(
                    CangJieBundle.message(
                        "invalid.cangjie.toolchain.02",
                        "cjpm"
                    )
                )
            }

            // 创建 cjpm update 命令
            val commandLine = ToolchainCommandLine(
                sdkId = sdk.id,
                executable = "tools/bin/cjpm",
                command = "update",
                workingDirectory = rootDir.pathAsPath
            )

            // 执行命令，使用当前 progress indicator 而非全局 indicator 以避免 EDT 阻塞
            LOG.info("Executing cjpm update in $rootDir")
            when (val result = with(commandLine) {
                val generalCommandLine = toGeneralCommandLine(intellijProject)
                generalCommandLine.execute(
                    owner = intellijProject,
                    stdIn = null,
                    runner = {
                        // 使用当前的 progress indicator 而非全局 indicator
                        val indicator = com.intellij.openapi.progress.ProgressManager.getInstance().progressIndicator
                        runProcess(indicator, null)
                    },
                    listener = null
                )
            }) {
                is CjResult.Ok -> {
                    LOG.info("cjpm update completed successfully for project $name")
                }

                is CjResult.Err -> {
                    val errorMessage = "cjpm update failed for project $name: ${result.err}"
//                    LOG.error(errorMessage)
                    throw CjpmUpdateException(errorMessage, result.err.toString(), result.err)
                }
            }
        } catch (e: CjpmUpdateException) {
            // 重新抛出 CjpmUpdateException
            throw e
        } catch (e: Exception) {
            val errorMessage = "Failed to execute cjpm update for project $name"
//            LOG.error(errorMessage, e)
            throw CjpmUpdateException(errorMessage, e.message, e)
        }
    }

    /**
     * 检查并创建模块目录
     *
     * 对于工作空间项目，检查每个模块是否有对应的物理目录，
     * 如果目录不存在，则调用 createProject 创建该模块
     *
     * @param onComplete 所有模块创建完成后的回调函数
     */
    private fun checkAndCreateModuleDirectories(onComplete: () -> Unit) {


        val workspaceConfig = config.value?.workspace
        if (workspaceConfig == null) {
            onComplete()
            return
        }

        val sdkConfig = CjProjectSdkConfig.getInstance(intellijProject)
        val sdk = sdkConfig.getProjectSdk()

        if (sdk == null || !sdk.isValid) {
            LOG.warn("No valid SDK found, skipping module directory creation")
            onComplete()
            return
        }

        // 收集需要创建的模块
        val modulesToCreate = mutableListOf<Pair<String, VirtualFile>>()

        workspaceConfig.members.forEach { memberPath ->
            val memberDir = rootDir.findFileByRelativePath(memberPath)

            if (memberDir == null || !memberDir.exists()) {
                val parentPath = memberPath.substringBeforeLast('/', "")
                val parentDir = if (parentPath.isEmpty()) {
                    rootDir
                } else {
                    rootDir.findFileByRelativePath(parentPath) ?: rootDir
                }

                val moduleName = memberPath.substringAfterLast('/')
                modulesToCreate.add(moduleName to parentDir)
            }
        }

        // 如果没有需要创建的模块，直接执行回调
        if (modulesToCreate.isEmpty()) {
            onComplete()
            return
        }

        LOG.info("Found ${modulesToCreate.size} modules to create")

        // 使用计数器跟踪完成的任务数
        val remainingTasks = java.util.concurrent.atomic.AtomicInteger(modulesToCreate.size)

        modulesToCreate.forEach { (moduleName, parentDir) ->
            val projectType = "static"

            LOG.info("Creating module: $moduleName")

            // 分两步: 先在EDT创建目录,然后在后台线程执行cjpm init
            invokeLater {
                val targetDir = try {
                    runWriteAction<VirtualFile> {
                        if (parentDir.findChild(moduleName) == null) {
                            parentDir.createChildDirectory(this, moduleName)
                        } else {
                            parentDir.findChild(moduleName)!!
                        }
                    }
                } catch (e: Exception) {
                    LOG.warn("Failed to create directory for module $moduleName: ${e.message}")

                    // 即使失败也要减少计数器
                    if (remainingTasks.decrementAndGet() == 0) {
                        onComplete()
                    }
                    return@invokeLater
                }

                // 在后台线程执行耗时的 cjpm init 操作
                ApplicationManager.getApplication().executeOnPooledThread {
                    try {
                        val result = CjProjectsService.getInstance(intellijProject)
                            .createProject(sdk.id, intellijProject, targetDir, projectType, moduleName)

                        when (result) {
                            is CjResult.Ok -> {
                                LOG.info("Successfully created module at $moduleName")
                            }

                            is CjResult.Err -> {
                                LOG.warn("Failed to create module at $moduleName: ${result.err}")
                            }
                        }
                    } finally {
                        // 完成一个任务，检查是否所有任务都完成
                        if (remainingTasks.decrementAndGet() == 0) {
                            LOG.info("All modules created, executing completion callback")
                            onComplete()
                        }
                    }
                }
            }
        }
    }

    override fun findModule(name: String): CjModule? {
        return if (isWorkspace) workspace?.modules?.find { it.name == name }
        else if (module?.name == name) {
            modulesCache.value
        } else null
    }

    private fun buildModule(): CjModule? {
        val config = this.config.value ?: return null
        var result: CjModule? = null

        // 如果是单模块项目
        config.`package`?.let { packageConfig ->

            result = CjpmModuleImpl(
                name = packageConfig.name,
                rootDir = rootDir,
                project = this,
                packageConfig = packageConfig
            )
        }


        return result
    }

    private fun buildWorkspace(): CjWorkspace? {
        val config = this.config.value ?: return null

        return if (config.workspace != null) {
            CjpmWorkspaceImpl(
                project = this,
                rootDir = rootDir,
                workspace = config.workspace
            )
        } else {
            null
        }
    }

    private fun buildIndexableDirectories(): List<VirtualFile> {
        val directories = mutableListOf<VirtualFile>()

        // 添加项目根目录
        directories.add(rootDir)


        if (isWorkspace) {

            // 添加所有模块的源码目录和输出目录
            for (module in workspace!!.modules) {
                for (sourceSet in module.sourceSets) {
                    directories.addAll(sourceSet.roots)
                }

            }

        } else {
            for (sourceSet in module!!.sourceSets) {
                directories.addAll(sourceSet.roots)
            }

        }

        return directories
    }
}


