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

package org.cangnova.cangjie.toolchain.impl

import org.cangnova.cangjie.toolchain.api.*
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.ConcurrentHashMap

/**
 * 官方CangJie编译器工具链的默认实现
 */
class OfficialCjToolchain(
    override val homePath: Path,
    override val platformType: PlatformType = PlatformType.LOCAL,
    override val executionTimeoutInMilliseconds: Int = DEFAULT_TIMEOUT
) : CjToolchain {

    override val toolsPath: Path = homePath.resolve("bin")

    override val version: String by lazy {
        try {
            val compiler = getCompiler()
            compiler.getVersion()?.semver?.rawVersion ?: "unknown"
        } catch (e: Exception) {
            "unknown"
        }
    }

    override val fileSeparator: String =
        if (platformType == PlatformType.WSL) "/" else System.getProperty("file.separator")

    private val toolCache = ConcurrentHashMap<String, CjTool>()
    private val customTools = ConcurrentHashMap<String, CjToolFactory<*>>()

    override fun pathToExecutable(toolName: String): Path {
        val execName = getExecutableName(toolName)
        return toolsPath.resolve(execName)
    }

    override fun hasExecutable(path: Path): Boolean {
        val resolvedPath = if (path.isAbsolute) path else homePath.resolve(path)
        return Files.exists(resolvedPath) && Files.isExecutable(resolvedPath)
    }

    override fun toLocalPath(remotePath: String): String {
        return when (platformType) {
            PlatformType.WSL -> {
                // 将WSL路径转换为Windows路径
                if (remotePath.startsWith("/")) {
                    "\\\\wsl$\\Ubuntu$remotePath".replace("/", "\\")
                } else {
                    remotePath
                }
            }

            PlatformType.DOCKER -> {
                // 对于Docker，这里可能需要更复杂的逻辑
                remotePath
            }

            else -> remotePath
        }
    }

    override fun toRemotePath(localPath: String): String {
        return when (platformType) {
            PlatformType.WSL -> {
                // 将Windows路径转换为WSL路径
                if (localPath.matches(Regex("^[A-Za-z]:\\\\.*"))) {
                    "/mnt/${localPath[0].lowercase()}/${localPath.substring(3).replace("\\", "/")}"
                } else {
                    localPath
                }
            }

            PlatformType.DOCKER -> {
                // 对于Docker，这里可能需要更复杂的逻辑
                localPath
            }

            else -> localPath
        }
    }

    override fun expandUserHome(remotePath: String): String {
        if (!remotePath.startsWith("~")) return remotePath

        val userHome = when (platformType) {
            PlatformType.WSL -> "/home/user" // 这里可能需要从WSL获取实际用户主目录
            else -> System.getProperty("user.home")
        }

        return if (remotePath == "~") {
            userHome
        } else if (remotePath.startsWith("~/")) {
            "$userHome/${remotePath.substring(2)}"
        } else {
            remotePath
        }
    }

    override fun getExecutableName(toolName: String): String {
        val extension = when (platformType) {
            PlatformType.LOCAL -> {
                if (System.getProperty("os.name").lowercase().contains("win")) {
                    ".exe"
                } else {
                    ""
                }
            }

            PlatformType.WSL -> ""
            else -> ""
        }
        return "$toolName$extension"
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T : CjTool> registerTool(toolId: String, toolFactory: CjToolFactory<T>) {
        customTools[toolId] = toolFactory
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T : CjTool> getTool(toolId: String): T? {
        val existingTool = toolCache[toolId]
        if (existingTool != null) {
            return existingTool as T
        }

        val toolFactory = customTools[toolId] ?: return null
        val tool = toolFactory.create(this) as T
        toolCache[toolId] = tool
        return tool
    }

    override fun getCompiler(): CjCompiler {
        return toolCache.computeIfAbsent(CjCompiler.NAME) {
            OfficialCjCompiler(this)
        } as CjCompiler
    }

    override fun getPackageManager(): CjPackageManager {
        return toolCache.computeIfAbsent(CjPackageManager.NAME) {
            OfficialCjPackageManager(this)
        } as CjPackageManager
    }

    companion object {
        const val DEFAULT_TIMEOUT = 60000 // 默认超时时间：60秒
    }
} 