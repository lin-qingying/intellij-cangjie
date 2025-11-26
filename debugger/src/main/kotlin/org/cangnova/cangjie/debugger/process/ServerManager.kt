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

package org.cangnova.cangjie.debugger.process

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.util.SystemInfo
import org.cangnova.cangjie.debugger.exception.ServerException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardCopyOption

/**
 * 服务器管理器
 *
 * 管理调试服务器二进制文件
 */
object ServerManager {

    private val LOG = Logger.getInstance(ServerManager::class.java)

    private const val SERVER_DIR = ".cangjie/debugger"
    private val serverPath = Paths.get(System.getProperty("user.home"), SERVER_DIR)

    /**
     * 获取服务器路径
     */
    fun getServerPath(): Path {
        val binaryName = getServerBinaryName()
        val binaryPath = serverPath.resolve(binaryName)

        // 如果不存在，则复制
        if (Files.notExists(binaryPath)) {
            copyServerBinary(binaryName, binaryPath)
        }

        // 确保可执行
        if (!SystemInfo.isWindows) {
            binaryPath.toFile().setExecutable(true)
        }

        return binaryPath
    }

    /**
     * 获取平台特定的服务器二进制文件名
     */
    private fun getServerBinaryName(): String {
        return when {
            SystemInfo.isWindows -> "dap_server.exe"

            SystemInfo.isMac -> when {
                SystemInfo.isAarch64 -> "dap_server-darwin_arm64"
                else -> "dap_server-darwin_x64"
            }

            SystemInfo.isLinux -> when {
                SystemInfo.isAarch64 -> "dap_server-linux_arm64"
                else -> "dap_server-linux_x64"
            }

            else -> throw ServerException("Unsupported platform: ${SystemInfo.OS_NAME}")
        }
    }

    /**
     * 复制服务器二进制文件
     */
    private fun copyServerBinary(binaryName: String, targetPath: Path) {
        try {
            LOG.info("Copying server binary: $binaryName")

            // 创建目录
            Files.createDirectories(targetPath.parent)

            // 从资源复制
            val classLoader = ServerManager::class.java.classLoader
            val resourcePath = "debugger/$binaryName"

            classLoader.getResourceAsStream(resourcePath)?.use { input ->
                Files.copy(input, targetPath, StandardCopyOption.REPLACE_EXISTING)
            } ?: throw ServerException("Server binary not found in resources: $resourcePath")

            LOG.info("Server binary copied to: $targetPath")
        } catch (e: Exception) {
            LOG.error("Failed to copy server binary", e)
            throw ServerException("Failed to copy server binary", e)
        }
    }

    /**
     * 检查服务器版本
     */
    fun checkServerVersion(): Result<String> {
        return try {
            val serverPath = getServerPath()
            val process = ProcessBuilder(serverPath.toString(), "--version")
                .redirectOutput(ProcessBuilder.Redirect.PIPE)
                .start()

            val version = process.inputStream.bufferedReader().readText().trim()
            process.waitFor()

            Result.success(version)
        } catch (e: Exception) {
            LOG.error("Failed to check server version", e)
            Result.failure(ServerException("Failed to check server version", e))
        }
    }
}