package org.cangnova.cangjie.protodebugger.services.impl

import com.intellij.execution.CommandLineUtil
import com.intellij.execution.ExecutionException
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.process.ProcessOutputTypes
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.NlsSafe
import com.intellij.openapi.util.SystemInfo
import com.pty4j.unix.Pty
import kotlinx.coroutines.sync.Mutex
import lldbprotobuf.Model
import lldbprotobuf.RequestOuterClass
import lldbprotobuf.ResponseOuterClass
import org.cangnova.cangjie.messages.DebuggerBundle
import org.cangnova.cangjie.protodebugger.data.LLDBFrame
import org.cangnova.cangjie.protodebugger.data.LLDBThread
import org.cangnova.cangjie.protodebugger.data.newLLDBFrame
import org.cangnova.cangjie.protodebugger.data.newLLThread
import org.cangnova.cangjie.protodebugger.exception.DebuggerCommandException
import org.cangnova.cangjie.protodebugger.execution.async.AsyncResult
import org.cangnova.cangjie.protodebugger.execution.state.TargetState
import org.cangnova.cangjie.protodebugger.ipc.WindowsPipe
import org.cangnova.cangjie.protodebugger.path.PathMapping
import org.cangnova.cangjie.protodebugger.process.HostMachine
import org.cangnova.cangjie.protodebugger.process.LocalHost
import org.cangnova.cangjie.protodebugger.process.ProcessOutputReaders
import org.cangnova.cangjie.protodebugger.protocol.ProtobufFactory
import org.cangnova.cangjie.protodebugger.services.SessionService
import org.cangnova.cangjie.protodebugger.transport.MessageBus
import org.cangnova.cangjie.protodebugger.util.SourceFileHash
import org.cangnova.cangjie.protodebugger.util.Installer
import org.cangnova.cangjie.protodebugger.core.DebuggerHandler

import lldbprotobuf.ResponseOuterClass.*
import org.cangnova.cangjie.protodebugger.core.DebuggerDriverFacade
import java.io.File
import java.io.OutputStream

/**
 * 调试会话服务的现代化实现
 *
 * 负责管理调试目标的生命周期，包括启动、附加、加载 core dump 等操作
 */
class SessionServiceImpl(
    val debuggerHandler: DebuggerHandler,
    private val messageBus: MessageBus,

    private val facade: DebuggerDriverFacade,
    private val capabilities: DebuggerCapabilities,
    private val stateProvider: () -> TargetState = { TargetState.Idle },
    private val stoppedThreadProvider: () -> LLDBThread? = { null }
) : SessionService, AutoCloseable {

    private val socketLock = Mutex()
    private val inferiorResult = AsyncResult.create<SessionService.Inferior>()

    // I/O 资源管理
    private val ioResources = IOResourceManager(
        debuggerHandler = debuggerHandler
    )

    override fun getProcessInput(): OutputStream? = ioResources.processInput

    // ==================== 目标加载 ====================

    override suspend fun loadForLaunch(installer: Installer, architecture: String?) {
        runCatching {
            val inferior = createLaunchInferior(installer, architecture)
            inferiorResult.complete(inferior)
        }.onFailure { ex ->
            when (ex) {
                is ExecutionException -> {
                    inferiorResult.completeExceptionally(ex)
                    throw ex
                }

                else -> throw ExecutionException("Failed to load target", ex)
            }
        }
    }

    override suspend fun loadForAttach(processId: Int): SessionService.Inferior {
        createEmptyTarget()
        return AttachInferior(processId,  messageBus, this,)
    }


    override suspend fun loadForRemote(
        installer: Installer,
        architecture: String?,
        platform: String,
        url: String
    ): SessionService.Inferior {
        val targetCommandLine = installer.install()
        createRemoteTarget(installer, architecture, platform, targetCommandLine)
        connectToRemotePlatform(platform, url)
        return LaunchInferior(targetCommandLine, installer,   messageBus, this, ioResources,facade.emulateTerminal)
    }

    // ==================== 目标控制 ====================

    override suspend fun startTarget() {
        inferiorResult.asCompletableFuture().get().start()
    }

    override suspend fun detach(): Boolean {
        val response = messageBus.request(
            ProtobufFactory.detach(),
            DetachResponse::class.java
        )
        return response.status.success
    }

    override suspend fun terminate(): Boolean {
        val response = messageBus.request(
            ProtobufFactory.terminate(),
            TerminateResponse::class.java
        )
        return response.status.success
    }

    override suspend fun disconnectTarget(shouldDestroy: Boolean) {
        val inferior = inferiorResult.asCompletableFuture().get()
        if (shouldDestroy) {
            inferior.destroy()
        } else {
            inferior.detach()
        }
    }

    override suspend fun exit(): Boolean {
        messageBus.send(ProtobufFactory.exit())
        return true
    }

    // ==================== 线程和帧信息 ====================

    override suspend fun getThreads(): List<LLDBThread> {
        val response = messageBus.request(
            ProtobufFactory.getThreads(),
            ThreadsResponse::class.java
        )

        response.status.ensureSuccess()
        return response.threadsList.map(::newLLThread)
    }

    override suspend fun getFrames(
        thread: LLDBThread,
        startFrame: Int,
        maxFrames: Int
    ): List<LLDBFrame> {
        val response = messageBus.request(
            ProtobufFactory.getFrames(thread.id, startFrame, maxFrames),
            FramesResponse::class.java
        )

        response.status.ensureSuccess()
        return response.framesList.map(::newLLDBFrame)
    }


    override fun getStoppedThread(): LLDBThread? = stoppedThreadProvider()
    override fun getState(): TargetState = stateProvider()





    // ==================== 控制台命令 ====================

    override suspend fun executeInterpreterCommand(
        threadId: Long,
        frameIndex: Int,
        command: String
    ): String {
        TODO()
//        val response = messageBus.request(
//            ProtobufFactory.handleConsoleCommand(threadId, frameIndex, command),
//            HandleConsoleCommandResponse::class.java
//        )
//
//        response.status.ensureSuccess()
//        return response.standardOutput
    }


    override fun isInPromptMode(): Boolean =
        stateProvider() == TargetState.Paused

    override suspend fun completeConsoleCommand(command: String, pos: Int): List<String> {
//        val response = messageBus.request(
//            ProtobufFactory.handleCompletion(command, pos),
//            ProtocolResponses.HandleCompletionResponse::class.java
//        )
//        return response.completionsList
        return emptyList()
    }

    override suspend fun resize(columns: Int, rows: Int) {
        messageBus.send(ProtobufFactory.resizeConsole(columns, rows))
    }

    // ==================== 功能检查 ====================

    override suspend fun checkErrors() {
        // 检查异步错误
    }

    override fun supportsCommandCancellation(): Boolean =
        capabilities.supportsCommandCancellation

    // ==================== 进程退出处理 ====================


    // ==================== 私有辅助方法 ====================

    private suspend fun createLaunchInferior(
        installer: Installer,
        architecture: String?
    ): SessionService.Inferior {
        val targetCommandLine = installer.install()
        createTarget(installer.executableFile.path, architecture)
        configureTarget()
        return LaunchInferior(targetCommandLine, installer,  messageBus, this, ioResources,facade.emulateTerminal)
    }

    private suspend fun createTarget(executablePath: String, architecture: String?) {
        val request = ProtobufFactory.createTarget(
            executablePath,
            architecture.orEmpty()
        )
        sendCreateTargetRequest(request)
        configureTarget()
    }

    private suspend fun createEmptyTarget() {
        sendCreateTargetRequest(ProtobufFactory.createTarget("", ""))
        configureTarget()
    }

    private suspend fun createRemoteTarget(
        installer: Installer,
        architecture: String?,
        platform: String,
        targetCommandLine: GeneralCommandLine
    ) {
        val request = ProtobufFactory.createRemoteTarget(
            installer.executableFile.path,
            platform,
            targetCommandLine.exePath,
            architecture.orEmpty(),
            null
        )
        sendCreateTargetRequest(request)
        configureTarget()
    }

    private suspend fun loadCoreFile(corePath: String) {
//        val response = messageBus.request(
//            ProtobufFactory.loadCoreDump(corePath),
//            ProtocolResponses.LoadCoreResponse::class.java
//        )
//
//        if (!response.status.success) {
//            throw ExecutionException(DebuggerBundle.message("error.cannot.load.core.dump"))
//        }
    }

    private suspend fun connectToRemotePlatform(platform: String, url: String) {
//        val response = messageBus.request(
//            ProtobufFactory.connectPlatform(platform, url),
//            ProtocolResponses.ConnectPlatformResponse::class.java
//        )
//
//        if (!response.status.success) {
//            throw ExecutionException(DebuggerBundle.message("error.cannot.connect.remote"))
//        }
    }

    private suspend fun sendCreateTargetRequest(request: RequestOuterClass.Request) {
        val response = messageBus.request(
            request,
            ResponseOuterClass.CreateTargetResponse::class.java
        )

        if (!response.status.success) {
            throw ExecutionException(DebuggerBundle.message("error.cannot.create.target"))
        }
    }

    private suspend fun configureTarget() {
        // 配置目标的各种设置（路径映射、符号设置等）
    }

    private suspend fun executePathMappingCommand(
        index: Int,
        mappings: List<PathMapping>,
        useTargetSourceMap: Boolean
    ) {
        if (mappings.isEmpty()) return

        val command = buildPathMappingCommand(index, mappings, useTargetSourceMap)
        executeConsoleCommand(command)
    }

    private fun buildPathMappingCommand(
        index: Int,
        mappings: List<PathMapping>,
        useTargetSourceMap: Boolean
    ): String = buildString {
        val prefix = when {
            useTargetSourceMap && index >= 0 -> "settings insert-before target.source-map "
            useTargetSourceMap -> "settings append target.source-map "
            index >= 0 -> "target modules search-paths insert "
            else -> "target modules search-paths add "
        }

        append(prefix)
        if (index >= 0) append("$index ")

        mappings.joinTo(this, separator = " ") { (from, to) ->
            "\"$from\" \"$to\""
        }
    }

    private fun buildSymbolCommand(symbols: File, module: File?): String =
        if (module != null) {
            "target module add \"${module.absolutePath}\" -s \"${symbols.absolutePath}\""
        } else {
            "target symbols add \"${symbols.absolutePath}\""
        }

    private suspend fun executeConsoleCommand(command: String) {
//        val response = messageBus.request(
//            ProtobufFactory.handleConsoleCommand(-1L, -1, command),
//            ProtocolResponses.HandleConsoleCommandResponse::class.java
//        )
//
//        if (!response.status.success) {
//            throw DebuggerCommandException(response.status.errorMessage)
//        }
    }

    private fun Model.Status.ensureSuccess() {
        if (!success) {
            throw DebuggerCommandException(message)
        }
    }

    // ==================== 资源清理 ====================

    /**
     * 关闭会话服务并清理所有I/O资源
     *
     * 该方法会关闭所有打开的I/O流（PTY、管道、输出读取器），
     * 确保在调试会话结束时释放所有系统资源。
     */
    override fun close() {
        LOG.debug("Closing SessionService and cleaning up I/O resources")
        ioResources.close()
    }

    companion object {
        private val LOG = Logger.getInstance(SessionServiceImpl::class.java)
        val USE_EXTERNAL_CONSOLE_KEY = Key.create<Boolean>("USE_EXTERNAL_CONSOLE")
    }
}

// ==================== 功能封装类 ====================

/**
 * 调试器功能封装
 */
@JvmInline
value class DebuggerCapabilities(private val flags: Long) {
    val supportsFileHashing: Boolean
        get() = (flags and FILE_HASHING_CAPABILITY) != 0L

    val supportsCommandCancellation: Boolean
        get() = (flags and COMMAND_CANCELLATION_CAPABILITY) != 0L

    companion object {
        private const val FILE_HASHING_CAPABILITY = 1L
        private const val COMMAND_CANCELLATION_CAPABILITY = 1L shl 6
    }
}

/**
 * I/O 资源管理器
 * 统一管理 PTY、管道和输出读取器
 */
class IOResourceManager(
    val debuggerHandler: DebuggerHandler
) : AutoCloseable {
    private var pty: Pty? = null
    private var outputReader: ProcessOutputReaders? = null
    private var input: OutputStream? = null

    val processInput: OutputStream? get() = input

    companion object {
        private val LOG = Logger.getInstance(IOResourceManager::class.java)
    }

    fun setupPty(): Pty {
        cleanup()
        return Pty().also {
            pty = it
            input = it.outputStream
        }
    }

    fun setupWindowsPipe(name: String): WindowsPipe {
        cleanup()
        return WindowsPipe.createOutboundPipe(name).also {
            input = it.outputStream
        }
    }

    fun setupOutputReader(
        host: HostMachine,
        commandLine: GeneralCommandLine,
        usePty: Boolean,
        emulateTerminal: Boolean,

        ): ProcessOutputReaders {
        val presentableName = CommandLineUtil.extractPresentableName(commandLine.commandLineString)

            .split(" ")
            .firstOrNull { it.isNotEmpty() }
            ?: "Debug Process"

        return object : ProcessOutputReaders(
            host,
            presentableName,
            commandLine.charset,
            usePty,
            emulateTerminal
        ) {
            override fun onTextAvailable(text: @NlsSafe String, key: Key<*>) {
                debuggerHandler.handleTargetOutput(text, key)
            }
        }.also { outputReader = it }
    }

    override fun close() = cleanup()

    private fun cleanup() {
        runCatching { pty?.close() }
        runCatching { outputReader?.close() }
        runCatching { input?.close() }
        pty = null
        outputReader = null
        input = null
    }

    /**
     * 处理进程退出时的资源清理
     */
    fun handleProcessExited(exitCode: Int) {
        LOG.info("Cleaning up resources after process exit, exit code: $exitCode")

        try {
            // 关闭所有I/O流
            input?.close()
            outputReader?.close()
            pty?.close()

            // 记录退出状态
            val exitStatus = when {
                exitCode == 0 -> "Normal exit"
                exitCode < 0 -> "Signal exit: ${-exitCode}"
                else -> "Error exit: $exitCode"
            }

            LOG.info("Process $exitStatus - Resources cleaned up successfully")

            // 清理引用
            pty = null
            outputReader = null
            input = null

        } catch (e: Exception) {
            LOG.warn("Error during process exit cleanup: ${e.message}")
        }
    }

    /**
     * 检查资源状态是否已清理
     */
    fun isCleanedUp(): Boolean {
        return pty == null && outputReader == null && input == null
    }
}

// ==================== Inferior 实现 ====================

/**
 * 启动类型的调试目标
 */
private class LaunchInferior(
    private val commandLine: GeneralCommandLine,
    private val installer: Installer,
    private val messageBus: MessageBus,
    private val sessionService: SessionServiceImpl,
    private val ioResources: IOResourceManager,
    private val emulateTerminal: Boolean
) : SessionService.Inferior {

    override fun getId(): Int = -1

    override suspend fun start(): Long {

        sessionService.debuggerHandler.handleTargetOutput(
            commandLine.commandLineString + "\n",
            ProcessOutputTypes.SYSTEM
        )
        val streamConfig = StreamConfiguration.from(commandLine, emulateTerminal)
        val streams = streamConfig.setup(ioResources)

        val response = messageBus.request(
            ProtobufFactory.launch(
                commandLine,
                streamConfig.useExternalConsole,
                emulateTerminal,
                streams.stdin,
                streams.stdout,
                streams.stderr
            ),
            LaunchResponse::class.java
        )

        if (!response.status.success) {
            throw ExecutionException(DebuggerBundle.message("error.cannot.launch"))
        }

        return response.process.id.toLong()
    }

    override suspend fun detach() {

        sessionService.detach()
    }

    override suspend fun destroy(): Boolean {


        ioResources.close()
        return sessionService.terminate()

    }
}

/**
 * 附加类型的调试目标
 */
private class AttachInferior(
    private val processId: Int,
    private val messageBus: MessageBus,
    private val sessionService: SessionServiceImpl
) : SessionService.Inferior {

    override fun getId(): Int = processId

    override suspend fun start(): Long {
        // 构建附加选项
        val options =  emptyMap<String,String>()

        val response = messageBus.request(
            ProtobufFactory.attach(processId.toLong(), options),
            AttachResponse::class.java
        )

        if (!response.status.success) {
            throw ExecutionException(DebuggerBundle.message("error.cannot.attach"))
        }

        return processId.toLong()
    }

    override suspend fun detach() {
        sessionService.detach()
    }

    override suspend fun destroy(): Boolean {
        sessionService.terminate()
        return true
    }
}


// ==================== 流配置 ====================

/**
 * 流配置信息
 */
private data class StreamConfiguration(
    val useExternalConsole: Boolean,
    val emulateTerminal: Boolean,
    val inputFile: File?,
    val commandLine: GeneralCommandLine
) {
    companion object {
        fun from(commandLine: GeneralCommandLine, emulateTerminal: Boolean) =
            StreamConfiguration(
                useExternalConsole = commandLine.getUserData(SessionServiceImpl.USE_EXTERNAL_CONSOLE_KEY) == true,
                emulateTerminal =  emulateTerminal,
                inputFile = commandLine.inputFile,
                commandLine = commandLine
            )
    }

    fun setup(ioResources: IOResourceManager): StreamPaths {
        var paths = when {
            inputFile != null -> setupWithInputFile(ioResources)
            !useExternalConsole -> setupInteractiveStreams(ioResources)
            else -> StreamPaths()
        }
        if (paths.stdout == null && !useExternalConsole) {

            val readers = ioResources.setupOutputReader(
                LocalHost,
                commandLine,
                !SystemInfo.isWindows, false
            )
            paths = paths.copy(
                stdout = readers.getOutFileAbsolutePath(), stderr = readers.getErrFileAbsolutePath()
            )

        }



        return paths
    }

    private fun setupWithInputFile(ioResources: IOResourceManager): StreamPaths {
        val file = inputFile ?: throw IllegalStateException()

        if (!file.isFile || !file.canRead()) {
            throw ExecutionException(
                DebuggerBundle.message("debug.driver.cannotReadInputFile", file.path)
            )
        }

        return when {
            emulateTerminal && !SystemInfo.isWindows -> {
                val pty = ioResources.setupPty()
                StreamPaths(file.path, pty.slaveName, pty.slaveName)
            }

            else -> StreamPaths(stdin = file.path)
        }
    }

    private fun setupInteractiveStreams(ioResources: IOResourceManager): StreamPaths {
        return if (SystemInfo.isWindows) {
            val pipe = ioResources.setupWindowsPipe("stdin")
            val (stdout, stderr) = if (emulateTerminal) {
                val readers = ioResources.setupOutputReader(
                    LocalHost,
                    commandLine,
                    false,
                    emulateTerminal
                )
                readers.getOutFileAbsolutePath() to null
            } else {
                null to null
            }
            StreamPaths(stdin = pipe.name, stdout = stdout, stderr = stderr)
        } else {
            val pty = ioResources.setupPty()

            val (stdout, stderr) = if (emulateTerminal) {

                pty.slaveName to pty.slaveName
            } else {
                null to null
            }
            StreamPaths(pty.slaveName, stdout, stderr)
        }
    }


}

/**
 * 流路径配置
 */
private data class StreamPaths(
    val stdin: String? = null,
    val stdout: String? = null,
    val stderr: String? = null
)