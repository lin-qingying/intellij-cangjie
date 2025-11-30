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

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.util.SystemInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.cangnova.cangjie.debugger.DebuggerNotFoundException
import org.cangnova.cangjie.debugger.DebuggerProvider
import org.cangnova.cangjie.debugger.toolchain.DebuggerDownloadInfo
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

    /** 调试器文件名（不含扩展名） */
    private const val DEBUGGER_NAME = "dap_server"

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
        get() = DEBUGGER_NAME + if (SystemInfo.isWindows) ".exe" else ""

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
                    "Debugger not found at: $path. " +
                            "Please ensure the debugger is installed in ~/.cangjie/debugger/"
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
                            "Debugger not found at: $path. " +
                                    "Please copy the debugger executable to this location."
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
        // 根据平台选择对应的文件名和校验信息
        val (fileName, fileSize, sha1, md5) = when {
            SystemInfo.isWindows -> {
                Tuple4(
                    "dap_server.exe",
                    5_242_880L, // 5.0 MB
                    "b0dea3677f78c8889711a978380bab6d3785fee7",
                    "44a03b53c35913e09672a40f40b4b407"
                )
            }
            SystemInfo.isMac && SystemInfo.isAarch64 -> {
                Tuple4(
                    "dap_server-macos_aarch64",
                    3_250_585L, // 3.1 MB
                    "c5a89af7a866fa352580065d68301c8f147c5e70",
                    "3ec3a0349babfa803902343975e9abc1"
                )
            }
            SystemInfo.isMac -> {
                Tuple4(
                    "dap_server-macos_x64",
                    2_936_012L, // 2.8 MB
                    "2cf7913481b35a92b214d97080cd12541c332a8a",
                    "dda52cd0984e16e8b1d2d2b9b8fc84a7"
                )
            }
            SystemInfo.isLinux && SystemInfo.isAarch64 -> {
                Tuple4(
                    "dap_server-linux_aarch64",
                    2_726_297L, // 2.6 MB
                    "bead73df71633e74c6cd15e1517eaff274c60ff6",
                    "b7d5b4b366df193fef4546954017cecd"
                )
            }
            SystemInfo.isLinux -> {
                Tuple4(
                    "dap_server-linux_x64",
                    2_621_440L, // 2.5 MB
                    "ad355226ef007298ff26ce28f55f14dc5b965012",
                    "372964a196a0965e93707d76c02a0db4"
                )
            }
            else -> {
                throw UnsupportedOperationException(
                    "Unsupported platform: ${SystemInfo.OS_NAME} ${SystemInfo.OS_ARCH}"
                )
            }
        }

        // 版本号（可以根据实际版本更新）
        val version = "1.0.0"

        // SourceForge 下载链接格式
        val downloadUrl = "https://downloads.sourceforge.net/project/intellij-cangjie-debugger/dap-server/$fileName"

        return DebuggerDownloadInfo(
            downloadUrl = downloadUrl,
            targetPath = executablePath,
            needExtract = false, // 直接下载可执行文件，不需要解压
            version = version,
            fileSize = fileSize,
            checksum = sha1,
            checksumType = "SHA1"
        )
    }

    /**
     * 辅助数据类，用于存储平台特定的下载信息
     */
    private data class Tuple4<A, B, C, D>(
        val first: A,
        val second: B,
        val third: C,
        val fourth: D
    )


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