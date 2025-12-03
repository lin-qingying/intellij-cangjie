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

package org.cangnova.cangjie.debugger.dap.provider

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.util.SystemInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.cangnova.cangjie.debugger.DebuggerDownloadException
import org.cangnova.cangjie.debugger.DebuggerNotFoundException
import org.cangnova.cangjie.debugger.DebuggerProvider
import org.cangnova.cangjie.debugger.toolchain.DebuggerDownloadInfo
import org.cangnova.cangjie.debugger.toolchain.DebuggerIndexFetcher
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

/**
 * 本地调试器提供者
 *
 * 从本地固定路径获取调试器。
 * 路径: ~/.cangjie/debugger/dap_server[.exe]
 */

object DapDebuggerProvider : DebuggerProvider {


    private val LOG = Logger.getInstance(DapDebuggerProvider::class.java)

    /** 调试器目录 */
    private const val DEBUGGER_DIR = ".cangjie/debugger"


    /** 调试器类型 */
    const val DEBUGGER_TYPE = "lldbapi"

    /** 日志目录 */
    const val LOG_DIR = ".cangjie/debugger/logs/server"

    override val name: String = "Debug Adapter Protocol"
    override val debuggerType: String = DEBUGGER_TYPE

    /**
     * 获取调试器可执行文件名
     */
    private val executableName: String
        get() = DebuggerExe.getFileName()

    /**
     * 获取调试器目录路径
     */
    val debuggerDirectory: Path
        get() = Paths.get(System.getProperty("user.home"), DEBUGGER_DIR)

    /**
     * 获取调试器可执行文件路径
     */
    val executablePath: Path
        get() = debuggerDirectory.resolve(executableName)

    /**
     * 获取日志目录路径
     */
    val logDirectory: Path
        get() = Paths.get(System.getProperty("user.home"), LOG_DIR)

    override suspend fun getServerPath(): Path {
        return withContext(Dispatchers.IO) {
            val path = executablePath

            if (!Files.exists(path)) {
                throw DebuggerNotFoundException(
                    "Debugger not found at: $path. " + "Please ensure the debugger is installed in ~/.cangjie/debugger/"
                )
            }

            if (!Files.isExecutable(path) && !SystemInfo.isWindows) {
                LOG.warn("Debugger file is not executable, attempting to set permissions")
                try {
                    path.toFile().setExecutable(true)
                } catch (e: Exception) {
                    LOG.error("Failed to set executable permission", e)
                }
            }

            LOG.info("Using debugger at: $path")
            path
        }
    }

    override suspend fun isAvailable(): Boolean {
        return withContext(Dispatchers.IO) {
            val path = executablePath
            Files.exists(path) && (SystemInfo.isWindows || Files.isExecutable(path))
        }
    }

    override suspend fun ensureReady(): Result<Path> {
        return withContext(Dispatchers.IO) {
            try {
                // 确保目录存在
                val dir = debuggerDirectory
                if (!Files.exists(dir)) {
                    Files.createDirectories(dir)
                    LOG.info("Created debugger directory: $dir")
                }

                // 检查调试器是否存在
                val path = executablePath
                if (!Files.exists(path)) {
                    return@withContext Result.failure(
                        DebuggerNotFoundException(
                            "Debugger not found at: $path. " + "Please copy the debugger executable to this location."
                        )
                    )
                }

                // 设置可执行权限（非Windows）
                if (!SystemInfo.isWindows && !Files.isExecutable(path)) {
                    path.toFile().setExecutable(true)
                    LOG.info("Set executable permission for: $path")
                }

                LOG.info("Debugger ready at: $path")
                Result.success(path)
            } catch (e: Exception) {
                LOG.error("Failed to ensure debugger ready", e)
                Result.failure(e)
            }
        }
    }

    override fun getDownloadInfo(): DebuggerDownloadInfo {
        // 获取当前平台的文件名
        val fileName = executableName

        // 从远程索引获取下载信息
        val fileInfo = DebuggerIndexFetcher.getFileInfo(fileName)
            ?: throw DebuggerDownloadException(
                "Failed to get download info for $fileName from remote index"
            )

        LOG.info("Using download URL from index: ${fileInfo.download}")

        return DebuggerDownloadInfo(
            downloadUrl = fileInfo.download, targetPath = executablePath, needExtract = false, // 直接下载可执行文件，不需要解压
            version = fileInfo.version, checksum = fileInfo.sha1, checksumType = "SHA1"
        )
    }

    /**
     * 清理日志文件
     */
    suspend fun clearLogFiles() {
        withContext(Dispatchers.IO) {
            try {
                val logDir = logDirectory
                if (Files.exists(logDir)) {
                    Files.list(logDir).forEach { file ->
                        try {
                            Files.delete(file)
                        } catch (e: Exception) {
                            LOG.warn("Failed to delete log file: $file", e)
                        }
                    }
                    LOG.info("Cleared log files in: $logDir")
                }
            } catch (e: Exception) {
                LOG.error("Failed to clear log files", e)
            }
        }
    }

}


private enum class DebuggerExe(val fileName: String) {
    Windows("dap_server.exe"), MacOS_x64("dap_server-macos_x64"), MacOS_aarch64("dap_server-macos_aarch64"), Linux_x64("dap_server-linux_x64"), Linux_aarch64(
        "dap_server-linux_aarch64"
    );

    companion object {
        /**
         * 根据当前系统返回
         */
        fun getFileName(): String {
            return when {
                SystemInfo.isWindows -> Windows.fileName
                SystemInfo.isMac && SystemInfo.isAarch64 -> MacOS_aarch64.fileName
                SystemInfo.isMac -> MacOS_x64.fileName
                SystemInfo.isLinux && SystemInfo.isAarch64 -> Linux_aarch64.fileName
                SystemInfo.isLinux -> Linux_x64.fileName
                else -> {
                    throw UnsupportedOperationException(
                        "Unsupported platform: ${SystemInfo.OS_NAME} ${SystemInfo.OS_ARCH}"
                    )
                }
            }
        }


    }
}