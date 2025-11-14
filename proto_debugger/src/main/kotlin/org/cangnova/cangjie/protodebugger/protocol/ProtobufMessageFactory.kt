package org.cangnova.cangjie.protodebugger.protocol

import com.google.protobuf.ByteString
import com.intellij.execution.configurations.GeneralCommandLine
import proto.Model
import proto.Model.ConsoleMode.*
import proto.Protocol.*
import java.io.File

/**
 * Protobuf消息工厂 - 精简版
 *
 * 提供调试器协议消息的统一创建接口
 * 使用 Kotlin DSL 风格简化消息构建
 */
object ProtobufMessageFactory {

    // ==================== 进程生命周期 ====================

    fun attach(pid: Int, continueAfter: Boolean = true) = composite {
        attach = attachRequest(pid, continueAfter)
    }

    fun remoteAttach(pid: Int, socket: String, continueAfter: Boolean = true) = composite {
        attach = attachRequest(pid, continueAfter) { setFdPassingSocket(socket) }
    }

    fun attachByName(name: String, waitForLaunch: Boolean = false, continueAfter: Boolean = true) = composite {
        attachByName = AttachByNameRequest.newBuilder()
            .setProcessName(name)
            .setWaitForLaunch(waitForLaunch)
            .setContinueAfterAttach(continueAfter)
            .build()
    }

    fun launch(
        cmdLine: GeneralCommandLine,
        useExternalConsole: Boolean = false,
        usePseudoConsole: Boolean = false,
        stdinPath: String? = null,
        stdoutPath: String? = null,
        stderrPath: String? = null
    ) = composite {
        launch = LaunchRequest.newBuilder()
            .setLaunchInfo(processLaunchInfo(cmdLine.exePath, cmdLine, stdinPath, stdoutPath, stderrPath))
            .setConsoleMode(consoleMode(usePseudoConsole, useExternalConsole))
            .build()
    }

    fun remoteLaunch(localPath: String, cmdLine: GeneralCommandLine, socket: String) = composite {
        remoteLaunch = RemoteLaunchRequest.newBuilder()
            .setLaunchInfo(processLaunchInfo(localPath, cmdLine))
            .setFdPassingSocket(socket)
            .build()
    }

    fun detach() = composite { detach = DetachRequest.getDefaultInstance() }
    fun kill() = composite { kill = KillRequest.getDefaultInstance() }
    fun exit() = composite { exit = ExitRequest.getDefaultInstance() }

    // ==================== 目标和平台 ====================

    fun createTarget(exePath: String, arch: String? = null) = composite {
        createTarget = CreateTargetRequest.newBuilder()
            .setExecutablePath(exePath)
            .applyIfNotNull(arch) { setArchitecture(it) }
            .build()
    }

    fun createRemoteTarget(
        exePath: String,
        platform: String,
        remoteExePath: String,
        arch: String? = null,
        sdkRoot: String? = null
    ) = composite {
        createTarget = CreateTargetRequest.newBuilder()
            .setExecutablePath(exePath)
            .setPlatform(platform)
            .setRemoteExecutablePath(remoteExePath)
            .applyIfNotNull(arch) { setArchitecture(it) }
            .applyIfNotNull(sdkRoot) { setPlatformSdkRoot(it) }
            .build()
    }

    fun connectPlatform(platform: String, url: String) = composite {
        connectPlatform = ConnectPlatformRequest.newBuilder()
            .setPlatform(platform)
            .setUrl(url)
            .build()
    }

    fun connectProcess(url: String, plugin: String? = null, continueAfter: Boolean = false) = composite {
        connectProcess = ConnectProcessRequest.newBuilder()
            .setUrl(url)
            .setContinueAfterConnect(continueAfter)
            .applyIfNotNull(plugin) { setPlugin(it) }
            .build()
    }

    fun loadCoreDump(coreDumpPath: String) = composite {
        loadCore = LoadCoreRequest.newBuilder().setCoreDumpPath(coreDumpPath).build()
    }

    // ==================== 断点管理 ====================

    fun addBreakpoint(
        path: String,
        line: Int,
        ignoreSourceHash: Boolean = false,
        condition: String? = null
    ) = composite {
        addBreakpoint = breakpointRequest {
            setFilePath(path)
            setLine(line)
            setIgnoreSourceHash(ignoreSourceHash)
            applyIfNotNull(condition) { setCondition(it) }
        }
    }

    fun addBreakpoint(address: Long, condition: String? = null) = composite {
        addBreakpoint = breakpointRequest {
            setAddress(address)
            applyIfNotNull(condition) { setCondition(it) }
        }
    }

    fun addBreakpoint(
        pattern: String,
        isRegex: Boolean = false,
        module: String? = null,
        condition: String? = null,
        threadId: Long = 0
    ) = composite {
        addBreakpoint = breakpointRequest {
            setSymbolPattern(pattern)
            setIsRegex(isRegex)
            setThreadId(threadId.toInt())
            applyIfNotNull(module) { setModuleName(it) }
            applyIfNotNull(condition) { setCondition(it) }
        }
    }

    fun removeBreakpoint(id: Int) = composite {
        removeBreakpoint = RemoveBreakpointRequest.newBuilder().setBreakpointId(id).build()
    }

    // ==================== 观察点管理 ====================

    fun addWatchpoint(
        valueId: Int,
        read: Boolean = true,
        write: Boolean = true,
        resolveLocation: Boolean = false,
        condition: String? = null
    ) = composite {
        addWatchpoint = AddWatchpointRequest.newBuilder()
            .setValueId(valueId)
            .setWatchRead(read)
            .setWatchWrite(write)
            .setResolveLocation(resolveLocation)
            .applyIfNotNull(condition) { setCondition(it) }
            .build()
    }

    fun removeWatchpoint(id: Int) = composite {
        removeWatchpoint = RemoveWatchpointRequest.newBuilder().setWatchpointId(id).build()
    }

    // ==================== 执行控制 ====================

    fun resume() = composite { `continue` = ContinueRequest.getDefaultInstance() }
    fun suspend() = composite { suspend = SuspendRequest.getDefaultInstance() }

    fun stepInto(threadId: Long, byInstruction: Boolean = false) = composite {
        stepInto = StepIntoRequest.newBuilder()
            .setThreadId(threadId.toInt())
            .setStepByInstruction(byInstruction)
            .build()
    }


    fun stepOver(threadId: Long, byInstruction: Boolean = false): CompositeRequest = composite {
        stepOver = StepOverRequest.newBuilder()
            .setThreadId(threadId.toInt())
            .setStepByInstruction(byInstruction)
            .build()
    }

    fun stepOut(threadId: Long) = composite {
        stepOut = StepOutRequest.newBuilder().setThreadId(threadId.toInt()).build()
    }

    fun stepScripted(threadId: Long, className: String) = composite {
        stepScripted = StepScriptedRequest.newBuilder()
            .setThreadId(threadId.toInt())
            .setStepClassName(className)
            .build()
    }

    fun jumpToLine(
        threadId: Long,
        filePath: String,
        line: Int,
        allowLeavingFunction: Boolean = false
    ) = composite {
        jumpToLine = JumpToLineRequest.newBuilder()
            .setThreadId(threadId.toInt())
            .setFilePath(filePath)
            .setLine(line)
            .setAllowLeavingFunction(allowLeavingFunction)
            .build()
    }

    fun jumpToAddress(
        threadId: Long,
        address: Long,
        allowLeavingFunction: Boolean = false
    ) = composite {
        jumpToAddress = JumpToAddressRequest.newBuilder()
            .setThreadId(threadId.toInt())
            .setAddress(address)
            .setAllowLeavingFunction(allowLeavingFunction)
            .build()
    }

    // ==================== 线程和帧 ====================

    fun getThreads() = composite { getThreads = GetThreadsRequest.getDefaultInstance() }

    fun freezeThread(threadId: Long) = composite {
        freezeThread = FreezeThreadRequest.newBuilder().setThreadId(threadId.toInt()).build()
    }

    fun unfreezeThread(threadId: Long) = composite {
        unfreezeThread = UnfreezeThreadRequest.newBuilder().setThreadId(threadId.toInt()).build()
    }

    fun getFrames(
        threadId: Long,
        startIndex: Int,
        count: Int,
        untilValidSourceLine: Boolean = false
    ) = composite {
        getFrames = GetFramesRequest.newBuilder()
            .setThreadId(threadId.toInt())
            .setStartIndex(startIndex)
            .setCount(count)
            .setUntilValidSourceLine(untilValidSourceLine)
            .build()
    }

    // ==================== 变量和值 ====================

    fun getVars(
        threadId: Long,
        frameIndex: Int,
        includeStatics: Boolean = false,
        includeGlobals: Boolean = false
    ) = composite {
        getVariables = GetVariablesRequest.newBuilder()
            .setThreadId(threadId.toInt())
            .setFrameIndex(frameIndex)
            .setIncludeStatics(includeStatics)
            .setIncludeGlobals(includeGlobals)
            .build()
    }

    fun evaluateExpression(threadId: Long, frameIndex: Int, expression: String) = composite {
        evaluateExpression = EvaluateExpressionRequest.newBuilder()
            .setThreadId(threadId.toInt())
            .setFrameIndex(frameIndex)
            .setExpression(expression)
            .build()
    }

    fun getValueChildren(valueId: Int, offset: Int = 0, count: Int = Int.MAX_VALUE) = composite {
        getValueChildren = GetValueChildrenRequest.newBuilder()
            .setValueId(valueId)
            .setOffset(offset)
            .setCount(count)
            .build()
    }

    fun getChildrenCount(valueId: Int) = composite {
        getChildrenCount = GetChildrenCountRequest.newBuilder().setValueId(valueId).build()
    }

    fun getValueData(valueId: Int, maxDescriptionLength: Int = 1000) = composite {
        getValueData = GetValueDataRequest.newBuilder()
            .setValueId(valueId)
            .setMaxDescriptionLength(maxDescriptionLength)
            .build()
    }

    fun getValueDescription(valueId: Int, maxLength: Int = 1000) = composite {
        getValueDescription = GetValueDescriptionRequest.newBuilder()
            .setValueId(valueId)
            .setMaxLength(maxLength)
            .build()
    }

    fun getValueAddress(valueId: Int) = composite {
        getValueAddress = GetValueAddressRequest.newBuilder().setValueId(valueId).build()
    }

    fun arraySlice(valueId: Int, offset: Int, count: Int) = composite {
        getArraySlice = GetArraySliceRequest.newBuilder()
            .setValueId(valueId)
            .setOffset(offset)
            .setCount(count)
            .build()
    }

    fun setValuesFilteringEnabled(enabled: Boolean) = composite {
        setValueFilteringPolicy = SetValueFilteringPolicyRequest.newBuilder()
            .setFilterEnabled(enabled)
            .build()
    }

    // ==================== 内存操作 ====================

    fun dumpMemory(startAddress: Long, endAddress: Long) = composite {
        dumpMemory = DumpMemoryRequest.newBuilder()
            .setStartAddress(startAddress)
            .setEndAddress(endAddress)
            .build()
    }

    fun writeMemory(address: Long, bytes: ByteArray) = composite {
        writeMemory = WriteMemoryRequest.newBuilder()
            .setAddress(address)
            .setData(ByteString.copyFrom(bytes))
            .build()
    }

    fun disassemble(startAddress: Long, endAddress: Long) = composite {
        disassemble = DisassembleRequest.newBuilder()
            .setStartAddress(startAddress)
            .setEndAddress(endAddress)
            .build()
    }

    fun disassembleUntilPivot(startAddress: Long, pivotAddress: Long, pivotInstrSize: Int) = composite {
        disassembleUntilPivot = DisassembleUntilPivotRequest.newBuilder()
            .setStartAddress(startAddress)
            .setPivotAddress(pivotAddress)
            .setPivotInstructionSize(pivotInstrSize)
            .build()
    }

    fun dumpSections() = composite { dumpSections = DumpSectionsRequest.getDefaultInstance() }

    fun contextInfo(address: Long, rangeStart: Long, rangeEnd: Long) = composite {
        getContextInfo = GetContextInfoRequest.newBuilder()
            .setAddress(address)
            .setRangeStart(rangeStart)
            .setRangeEnd(rangeEnd)
            .build()
    }

    // ==================== 寄存器 ====================

    fun getRegisters(threadId: Long, frameIndex: Int, regNames: Set<String> = emptySet()) = composite {
        getRegisters = GetRegistersRequest.newBuilder()
            .setThreadId(threadId.toInt())
            .setFrameIndex(frameIndex)
            .addAllRegisterNames(regNames)
            .build()
    }

    fun getArch() = composite { getArchitecture = GetArchitectureRequest.getDefaultInstance() }

    fun getRegisterSets() = composite { getRegisterSets = GetRegisterSetsRequest.getDefaultInstance() }

    // ==================== 控制台和命令 ====================

    fun handleConsoleCommand(threadId: Long, frameIndex: Int, command: String) = composite {
        handleConsoleCommand = HandleConsoleCommandRequest.newBuilder()
            .setThreadId(threadId.toInt())
            .setFrameIndex(frameIndex)
            .setCommand(command)
            .build()
    }

    fun dispatchInput(input: String, target: Model.DispatchTarget) = composite {
        dispatchInput = DispatchInputRequest.newBuilder()
            .setInput(input)
            .setTarget(target)
            .build()
    }

    fun handleCompletion(command: String, cursorPos: Int) = composite {
        handleCompletion = HandleCompletionRequest.newBuilder()
            .setCommand(command)
            .setCursorPosition(cursorPos)
            .build()
    }

    fun resizeConsole(columns: Int, rows: Int) = composite {
        resizeConsole = ResizeConsoleRequest.newBuilder()
            .setColumns(columns)
            .setRows(rows)
            .build()
    }

    fun executeShellCommand(command: String, workingDir: String? = null, timeoutSecs: Int = 30) = composite {
        executeShellCommand = ExecuteShellCommandRequest.newBuilder()
            .setCommand(command)
            .setTimeoutSeconds(timeoutSecs)
            .applyIfNotNull(workingDir) { setWorkingDirectory(it) }
            .build()
    }

    // ==================== 信号和符号 ====================

    fun handleSignal(signalName: String, stop: Boolean, pass: Boolean, notify: Boolean) = composite {
        handleSignal = HandleSignalRequest.newBuilder()
            .setSignalName(signalName)
            .setShouldStop(stop)
            .setShouldPass(pass)
            .setShouldNotify(notify)
            .build()
    }

    fun cancelSymbolsDownload(details: String) = composite {
        cancelSymbolsDownload = CancelSymbolsDownloadRequest.newBuilder()
            .setDetails(details)
            .build()
    }

    // ==================== 内部辅助函数 ====================

    private inline fun composite(block: CompositeRequest.Builder.() -> Unit) =
        CompositeRequest.newBuilder().apply(block).build()

    private inline fun <T, B> B.applyIfNotNull(value: T?, block: B.(T) -> Unit): B = apply {
        value?.let { block(it) }
    }

    private fun consoleMode(usePseudo: Boolean, useExternal: Boolean) = when {
        usePseudo -> CONSOLE_MODE_PSEUDO
        useExternal -> CONSOLE_MODE_EXTERNAL
        else -> CONSOLE_MODE_PARENT
    }

    private inline fun attachRequest(
        pid: Int,
        continueAfter: Boolean,
        extraConfig: AttachRequest.Builder.() -> Unit = {}
    ) = AttachRequest.newBuilder()
        .setProcessId(pid)
        .setContinueAfterAttach(continueAfter)
        .apply(extraConfig)
        .build()

    private inline fun breakpointRequest(block: AddBreakpointRequest.Builder.() -> Unit) =
        AddBreakpointRequest.newBuilder().apply(block).build()

    private fun <T : com.google.protobuf.GeneratedMessageV3.Builder<T>> stepRequest(
        threadId: Long,
        byInstruction: Boolean,
        builder: T
    ): com.google.protobuf.Message {
        return builder.apply {
            javaClass.getMethod("setThreadId", Int::class.java).invoke(this, threadId.toInt())
            javaClass.getMethod("setStepByInstruction", Boolean::class.java).invoke(this, byInstruction)
        }.build()
    }

    private fun processLaunchInfo(
        execPath: String,
        cmdLine: GeneralCommandLine,
        stdinPath: String? = null,
        stdoutPath: String? = null,
        stderrPath: String? = null
    ) = Model.ProcessLaunchInfo.newBuilder()
        .setExecutablePath(execPath)
        .setWorkingDirectory((cmdLine.workDirectory ?: File(execPath).parentFile).absolutePath)
        .addAllEnvironment(cmdLine.effectiveEnvironment.map { (name, value) ->
            Model.EnvironmentVariable.newBuilder().setName(name).setValue(value).build()
        })
        .addAllArguments(cmdLine.parametersList.array.toList())
        .applyIfNotNull(stdinPath) { setStdinPath(it) }
        .applyIfNotNull(stdoutPath) { setStdoutPath(it) }
        .applyIfNotNull(stderrPath) { setStderrPath(it) }
        .build()
}