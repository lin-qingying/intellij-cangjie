package org.cangnova.cangjie.protodebugger.core

import com.intellij.execution.ExecutionException
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.process.*
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectRootUtil
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.SystemInfo
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.util.registry.Registry
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.PathUtil
import com.intellij.util.SlowOperations
import com.intellij.util.concurrency.AppExecutorUtil
import com.intellij.util.io.DigestUtil
import com.intellij.util.system.CpuArch
import com.intellij.xdebugger.XDebugSession
import com.intellij.xdebugger.XDebuggerUtil
import com.intellij.xdebugger.XSourcePosition
import kotlinx.coroutines.*
import kotlinx.coroutines.future.asCompletableFuture
import org.cangnova.cangjie.messages.DebuggerBundle
import org.cangnova.cangjie.protodebugger.exception.DebuggerCommandException
import org.cangnova.cangjie.protodebugger.execution.state.TargetState
import org.cangnova.cangjie.protodebugger.memory.Address
import org.cangnova.cangjie.protodebugger.memory.MemoryViewFacade
import org.cangnova.cangjie.protodebugger.path.getBinFile
import org.cangnova.cangjie.protodebugger.process.DebuggerProcessFactory
import org.cangnova.cangjie.protodebugger.process.HostMachine
import org.cangnova.cangjie.protodebugger.process.LocalHost
import org.cangnova.cangjie.protodebugger.services.*
import org.cangnova.cangjie.protodebugger.services.impl.*
import org.cangnova.cangjie.protodebugger.settings.ArchitectureType
import org.cangnova.cangjie.protodebugger.settings.ArchitectureType.*
import org.cangnova.cangjie.protodebugger.settings.DebuggerSettings
import org.cangnova.cangjie.protodebugger.transport.EventBroadcastHandler
import org.cangnova.cangjie.protodebugger.transport.MessageBus
import org.cangnova.cangjie.protodebugger.transport.ServerSocketTransport
import org.cangnova.cangjie.protodebugger.transport.isTest
import org.cangnova.cangjie.protodebugger.util.SourceFileHash
import org.cangnova.cangjie.protodebugger.util.ToolVersion
import org.cangnova.cangjie.protodebugger.util.appendSearchPath
import java.io.File
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

/**
 * 调试器驱动门面
 *
 * 组合所有服务层，提供统一的调试器接口
 * 这是重构后的DebuggerDriver的替代品
 */
class DebuggerDriverFacade(

    val session: XDebugSession,
    private val debuggerHandler: DebuggerHandler,
    private val architectureType: ArchitectureType,


    ) : AutoCloseable {
    companion object {
        private val LOG = Logger.getInstance(DebuggerDriverFacade::class.java)

        @Throws(DebuggerCommandException::class)
        fun parseAddressSafe(str: String): Address {
            return try {
                Address.Companion.Parser.hex(str)
            } catch (numberFormatException: NumberFormatException) {
                throw DebuggerCommandException(numberFormatException)
            }
        }
    }

    /**
     * 负责释放所有依托该类的子类资源，所有相关资源释放必须要要使用该属性注册
     */
    val facadeDisposable = Disposer.newDisposable()
    val project get() = session.project

    val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // 传输层
    private val transport: ServerSocketTransport = ServerSocketTransport()

    val messageBus: MessageBus = MessageBus(transport)

    // 状态管理器 - 单一真实来源
    val stateManager: DebuggerStateManager = DebuggerStateManager()

    private val eventBroadcastHandler: EventBroadcastHandler = EventBroadcastHandler(
        messageBus,
        stateManager,
        debuggerHandler,
        scope
    )

    // ==================== 配置属性 ====================

    val isElevated: Boolean get() = false
    val emulateTerminal: Boolean get() = DebuggerSettings.getInstance().isEmulateTerminal()
    val isContinueAfterAttachNeeded: Boolean get() = true
    val hostMachine: HostMachine get() = LocalHost
    val useSTLRenderers: Boolean get() = DebuggerSettings.getInstance().isStlRenderersEnabled()
    val disableASLR: Boolean get() = DebuggerSettings.getInstance().isDisableASLR()

    fun isStaticVarsLoadingEnabled() = Registry.`is`("cangjie.debugger.lldb.statics")

    fun convertToProjectModelPath(absolutePath: String?) = absolutePath ?: "null"
    fun convertToLocalPath(absolutePath: String?) = absolutePath
    fun convertToEnvPath(localPath: String?) = localPath

    // 服务层
    val breakpointService: BreakpointService
    val steppingService: SteppingService
    val evalService: EvalService
    val memoryService: MemoryService
    val disasmService: DisasmService
    val sessionService: SessionService
    val commandService: CommandService

    // 内存视图门面
    val memoryViewFacade: MemoryViewFacade

    // 进程处理器
    private val commandLine = DebuggerProcessFactory.createDriverCommandLine(port, architectureType)
    lateinit var frontendHandler: BaseProcessHandler<*>


    init {


        eventBroadcastHandler.start()

        // 初始化服务层（使用broadcastHandler获取capabilities）
        // 先创建 sessionService，因为其他服务可能需要依赖它
        sessionService = SessionServiceImpl(
            debuggerHandler,
            messageBus,
            this,
            DebuggerCapabilities(0),
            stateProvider = { stateManager.getState() },
            stoppedThreadProvider = { stateManager.getStoppedThread() }
        )
        breakpointService = BreakpointServiceImpl(this, messageBus, this, 0L)
        steppingService = SteppingServiceImpl(messageBus, this, 0L)
        evalService = EvalServiceImpl(messageBus, this)
        memoryService = MemoryServiceImpl(messageBus, 0L)
        disasmService = DisasmServiceImpl(messageBus, 0L, session)
        commandService = CommandServiceImpl(messageBus)

        // 创建内存视图门面
        memoryViewFacade = MemoryViewFacade(this)

        if (!isTest) {
            // 创建前端进程处理器
            frontendHandler = DebuggerProcessFactory.createDebugProcessHandler(commandLine, isElevated, hostMachine)
            frontendHandler.addProcessListener(
                object : ProcessAdapter() {


                    override fun onTextAvailable(event: ProcessEvent, outputType: Key<*>) {

                        val text = event.text
                        if (text != null) {
                            if (ProcessOutputType.isStderr(outputType) || ProcessOutputType.isStdout(outputType)) {
                                if (LOG.isDebugEnabled) {
                                    LOG.debug(PathUtil.getFileName(commandLine.exePath) + " [" + outputType + "]: " + text)
                                }
                                debuggerHandler.handleDebuggerOutput(text, outputType)
                            }
                        }
                    }

                    override fun processTerminated(event: ProcessEvent) {
                        val exitCode = event.exitCode
                        LOG.info("Debugger frontend process terminated with exit code: $exitCode")


                        // 清理I/O资源
                        cleanupResources()

                        // 通知handler进程已终止
                        debuggerHandler.handleExited(exitCode)
                    }


                    private fun cleanupResources() {
                        try {
                            // 关闭服务
                            messageBus.close()

                            // 取消协程作用域
                            // scope.cancel() 本身不会抛出异常，但会取消所有正在执行的协程
                            scope.cancel()

                            // 清理SessionService中的I/O资源
                            sessionService.let { service ->
                                // 关闭I/O资源管理器
                                service.close()
                            }
                        } catch (e: Exception) {
                            LOG.warn("Error cleaning up session service resources", e)
                        }
                    }
                }, facadeDisposable
            )
            // 启动前端进程
            startFrontend()
        }


    }

    /**
     * 获取传输层端口
     */
    val port: Int
        get() = transport.port

    /**
     * 等待连接建立
     */
    suspend fun waitForConnection() {
        transport.waitForConnection()
    }

    /**
     * 等待调试器初始化完成
     *
     * 等待收到 InitializedEvent 广播，确保调试器服务器完全就绪后再发送命令。
     * 这是启动调试会话的第一步，必须在发送任何其他命令（如 create_target）之前调用。
     *
     * @throws kotlinx.coroutines.TimeoutCancellationException 如果超时（30秒）
     */
    suspend fun waitForInitialization() {
        eventBroadcastHandler.waitForInitialization()
    }

    /**
     * 是否已连接
     */
    val isConnected: Boolean
        get() = transport.isConnected

    private fun startFrontend() {
        try {
            frontendHandler.startNotify()
            LOG.info("Debugger frontend started on port $port")
        } catch (e: Exception) {
            LOG.error("Failed to start debugger frontend", e)
            throw e
        }
    }

    fun exit(): Boolean {

        return executeCommand {

            sessionService.exit()
        }.get()


    }

    private fun executeSyncCommand(block: suspend () -> Unit): Boolean {
        return try {
            runBlocking(coroutineDispatcher) {
                withTimeout(2000L) {
                    block()
                }
            }
            true
        } catch (e: TimeoutCancellationException) {
            LOG.warn("Command timeout")
            false
        } catch (e: Exception) {
            LOG.warn("Command failed: ${e.message}")
            false
        }
    }

    override fun close() {
        try {
            if (!isTest) {
                // 停止前端进程
                if (!frontendHandler.isProcessTerminated) {
                    hostMachine.destroyProcess(frontendHandler)
                }
            } else {
                messageBus.close()
                scope.cancel()

            }

            Disposer.dispose(facadeDisposable)

            LOG.info("Debugger driver closed")
        } catch (e: Exception) {
            LOG.error("Error closing debugger driver", e)
        }
    }


    // ==================== 协程调度器配置 ====================
    /**
     * 主协程调度器
     *
     * 使用单线程执行器确保调试器命令按顺序执行，避免并发问题。
     * 所有调试器命令（如断点操作、步进控制等）都通过此调度器执行。
     */
    private val coroutineDispatcher: ExecutorCoroutineDispatcher

    /**
     * 备用协程调度器
     *
     * 用于需要独立执行的特殊操作，避免与主调度器竞争。
     * 主要用于执行器管理或其他后台任务。
     */
    private val alternativeCoroutineDispatcher: ExecutorCoroutineDispatcher

    init {
        // 创建主调试器命令调度器
        // 使用单线程执行器确保命令执行的顺序性和线程安全
        val createBoundedApplicationPoolExecutor =
            AppExecutorUtil.createBoundedApplicationPoolExecutor(javaClass.getSimpleName(), 1)
        this.coroutineDispatcher = createBoundedApplicationPoolExecutor.asCoroutineDispatcher()

        // 创建备用调度器
        // 用于独立的后台任务，避免与主调试器命令冲突
        val createBoundedApplicationPoolExecutor2 =
            AppExecutorUtil.createBoundedApplicationPoolExecutor(javaClass.getSimpleName(), 1)
        this.alternativeCoroutineDispatcher = createBoundedApplicationPoolExecutor2.asCoroutineDispatcher()

        // 注册资源清理，确保IDE关闭时正确释放调度器资源
        Disposer.register(facadeDisposable) {
            LOG.debug("Closing coroutine dispatchers")
            coroutineDispatcher.close()
            alternativeCoroutineDispatcher.close()
        }
    }

    fun locateFileByPath(
        path: String,
        hash: SourceFileHash?
    ): VirtualFile? {
        val file = LocalFileSystem.getInstance().findFileByPath(path)
        return if (file == null || hash != null && !isHashMatched(file, hash)) null else file
    }

    private fun getDigest(type: SourceFileHash.Type): MessageDigest {
        return when (type) {
            SourceFileHash.Type.MD5 -> DigestUtil.md5()
            SourceFileHash.Type.SHA1 -> DigestUtil.sha1()
            SourceFileHash.Type.SHA256 -> DigestUtil.sha256()
        }
    }

    private fun isHashMatched(
        file: VirtualFile,
        hash: SourceFileHash
    ): Boolean {
        ProgressManager.checkCanceled()
        val digest = getDigest(hash.type)
        try {
            digest.update(file.contentsToByteArray())
        } catch (e: IOException) {
            return false
        }
        return DigestUtil.digestToHash(digest) == hash.hash
    }

    /**
     * 异步执行调试器命令
     *
     * 此方法提供线程安全的调试器命令执行，确保消息发送的顺序性和并发安全。
     *
     * 特性：
     * - 自动等待初始化：在执行命令前等待 InitializedEvent，确保调试器服务器就绪
     * - 线程安全：使用单线程调度器确保所有命令按顺序执行
     * - 状态检查：在执行前检查调试器状态，防止在不合适的时候执行命令
     * - 错误处理：统一的异常处理机制，确保错误能够正确传播
     * - 异步执行：返回 CompletableFuture，支持异步编程模式
     *
     * 参数说明：
     * @param canExecuteWhileRunning 是否允许在目标运行时执行命令，默认为 true
     * @param useAlternativeDispatcher 是否使用备用调度器，默认为 false
     * @param block 要执行的调试器命令 suspend 函数
     *
     * 状态检查逻辑：
     * - 当 canExecuteWhileRunning = false 时，只允许在 TargetState.Paused 状态下执行
     * - 当 canExecuteWhileRunning = true 时，允许在任何状态下执行
     * - 如果状态不允许执行命令，任务会被优雅地取消而不是抛出异常
     *
     * 异常处理：
     * - CancellationException：当状态检查失败时任务被取消
     * - ExecutionException：当命令执行失败时包装原始异常
     *
     * 使用示例：
     * ```kotlin
     * // 在调试器暂停时执行命令（默认行为）
     * facade.executeCommand {
     *     breakpointService.addLineBreakpoint("file.cj", 10, null)
     * }
     *
     * // 允许在运行时执行命令
     * facade.executeCommand(canExecuteWhileRunning = true) {
     *     someRunningCommand()
     * }
     * ```
     *
     * 线程模型：
     * - 所有命令都通过单线程调度器执行，保证顺序性
     * - 网络通信使用 Mutex 保护，避免并发冲突
     * - 支持协程挂起和恢复，不阻塞调用线程
     */
    fun <T> executeCommand(
        canExecuteWhileRunning: Boolean = true,
        useAlternativeDispatcher: Boolean = false,
        block: suspend () -> T
    ): CompletableFuture<T> {
        // 使用单线程调度器确保消息发送的顺序性
        val dispatcher = if (useAlternativeDispatcher) alternativeCoroutineDispatcher else coroutineDispatcher

        return scope.async(dispatcher) {
            // 等待调试器初始化完成
            waitForInitialization()

            // 检查调试器状态，如果不允许在运行时执行且当前状态不是暂停，则取消任务
            if (!canExecuteWhileRunning && stateManager.getState() !== TargetState.Paused) {
                LOG.debug("Cannot execute command while debugger is running, cancelling task")
                // 取消协程并返回取消的Future
                coroutineContext.cancel()
                kotlinx.coroutines.yield() // 让出执行权，确保取消生效
                // 协程被取消后这里不会执行
            }

            try {
                block()
            } catch (e: CancellationException) {
                // 协程取消是正常的流程控制，直接重新抛出，不包装
                LOG.debug("Command execution cancelled")
                throw e
            } catch (e: ExecutionException) {
                throw e
            } catch (e: Exception) {
                throw ExecutionException("Failed to execute debugger command", e)
            }
        }.asCompletableFuture()
    }

    /**
     * 异步执行调试器命令（不等待初始化）
     *
     * 此方法用于已经确定调试器初始化完成的场景，跳过等待初始化的步骤。
     * 大部分情况下应该使用 executeCommand() 方法，它会自动等待初始化。
     *
     * @param canExecuteWhileRunning 是否允许在目标运行时执行命令，默认为 true
     * @param useAlternativeDispatcher 是否使用备用调度器，默认为 false
     * @param block 要执行的调试器命令 suspend 函数
     */
    fun <T> executeCommandWithoutWait(
        canExecuteWhileRunning: Boolean = true,
        useAlternativeDispatcher: Boolean = false,
        block: suspend () -> T
    ): CompletableFuture<T> {
        // 使用单线程调度器确保消息发送的顺序性
        val dispatcher = if (useAlternativeDispatcher) alternativeCoroutineDispatcher else coroutineDispatcher

        return scope.async(dispatcher) {
            // 检查调试器状态，如果不允许在运行时执行且当前状态不是暂停，则取消任务
            if (!canExecuteWhileRunning && stateManager.getState() !== TargetState.Paused) {
                LOG.debug("Cannot execute command while debugger is running, cancelling task")
                // 取消协程并返回取消的Future
                coroutineContext.cancel()
                kotlinx.coroutines.yield() // 让出执行权，确保取消生效
                // 协程被取消后这里不会执行
            }

            try {
                block()
            } catch (e: CancellationException) {
                // 协程取消是正常的流程控制，直接重新抛出，不包装
                LOG.debug("Command execution cancelled")
                throw e
            } catch (e: ExecutionException) {
                throw e
            } catch (e: Exception) {
                throw ExecutionException("Failed to execute debugger command", e)
            }
        }.asCompletableFuture()
    }

    /**
     * 异步执行命令，不等待结果返回
     *
     * @param block 要执行的挂起函数块
     */
    fun executeCommandAsync(
        block: suspend () -> Unit
    ) {
        scope.launch {
            block()
        }
    }

    /**
     * 获取当前调试器状态
     */
    fun getState() = stateManager.getState()

    /**
     * 获取当前停止的线程
     */
    fun getStoppedThread() = stateManager.getStoppedThread()
    fun getStopPlace() = stateManager.getStopPlace()

    /**
     * 获取调试器能力标志
     */
    fun getCapabilities() = stateManager.getCapabilities()


    suspend fun resize(columns: Int, rows: Int) {


    }


    /**
     * 设置变量值
     *
     * @param variable 要修改的变量
     * @param newValue 新的值
     * @return 修改是否成功
     */
    suspend fun setVariableValue(
        variable: org.cangnova.cangjie.protodebugger.data.LLDBVariable,
        newValue: String
    ): Boolean {
        return evalService.setValue(variable, newValue)
    }

    /**
     * 检查变量是否可修改
     *
     * @param variable 变量
     * @return 如果变量可修改返回true，否则返回false
     */
    suspend fun isVariableMutable(variable: org.cangnova.cangjie.protodebugger.data.LLDBVariable): Boolean {
        return evalService.isMutable(variable)
    }

    fun createSourcePosition(filePath: String, hash: SourceFileHash?, line: Int): XSourcePosition? {
        val file = locateFileByPath(filePath, hash)
        return file?.let {
            XDebuggerUtil.getInstance().createPosition(it, line)
        }

    }


}


