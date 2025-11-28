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

package org.cangnova.cangjie.dapdebugger.process

import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.openapi.Disposable
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.SystemInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.cangnova.cangjie.dapdebugger.config.ServerConfig
import org.cangnova.cangjie.dapdebugger.exception.ServerException
import org.cangnova.cangjie.process.CjProcessHandler
import org.cangnova.cangjie.toolchain.api.CjProjectSdkConfig
import java.io.IOException
import java.net.Socket
import java.nio.file.Files
import java.nio.file.Paths

/**
 * 进程管理器
 *
 * 管理调试服务器进程的生命周期
 */
class ProcessManager(
    private val project: Project
) : Disposable {

    companion object {
        private val LOG = Logger.getInstance(ProcessManager::class.java)
    }

    private var serverProcess: CjProcessHandler? = null
    private val portManager = PortManager()

    /**
     * 启动调试服务器
     */
    suspend fun startServer(config: ServerConfig): Result<ServerInfo> {
        return withContext(Dispatchers.IO) {
            try {
                LOG.info("Starting debug server")

                // 查找可用端口
                val port = portManager.findAvailablePort(
                    config.portRange.first,
                    config.portRange.last
                ).getOrThrow()

                // 创建命令行
                val commandLine = createServerCommandLine(config, port)

                // 启动进程
                val processHandler = CjProcessHandler(commandLine, processColors = false)
                processHandler.startNotify()

                serverProcess = processHandler

                // 等待服务器就绪
                waitForServerReady(port, config.startupTimeoutMs).getOrThrow()

                val serverInfo = ServerInfo(
                    host = "localhost",
                    port = port,
                    processId = processHandler.process.pid()
                )

                LOG.info("Debug server started: $serverInfo")
                Result.success(serverInfo)
            } catch (e: Exception) {
                LOG.error("Failed to start debug server", e)
                Result.failure(ServerException("Failed to start server", e))
            }
        }
    }

    /**
     * 停止调试服务器
     */
    suspend fun stopServer(): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                serverProcess?.let { process ->
                    LOG.info("Stopping debug server")

                    process.destroyProcess()

                    // 等待进程结束
                    if (!process.waitFor(5000)) {
                        LOG.warn("Server did not stop gracefully, force killing")
                        process.killProcess()
                    }

                    serverProcess = null
                    LOG.info("Debug server stopped")
                }

                Result.success(Unit)
            } catch (e: Exception) {
                LOG.error("Failed to stop debug server", e)
                Result.failure(ServerException("Failed to stop server", e))
            }
        }
    }

    private fun createServerCommandLine(config: ServerConfig, port: Int): GeneralCommandLine {
        val sdk = CjProjectSdkConfig.getInstance(project).getProjectSdk()
        val serverPath = ServerManager.getServerPath()
        val logPath = Paths.get(project.basePath ?: ".", ".idea", "log", "dap-server")

        // 确保日志目录存在
        Files.createDirectories(logPath)

        return GeneralCommandLine().apply {
            withParentEnvironmentType(GeneralCommandLine.ParentEnvironmentType.CONSOLE)
            withCharset(Charsets.UTF_8)
            exePath = serverPath.toString()
            setWorkDirectory(project.basePath)

            addParameter("--port=$port")
            addParameter("--logpath=${logPath.toAbsolutePath()}")
            addParameter("--debuggertype=${config.debuggerType}")

            // 添加SDK环境变量
            sdk?.getEnvironment()?.let { environment.putAll(it) }

            // 添加LLDB库路径
            sdk?.homePath?.let { sdkHome ->
                val lldbLibPath = "$sdkHome/third_party/llvm/lldb/lib/"
                when {
                    SystemInfo.isLinux || SystemInfo.isMac -> {
                        environment["LD_LIBRARY_PATH"] = lldbLibPath
                    }

                    SystemInfo.isWindows -> {
                        environment["PATH"] = "$lldbLibPath;${environment["PATH"]}"
                    }
                }
            }

            LOG.debug("Server command: $commandLineString")
        }
    }

    private suspend fun waitForServerReady(port: Int, timeoutMs: Long): Result<Unit> {
        return withContext(Dispatchers.IO) {
            val startTime = System.currentTimeMillis()

            while (System.currentTimeMillis() - startTime < timeoutMs) {
                try {
                    Socket("localhost", port).use {
                        LOG.info("Server is ready on port $port")
                        return@withContext Result.success(Unit)
                    }
                } catch (e: IOException) {
                    // 服务器还未就绪，继续等待
                    delay(100)
                }
            }

            Result.failure(ServerException("Server did not start within ${timeoutMs}ms"))
        }
    }

    override fun dispose() {
        runBlocking {
            stopServer()
        }
    }
}

/**
 * 服务器信息
 */
data class ServerInfo(
    val host: String,
    val port: Int,
    val processId: Long
)