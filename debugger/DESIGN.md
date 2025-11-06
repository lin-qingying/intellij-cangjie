# Debugger 模块完整设计文档

## 文档概述

本文档描述了 intellij-cangjie 项目中全新 debugger 模块的完整架构设计。该模块旨在为仓颉语言提供现代化、可扩展的调试支持。

**版本**: 1.0
**日期**: 2025-11-06
**状态**: 设计阶段

## 目录

1. [架构概览](#1-架构概览)
2. [核心组件设计](#2-核心组件设计)
3. [协议适配器设计](#3-协议适配器设计)
4. [服务层设计](#4-服务层设计)
5. [进程管理设计](#5-进程管理设计)
6. [配置系统设计](#6-配置系统设计)
7. [异常处理设计](#7-异常处理设计)
8. [UI集成设计](#8-ui集成设计)
9. [测试策略](#9-测试策略)
10. [实施路线图](#10-实施路线图)

---

## 1. 架构概览

### 1.1 设计原则

本模块遵循以下核心设计原则：

1. **分层架构**: 清晰的层次划分，职责分离
2. **接口驱动**: 面向接口编程，便于测试和扩展
3. **异步优先**: 所有IO操作异步执行，避免阻塞UI
4. **配置驱动**: 参数可配置，支持多环境
5. **资源管理**: 自动资源清理，防止泄漏
6. **可观测性**: 完善的日志和监控
7. **可扩展性**: 支持多种调试协议
8. **平台兼容**: 跨平台支持

### 1.2 架构分层

```
┌─────────────────────────────────────────────────────────┐
│                    表示层 (Presentation)                  │
│  - UI组件                                                │
│  - 断点可视化                                            │
│  - 用户交互                                              │
└─────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────┐
│                     服务层 (Service)                      │
│  - DebugSessionService    调试会话管理                   │
│  - BreakpointService      断点管理服务                   │
│  - EvaluationService      表达式求值服务                 │
│  - VariableService        变量查看服务                   │
└─────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────┐
│                    适配器层 (Adapter)                     │
│  - DapAdapter             DAP协议适配器                  │
│  - ConnectionManager      连接管理                       │
│  - EventDispatcher        事件分发                       │
│  - ProtocolTranslator     协议转换                       │
└─────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────┐
│                 基础设施层 (Infrastructure)               │
│  - ProcessManager         进程管理                       │
│  - PortManager            端口管理                       │
│  - ConfigManager          配置管理                       │
│  - LoggerFactory          日志工厂                       │
└─────────────────────────────────────────────────────────┘
```

### 1.3 核心组件关系

```
CangJieDebugProcess (IntelliJ XDebugProcess)
    │
    ├─→ DebugSessionService
    │       ├─→ DapAdapter
    │       │       ├─→ DapConnection
    │       │       ├─→ DapClient
    │       │       └─→ DapEventHandler
    │       │
    │       └─→ ProcessManager
    │               ├─→ ServerManager
    │               └─→ PortManager
    │
    ├─→ BreakpointService
    │       └─→ BreakpointSynchronizer
    │
    ├─→ EvaluationService
    │       └─→ ExpressionEvaluator
    │
    └─→ VariableService
            └─→ VariableResolver
```

### 1.4 技术栈

- **语言**: Kotlin
- **平台**: IntelliJ Platform SDK
- **协议**: DAP (Debug Adapter Protocol) via LSP4J
- **并发**: Kotlin Coroutines / CompletableFuture
- **日志**: IntelliJ Logger
- **测试**: JUnit 5, MockK
- **构建**: Gradle

---

## 2. 核心组件设计

### 2.1 DebugSession 接口

调试会话的核心抽象，管理整个调试生命周期。

```kotlin
/**
 * 调试会话接口
 *
 * 管理调试会话的完整生命周期，包括启动、暂停、继续、停止等操作。
 */
interface DebugSession : Disposable {

    /**
     * 会话唯一标识
     */
    val sessionId: String

    /**
     * 会话状态
     */
    val state: StateFlow<SessionState>

    /**
     * 调试适配器
     */
    val adapter: DebugAdapter

    /**
     * 启动调试会话
     *
     * @param config 启动配置
     * @return 启动结果
     */
    suspend fun start(config: LaunchConfig): Result<Unit>

    /**
     * 继续执行
     *
     * @param threadId 线程ID，null表示所有线程
     */
    suspend fun resume(threadId: Long? = null): Result<Unit>

    /**
     * 暂停执行
     *
     * @param threadId 线程ID，null表示所有线程
     */
    suspend fun pause(threadId: Long? = null): Result<Unit>

    /**
     * 单步执行
     */
    suspend fun stepOver(threadId: Long): Result<Unit>
    suspend fun stepInto(threadId: Long): Result<Unit>
    suspend fun stepOut(threadId: Long): Result<Unit>

    /**
     * 停止调试会话
     */
    suspend fun stop(): Result<Unit>

    /**
     * 获取当前线程列表
     */
    suspend fun getThreads(): Result<List<ThreadInfo>>

    /**
     * 获取堆栈帧
     */
    suspend fun getStackTrace(threadId: Long): Result<List<StackFrameInfo>>

    /**
     * 订阅会话事件
     */
    fun subscribeEvents(handler: (DebugEvent) -> Unit): Subscription
}

/**
 * 会话状态
 */
sealed class SessionState {
    object Idle : SessionState()
    object Starting : SessionState()
    object Running : SessionState()
    data class Paused(val threadId: Long, val reason: PauseReason) : SessionState()
    object Stopping : SessionState()
    object Stopped : SessionState()
    data class Error(val error: Throwable) : SessionState()
}

/**
 * 暂停原因
 */
enum class PauseReason {
    BREAKPOINT,
    STEP,
    PAUSE,
    EXCEPTION,
    ENTRY
}
```

### 2.2 DebugAdapter 接口

调试协议适配器的抽象接口，支持多种调试协议。

```kotlin
/**
 * 调试适配器接口
 *
 * 抽象不同调试协议的实现细节，提供统一的调试操作接口。
 */
interface DebugAdapter : Disposable {

    /**
     * 适配器类型
     */
    val type: AdapterType

    /**
     * 连接状态
     */
    val connectionState: StateFlow<ConnectionState>

    /**
     * 初始化适配器
     */
    suspend fun initialize(config: AdapterConfig): Result<Capabilities>

    /**
     * 启动调试目标
     */
    suspend fun launch(args: LaunchArguments): Result<Unit>

    /**
     * 附加到运行中的进程
     */
    suspend fun attach(args: AttachArguments): Result<Unit>

    /**
     * 断开连接
     */
    suspend fun disconnect(): Result<Unit>

    /**
     * 设置断点
     */
    suspend fun setBreakpoints(
        source: SourceFile,
        breakpoints: List<BreakpointSpec>
    ): Result<List<BreakpointResult>>

    /**
     * 求值表达式
     */
    suspend fun evaluate(
        expression: String,
        frameId: Long,
        context: EvaluationContext
    ): Result<EvaluationResult>

    /**
     * 获取变量
     */
    suspend fun getVariables(
        variablesReference: Long
    ): Result<List<Variable>>

    /**
     * 订阅适配器事件
     */
    fun subscribeEvents(handler: (AdapterEvent) -> Unit): Subscription
}

/**
 * 适配器类型
 */
enum class AdapterType {
    DAP,    // Debug Adapter Protocol
    GDB,    // GDB/MI Protocol
    LLDB    // LLDB Protocol
}

/**
 * 连接状态
 */
sealed class ConnectionState {
    object Disconnected : ConnectionState()
    object Connecting : ConnectionState()
    object Connected : ConnectionState()
    data class Failed(val error: Throwable) : ConnectionState()
}
```

### 2.3 BreakpointManager 接口

断点管理器，负责断点的生命周期管理。

```kotlin
/**
 * 断点管理器接口
 */
interface BreakpointManager {

    /**
     * 所有断点
     */
    val breakpoints: StateFlow<List<ManagedBreakpoint>>

    /**
     * 注册断点
     */
    suspend fun registerBreakpoint(
        breakpoint: XLineBreakpoint<*>
    ): Result<ManagedBreakpoint>

    /**
     * 注销断点
     */
    suspend fun unregisterBreakpoint(
        breakpoint: XLineBreakpoint<*>
    ): Result<Unit>

    /**
     * 同步断点到调试适配器
     */
    suspend fun synchronize(): Result<Unit>

    /**
     * 更新断点状态
     */
    suspend fun updateBreakpointState(
        breakpointId: String,
        state: BreakpointState
    ): Result<Unit>
}

/**
 * 托管断点
 */
data class ManagedBreakpoint(
    val id: String,
    val ideBreakpoint: XLineBreakpoint<*>,
    val state: BreakpointState,
    val serverBreakpoint: ServerBreakpoint?
)

/**
 * 断点状态
 */
sealed class BreakpointState {
    object Pending : BreakpointState()
    object Verified : BreakpointState()
    data class Failed(val reason: String) : BreakpointState()
}
```

### 2.4 EvaluationEngine 接口

表达式求值引擎。

```kotlin
/**
 * 表达式求值引擎接口
 */
interface EvaluationEngine {

    /**
     * 求值表达式
     */
    suspend fun evaluate(
        expression: String,
        context: EvaluationContext
    ): Result<EvaluationResult>

    /**
     * 验证表达式语法
     */
    fun validate(expression: String): ValidationResult

    /**
     * 获取表达式补全建议
     */
    suspend fun getCompletions(
        expression: String,
        position: Int,
        context: EvaluationContext
    ): Result<List<CompletionItem>>
}

/**
 * 求值上下文
 */
data class EvaluationContext(
    val frameId: Long,
    val threadId: Long,
    val type: EvaluationType
)

/**
 * 求值类型
 */
enum class EvaluationType {
    WATCH,      // 监视表达式
    REPL,       // REPL控制台
    HOVER,      // 悬停提示
    CONDITIONAL // 条件断点
}

/**
 * 求值结果
 */
data class EvaluationResult(
    val value: String,
    val type: String?,
    val variablesReference: Long,
    val presentationHint: PresentationHint?
)
```

---

## 3. 协议适配器设计

详细的 DAP 协议适配器实现设计。

### 3.1 DapAdapter 实现

```kotlin
/**
 * DAP协议适配器实现
 */
class DapAdapter(
    private val project: Project,
    private val config: DapAdapterConfig
) : DebugAdapter {

    companion object {
        private val LOG = Logger.getInstance(DapAdapter::class.java)
    }

    override val type = AdapterType.DAP

    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    override val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private lateinit var connection: DapConnection
    private lateinit var client: DapClient
    private lateinit var server: IDebugProtocolServer

    private val eventHandlers = CopyOnWriteArrayList<(AdapterEvent) -> Unit>()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override suspend fun initialize(config: AdapterConfig): Result<Capabilities> {
        return withContext(Dispatchers.IO) {
            try {
                LOG.info("Initializing DAP adapter")
                _connectionState.value = ConnectionState.Connecting

                // 创建连接
                connection = DapConnection(
                    host = config.host,
                    port = config.port,
                    config = config.connectionConfig
                )

                // 创建客户端
                client = DapClient(
                    eventDispatcher = ::dispatchEvent
                )

                // 连接到服务器
                server = connection.connect(client).getOrThrow()

                // 发送初始化请求
                val capabilities = sendInitializeRequest().getOrThrow()

                _connectionState.value = ConnectionState.Connected
                LOG.info("DAP adapter initialized successfully")

                Result.success(capabilities)
            } catch (e: Exception) {
                LOG.error("Failed to initialize DAP adapter", e)
                _connectionState.value = ConnectionState.Failed(e)
                Result.failure(DapAdapterException("Initialization failed", e))
            }
        }
    }

    override suspend fun launch(args: LaunchArguments): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                LOG.info("Launching debug target: ${args.program}")

                val launchArgs = mapOf(
                    "type" to "cangjie",
                    "request" to "launch",
                    "program" to args.program,
                    "args" to args.arguments,
                    "cwd" to args.workingDirectory,
                    "env" to args.environment
                )

                server.launch(launchArgs).await()

                LOG.info("Debug target launched successfully")
                Result.success(Unit)
            } catch (e: Exception) {
                LOG.error("Failed to launch debug target", e)
                Result.failure(DapAdapterException("Launch failed", e))
            }
        }
    }

    override suspend fun setBreakpoints(
        source: SourceFile,
        breakpoints: List<BreakpointSpec>
    ): Result<List<BreakpointResult>> {
        return withContext(Dispatchers.IO) {
            try {
                val args = SetBreakpointsArguments().apply {
                    this.source = Source().apply {
                        path = source.path
                        name = source.name
                    }
                    this.breakpoints = breakpoints.map { spec ->
                        SourceBreakpoint().apply {
                            line = spec.line
                            column = spec.column
                            condition = spec.condition
                            logMessage = spec.logMessage
                        }
                    }.toTypedArray()
                }

                val response = server.setBreakpoints(args).await()

                val results = response.breakpoints.map { bp ->
                    BreakpointResult(
                        id = bp.id,
                        verified = bp.isVerified,
                        line = bp.line,
                        message = bp.message
                    )
                }

                LOG.debug("Set ${results.size} breakpoints in ${source.path}")
                Result.success(results)
            } catch (e: Exception) {
                LOG.error("Failed to set breakpoints", e)
                Result.failure(DapAdapterException("Set breakpoints failed", e))
            }
        }
    }

    override fun subscribeEvents(handler: (AdapterEvent) -> Unit): Subscription {
        eventHandlers.add(handler)
        return object : Subscription {
            override fun unsubscribe() {
                eventHandlers.remove(handler)
            }
        }
    }

    private fun dispatchEvent(event: AdapterEvent) {
        eventHandlers.forEach { handler ->
            try {
                handler(event)
            } catch (e: Exception) {
                LOG.error("Error dispatching event", e)
            }
        }
    }

    override fun dispose() {
        scope.cancel()
        if (::connection.isInitialized) {
            connection.dispose()
        }
    }
}
```

### 3.2 DapConnection 实现

```kotlin
/**
 * DAP连接管理器
 */
class DapConnection(
    private val host: String,
    private val port: Int,
    private val config: ConnectionConfig
) : Disposable {

    companion object {
        private val LOG = Logger.getInstance(DapConnection::class.java)
    }

    private var socket: Socket? = null
    private var launcher: Launcher<IDebugProtocolServer>? = null
    private var listenFuture: Future<Void>? = null

    /**
     * 连接到DAP服务器
     */
    suspend fun connect(client: IDebugProtocolClient): Result<IDebugProtocolServer> {
        return withContext(Dispatchers.IO) {
            try {
                connectWithRetry(client)
            } catch (e: Exception) {
                Result.failure(DapConnectionException("Connection failed", e))
            }
        }
    }

    private suspend fun connectWithRetry(client: IDebugProtocolClient): Result<IDebugProtocolServer> {
        var lastException: Exception? = null

        repeat(config.maxRetries) { attempt ->
            try {
                LOG.info("Connection attempt ${attempt + 1}/${config.maxRetries} to $host:$port")

                val sock = withTimeout(config.connectionTimeoutMs) {
                    Socket(host, port)
                }

                if (!sock.isConnected) {
                    throw IOException("Socket not connected")
                }

                socket = sock

                val launch = DSPLauncher.createClientLauncher(
                    client,
                    sock.inputStream,
                    sock.outputStream
                )

                launcher = launch
                val server = launch.remoteProxy
                listenFuture = launch.startListening()

                LOG.info("Successfully connected to DAP server at $host:$port")
                return Result.success(server)

            } catch (e: Exception) {
                lastException = e
                LOG.warn("Connection attempt ${attempt + 1} failed: ${e.message}")

                if (attempt < config.maxRetries - 1) {
                    delay(config.retryDelayMs)
                }
            }
        }

        return Result.failure(
            DapConnectionException(
                "Failed to connect after ${config.maxRetries} attempts",
                lastException
            )
        )
    }

    override fun dispose() {
        listenFuture?.cancel(true)
        socket?.close()
        LOG.info("DAP connection closed")
    }
}

/**
 * 连接配置
 */
data class ConnectionConfig(
    val maxRetries: Int = 10,
    val retryDelayMs: Long = 500,
    val connectionTimeoutMs: Long = 5000
)
```

# Debugger 模块设计文档 - 第二部分

## 3.3 DapClient 实现

```kotlin
/**
 * DAP客户端实现
 *
 * 处理来自DAP服务器的事件和请求
 */
class DapClient(
    private val eventDispatcher: (AdapterEvent) -> Unit
) : IDebugProtocolClient {

    companion object {
        private val LOG = Logger.getInstance(DapClient::class.java)
    }

    // 管理启动的进程
    private val managedProcesses = ConcurrentHashMap<Int, Process>()

    override fun initialized() {
        LOG.info("DAP server initialized")
        eventDispatcher(AdapterEvent.Initialized)
    }

    override fun stopped(args: StoppedEventArguments) {
        LOG.info("Stopped: reason=${args.reason}, threadId=${args.threadId}")

        val event = AdapterEvent.Stopped(
            reason = mapStopReason(args.reason),
            threadId = args.threadId?.toLong() ?: 0,
            allThreadsStopped = args.isAllThreadsStopped ?: false,
            description = args.description,
            text = args.text
        )

        eventDispatcher(event)
    }

    override fun continued(args: ContinuedEventArguments) {
        LOG.info("Continued: threadId=${args.threadId}")

        val event = AdapterEvent.Continued(
            threadId = args.threadId.toLong(),
            allThreadsContinued = args.isAllThreadsContinued ?: false
        )

        eventDispatcher(event)
    }

    override fun exited(args: ExitedEventArguments) {
        LOG.info("Exited: exitCode=${args.exitCode}")
        eventDispatcher(AdapterEvent.Exited(args.exitCode))
    }

    override fun terminated(args: TerminatedEventArguments?) {
        LOG.info("Terminated")
        cleanupProcesses()
        eventDispatcher(AdapterEvent.Terminated)
    }

    override fun thread(args: ThreadEventArguments) {
        LOG.debug("Thread event: reason=${args.reason}, threadId=${args.threadId}")

        val event = when (args.reason) {
            "started" -> AdapterEvent.ThreadStarted(args.threadId.toLong())
            "exited" -> AdapterEvent.ThreadExited(args.threadId.toLong())
            else -> return
        }

        eventDispatcher(event)
    }

    override fun output(args: OutputEventArguments) {
        val content = args.output ?: return

        LOG.debug("Output: category=${args.category}, length=${content.length}")

        val event = AdapterEvent.Output(
            category = args.category ?: "console",
            output = content,
            source = args.source?.path,
            line = args.line,
            column = args.column
        )

        eventDispatcher(event)
    }

    override fun breakpoint(args: BreakpointEventArguments) {
        LOG.info("Breakpoint event: reason=${args.reason}, id=${args.breakpoint.id}")

        val event = AdapterEvent.BreakpointChanged(
            reason = args.reason,
            breakpoint = mapBreakpoint(args.breakpoint)
        )

        eventDispatcher(event)
    }

    override fun module(args: ModuleEventArguments) {
        LOG.debug("Module event: reason=${args.reason}")
        // 可以在这里处理模块加载事件
    }

    override fun loadedSource(args: LoadedSourceEventArguments) {
        LOG.debug("Loaded source: reason=${args.reason}")
        // 可以在这里处理源文件加载事件
    }

    override fun process(args: ProcessEventArguments) {
        LOG.info("Process event: name=${args.name}, startMethod=${args.startMethod}")
        // 可以在这里处理进程事件
    }

    override fun capabilities(args: CapabilitiesEventArguments) {
        LOG.debug("Capabilities changed")
        // 可以在这里处理能力变化事件
    }

    override fun progressStart(args: ProgressStartEventArguments) {
        LOG.debug("Progress start: ${args.title}")
    }

    override fun progressUpdate(args: ProgressUpdateEventArguments) {
        LOG.debug("Progress update: ${args.message}")
    }

    override fun progressEnd(args: ProgressEndEventArguments) {
        LOG.debug("Progress end")
    }

    override fun invalidated(args: InvalidatedEventArguments) {
        LOG.info("Invalidated: areas=${args.areas?.joinToString()}")
        eventDispatcher(AdapterEvent.Invalidated(args.areas?.toList() ?: emptyList()))
    }

    override fun memory(args: MemoryEventArguments) {
        LOG.debug("Memory event: offset=${args.offset}, count=${args.count}")
    }

    override fun runInTerminal(
        args: RunInTerminalRequestArguments
    ): CompletableFuture<RunInTerminalResponse> {
        return CompletableFuture.supplyAsync {
            try {
                LOG.info("Running in terminal: ${args.args.joinToString(" ")}")

                val processBuilder = ProcessBuilder(*args.args)

                // 设置工作目录
                args.cwd?.let { processBuilder.directory(File(it)) }

                // 设置环境变量
                args.env?.forEach { (key, value) ->
                    processBuilder.environment()[key] = value
                }

                // 启动进程
                val process = processBuilder.start()
                val pid = process.pid().toInt()

                // 管理进程生命周期
                managedProcesses[pid] = process

                LOG.info("Process started: pid=$pid")

                RunInTerminalResponse().apply {
                    processId = pid
                    shellProcessId = null
                }
            } catch (e: Exception) {
                LOG.error("Failed to run in terminal", e)
                throw DapClientException("Failed to run in terminal", e)
            }
        }
    }

    private fun mapStopReason(reason: String): StopReason {
        return when (reason) {
            "step" -> StopReason.STEP
            "breakpoint" -> StopReason.BREAKPOINT
            "exception" -> StopReason.EXCEPTION
            "pause" -> StopReason.PAUSE
            "entry" -> StopReason.ENTRY
            else -> StopReason.UNKNOWN
        }
    }

    private fun mapBreakpoint(bp: org.eclipse.lsp4j.debug.Breakpoint): BreakpointInfo {
        return BreakpointInfo(
            id = bp.id,
            verified = bp.isVerified,
            line = bp.line,
            column = bp.column,
            message = bp.message,
            source = bp.source?.path
        )
    }

    private fun cleanupProcesses() {
        managedProcesses.values.forEach { process ->
            if (process.isAlive) {
                LOG.info("Terminating process: pid=${process.pid()}")
                process.destroy()

                // 等待一段时间后强制终止
                if (!process.waitFor(5, TimeUnit.SECONDS)) {
                    LOG.warn("Force killing process: pid=${process.pid()}")
                    process.destroyForcibly()
                }
            }
        }
        managedProcesses.clear()
    }
}

/**
 * 适配器事件
 */
sealed class AdapterEvent {
    object Initialized : AdapterEvent()

    data class Stopped(
        val reason: StopReason,
        val threadId: Long,
        val allThreadsStopped: Boolean,
        val description: String?,
        val text: String?
    ) : AdapterEvent()

    data class Continued(
        val threadId: Long,
        val allThreadsContinued: Boolean
    ) : AdapterEvent()

    data class Exited(val exitCode: Int) : AdapterEvent()
    object Terminated : AdapterEvent()

    data class ThreadStarted(val threadId: Long) : AdapterEvent()
    data class ThreadExited(val threadId: Long) : AdapterEvent()

    data class Output(
        val category: String,
        val output: String,
        val source: String?,
        val line: Int?,
        val column: Int?
    ) : AdapterEvent()

    data class BreakpointChanged(
        val reason: String,
        val breakpoint: BreakpointInfo
    ) : AdapterEvent()

    data class Invalidated(val areas: List<String>) : AdapterEvent()
}

/**
 * 停止原因
 */
enum class StopReason {
    STEP,
    BREAKPOINT,
    EXCEPTION,
    PAUSE,
    ENTRY,
    UNKNOWN
}
```

---

## 4. 服务层设计

### 4.1 DebugSessionService

```kotlin
/**
 * 调试会话服务
 *
 * 管理调试会话的生命周期和状态
 */
class DebugSessionService(
    private val project: Project,
    private val xDebugSession: XDebugSession
) : DebugSession {

    companion object {
        private val LOG = Logger.getInstance(DebugSessionService::class.java)
    }

    override val sessionId: String = UUID.randomUUID().toString()

    private val _state = MutableStateFlow<SessionState>(SessionState.Idle)
    override val state: StateFlow<SessionState> = _state.asStateFlow()

    override lateinit var adapter: DebugAdapter

    private val eventSubscriptions = mutableListOf<Subscription>()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /**
     * 初始化会话
     */
    fun initialize(adapter: DebugAdapter) {
        this.adapter = adapter

        // 订阅适配器事件
        val subscription = adapter.subscribeEvents { event ->
            handleAdapterEvent(event)
        }
        eventSubscriptions.add(subscription)
    }

    override suspend fun start(config: LaunchConfig): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                LOG.info("Starting debug session: $sessionId")
                _state.value = SessionState.Starting

                // 初始化适配器
                adapter.initialize(config.adapterConfig).getOrThrow()

                // 启动调试目标
                val launchArgs = LaunchArguments(
                    program = config.program,
                    arguments = config.arguments,
                    workingDirectory = config.workingDirectory,
                    environment = config.environment
                )

                adapter.launch(launchArgs).getOrThrow()

                _state.value = SessionState.Running
                LOG.info("Debug session started successfully")

                Result.success(Unit)
            } catch (e: Exception) {
                LOG.error("Failed to start debug session", e)
                _state.value = SessionState.Error(e)
                Result.failure(DebugSessionException("Failed to start session", e))
            }
        }
    }

    override suspend fun resume(threadId: Long?): Result<Unit> {
        return executeCommand("resume") {
            // 实现继续执行逻辑
            _state.value = SessionState.Running
            Result.success(Unit)
        }
    }

    override suspend fun pause(threadId: Long?): Result<Unit> {
        return executeCommand("pause") {
            // 实现暂停逻辑
            Result.success(Unit)
        }
    }

    override suspend fun stepOver(threadId: Long): Result<Unit> {
        return executeCommand("stepOver") {
            // 实现单步执行逻辑
            Result.success(Unit)
        }
    }

    override suspend fun stepInto(threadId: Long): Result<Unit> {
        return executeCommand("stepInto") {
            // 实现步入逻辑
            Result.success(Unit)
        }
    }

    override suspend fun stepOut(threadId: Long): Result<Unit> {
        return executeCommand("stepOut") {
            // 实现步出逻辑
            Result.success(Unit)
        }
    }

    override suspend fun stop(): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                LOG.info("Stopping debug session: $sessionId")
                _state.value = SessionState.Stopping

                adapter.disconnect().getOrThrow()

                _state.value = SessionState.Stopped
                LOG.info("Debug session stopped")

                Result.success(Unit)
            } catch (e: Exception) {
                LOG.error("Failed to stop debug session", e)
                Result.failure(DebugSessionException("Failed to stop session", e))
            }
        }
    }

    override suspend fun getThreads(): Result<List<ThreadInfo>> {
        // 实现获取线程列表
        return Result.success(emptyList())
    }

    override suspend fun getStackTrace(threadId: Long): Result<List<StackFrameInfo>> {
        // 实现获取堆栈跟踪
        return Result.success(emptyList())
    }

    override fun subscribeEvents(handler: (DebugEvent) -> Unit): Subscription {
        // 实现事件订阅
        return object : Subscription {
            override fun unsubscribe() {}
        }
    }

    private fun handleAdapterEvent(event: AdapterEvent) {
        scope.launch {
            when (event) {
                is AdapterEvent.Stopped -> {
                    _state.value = SessionState.Paused(
                        threadId = event.threadId,
                        reason = mapPauseReason(event.reason)
                    )

                    // 通知IntelliJ平台
                    ApplicationManager.getApplication().invokeLater {
                        // 更新UI
                    }
                }

                is AdapterEvent.Continued -> {
                    _state.value = SessionState.Running
                }

                is AdapterEvent.Terminated -> {
                    _state.value = SessionState.Stopped
                }

                else -> {
                    // 处理其他事件
                }
            }
        }
    }

    private fun mapPauseReason(reason: StopReason): PauseReason {
        return when (reason) {
            StopReason.STEP -> PauseReason.STEP
            StopReason.BREAKPOINT -> PauseReason.BREAKPOINT
            StopReason.EXCEPTION -> PauseReason.EXCEPTION
            StopReason.PAUSE -> PauseReason.PAUSE
            StopReason.ENTRY -> PauseReason.ENTRY
            StopReason.UNKNOWN -> PauseReason.PAUSE
        }
    }

    private suspend fun <T> executeCommand(
        name: String,
        block: suspend () -> Result<T>
    ): Result<T> {
        return try {
            LOG.debug("Executing command: $name")
            block()
        } catch (e: Exception) {
            LOG.error("Command failed: $name", e)
            Result.failure(e)
        }
    }

    override fun dispose() {
        eventSubscriptions.forEach { it.unsubscribe() }
        scope.cancel()
        if (::adapter.isInitialized) {
            adapter.dispose()
        }
    }
}

/**
 * 启动配置
 */
data class LaunchConfig(
    val program: String,
    val arguments: List<String> = emptyList(),
    val workingDirectory: String,
    val environment: Map<String, String> = emptyMap(),
    val adapterConfig: AdapterConfig
)

/**
 * 适配器配置
 */
data class AdapterConfig(
    val host: String = "localhost",
    val port: Int,
    val connectionConfig: ConnectionConfig = ConnectionConfig()
)
```

### 4.2 BreakpointService

```kotlin
/**
 * 断点服务
 *
 * 管理断点的注册、同步和状态更新
 */
class BreakpointService(
    private val project: Project,
    private val adapter: DebugAdapter
) : BreakpointManager {

    companion object {
        private val LOG = Logger.getInstance(BreakpointService::class.java)
    }

    private val _breakpoints = MutableStateFlow<List<ManagedBreakpoint>>(emptyList())
    override val breakpoints: StateFlow<List<ManagedBreakpoint>> = _breakpoints.asStateFlow()

    // 断点映射: IDE断点 -> 托管断点
    private val breakpointMap = ConcurrentHashMap<XLineBreakpoint<*>, ManagedBreakpoint>()

    // 文件断点分组: 文件路径 -> 断点列表
    private val fileBreakpoints = ConcurrentHashMap<String, MutableSet<ManagedBreakpoint>>()

    override suspend fun registerBreakpoint(
        breakpoint: XLineBreakpoint<*>
    ): Result<ManagedBreakpoint> {
        return withContext(Dispatchers.IO) {
            try {
                val file = breakpoint.sourcePosition?.file ?: run {
                    return@withContext Result.failure(
                        BreakpointException("Breakpoint has no source position")
                    )
                }

                val managedBp = ManagedBreakpoint(
                    id = UUID.randomUUID().toString(),
                    ideBreakpoint = breakpoint,
                    state = BreakpointState.Pending,
                    serverBreakpoint = null
                )

                breakpointMap[breakpoint] = managedBp

                val filePath = file.path
                fileBreakpoints.computeIfAbsent(filePath) { ConcurrentHashMap.newKeySet() }
                    .add(managedBp)

                updateBreakpointsList()

                LOG.info("Registered breakpoint: ${managedBp.id} at $filePath:${breakpoint.line}")

                // 同步到服务器
                synchronizeFile(filePath)

                Result.success(managedBp)
            } catch (e: Exception) {
                LOG.error("Failed to register breakpoint", e)
                Result.failure(BreakpointException("Failed to register breakpoint", e))
            }
        }
    }

    override suspend fun unregisterBreakpoint(
        breakpoint: XLineBreakpoint<*>
    ): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val managedBp = breakpointMap.remove(breakpoint) ?: run {
                    return@withContext Result.success(Unit)
                }

                val file = breakpoint.sourcePosition?.file
                if (file != null) {
                    fileBreakpoints[file.path]?.remove(managedBp)
                    synchronizeFile(file.path)
                }

                updateBreakpointsList()

                LOG.info("Unregistered breakpoint: ${managedBp.id}")
                Result.success(Unit)
            } catch (e: Exception) {
                LOG.error("Failed to unregister breakpoint", e)
                Result.failure(BreakpointException("Failed to unregister breakpoint", e))
            }
        }
    }

    override suspend fun synchronize(): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                LOG.info("Synchronizing all breakpoints")

                fileBreakpoints.keys.forEach { filePath ->
                    synchronizeFile(filePath).getOrThrow()
                }

                LOG.info("All breakpoints synchronized")
                Result.success(Unit)
            } catch (e: Exception) {
                LOG.error("Failed to synchronize breakpoints", e)
                Result.failure(BreakpointException("Failed to synchronize breakpoints", e))
            }
        }
    }

    override suspend fun updateBreakpointState(
        breakpointId: String,
        state: BreakpointState
    ): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val managedBp = breakpointMap.values.find { it.id == breakpointId } ?: run {
                    return@withContext Result.failure(
                        BreakpointException("Breakpoint not found: $breakpointId")
                    )
                }

                val updated = managedBp.copy(state = state)
                breakpointMap[managedBp.ideBreakpoint] = updated

                updateBreakpointsList()

                // 更新UI
                ApplicationManager.getApplication().invokeLater {
                    updateBreakpointPresentation(updated)
                }

                LOG.debug("Updated breakpoint state: $breakpointId -> $state")
                Result.success(Unit)
            } catch (e: Exception) {
                LOG.error("Failed to update breakpoint state", e)
                Result.failure(BreakpointException("Failed to update breakpoint state", e))
            }
        }
    }

    private suspend fun synchronizeFile(filePath: String): Result<Unit> {
        return try {
            val breakpoints = fileBreakpoints[filePath] ?: emptySet()

            val specs = breakpoints
                .filter { it.ideBreakpoint.isEnabled }
                .map { bp ->
                    BreakpointSpec(
                        line = bp.ideBreakpoint.line + 1, // DAP uses 1-based lines
                        column = 0,
                        condition = bp.ideBreakpoint.conditionExpression?.expression,
                        logMessage = null
                    )
                }

            val source = SourceFile(
                path = filePath,
                name = File(filePath).name
            )

            val results = adapter.setBreakpoints(source, specs).getOrThrow()

            // 更新断点状态
            breakpoints.zip(results).forEach { (managedBp, result) ->
                val newState = if (result.verified) {
                    BreakpointState.Verified
                } else {
                    BreakpointState.Failed(result.message ?: "Unknown error")
                }

                val serverBp = ServerBreakpoint(
                    id = result.id,
                    verified = result.verified,
                    line = result.line,
                    message = result.message
                )

                val updated = managedBp.copy(
                    state = newState,
                    serverBreakpoint = serverBp
                )

                breakpointMap[managedBp.ideBreakpoint] = updated
            }

            updateBreakpointsList()

            LOG.debug("Synchronized ${breakpoints.size} breakpoints in $filePath")
            Result.success(Unit)
        } catch (e: Exception) {
            LOG.error("Failed to synchronize file: $filePath", e)
            Result.failure(e)
        }
    }

    private fun updateBreakpointsList() {
        _breakpoints.value = breakpointMap.values.toList()
    }

    private fun updateBreakpointPresentation(breakpoint: ManagedBreakpoint) {
        val icon = when (breakpoint.state) {
            is BreakpointState.Pending -> AllIcons.Debugger.Db_set_breakpoint
            is BreakpointState.Verified -> AllIcons.Debugger.Db_verified_breakpoint
            is BreakpointState.Failed -> AllIcons.Debugger.Db_invalid_breakpoint
        }

        val errorMessage = when (val state = breakpoint.state) {
            is BreakpointState.Failed -> state.reason
            else -> null
        }

        XDebuggerManager.getInstance(project).breakpointManager.updateBreakpointPresentation(
            breakpoint.ideBreakpoint,
            icon,
            errorMessage
        )
    }
}

/**
 * 断点规格
 */
data class BreakpointSpec(
    val line: Int,
    val column: Int?,
    val condition: String?,
    val logMessage: String?
)

/**
 * 断点结果
 */
data class BreakpointResult(
    val id: Int,
    val verified: Boolean,
    val line: Int,
    val message: String?
)

/**
 * 服务器断点
 */
data class ServerBreakpoint(
    val id: Int,
    val verified: Boolean,
    val line: Int,
    val message: String?
)

/**
 * 源文件
 */
data class SourceFile(
    val path: String,
    val name: String
)
```

# Debugger 模块设计文档 - 第三部分

## 4.3 EvaluationService

```kotlin
/**
 * 表达式求值服务
 */
class EvaluationService(
    private val adapter: DebugAdapter
) : EvaluationEngine {

    companion object {
        private val LOG = Logger.getInstance(EvaluationService::class.java)
    }

    override suspend fun evaluate(
        expression: String,
        context: EvaluationContext
    ): Result<EvaluationResult> {
        return withContext(Dispatchers.IO) {
            try {
                LOG.debug("Evaluating expression: $expression in context: $context")

                val result = adapter.evaluate(
                    expression = expression,
                    frameId = context.frameId,
                    context = context.type
                ).getOrThrow()

                LOG.debug("Evaluation result: ${result.value}")
                Result.success(result)
            } catch (e: Exception) {
                LOG.error("Failed to evaluate expression: $expression", e)
                Result.failure(EvaluationException("Evaluation failed", e))
            }
        }
    }

    override fun validate(expression: String): ValidationResult {
        // 实现表达式语法验证
        return if (expression.isBlank()) {
            ValidationResult.Invalid("Expression cannot be empty")
        } else {
            ValidationResult.Valid
        }
    }

    override suspend fun getCompletions(
        expression: String,
        position: Int,
        context: EvaluationContext
    ): Result<List<CompletionItem>> {
        // 实现代码补全
        return Result.success(emptyList())
    }
}

/**
 * 验证结果
 */
sealed class ValidationResult {
    object Valid : ValidationResult()
    data class Invalid(val message: String) : ValidationResult()
}

/**
 * 补全项
 */
data class CompletionItem(
    val label: String,
    val detail: String?,
    val documentation: String?
)
```

### 4.4 VariableService

```kotlin
/**
 * 变量服务
 *
 * 管理变量的查看和展开
 */
class VariableService(
    private val adapter: DebugAdapter
) {

    companion object {
        private val LOG = Logger.getInstance(VariableService::class.java)
    }

    /**
     * 获取作用域
     */
    suspend fun getScopes(frameId: Long): Result<List<Scope>> {
        return withContext(Dispatchers.IO) {
            try {
                // 调用适配器获取作用域
                // 这里需要扩展 DebugAdapter 接口
                Result.success(emptyList())
            } catch (e: Exception) {
                LOG.error("Failed to get scopes", e)
                Result.failure(VariableException("Failed to get scopes", e))
            }
        }
    }

    /**
     * 获取变量
     */
    suspend fun getVariables(variablesReference: Long): Result<List<Variable>> {
        return withContext(Dispatchers.IO) {
            try {
                LOG.debug("Getting variables: reference=$variablesReference")

                val variables = adapter.getVariables(variablesReference).getOrThrow()

                LOG.debug("Got ${variables.size} variables")
                Result.success(variables)
            } catch (e: Exception) {
                LOG.error("Failed to get variables", e)
                Result.failure(VariableException("Failed to get variables", e))
            }
        }
    }

    /**
     * 设置变量值
     */
    suspend fun setVariable(
        variablesReference: Long,
        name: String,
        value: String
    ): Result<Variable> {
        return withContext(Dispatchers.IO) {
            try {
                LOG.info("Setting variable: $name = $value")

                // 调用适配器设置变量
                // 这里需要扩展 DebugAdapter 接口

                Result.success(Variable(name, value, "", 0))
            } catch (e: Exception) {
                LOG.error("Failed to set variable", e)
                Result.failure(VariableException("Failed to set variable", e))
            }
        }
    }
}

/**
 * 作用域
 */
data class Scope(
    val name: String,
    val variablesReference: Long,
    val expensive: Boolean
)

/**
 * 变量
 */
data class Variable(
    val name: String,
    val value: String,
    val type: String,
    val variablesReference: Long,
    val presentationHint: String? = null
)
```

---

## 5. 进程管理设计

### 5.1 ProcessManager

```kotlin
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
 * 服务器配置
 */
data class ServerConfig(
    val portRange: IntRange = 58920..58930,
    val debuggerType: String = "lldbapi",
    val startupTimeoutMs: Long = 10000
)

/**
 * 服务器信息
 */
data class ServerInfo(
    val host: String,
    val port: Int,
    val processId: Long
)
```

### 5.2 PortManager

```kotlin
/**
 * 端口管理器
 *
 * 管理端口的分配和检测
 */
class PortManager {

    companion object {
        private val LOG = Logger.getInstance(PortManager::class.java)
    }

    /**
     * 查找可用端口
     */
    fun findAvailablePort(start: Int, end: Int): Result<Int> {
        for (port in start..end) {
            if (isPortAvailable(port)) {
                LOG.info("Found available port: $port")
                return Result.success(port)
            }
        }

        return Result.failure(
            PortException("No available ports in range $start-$end")
        )
    }

    /**
     * 检查端口是否可用
     */
    fun isPortAvailable(port: Int): Boolean {
        return try {
            ServerSocket(port).use { true }
        } catch (e: IOException) {
            false
        }
    }

    /**
     * 获取随机可用端口
     */
    fun getRandomAvailablePort(): Result<Int> {
        return try {
            ServerSocket(0).use { socket ->
                val port = socket.localPort
                LOG.info("Got random available port: $port")
                Result.success(port)
            }
        } catch (e: IOException) {
            Result.failure(PortException("Failed to get random port", e))
        }
    }
}
```

### 5.3 ServerManager

```kotlin
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
```

---

## 6. 配置系统设计

### 6.1 DebuggerConfig

```kotlin
/**
 * 调试器配置
 */
data class DebuggerConfig(
    val server: ServerConfig = ServerConfig(),
    val connection: ConnectionConfig = ConnectionConfig(),
    val adapter: AdapterConfig = AdapterConfig(),
    val logging: LoggingConfig = LoggingConfig()
) {
    companion object {
        /**
         * 从项目设置加载配置
         */
        fun load(project: Project): DebuggerConfig {
            // 从项目设置加载配置
            return DebuggerConfig()
        }

        /**
         * 默认配置
         */
        val DEFAULT = DebuggerConfig()
    }

    /**
     * 保存配置到项目设置
     */
    fun save(project: Project) {
        // 保存配置到项目设置
    }
}

/**
 * 日志配置
 */
data class LoggingConfig(
    val level: LogLevel = LogLevel.INFO,
    val logToFile: Boolean = true,
    val logPath: String? = null
)

/**
 * 日志级别
 */
enum class LogLevel {
    TRACE,
    DEBUG,
    INFO,
    WARN,
    ERROR
}
```

### 6.2 PlatformConfig

```kotlin
/**
 * 平台配置
 *
 * 提供平台特定的配置和路径
 */
object PlatformConfig {

    /**
     * 获取平台信息
     */
    fun getPlatformInfo(): PlatformInfo {
        return PlatformInfo(
            os = getOS(),
            arch = getArchitecture(),
            separator = File.separator
        )
    }

    private fun getOS(): OS {
        return when {
            SystemInfo.isWindows -> OS.WINDOWS
            SystemInfo.isMac -> OS.MACOS
            SystemInfo.isLinux -> OS.LINUX
            else -> OS.UNKNOWN
        }
    }

    private fun getArchitecture(): Architecture {
        return when {
            SystemInfo.isAarch64 -> Architecture.ARM64
            SystemInfo.isIntel64 -> Architecture.X64
            else -> Architecture.UNKNOWN
        }
    }

    /**
     * 获取平台特定的库路径
     */
    fun getLibraryPath(sdkHome: String): String {
        return when (getOS()) {
            OS.WINDOWS -> "$sdkHome\\third_party\\llvm\\lldb\\lib"
            OS.MACOS, OS.LINUX -> "$sdkHome/third_party/llvm/lldb/lib"
            OS.UNKNOWN -> ""
        }
    }
}

/**
 * 平台信息
 */
data class PlatformInfo(
    val os: OS,
    val arch: Architecture,
    val separator: String
)

/**
 * 操作系统
 */
enum class OS {
    WINDOWS,
    MACOS,
    LINUX,
    UNKNOWN
}

/**
 * 架构
 */
enum class Architecture {
    X64,
    ARM64,
    UNKNOWN
}
```

---

## 7. 异常处理设计

### 7.1 异常层次结构

```kotlin
/**
 * 调试器异常基类
 */
sealed class DebuggerException(
    message: String,
    cause: Throwable? = null
) : Exception(message, cause)

/**
 * 会话异常
 */
class DebugSessionException(
    message: String,
    cause: Throwable? = null
) : DebuggerException(message, cause)

/**
 * 适配器异常
 */
sealed class AdapterException(
    message: String,
    cause: Throwable? = null
) : DebuggerException(message, cause)

class DapAdapterException(
    message: String,
    cause: Throwable? = null
) : AdapterException(message, cause)

/**
 * 连接异常
 */
class DapConnectionException(
    message: String,
    cause: Throwable? = null
) : AdapterException(message, cause)

/**
 * 客户端异常
 */
class DapClientException(
    message: String,
    cause: Throwable? = null
) : AdapterException(message, cause)

/**
 * 断点异常
 */
class BreakpointException(
    message: String,
    cause: Throwable? = null
) : DebuggerException(message, cause)

/**
 * 求值异常
 */
class EvaluationException(
    message: String,
    cause: Throwable? = null
) : DebuggerException(message, cause)

/**
 * 变量异常
 */
class VariableException(
    message: String,
    cause: Throwable? = null
) : DebuggerException(message, cause)

/**
 * 服务器异常
 */
class ServerException(
    message: String,
    cause: Throwable? = null
) : DebuggerException(message, cause)

/**
 * 端口异常
 */
class PortException(
    message: String,
    cause: Throwable? = null
) : DebuggerException(message, cause)
```

### 7.2 错误恢复策略

```kotlin
/**
 * 错误恢复管理器
 */
class ErrorRecoveryManager {

    companion object {
        private val LOG = Logger.getInstance(ErrorRecoveryManager::class.java)
    }

    /**
     * 尝试恢复错误
     */
    suspend fun <T> tryRecover(
        operation: String,
        maxAttempts: Int = 3,
        block: suspend () -> Result<T>
    ): Result<T> {
        var lastError: Throwable? = null

        repeat(maxAttempts) { attempt ->
            try {
                LOG.debug("Attempting $operation (attempt ${attempt + 1}/$maxAttempts)")

                val result = block()
                if (result.isSuccess) {
                    return result
                }

                lastError = result.exceptionOrNull()
            } catch (e: Exception) {
                lastError = e
                LOG.warn("$operation failed (attempt ${attempt + 1}/$maxAttempts)", e)
            }

            if (attempt < maxAttempts - 1) {
                delay(1000 * (attempt + 1)) // 指数退避
            }
        }

        return Result.failure(
            lastError ?: Exception("Operation failed after $maxAttempts attempts")
        )
    }
}
```

# Debugger 模块设计文档 - 第四部分

## 8. UI集成设计

### 8.1 CangJieDebugProcess

```kotlin
/**
 * 仓颉调试进程
 *
 * IntelliJ平台的调试进程实现，集成所有服务
 */
class CangJieDebugProcess(
    private val runConfig: CangJieRunConfiguration,
    session: XDebugSession
) : XDebugProcess(session) {

    companion object {
        private val LOG = Logger.getInstance(CangJieDebugProcess::class.java)
    }

    // 服务组件
    private lateinit var processManager: ProcessManager
    private lateinit var debugSession: DebugSessionService
    private lateinit var breakpointService: BreakpointService
    private lateinit var evaluationService: EvaluationService
    private lateinit var variableService: VariableService

    // UI组件
    private val editorsProvider = CangJieDebuggerEditorsProvider()
    private val breakpointHandler = CangJieBreakpointHandler(this)

    init {
        initialize()
    }

    private fun initialize() {
        try {
            LOG.info("Initializing CangJie debug process")

            // 创建进程管理器
            processManager = ProcessManager(session.project)

            // 启动调试服务器
            val serverInfo = runBlocking {
                processManager.startServer(ServerConfig()).getOrThrow()
            }

            // 创建适配器
            val adapter = DapAdapter(
                project = session.project,
                config = DapAdapterConfig(
                    host = serverInfo.host,
                    port = serverInfo.port
                )
            )

            // 创建服务
            debugSession = DebugSessionService(session.project, session)
            debugSession.initialize(adapter)

            breakpointService = BreakpointService(session.project, adapter)
            evaluationService = EvaluationService(adapter)
            variableService = VariableService(adapter)

            // 启动调试会话
            val launchConfig = createLaunchConfig(serverInfo)
            runBlocking {
                debugSession.start(launchConfig).getOrThrow()
            }

            // 同步断点
            runBlocking {
                breakpointService.synchronize()
            }

            LOG.info("CangJie debug process initialized successfully")
        } catch (e: Exception) {
            LOG.error("Failed to initialize debug process", e)
            session.reportError("Failed to start debugger: ${e.message}")
            throw e
        }
    }

    private fun createLaunchConfig(serverInfo: ServerInfo): LaunchConfig {
        return LaunchConfig(
            program = runConfig.programPath,
            arguments = runConfig.programArguments,
            workingDirectory = runConfig.workingDirectory,
            environment = runConfig.environmentVariables,
            adapterConfig = AdapterConfig(
                host = serverInfo.host,
                port = serverInfo.port
            )
        )
    }

    override fun getEditorsProvider(): XDebuggerEditorsProvider {
        return editorsProvider
    }

    override fun getBreakpointHandlers(): Array<XBreakpointHandler<*>> {
        return arrayOf(breakpointHandler)
    }

    override fun resume(context: XSuspendContext?) {
        runBlocking {
            val threadId = (context as? CangJieSuspendContext)?.activeThreadId
            debugSession.resume(threadId).onFailure { error ->
                session.reportError("Failed to resume: ${error.message}")
            }
        }
    }

    override fun startStepOver(context: XSuspendContext?) {
        runBlocking {
            val threadId = (context as? CangJieSuspendContext)?.activeThreadId ?: return@runBlocking
            debugSession.stepOver(threadId).onFailure { error ->
                session.reportError("Failed to step over: ${error.message}")
            }
        }
    }

    override fun startStepInto(context: XSuspendContext?) {
        runBlocking {
            val threadId = (context as? CangJieSuspendContext)?.activeThreadId ?: return@runBlocking
            debugSession.stepInto(threadId).onFailure { error ->
                session.reportError("Failed to step into: ${error.message}")
            }
        }
    }

    override fun startStepOut(context: XSuspendContext?) {
        runBlocking {
            val threadId = (context as? CangJieSuspendContext)?.activeThreadId ?: return@runBlocking
            debugSession.stepOut(threadId).onFailure { error ->
                session.reportError("Failed to step out: ${error.message}")
            }
        }
    }

    override fun stop() {
        try {
            LOG.info("Stopping debug process")

            runBlocking {
                debugSession.stop()
                processManager.stopServer()
            }

            LOG.info("Debug process stopped")
        } catch (e: Exception) {
            LOG.error("Error stopping debug process", e)
        }
    }

    override fun runToPosition(position: XSourcePosition, context: XSuspendContext?) {
        // 实现运行到光标位置
        runBlocking {
            // 1. 设置临时断点
            // 2. 继续执行
        }
    }

    // 提供给其他组件使用的方法
    fun getBreakpointService(): BreakpointService = breakpointService
    fun getEvaluationService(): EvaluationService = evaluationService
    fun getVariableService(): VariableService = variableService
}
```

### 8.2 CangJieBreakpointHandler

```kotlin
/**
 * 仓颉断点处理器
 */
class CangJieBreakpointHandler(
    private val debugProcess: CangJieDebugProcess
) : XBreakpointHandler<XLineBreakpoint<CangJieLineBreakpointProperties>>(
    CangJieLineBreakpointType::class.java
) {

    companion object {
        private val LOG = Logger.getInstance(CangJieBreakpointHandler::class.java)
    }

    override fun registerBreakpoint(breakpoint: XLineBreakpoint<CangJieLineBreakpointProperties>) {
        runBlocking {
            debugProcess.getBreakpointService()
                .registerBreakpoint(breakpoint)
                .onFailure { error ->
                    LOG.error("Failed to register breakpoint", error)
                }
        }
    }

    override fun unregisterBreakpoint(
        breakpoint: XLineBreakpoint<CangJieLineBreakpointProperties>,
        temporary: Boolean
    ) {
        runBlocking {
            debugProcess.getBreakpointService()
                .unregisterBreakpoint(breakpoint)
                .onFailure { error ->
                    LOG.error("Failed to unregister breakpoint", error)
                }
        }
    }
}

/**
 * 仓颉行断点类型
 */
class CangJieLineBreakpointType : XLineBreakpointType<CangJieLineBreakpointProperties>(
    "cangjie-line",
    "CangJie Line Breakpoints"
) {

    override fun createBreakpointProperties(
        file: VirtualFile,
        line: Int
    ): CangJieLineBreakpointProperties? {
        return CangJieLineBreakpointProperties()
    }

    override fun canPutAt(file: VirtualFile, line: Int, project: Project): Boolean {
        // 检查是否可以在该位置设置断点
        return file.extension == "cj"
    }
}

/**
 * 仓颉行断点属性
 */
class CangJieLineBreakpointProperties : XBreakpointProperties<CangJieLineBreakpointProperties>() {

    var condition: String? = null
    var logMessage: String? = null
    var enabled: Boolean = true

    override fun getState(): CangJieLineBreakpointProperties = this

    override fun loadState(state: CangJieLineBreakpointProperties) {
        condition = state.condition
        logMessage = state.logMessage
        enabled = state.enabled
    }
}
```

### 8.3 CangJieSuspendContext

```kotlin
/**
 * 仓颉暂停上下文
 */
class CangJieSuspendContext(
    private val debugProcess: CangJieDebugProcess,
    val activeThreadId: Long,
    private val threads: List<ThreadInfo>,
    private val frames: List<StackFrameInfo>
) : XSuspendContext() {

    override fun getActiveExecutionStack(): XExecutionStack? {
        val activeThread = threads.find { it.id == activeThreadId } ?: return null
        return CangJieExecutionStack(debugProcess, activeThread, frames)
    }

    override fun getExecutionStacks(): Array<XExecutionStack> {
        return threads.map { thread ->
            CangJieExecutionStack(debugProcess, thread, emptyList())
        }.toTypedArray()
    }
}

/**
 * 仓颉执行栈
 */
class CangJieExecutionStack(
    private val debugProcess: CangJieDebugProcess,
    private val thread: ThreadInfo,
    private val frames: List<StackFrameInfo>
) : XExecutionStack(thread.name) {

    override fun getTopFrame(): XStackFrame? {
        return frames.firstOrNull()?.let { frame ->
            CangJieStackFrame(debugProcess, thread.id, frame)
        }
    }

    override fun computeStackFrames(firstFrameIndex: Int, container: XStackFrameContainer) {
        val framesToShow = frames.drop(firstFrameIndex).map { frame ->
            CangJieStackFrame(debugProcess, thread.id, frame)
        }

        container.addStackFrames(framesToShow, true)
    }
}

/**
 * 仓颉栈帧
 */
class CangJieStackFrame(
    private val debugProcess: CangJieDebugProcess,
    private val threadId: Long,
    private val frameInfo: StackFrameInfo
) : XStackFrame() {

    companion object {
        private val LOG = Logger.getInstance(CangJieStackFrame::class.java)
    }

    @Volatile
    private var loadingVariables = false
    private val loadLock = Any()

    override fun getSourcePosition(): XSourcePosition? {
        val sourcePath = frameInfo.source?.path ?: return null
        val virtualFile = LocalFileSystem.getInstance().findFileByPath(sourcePath) ?: return null

        return XSourcePositionImpl.create(virtualFile, frameInfo.line - 1)
    }

    override fun customizePresentation(component: ColoredTextContainer) {
        val position = sourcePosition

        if (position != null) {
            component.append(position.file.name, SimpleTextAttributes.REGULAR_ATTRIBUTES)
            component.append(":${position.line + 1}", SimpleTextAttributes.REGULAR_ATTRIBUTES)
            component.setIcon(AllIcons.Debugger.Frame)
        } else {
            component.append(frameInfo.name, SimpleTextAttributes.GRAYED_ATTRIBUTES)
            component.setIcon(AllIcons.Debugger.Frame)
        }
    }

    override fun computeChildren(node: XCompositeNode) {
        synchronized(loadLock) {
            if (loadingVariables) {
                LOG.debug("Variables already loading for frame ${frameInfo.id}")
                return
            }
            loadingVariables = true
        }

        // 异步加载变量
        ApplicationManager.getApplication().executeOnPooledThread {
            try {
                val scopes = runBlocking {
                    debugProcess.getVariableService()
                        .getScopes(frameInfo.id)
                        .getOrThrow()
                }

                val childrenList = XValueChildrenList()

                scopes.forEach { scope ->
                    val variables = runBlocking {
                        debugProcess.getVariableService()
                            .getVariables(scope.variablesReference)
                            .getOrThrow()
                    }

                    if (variables.isNotEmpty()) {
                        childrenList.addTopGroup(
                            CangJieScopeGroup(scope.name, variables, debugProcess)
                        )
                    }
                }

                node.addChildren(childrenList, true)
            } catch (e: Exception) {
                LOG.error("Failed to load variables", e)
                node.setErrorMessage("Failed to load variables: ${e.message}")
            } finally {
                loadingVariables = false
            }
        }
    }

    override fun getEvaluator(): XDebuggerEvaluator {
        return CangJieEvaluator(debugProcess, frameInfo.id, threadId)
    }
}

/**
 * 仓颉作用域组
 */
class CangJieScopeGroup(
    private val scopeName: String,
    private val variables: List<Variable>,
    private val debugProcess: CangJieDebugProcess
) : XValueGroup(scopeName) {

    override fun computeChildren(node: XCompositeNode) {
        val childrenList = XValueChildrenList()

        variables.forEach { variable ->
            childrenList.add(
                variable.name,
                CangJieVariable(debugProcess, variable)
            )
        }

        node.addChildren(childrenList, true)
    }
}

/**
 * 仓颉变量
 */
class CangJieVariable(
    private val debugProcess: CangJieDebugProcess,
    private val variable: Variable
) : XNamedValue(variable.name) {

    override fun computePresentation(node: XValueNode, place: XValuePlace) {
        node.setPresentation(
            AllIcons.Debugger.Value,
            variable.type,
            variable.value,
            variable.variablesReference > 0
        )
    }

    override fun computeChildren(node: XCompositeNode) {
        if (variable.variablesReference <= 0) {
            node.addChildren(XValueChildrenList.EMPTY, true)
            return
        }

        ApplicationManager.getApplication().executeOnPooledThread {
            try {
                val children = runBlocking {
                    debugProcess.getVariableService()
                        .getVariables(variable.variablesReference)
                        .getOrThrow()
                }

                val childrenList = XValueChildrenList()
                children.forEach { child ->
                    childrenList.add(child.name, CangJieVariable(debugProcess, child))
                }

                node.addChildren(childrenList, true)
            } catch (e: Exception) {
                node.setErrorMessage("Failed to load children: ${e.message}")
            }
        }
    }
}
```

### 8.4 CangJieEvaluator

```kotlin
/**
 * 仓颉表达式求值器
 */
class CangJieEvaluator(
    private val debugProcess: CangJieDebugProcess,
    private val frameId: Long,
    private val threadId: Long
) : XDebuggerEvaluator() {

    companion object {
        private val LOG = Logger.getInstance(CangJieEvaluator::class.java)
    }

    override fun evaluate(
        expression: String,
        callback: XEvaluationCallback,
        expressionPosition: XSourcePosition?
    ) {
        ApplicationManager.getApplication().executeOnPooledThread {
            try {
                val context = EvaluationContext(
                    frameId = frameId,
                    threadId = threadId,
                    type = EvaluationType.WATCH
                )

                val result = runBlocking {
                    debugProcess.getEvaluationService()
                        .evaluate(expression, context)
                        .getOrThrow()
                }

                val variable = Variable(
                    name = expression,
                    value = result.value,
                    type = result.type ?: "",
                    variablesReference = result.variablesReference
                )

                callback.evaluated(CangJieVariable(debugProcess, variable))
            } catch (e: Exception) {
                LOG.error("Failed to evaluate expression: $expression", e)
                callback.errorOccurred(e.message ?: "Evaluation failed")
            }
        }
    }
}
```

### 8.5 CangJieDebuggerEditorsProvider

```kotlin
/**
 * 仓颉调试器编辑器提供者
 */
class CangJieDebuggerEditorsProvider : XDebuggerEditorsProvider() {

    override fun getFileType(): FileType {
        return CangJieFileType.INSTANCE
    }

    override fun createDocument(
        project: Project,
        expression: XExpression,
        sourcePosition: XSourcePosition?,
        context: EvaluationMode
    ): Document {
        return EditorFactory.getInstance().createDocument(expression.expression)
    }
}
```

---

## 9. 测试策略

### 9.1 单元测试

```kotlin
/**
 * DapAdapter 单元测试
 */
class DapAdapterTest {

    private lateinit var adapter: DapAdapter
    private lateinit var mockServer: IDebugProtocolServer

    @BeforeEach
    fun setup() {
        mockServer = mockk()
        // 设置测试环境
    }

    @Test
    fun `test initialize adapter`() = runBlocking {
        // Given
        val config = AdapterConfig(host = "localhost", port = 12345)

        // When
        val result = adapter.initialize(config)

        // Then
        assertTrue(result.isSuccess)
        assertEquals(ConnectionState.Connected, adapter.connectionState.value)
    }

    @Test
    fun `test set breakpoints`() = runBlocking {
        // Given
        val source = SourceFile("/path/to/file.cj", "file.cj")
        val breakpoints = listOf(
            BreakpointSpec(line = 10, column = 0, condition = null, logMessage = null)
        )

        // When
        val result = adapter.setBreakpoints(source, breakpoints)

        // Then
        assertTrue(result.isSuccess)
        val results = result.getOrThrow()
        assertEquals(1, results.size)
        assertTrue(results[0].verified)
    }
}

/**
 * BreakpointService 单元测试
 */
class BreakpointServiceTest {

    private lateinit var service: BreakpointService
    private lateinit var mockAdapter: DebugAdapter
    private lateinit var mockProject: Project

    @BeforeEach
    fun setup() {
        mockAdapter = mockk()
        mockProject = mockk()
        service = BreakpointService(mockProject, mockAdapter)
    }

    @Test
    fun `test register breakpoint`() = runBlocking {
        // Given
        val breakpoint = mockk<XLineBreakpoint<*>>()
        every { breakpoint.sourcePosition } returns mockk {
            every { file } returns mockk {
                every { path } returns "/path/to/file.cj"
            }
        }
        every { breakpoint.line } returns 10

        // When
        val result = service.registerBreakpoint(breakpoint)

        // Then
        assertTrue(result.isSuccess)
        val managedBp = result.getOrThrow()
        assertEquals(BreakpointState.Pending, managedBp.state)
    }
}
```

### 9.2 集成测试

```kotlin
/**
 * 调试会话集成测试
 */
class DebugSessionIntegrationTest {

    private lateinit var project: Project
    private lateinit var session: DebugSessionService

    @BeforeEach
    fun setup() {
        // 设置测试项目和环境
    }

    @Test
    fun `test complete debug session lifecycle`() = runBlocking {
        // Given
        val config = LaunchConfig(
            program = "/path/to/program",
            workingDirectory = "/path/to/workdir",
            adapterConfig = AdapterConfig(port = 12345)
        )

        // When - 启动会话
        val startResult = session.start(config)
        assertTrue(startResult.isSuccess)
        assertEquals(SessionState.Running, session.state.value)

        // When - 暂停
        val pauseResult = session.pause()
        assertTrue(pauseResult.isSuccess)

        // When - 继续
        val resumeResult = session.resume()
        assertTrue(resumeResult.isSuccess)

        // When - 停止
        val stopResult = session.stop()
        assertTrue(stopResult.isSuccess)
        assertEquals(SessionState.Stopped, session.state.value)
    }
}
```

### 9.3 测试覆盖率目标

- **核心组件**: 90%+ 代码覆盖率
- **服务层**: 85%+ 代码覆盖率
- **适配器层**: 80%+ 代码覆盖率
- **UI层**: 70%+ 代码覆盖率

---

## 10. 实施路线图

### 阶段1: 基础设施 (1-2周)

**目标**: 建立项目基础和核心接口

- [ ] 创建模块结构
- [ ] 定义核心接口
- [ ] 实现异常体系
- [ ] 实现配置系统
- [ ] 实现日志系统
- [ ] 编写基础单元测试

**交付物**:

- 完整的模块结构
- 核心接口定义
- 基础设施代码

### 阶段2: 进程和连接管理 (1-2周)

**目标**: 实现服务器和连接管理

- [ ] 实现 ProcessManager
- [ ] 实现 PortManager
- [ ] 实现 ServerManager
- [ ] 实现 DapConnection
- [ ] 实现连接重试机制
- [ ] 编写集成测试

**交付物**:

- 可工作的服务器启动和连接
- 端口管理功能
- 连接管理功能

### 阶段3: DAP协议适配器 (2-3周)

**目标**: 实现完整的DAP协议支持

- [ ] 实现 DapAdapter
- [ ] 实现 DapClient
- [ ] 实现事件处理
- [ ] 实现协议转换
- [ ] 编写协议测试

**交付物**:

- 完整的DAP协议实现
- 事件处理机制
- 协议测试套件

### 阶段4: 服务层 (2-3周)

**目标**: 实现核心调试服务

- [ ] 实现 DebugSessionService
- [ ] 实现 BreakpointService
- [ ] 实现 EvaluationService
- [ ] 实现 VariableService
- [ ] 编写服务测试

**交付物**:

- 完整的服务层实现
- 服务测试套件

### 阶段5: UI集成 (2-3周)

**目标**: 集成IntelliJ平台UI

- [ ] 实现 CangJieDebugProcess
- [ ] 实现 CangJieBreakpointHandler
- [ ] 实现 CangJieSuspendContext
- [ ] 实现 CangJieStackFrame
- [ ] 实现 CangJieEvaluator
- [ ] 编写UI测试

**交付物**:

- 完整的UI集成
- 可用的调试器

### 阶段6: 测试和优化 (1-2周)

**目标**: 全面测试和性能优化

- [ ] 端到端测试
- [ ] 性能测试
- [ ] 内存泄漏检测
- [ ] 错误处理测试
- [ ] 文档完善

**交付物**:

- 完整的测试套件
- 性能优化报告
- 用户文档

### 总计: 9-15周

---

## 11. 依赖和技术要求

### 11.1 Gradle依赖

```kotlin
dependencies {
    // IntelliJ Platform
    implementation(project(":"))
    implementation(project(":psi"))
    implementation(project(":common"))
    implementation(project(":util"))
    implementation(project(":toolchain"))
    implementation(project(":cangjie-project"))

    // LSP4J for DAP
    implementation("org.eclipse.lsp4j:org.eclipse.lsp4j.debug:0.21.0")

    // Kotlin Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:1.7.3")

    // Testing
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.0")
    testImplementation("io.mockk:mockk:1.13.8")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
}
```

### 11.2 最低要求

- **IntelliJ Platform**: 2024.1+
- **Kotlin**: 1.9+
- **JDK**: 17+
- **Gradle**: 8.0+

---

## 12. 附录

### 12.1 术语表

- **DAP**: Debug Adapter Protocol - 调试适配器协议
- **LSP4J**: Language Server Protocol for Java - Java的语言服务器协议实现
- **XDebugProcess**: IntelliJ平台的调试进程抽象
- **Suspend Context**: 暂停上下文，表示调试器暂停时的状态

### 12.2 参考资料

- [Debug Adapter Protocol Specification](https://microsoft.github.io/debug-adapter-protocol/)
- [IntelliJ Platform SDK](https://plugins.jetbrains.com/docs/intellij/)
- [LSP4J Documentation](https://github.com/eclipse/lsp4j)
- [Kotlin Coroutines Guide](https://kotlinlang.org/docs/coroutines-guide.html)

### 12.3 设计决策记录

#### 为什么选择DAP协议？

DAP是微软开发的标准调试协议，具有以下优势：

- 语言无关，易于扩展
- 有成熟的实现（LSP4J）
- 社区支持良好
- 与LSP协议一致的设计理念

#### 为什么使用Kotlin Coroutines？

- 简化异步代码
- 更好的错误处理
- 结构化并发
- 与IntelliJ平台集成良好

#### 为什么采用分层架构？

- 清晰的职责分离
- 易于测试
- 易于维护和扩展
- 符合SOLID原则

---

## 结论

本设计文档提供了debugger模块的完整架构设计，包括：

1. **清晰的分层架构**: 表示层、服务层、适配器层、基础设施层
2. **完整的组件设计**: 所有核心组件的接口和实现
3. **健壮的错误处理**: 完整的异常体系和恢复策略
4. **全面的测试策略**: 单元测试、集成测试、端到端测试
5. **详细的实施计划**: 分6个阶段，9-15周完成

该设计解决了现有dap-debugger模块的所有问题，并提供了一个现代化、可扩展、易维护的调试器实现。
