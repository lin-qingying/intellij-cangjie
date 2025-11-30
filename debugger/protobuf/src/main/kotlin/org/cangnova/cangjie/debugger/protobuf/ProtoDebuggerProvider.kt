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

package org.cangnova.cangjie.debugger.protobuf

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.SystemInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.cangnova.cangjie.debugger.DebuggerNotFoundException
import org.cangnova.cangjie.debugger.DebuggerProvider
import org.cangnova.cangjie.debugger.toolchain.DebuggerDownloadInfo
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

/**
 * Protobuf 调试器提供者
 *
 * 使用 LLDB 前端和框架文件进行调试。
 * 这是传统的基于 protobuf 通信的调试器实现。
 */
object ProtoDebuggerProvider : DebuggerProvider {

    private val LOG = Logger.getInstance(ProtoDebuggerProvider::class.java)

    override val name: String = "Proto LLDB Debugger"
    override val debuggerType: String = "lldbproto"

    /** 调试器目录 */
    private const val DEBUGGER_DIR = ".cangjie/debugger"

    /** LLDB 前端文件名 */
    private const val FRONTEND_NAME_BASE = "CangJieLLDBFrontend"

    /** LLDB 框架文件名 */
    private val FRAMEWORK_NAME = when {
        SystemInfo.isWindows -> "liblldb.dll"
        SystemInfo.isMac -> "LLDB.framework"
        else -> "liblldb.so"
    }

    /**
     * 获取前端可执行文件名
     */
    private val frontendExecutableName: String
        get() = FRONTEND_NAME_BASE + if (SystemInfo.isWindows) ".exe" else ""

    /**
     * 获取调试器目录路径
     */
    val debuggerDirectory: Path
        get() = Paths.get(System.getProperty("user.home"), DEBUGGER_DIR)

    /**
     * 获取前端可执行文件路径
     */
    val frontendPath: Path
        get() = debuggerDirectory.resolve(frontendExecutableName)

    /**
     * 获取框架文件路径
     */
    val frameworkPath: Path
        get() = debuggerDirectory.resolve(FRAMEWORK_NAME)

    override suspend fun getServerPath(): Path {
        return withContext(Dispatchers.IO) {
            val path = frontendPath

            if (!Files.exists(path)) {
                throw DebuggerNotFoundException(
                    "LLDB Frontend not found at: $path. " +
                            "Please ensure the debugger is installed in ~/.cangjie/debugger/"
                )
            }

            if (!Files.isExecutable(path) && !SystemInfo.isWindows) {
                LOG.warn("Frontend file is not executable, attempting to set permissions")
                try {
                    path.toFile().setExecutable(true)
                } catch (e: Exception) {
                    LOG.error("Failed to set executable permission", e)
                }
            }

            LOG.info("Using LLDB frontend at: $path")
            path
        }
    }

    override suspend fun isAvailable(): Boolean {
        return withContext(Dispatchers.IO) {
            val frontendExists = Files.exists(frontendPath) &&
                                (SystemInfo.isWindows || Files.isExecutable(frontendPath))
            val frameworkExists = Files.exists(frameworkPath)

            frontendExists && frameworkExists
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

                // 检查前端是否存在
                val frontend = frontendPath
                if (!Files.exists(frontend)) {
                    return@withContext Result.failure(
                        DebuggerNotFoundException(
                            "LLDB Frontend not found at: $frontend. " +
                                    "Please copy the frontend executable to this location."
                        )
                    )
                }

                // 检查框架是否存在
                val framework = frameworkPath
                if (!Files.exists(framework)) {
                    return@withContext Result.failure(
                        DebuggerNotFoundException(
                            "LLDB framework not found at: $framework. " +
                                    "Please copy the LLDB framework to this location."
                        )
                    )
                }

                // 设置可执行权限（非Windows）
                if (!SystemInfo.isWindows && !Files.isExecutable(frontend)) {
                    frontend.toFile().setExecutable(true)
                    LOG.info("Set executable permission for: $frontend")
                }

                LOG.info("Proto debugger ready - frontend: $frontend, framework: $framework")
                Result.success(frontend)
            } catch (e: Exception) {
                LOG.error("Failed to ensure proto debugger ready", e)
                Result.failure(e)
            }
        }
    }

    override fun getDownloadInfo(): DebuggerDownloadInfo {
        TODO("Not yet implemented")
    }

}