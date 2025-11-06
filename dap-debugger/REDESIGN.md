# DAP Debugger 模块重新设计方案

## 当前问题总结

### 1. DapProcessHandler 问题

- 拼写错误: `press` 应为 `ptyProcess`
- 类型不安全: 强制转换可能导致 ClassCastException
- 未复用 common 模块的 CjProcessHandler

### 2. DapConnection 问题

- 使用 `Thread.sleep()` 阻塞主线程
- 硬编码重试参数
- 使用 `println()` 而非日志系统
- 资源泄漏风险

### 3. DapClient 问题

- 使用 `println()` 调试输出
- 异常处理过于宽泛
- runInTerminal 进程生命周期管理缺失
- 未完成的 TODO (断点状态更新)

### 4. 其他问题

- 硬编码调试端口 (58920)
- 缺少端口冲突检测
- 平台支持不完整 (缺少 macOS/ARM)
- 线程安全问题

## 新架构设计

### 核心组件

```
CangJieDebugProcess (调试进程主控制器)
    ├─ DapServerManager (DAP服务器管理)
    │   ├─ 服务器生命周期管理
    │   ├─ 端口分配和冲突检测
    │   └─ 平台特定二进制选择
    │
    ├─ DapConnection (DAP连接管理)
    │   ├─ 异步连接和重试
    │   ├─ 连接状态监控
    │   └─ 资源自动清理
    │
    ├─ DapClient (DAP客户端)
    │   ├─ 事件处理
    │   ├─ 日志记录
    │   └─ 错误恢复
    │
    └─ CangJieBreakpointHandler (断点管理)
        ├─ 断点同步
        ├─ 状态更新
        └─ UI反馈
```

### 详细设计

#### 1. DapProcessHandler 重构

**目标**: 统一使用 CjProcessHandler

```kotlin
/**
 * DAP调试适配器进程处理器
 * 继承自CjProcessHandler以获得统一的进程管理能力
 */
class DapProcessHandler(
    commandLine: GeneralCommandLine,
    processColors: Boolean = false
) : CjProcessHandler(commandLine, processColors)
```

**优势**:

- 统一的进程管理
- ANSI转义码处理
- PTY支持
- 移除类型不安全的强制转换

#### 2. DapConnection 重构

**目标**: 异步连接、配置化、日志化

```kotlin
class DapConnection(
    private val debugSession: XDebugSession,
    private val parameters: RunParameters,
    private val breakpointHandler: CangJieBreakpointHandler,
    private val config: DapConnectionConfig = DapConnectionConfig.DEFAULT
) : Disposable {

    companion object {
        private val LOG = Logger.getInstance(DapConnection::class.java)
    }

    // 异步连接
    fun connectAsync(): CompletableFuture<IDebugProtocolServer> {
        return CompletableFuture.supplyAsync {
            connectWithRetry()
        }
    }

    // 带重试的连接
    private fun connectWithRetry(): IDebugProtocolServer {
        var lastException: Exception? = null

        repeat(config.maxRetries) { attempt ->
            try {
                return attemptConnection()
            } catch (e: IOException) {
                lastException = e
                LOG.warn("Connection attempt ${attempt + 1} failed", e)
                if (attempt < config.maxRetries - 1) {
                    Thread.sleep(config.retryDelayMs)
                }
            }
        }

        throw DapConnectionException("Failed to connect after ${config.maxRetries} attempts", lastException)
    }

    override fun dispose() {
        // 资源清理
    }
}

data class DapConnectionConfig(
    val host: String = "localhost",
    val maxRetries: Int = 10,
    val retryDelayMs: Long = 500,
    val connectionTimeoutMs: Long = 5000
) {
    companion object {
        val DEFAULT = DapConnectionConfig()
    }
}
```

**改进**:

- 使用 Logger 替代 println
- 配置化参数
- 异步连接支持
- 实现 Disposable 确保资源清理
- 自定义异常类型

#### 3. DapClient 重构

**目标**: 改进日志、异常处理、进程管理

```kotlin
class DapClient(
    private val debugSession: XDebugSession,
    private val connection: DapConnection,
    private val parameters: RunParameters,
    private val breakpointHandler: CangJieBreakpointHandler
) : IDebugProtocolClient {

    companion object {
        private val LOG = Logger.getInstance(DapClient::class.java)
    }

    // 管理启动的进程
    private val managedProcesses = ConcurrentHashMap<Int, Process>()

    override fun output(args: OutputEventArguments) {
        ApplicationManager.getApplication().invokeLater {
            val content = args.output ?: return@invokeLater

            when (args.category) {
                "stdout" -> {
                    LOG.debug("DAP stdout: $content")
                    debugSession.consoleView.print(content, ConsoleViewContentType.NORMAL_OUTPUT)
                }
                "stderr" -> {
                    LOG.debug("DAP stderr: $content")
                    debugSession.consoleView.print(content, ConsoleViewContentType.ERROR_OUTPUT)
                }
                else -> {
                    LOG.debug("DAP output: $content")
                    debugSession.consoleView.print(content, ConsoleViewContentType.SYSTEM_OUTPUT)
                }
            }
        }
    }

    override fun breakpoint(args: BreakpointEventArguments) {
        ApplicationManager.getApplication().invokeLater {
            val breakpoint = args.breakpoint
            LOG.info("Breakpoint event: id=${breakpoint.id}, verified=${breakpoint.isVerified}")

            // 更新断点状态
            breakpointHandler.updateBreakpointFromServer(breakpoint)
        }
    }

    override fun runInTerminal(args: RunInTerminalRequestArguments): CompletableFuture<RunInTerminalResponse> {
        return CompletableFuture.supplyAsync {
            val response = RunInTerminalResponse()

            try {
                val processBuilder = ProcessBuilder(*args.args)

                args.cwd?.let { processBuilder.directory(File(it)) }
                args.env?.forEach { (key, value) ->
                    processBuilder.environment()[key] = value
                }

                val process = processBuilder.start()
                val pid = process.pid().toInt()

                // 管理进程生命周期
                managedProcesses[pid] = process

                response.processId = pid
                response.shellProcessId = null

                LOG.info("Started terminal process: pid=$pid, command=${args.args.joinToString(" ")}")

            } catch (e: IOException) {
                LOG.error("Failed to start terminal process", e)
                throw DapClientException("Failed to start terminal process", e)
            }

            response
        }
    }

    fun cleanup() {
        // 清理所有管理的进程
        managedProcesses.values.forEach { process ->
            if (process.isAlive) {
                process.destroy()
            }
        }
        managedProcesses.clear()
    }
}
```

**改进**:

- 使用 Logger 记录所有事件
- 进程生命周期管理
- 具体的异常类型
- 完成断点状态更新 TODO

#### 4. DapServerManager 重构

**目标**: 端口管理、平台支持

```kotlin
object DapServerManager {
    private val LOG = Logger.getInstance(DapServerManager::class.java)

    private const val DAP_SERVER_DIR = ".cangjie/debugger"
    private const val DEFAULT_PORT = 58920
    private const val PORT_RANGE_START = 58920
    private const val PORT_RANGE_END = 58930

    val dapServerPath = Paths.get(System.getProperty("user.home"), DAP_SERVER_DIR)

    /**
     * 获取可用端口
     */
    fun findAvailablePort(): Int {
        for (port in PORT_RANGE_START..PORT_RANGE_END) {
            if (isPortAvailable(port)) {
                LOG.info("Found available port: $port")
                return port
            }
        }
        throw DapServerException("No available ports in range $PORT_RANGE_START-$PORT_RANGE_END")
    }

    private fun isPortAvailable(port: Int): Boolean {
        return try {
            ServerSocket(port).use { true }
        } catch (e: IOException) {
            false
        }
    }

    /**
     * 获取平台特定的DAP服务器二进制文件名
     */
    fun getDapServerBinaryName(): String {
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
            else -> throw DapServerException("Unsupported platform: ${SystemInfo.OS_NAME}")
        }
    }

    /**
     * 获取调试服务器命令行
     */
    fun getCommandLine(project: Project, port: Int): GeneralCommandLine {
        val sdk = CjProjectSdkConfig.getInstance(project).getProjectSdk()
        val logPath = Paths.get(project.basePath ?: ".", ".idea", "log", "dap-server")

        // 确保日志目录存在
        Files.createDirectories(logPath)

        return GeneralCommandLine().apply {
            withParentEnvironmentType(GeneralCommandLine.ParentEnvironmentType.CONSOLE)
            withCharset(Charsets.UTF_8)
            exePath = getDebugServerPath()
            setWorkDirectory(project.basePath)

            addParameter("--port=$port")
            addParameter("--logpath=${logPath.toAbsolutePath()}")
            addParameter("--debuggertype=lldbapi")

            sdk?.getEnvironment()?.let { environment.putAll(it) }
            sdk?.homePath?.let { sdkHome ->
                environment["LD_LIBRARY_PATH"] = "$sdkHome/third_party/llvm/lldb/lib/"
            }

            LOG.info("DAP server command: $commandLineString")
        }
    }
}
```

**改进**:

- 端口冲突检测和自动分配
- 完整的平台支持 (Windows/Linux/macOS, x64/ARM)
- 日志目录自动创建
- 详细的日志记录

#### 5. CangJieBreakpointHandler 改进

**目标**: 完成断点状态更新

```kotlin
class CangJieBreakpointHandler(
    private val debugProcess: CangJieDebugProcess
) : XBreakpointHandler<XLineBreakpoint<CangJieLineBreakpointType.Properties>>(
    CangJieLineBreakpointType::class.java
) {
    companion object {
        private val LOG = Logger.getInstance(CangJieBreakpointHandler::class.java)
    }

    private val activeBreakpoints =
        ConcurrentHashMap<String, MutableSet<XLineBreakpoint<CangJieLineBreakpointType.Properties>>>()
    private val breakpointIdMap = ConcurrentHashMap<Int, XLineBreakpoint<CangJieLineBreakpointType.Properties>>()

    /**
     * 从服务器更新断点状态
     */
    fun updateBreakpointFromServer(serverBreakpoint: Breakpoint) {
        val breakpoint = breakpointIdMap[serverBreakpoint.id] ?: run {
            LOG.warn("Received breakpoint event for unknown breakpoint id: ${serverBreakpoint.id}")
            return
        }

        ApplicationManager.getApplication().invokeLater {
            val project = debugProcess.session.project
            val icon = when {
                !breakpoint.isEnabled -> AllIcons.Debugger.Db_disabled_breakpoint
                !debugProcess.session.isStopped && serverBreakpoint.isVerified ->
                    AllIcons.Debugger.Db_verified_breakpoint
                !debugProcess.session.isStopped && !serverBreakpoint.isVerified ->
                    AllIcons.Debugger.Db_invalid_breakpoint
                else -> null
            }

            val errorMessage = if (!serverBreakpoint.isVerified) {
                serverBreakpoint.message ?: "Breakpoint could not be verified"
            } else null

            XDebuggerManager.getInstance(project).breakpointManager.updateBreakpointPresentation(
                breakpoint,
                icon,
                errorMessage
            )

            LOG.info("Updated breakpoint ${serverBreakpoint.id}: verified=${serverBreakpoint.isVerified}")
        }
    }
}
```

#### 6. CangJieStackFrame 线程安全改进

```kotlin
class CangJieStackFrame(
    val debugProcess: CangJieDebugProcess,
    val threadId: Long,
    val stackFrame: StackFrame
) : XStackFrame() {

    companion object {
        private val LOG = Logger.getInstance(CangJieStackFrame::class.java)
    }

    @Volatile
    private var loadingVariables = false
    private val loadLock = Any()

    override fun computeChildren(node: XCompositeNode) {
        synchronized(loadLock) {
            if (loadingVariables) {
                LOG.debug("Variables already loading for frame ${stackFrame.id}")
                return
            }
            loadingVariables = true
        }

        // ... 其余代码
    }
}
```

### 自定义异常类型

```kotlin
// DapExceptions.kt
sealed class DapException(message: String, cause: Throwable? = null) : Exception(message, cause)

class DapConnectionException(message: String, cause: Throwable? = null) : DapException(message, cause)
class DapServerException(message: String, cause: Throwable? = null) : DapException(message, cause)
class DapClientException(message: String, cause: Throwable? = null) : DapException(message, cause)
```

## 实施计划

1. ✅ 创建设计文档
2. 创建自定义异常类
3. 重构 DapProcessHandler
4. 重构 DapServerManager (端口管理)
5. 重构 DapConnection (异步连接)
6. 重构 DapClient (日志和进程管理)
7. 改进 CangJieBreakpointHandler
8. 改进 CangJieStackFrame 线程安全
9. 更新 CangJieDebugProcess 使用新组件
10. 测试所有调试功能

## 测试清单

- [ ] 基本调试启动和停止
- [ ] 断点设置和命中
- [ ] 单步执行 (step over/into/out)
- [ ] 变量查看
- [ ] 表达式求值
- [ ] 多线程调试
- [ ] 端口冲突处理
- [ ] 连接失败重试
- [ ] 资源清理
- [ ] 跨平台兼容性

## 向后兼容性

所有公共 API 保持不变,内部实现改进不影响外部使用。