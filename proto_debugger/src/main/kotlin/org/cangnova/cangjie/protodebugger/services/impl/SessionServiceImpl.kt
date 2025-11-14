package org.cangnova.cangjie.protodebugger.services.impl

import com.intellij.execution.ExecutionException
import com.intellij.execution.configurations.GeneralCommandLine

import com.intellij.openapi.util.Key
import com.intellij.openapi.util.NlsSafe
import com.intellij.openapi.util.SystemInfo
import com.pty4j.unix.Pty
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.cangnova.cangjie.messages.DebuggerBundle
import org.cangnova.cangjie.protodebugger.core.DebuggerDriverConfiguration
import org.cangnova.cangjie.protodebugger.data.LLFrame
import org.cangnova.cangjie.protodebugger.data.LLThread
import org.cangnova.cangjie.protodebugger.data.newLLFrame
import org.cangnova.cangjie.protodebugger.data.newLLThread
import org.cangnova.cangjie.protodebugger.exception.DebuggerCommandException
import org.cangnova.cangjie.protodebugger.execution.ExecutionResult
import org.cangnova.cangjie.protodebugger.execution.TargetState
import org.cangnova.cangjie.protodebugger.ipc.WinPipe
import org.cangnova.cangjie.protodebugger.path.PathMapping
import org.cangnova.cangjie.protodebugger.process.HostMachine
import org.cangnova.cangjie.protodebugger.process.LocalHost
import org.cangnova.cangjie.protodebugger.process.ProcessOutputReaders
import org.cangnova.cangjie.protodebugger.protocol.ProtobufMessageFactory
import org.cangnova.cangjie.protodebugger.services.SessionService
import org.cangnova.cangjie.protodebugger.transport.MessageBus
import org.cangnova.cangjie.protodebugger.util.DebuggerSourceFileHash
import org.cangnova.cangjie.protodebugger.util.Installer
import proto.Model
import proto.Protocol
import proto.ProtocolResponses
import java.io.File
import java.io.IOException
import java.io.OutputStream
import com.intellij.openapi.diagnostic.Logger

/**
 * 调试会话服务的现代化实现
 *
 * 负责管理调试目标的生命周期，包括启动、附加、加载 core dump 等操作
 */
class SessionServiceImpl(
    private val messageBus: MessageBus,
    private val configuration: DebuggerDriverConfiguration,
    private val capabilities: DebuggerCapabilities,
    private val stateProvider: () -> TargetState = { TargetState.NOT_READY },
    private val stoppedThreadProvider: () -> LLThread? = { null }
) : SessionService {

    private val socketLock = Mutex()
    private val inferiorResult = ExecutionResult<SessionService.Inferior>()

    // I/O 资源管理
    private val ioResources = IOResourceManager()

    override fun getProcessInput(): OutputStream? = ioResources.processInput

    // ==================== 目标加载 ====================

    override suspend fun loadForLaunch(installer: Installer, architecture: String?) {
        runCatching {
            val inferior = createLaunchInferior(installer, architecture)
            inferiorResult.set(inferior)
        }.onFailure { ex ->
            when (ex) {
                is ExecutionException -> {
                    inferiorResult.setException(ex)
                    throw ex
                }
                else -> throw ExecutionException("Failed to load target", ex)
            }
        }
    }

    override suspend fun loadForAttach(processId: Int): SessionService.Inferior {
        createEmptyTarget()
        return AttachInferior(processId, configuration, messageBus, this)
    }

    override suspend fun loadCoreDump(
        exePath: String,
        corePath: String,
        architecture: String?
    ): SessionService.Inferior {
        createTarget(exePath, architecture)
        loadCoreFile(corePath)
        return CoreDumpInferior
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
        return LaunchInferior(targetCommandLine, installer, configuration, messageBus, ioResources)
    }

    // ==================== 目标控制 ====================

    override suspend fun startTarget() {
        inferiorResult.get().start()
    }

    override suspend fun detach(): Boolean {
        val response = messageBus.request(
            ProtobufMessageFactory.detach(),
            ProtocolResponses.DetachResponse::class.java
        )
        return response.status.success
    }

    override suspend fun kill(): Boolean {
        val response = messageBus.request(
            ProtobufMessageFactory.kill(),
            ProtocolResponses.KillResponse::class.java
        )
        return response.status.success
    }

    override suspend fun exit(): Boolean {
        messageBus.send(ProtobufMessageFactory.exit())
        return true
    }

    // ==================== 线程和帧信息 ====================

    override suspend fun getThreads(): List<LLThread> {
        val response = messageBus.request(
            ProtobufMessageFactory.getThreads(),
            ProtocolResponses.GetThreadsResponse::class.java
        )

        response.status.ensureSuccess()
        return response.threadsList.map(::newLLThread)
    }

    override suspend fun getFrames(
        thread: LLThread,
        startFrame: Int,
        maxFrames: Int
    ): List<LLFrame> {
        val response = messageBus.request(
            ProtobufMessageFactory.getFrames(thread.id, startFrame, maxFrames),
            ProtocolResponses.GetFramesResponse::class.java
        )

        response.status.ensureSuccess()
        return response.framesList.map(::newLLFrame)
    }

    override fun getStoppedThread(): LLThread? = stoppedThreadProvider()
    override fun getState(): TargetState = stateProvider()

    // ==================== 路径映射 ====================

    override suspend fun addPathMapping(index: Int, from: String, to: String) {
        val mapping = PathMapping(
            configuration.convertToProjectModelPath(from),
            configuration.convertToProjectModelPath(to)
        )
        executePathMappingCommand(index, listOf(mapping), useTargetSourceMap = true)
    }

    override suspend fun addForcedFileMapping(
        index: Int,
        from: String,
        hash: DebuggerSourceFileHash?,
        to: String
    ) {
        val sourcePath = if (capabilities.supportsFileHashing && hash != null) {
            "${hash.type}:${hash.hash}"
        } else {
            from
        }
        addPathMapping(index, sourcePath, to)
    }

    // ==================== 符号管理 ====================

    override suspend fun addSymbolsFile(symbols: File, module: File?) {
        val command = buildSymbolCommand(symbols, module)
        executeConsoleCommand(command)
    }

    override suspend fun cancelSymbolsDownload(details: String) {
        messageBus.send(ProtobufMessageFactory.cancelSymbolsDownload(details))
    }

    // ==================== 控制台命令 ====================

    override suspend fun executeInterpreterCommand(
        threadId: Long,
        frameIndex: Int,
        command: String
    ): String {
        val response = messageBus.request(
            ProtobufMessageFactory.handleConsoleCommand(threadId, frameIndex, command),
            ProtocolResponses.HandleConsoleCommandResponse::class.java
        )

        response.status.ensureSuccess()
        return response.standardOutput
    }

    override suspend fun getPromptText(): String = "lldb"

    override fun isInPromptMode(): Boolean =
        stateProvider() == TargetState.SUSPENDED

    override suspend fun completeConsoleCommand(command: String, pos: Int): List<String> {
        val response = messageBus.request(
            ProtobufMessageFactory.handleCompletion(command, pos),
            ProtocolResponses.HandleCompletionResponse::class.java
        )
        return response.completionsList
    }

    override suspend fun resize(columns: Int, rows: Int) {
        messageBus.send(ProtobufMessageFactory.resizeConsole(columns, rows))
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
        return LaunchInferior(targetCommandLine, installer, configuration, messageBus, ioResources)
    }

    private suspend fun createTarget(executablePath: String, architecture: String?) {
        val request = ProtobufMessageFactory.createTarget(
            executablePath,
            architecture.orEmpty()
        )
        sendCreateTargetRequest(request)
        configureTarget()
    }

    private suspend fun createEmptyTarget() {
        sendCreateTargetRequest(ProtobufMessageFactory.createTarget("", ""))
        configureTarget()
    }

    private suspend fun createRemoteTarget(
        installer: Installer,
        architecture: String?,
        platform: String,
        targetCommandLine: GeneralCommandLine
    ) {
        val request = ProtobufMessageFactory.createRemoteTarget(
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
        val response = messageBus.request(
            ProtobufMessageFactory.loadCoreDump(corePath),
            ProtocolResponses.LoadCoreResponse::class.java
        )

        if (!response.status.success) {
            throw ExecutionException(DebuggerBundle.message("error.cannot.load.core.dump"))
        }
    }

    private suspend fun connectToRemotePlatform(platform: String, url: String) {
        val response = messageBus.request(
            ProtobufMessageFactory.connectPlatform(platform, url),
            ProtocolResponses.ConnectPlatformResponse::class.java
        )

        if (!response.status.success) {
            throw ExecutionException(DebuggerBundle.message("error.cannot.connect.remote"))
        }
    }

    private suspend fun sendCreateTargetRequest(request: Protocol.CompositeRequest) {
        val response = messageBus.request(
            request,
            ProtocolResponses.CreateTargetResponse::class.java
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
        val response = messageBus.request(
            ProtobufMessageFactory.handleConsoleCommand(-1L, -1, command),
            ProtocolResponses.HandleConsoleCommandResponse::class.java
        )

        if (!response.status.success) {
            throw DebuggerCommandException(response.status.errorMessage)
        }
    }

    private fun ProtocolResponses.ResponseStatus.ensureSuccess() {
        if (!success) {
            throw DebuggerCommandException(errorMessage)
        }
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
class IOResourceManager : AutoCloseable {
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

    fun setupWindowsPipe(name: String): WinPipe {
        cleanup()
        return WinPipe.createOutboundPipe(name).also {
            input = it.outputStream
        }
    }

    fun setupOutputReader(
        host: HostMachine,
        commandLine: GeneralCommandLine,
        usePty: Boolean,
        onOutput: (String, Key<*>) -> Unit
    ): ProcessOutputReaders {
        val presentableName = commandLine.commandLineString
            .split(" ")
            .firstOrNull { it.isNotEmpty() }
            ?: "Debug Process"

        return object : ProcessOutputReaders(
            host,
            presentableName,
            commandLine.charset,
            usePty,
            usePty
        ) {
            override fun onTextAvailable(text: @NlsSafe String, key: Key<*>) {
                onOutput(text, key)
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
    private val config: DebuggerDriverConfiguration,
    private val messageBus: MessageBus,
    private val ioResources: IOResourceManager
) : SessionService.Inferior {

    override fun getId(): Int = -1

    override suspend fun start(): Long {
        val streamConfig = StreamConfiguration.from(commandLine, config)
        val streams = streamConfig.setup(ioResources)

        val response = messageBus.request(
            ProtobufMessageFactory.launch(
                commandLine,
                streamConfig.useExternalConsole,
                config.emulateTerminal,
                streams.stdin,
                streams.stdout,
                streams.stderr
            ),
            ProtocolResponses.LaunchResponse::class.java
        )

        if (!response.status.success) {
            throw ExecutionException(DebuggerBundle.message("error.cannot.launch"))
        }

        return response.processId
    }

    override suspend fun detach() {
        // 实现通过外部引用实现
    }

    override suspend fun destroy(): Boolean {
        ioResources.close()
        return true
    }
}

/**
 * 附加类型的调试目标
 */
private class AttachInferior(
    private val processId: Int,
    private val config: DebuggerDriverConfiguration,
    private val messageBus: MessageBus,
    private val sessionService: SessionServiceImpl
) : SessionService.Inferior {

    override fun getId(): Int = processId

    override suspend fun start(): Long {
        val response = messageBus.request(
            ProtobufMessageFactory.attach(processId, config.isContinueAfterAttachNeeded),
            ProtocolResponses.AttachResponse::class.java
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
        sessionService.kill()
        return true
    }
}

/**
 * Core Dump 类型的调试目标
 */
private object CoreDumpInferior : SessionService.Inferior {
    override fun getId(): Int = -1
    override suspend fun start(): Long = -1
    override suspend fun detach() {}
    override suspend fun destroy(): Boolean = true
}

// ==================== 流配置 ====================

/**
 * 流配置信息
 */
private data class StreamConfiguration(
    val useExternalConsole: Boolean,
    val emulateTerminal: Boolean,
    val inputFile: File?
) {
    companion object {
        fun from(commandLine: GeneralCommandLine, config: DebuggerDriverConfiguration) =
            StreamConfiguration(
                useExternalConsole = commandLine.getUserData(SessionServiceImpl.USE_EXTERNAL_CONSOLE_KEY) == true,
                emulateTerminal = config.emulateTerminal,
                inputFile = commandLine.inputFile
            )
    }

    fun setup(ioResources: IOResourceManager): StreamPaths {
        return when {
            inputFile != null -> setupWithInputFile(ioResources)
            !useExternalConsole -> setupInteractiveStreams(ioResources)
            else -> StreamPaths()
        }
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
            StreamPaths(stdin = pipe.name)
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