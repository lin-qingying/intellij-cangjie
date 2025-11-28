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
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

/**
 * 本地调试器提供者
 *
 * 从本地固定路径获取调试器。
 * 路径: ~/.cangjie/debugger/dap_server[.exe]
 */
@Service(Service.Level.APP)
class LocalDebuggerProvider : DebuggerProvider {

    companion object {
        private val LOG = Logger.getInstance(LocalDebuggerProvider::class.java)

        /** 调试器目录 */
        private const val DEBUGGER_DIR = ".cangjie/debugger"

        /** 调试器文件名（不含扩展名） */
        private const val DEBUGGER_NAME = "dap_server"

        /** 调试器类型 */
        const val DEBUGGER_TYPE = "lldbapi"

        /** 日志目录 */
        const val LOG_DIR = ".cangjie/debugger/logs/server"

        fun getInstance(): LocalDebuggerProvider {
            return ApplicationManager
                .getApplication()
                .getService(LocalDebuggerProvider::class.java)
        }
    }

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