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

package org.cangnova.cangjie.debugger.dap.server

import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.SystemInfo
import com.intellij.util.io.systemIndependentPath
import kotlinx.coroutines.*
import org.cangnova.cangjie.debugger.dap.exception.ServerException
import org.cangnova.cangjie.debugger.dap.provider.LocalDebuggerProvider
import org.cangnova.cangjie.ide.project.cangjieSettings
import org.cangnova.cangjie.process.CjProcessHandler
import java.nio.file.Files
import java.nio.file.Paths
import java.util.*
import java.util.concurrent.ConcurrentHashMap

/**
 * 默认服务器生命周期管理器
 *
 * 作为Application级别的服务注册
 */
@Service(Service.Level.PROJECT)
class DefaultServerLifecycleManager(
    private val project: Project
) : ServerLifecycleManager {

    companion object {
        private val LOG = Logger.getInstance(DefaultServerLifecycleManager::class.java)

        fun getInstance(project: Project): DefaultServerLifecycleManager {
            return project.getService(DefaultServerLifecycleManager::class.java)
        }
    }

    private val servers = ConcurrentHashMap<String, ServerProcess>()
    private val portAllocator = PortAllocator.getInstance()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override suspend fun startServer(config: ServerConfig): Result<ServerProcess> {
        return withContext(Dispatchers.IO) {
            try {
                val serverId = UUID.randomUUID().toString()
                LOG.info("Starting server $serverId with config: $config")

                // 分配端口
                val portLease = allocatePort(config.portStrategy).getOrThrow()
                LOG.info("Allocated port ${portLease.port} for server $serverId")

                // 获取服务器可执行文件路径
                val executablePath = if (config.executablePath.isNotEmpty()) {
                    config.executablePath
                } else {
                    // 使用 LocalDebuggerProvider 获取默认路径
                    LocalDebuggerProvider.getInstance().getServerPath().toString()
                }

                // 创建命令行
                val commandLine = createCommandLine(
                    executablePath = executablePath,
                    port = portLease.port,
                    config = config
                )

                // 启动进程
                val processHandler = CjProcessHandler(commandLine, processColors = false)
                processHandler.startNotify()

                // 注册进程
                ProcessRegistry.register(
                    id = serverId,
                    process = processHandler.process,
                    pid = processHandler.process.pid(),
                    metadata = mapOf(
                        "port" to portLease.port,
                        "config" to config
                    )
                )

                // 创建ServerProcess
                val serverProcess = DefaultServerProcess(
                    id = serverId,
                    config = config,
                    port = portLease.port,
                    processHandler = processHandler,
                    portLease = portLease
                )

                // 等待服务器就绪
                serverProcess.waitForReady(config.startupTimeoutMs).getOrElse { error ->
                    // 启动失败，清理资源
                    LOG.error("Server $serverId failed to start", error)
                    serverProcess.stop(gracefulTimeoutMs = 1000)
                    return@withContext Result.failure(error)
                }

                // 注册到服务器列表
                servers[serverId] = serverProcess

                LOG.info("Server $serverId started successfully on port ${portLease.port}")
                Result.success(serverProcess)
            } catch (e: Exception) {
                LOG.error("Failed to start server", e)
                Result.failure(ServerException("Failed to start server", e))
            }
        }
    }

    override suspend fun stopServer(processId: String, gracefulTimeoutMs: Long): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val server = servers.remove(processId)
                    ?: return@withContext Result.failure(
                        ServerException("Server $processId not found")
                    )

                LOG.info("Stopping server $processId")
                server.stop(gracefulTimeoutMs)
            } catch (e: Exception) {
                LOG.error("Failed to stop server $processId", e)
                Result.failure(ServerException("Failed to stop server", e))
            }
        }
    }

    override fun getServer(processId: String): ServerProcess? = servers[processId]

    override fun getAllServers(): List<ServerProcess> = servers.values.toList()

    override suspend fun stopAllServers(gracefulTimeoutMs: Long) {
        val serverList = servers.values.toList()
        if (serverList.isEmpty()) {
            return
        }

        LOG.info("Stopping all ${serverList.size} servers")

        coroutineScope {
            serverList.map { server ->
                async {
                    try {
                        server.stop(gracefulTimeoutMs)
                    } catch (e: Exception) {
                        LOG.error("Failed to stop server ${server.id}", e)
                    }
                }
            }.awaitAll()
        }

        servers.clear()
        LOG.info("All servers stopped")
    }

    /**
     * 分配端口
     */
    private suspend fun allocatePort(strategy: PortStrategy): Result<PortLease> {
        return when (strategy) {
            is PortStrategy.AutoAllocate -> {
                portAllocator.allocatePort()
            }

            is PortStrategy.UsePort -> {
                portAllocator.allocatePort(strategy.port, strategy.port)
            }

            is PortStrategy.AllocateFromRange -> {
                portAllocator.allocatePort(strategy.startPort, strategy.endPort)
            }
        }
    }

    /**
     * 创建命令行
     */
    private fun createCommandLine(
        executablePath: String,
        port: Int,
        config: ServerConfig
    ): GeneralCommandLine {
        val cangjieSettings = project.cangjieSettings
        val toolchain = cangjieSettings.state.toolchain
        val logPath = Paths.get(project.basePath ?: ".", ".idea", "log", "dap-server")

        // 确保日志目录存在
        Files.createDirectories(logPath)

        return GeneralCommandLine().apply {
            withParentEnvironmentType(GeneralCommandLine.ParentEnvironmentType.CONSOLE)
            withCharset(Charsets.UTF_8)
            exePath = executablePath

            // 设置工作目录
            config.workingDirectory?.let {
                setWorkDirectory(it)
            } ?: setWorkDirectory(project.basePath)

            // 添加基本参数
            addParameter("--port=$port")
            addParameter("--logpath=${logPath.toAbsolutePath()}")
            addParameter("--debuggertype=${config.debuggerType}")

            // 添加自定义参数
            config.arguments.forEach { addParameter(it) }

            // 添加工具链环境变量
            toolchain?.getEnvironment()?.let { environment.putAll(it) }

            // 添加自定义环境变量
            environment.putAll(config.environment)

            // 添加LLDB库路径（参考 CangJieDebuggerServerManager）
            toolchain?.homePath?.let { toolchainPath ->
                val lldbLibPath = toolchainPath.systemIndependentPath + "/third_party/llvm/lldb/lib/"
                when {
                    SystemInfo.isLinux || SystemInfo.isMac -> {
                        val existing = environment["LD_LIBRARY_PATH"] ?: ""
                        environment["LD_LIBRARY_PATH"] = if (existing.isEmpty()) {
                            lldbLibPath
                        } else {
                            "$lldbLibPath:$existing"
                        }
                    }

                    SystemInfo.isWindows -> {
                        val existing = environment["PATH"] ?: ""
                        environment["PATH"] = "$lldbLibPath;$existing"
                    }
                }
            }

            LOG.debug("Server command: $commandLineString")
        }
    }

    override fun dispose() {
        LOG.info("Disposing ServerLifecycleManager")
        scope.cancel()

        // 异步清理所有服务器
        ApplicationManager.getApplication().executeOnPooledThread {
            runBlocking {
                try {
                    stopAllServers(gracefulTimeoutMs = 3000)
                } catch (e: Exception) {
                    LOG.error("Error during server cleanup", e)
                }
            }
        }
    }
}