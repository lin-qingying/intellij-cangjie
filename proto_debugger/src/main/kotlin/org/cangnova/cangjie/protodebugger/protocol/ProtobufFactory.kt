package org.cangnova.cangjie.protodebugger.protocol

import com.intellij.execution.configurations.GeneralCommandLine
import java.util.concurrent.atomic.AtomicLong

import lldbprotobuf.Model.*
import lldbprotobuf.RequestOuterClass.*
import java.io.File


object ProtobufFactory {

    // 用于生成唯一请求哈希的原子计数器
    private val hashGenerator = AtomicLong(1)

    // ==================== 进程生命周期管理 ====================

    /**
     * 附加到正在运行的进程
     *
     * @param pid 目标进程 ID
     * @param options 可扩展选项（例如 {"wait_for":"attach"}）
     * @return 附加请求消息
     */
    fun attach(pid: Long, options: Map<String, String> = emptyMap()): Request =
        buildComposite {
            attach = AttachRequest.newBuilder()
                .setProcessId(buildId(pid))
                .setPid(pid)
                .putAllOptions(options)
                .build()
        }


    /**
     * 启动新进程
     *
     * @param cmdLine 命令行配置（包含可执行文件路径、参数、环境变量等）
     * @param useExternalConsole 是否使用外部控制台窗口
     * @param usePseudoConsole 是否使用伪终端（适用于需要交互的程序）
     * @param stdinPath 标准输入重定向文件路径
     * @param stdoutPath 标准输出重定向文件路径
     * @param stderrPath 标准错误重定向文件路径
     * @return 启动请求消息
     */
    fun launch(
        cmdLine: GeneralCommandLine,
        useExternalConsole: Boolean = false,
        usePseudoConsole: Boolean = false,
        stdinPath: String? = null,
        stdoutPath: String? = null,
        stderrPath: String? = null
    ): Request = buildComposite {
        launch = LaunchRequest.newBuilder()
            .setLaunchInfo(buildProcessLaunchInfo(cmdLine.exePath, cmdLine, stdinPath, stdoutPath, stderrPath))
            .setConsoleMode(determineConsoleMode(usePseudoConsole, useExternalConsole))
            .build()
    }

//    /**
//     * 启动远程进程
//     *
//     * @param localPath 本地可执行文件路径（用于符号解析）
//     * @param cmdLine 命令行配置
//     * @param socket Unix socket 路径
//     * @return 远程启动请求消息
//     */
//    fun remoteLaunch(
//        localPath: String,
//        cmdLine: GeneralCommandLine,
//        socket: String
//    ): Request = buildComposite {
//        remoteLaunch = RemoteLaunchRequest.newBuilder()
//            .setLaunchInfo(buildLaunchInfo(localPath, cmdLine))
//            .setFdPassingSocket(socket)
//            .build()
//    }

    /**
     * 分离调试器（进程继续运行）
     */
    fun detach(): Request = buildComposite {
        detach = DetachRequest.getDefaultInstance()
    }

    /**
     * 终止被调试进程
     */
    fun terminate(): Request = buildComposite {
        terminate = TerminateRequest.getDefaultInstance()
    }

    /**
     * 退出调试器
     */
    fun exit(): Request = buildComposite {
        exit = ExitRequest.getDefaultInstance()
    }

    // ==================== 目标和平台配置 ====================

    /**
     * 创建调试目标
     *
     * @param exePath 可执行文件路径
     * @param arch 目标架构（如 "x86_64", "arm64"），null 则自动检测
     * @return 创建目标请求消息
     */
    fun createTarget(exePath: String, arch: String? = null): Request =
        buildComposite {
            createTarget = CreateTargetRequest.newBuilder()
                .setFilePath(exePath)

                .build()
        }

    /**
     * 创建远程调试目标
     *
     * @param exePath 本地可执行文件路径
     * @param platform 远程平台名称（如 "remote-linux"）
     * @param remoteExePath 远程可执行文件路径
     * @param arch 目标架构
     * @param sdkRoot 平台 SDK 根目录
     * @return 创建远程目标请求消息
     */
    fun createRemoteTarget(
        exePath: String,
        platform: String,
        remoteExePath: String,
        arch: String? = null,
        sdkRoot: String? = null
    ): Request = buildComposite {
//        createTarget = CreateTargetRequest.newBuilder()
//            .setExecutablePath(exePath)
//            .setPlatform(platform)
//            .setRemoteExecutablePath(remoteExePath)
//            .applyIfNotNull(arch) { setArchitecture(it) }
//            .applyIfNotNull(sdkRoot) { setPlatformSdkRoot(it) }
//            .build()
    }

    /**
     * 连接到远程平台
     *
     * @param platform 平台名称
     * @param url 连接 URL（格式依赖于平台类型）
     * @return 连接平台请求消息
     */
    fun connectPlatform(platform: String, url: String): Request =
        buildComposite {
//            connectPlatform = ConnectPlatformRequest.newBuilder()
//                .setPlatform(platform)
//                .setUrl(url)
//                .build()
        }

    /**
     * 连接到正在运行的调试服务器
     *
     * @param url 服务器 URL（如 "localhost:1234" 或 "gdbserver://host:port"）
     * @param plugin 调试服务器插件类型（如 "gdb-remote"）
     * @param continueAfter 连接后是否继续执行
     * @return 连接进程请求消息
     */
    fun connectProcess(
        url: String,
        plugin: String? = null,
        continueAfter: Boolean = false
    ): Request = buildComposite {
//        connectProcess = ConnectProcessRequest.newBuilder()
//            .setUrl(url)
//            .setContinueAfterConnect(continueAfter)
//            .applyIfNotNull(plugin) { setPlugin(it) }
//            .build()
    }

    /**
     * 加载核心转储文件进行事后调试
     *
     * @param coreDumpPath 核心转储文件路径
     * @return 加载核心转储请求消息
     */
    fun loadCoreDump(coreDumpPath: String): Request =
        buildComposite {
//            loadCore = LoadCoreRequest.newBuilder()
//                .setCoreDumpPath(coreDumpPath)
//                .build()
        }

    // ==================== 断点管理 ====================
    fun buildId(id: Long): Id {
        return Id.newBuilder().setId(id).build()
    }


    private fun addBreakpoint(
        thread_id: Long = 0,

        lineBreakpoint: LineBreakpoint? = null,
        watchBreakpoint: WatchBreakpoint? = null,
        symbolBreakpoint: SymbolBreakpoint? = null,
        addressBreakpoint: AddressBreakpoint? = null,
        functionBreakpoint: FunctionBreakpoint? = null,


        condition: String? = null,
        enabled: Boolean = true,

        ): Request = buildComposite {
        addBreakpoint = buildBreakpointRequest {
            threadId = buildId(thread_id)
            this.enabled = enabled
            this.condition = condition ?: ""
            lineBreakpoint?.let {
                line = it
            }
            watchBreakpoint?.let {
                watchpoint = it
            }
            symbolBreakpoint?.let {
                symbol = it
            }
            addressBreakpoint?.let {
                address = it
            }
            functionBreakpoint?.let {
                function = it
            }
        }

    }

    /**
     * 在源代码行添加断点
     *
     * @param path 源文件路径（相对于项目根目录或绝对路径）
     * @param line 行号（从 1 开始）
     * @param ignoreSourceHash 是否忽略源代码哈希校验（源码修改后仍可命中）
     * @param condition 条件表达式（表达式为 true 时断点才会触发）
     * @return 添加断点请求消息
     */


    fun addLineBreakpoint(
        path: String,
        line: Int,
        thread_id: Long = 0,
        ignoreSourceHash: Boolean = false,
        condition: String? = null
    ): Request {
        return addBreakpoint(
            thread_id = thread_id,
            condition = condition,
            lineBreakpoint = LineBreakpoint.newBuilder()
                .setFile(path)
                .setLine(line)
                .setIgnoreSourceHash(ignoreSourceHash)
                .build()
        )
    }

    /**
     * 在指定内存地址添加断点
     *
     * @param address 内存地址
     * @param condition 条件表达式
     * @return 添加断点请求消息
     */
    fun addAddressBreakpoint(address: Long, condition: String? = null): Request =
        addBreakpoint(
            addressBreakpoint = AddressBreakpoint.newBuilder()
                .setAddress(address)
                .build(),
            condition = condition
        )

    /**
     * 通过符号名称添加断点
     *
     * @param pattern 符号名称或模式（支持通配符或正则表达式）
     * @param isRegex 是否使用正则表达式匹配
     * @param module 限定模块名称（null 表示搜索所有模块）
     * @param condition 条件表达式
     * @param threadId 限定线程 ID（0 表示所有线程）
     * @return 添加断点请求消息
     */
    fun addSymbolBreakpoint(
        pattern: String,
        isRegex: Boolean = false,
        module: String? = null,
        condition: String? = null,
        threadId: Long = 0
    ): Request =

        addBreakpoint(
            thread_id = threadId,
            condition = condition,
            symbolBreakpoint = SymbolBreakpoint.newBuilder()
                .setPattern(pattern)
                .setIsRegex(isRegex)
                .applyIfNotNull(module) { setModule(it) }
                .build()
        )


    // ==================== 观察点管理 ====================

    /**
     * 添加内存观察点（数据断点）
     *
     * @param valueId 要观察的变量值 ID
     * @param read 是否在读取时触发
     * @param write 是否在写入时触发
     * @param resolveLocation 是否解析内存位置到源代码
     * @param condition 条件表达式
     * @return 添加观察点请求消息
     */
    fun addWatchpoint(
        valueId: Long,
        read: Boolean = true,
        write: Boolean = true,
        resolveLocation: Boolean = false,
        condition: String? = null
    ): Request = addBreakpoint(
        watchBreakpoint = WatchBreakpoint.newBuilder()
            .setValueId(buildId(valueId))
            .setWatchRead(read)
            .setWatchWrite(write)
            .setResolveLocation(resolveLocation)
            .build(),
        condition = condition
    )

    fun removeBreakpoint(id: Long): Request =
        buildComposite {
            removeBreakpoint = RemoveBreakpointRequest.newBuilder()
                .setBreakpointId(buildId(id))
                .build()
        }

    /**
     * 移除观察点
     *
     * @param id 观察点 ID
     * @return 移除观察点请求消息
     */
    fun removeWatchpoint(id: Long): Request = removeBreakpoint(id)

    // ==================== 执行控制 ====================

    /**
     * 恢复程序执行（继续运行）
     */
    fun resume(): Request = buildComposite {
        `continue` = ContinueRequest.getDefaultInstance()
    }

    /**
     * 暂停程序执行
     */
    fun suspend(): Request = buildComposite {
        suspend = SuspendRequest.getDefaultInstance()
    }

    /**
     * 单步进入（Step Into）
     *
     * 执行一行代码，如果遇到函数调用则进入函数内部
     *
     * @param threadId 目标线程 ID
     * @param byInstruction 是否按机器指令单步（false 表示按源代码行）
     * @return 单步进入请求消息
     */
    fun step(threadId: Long, byInstruction: Boolean = false): Request =
        buildComposite {
            stepInto = StepIntoRequest.newBuilder()
                .setThreadId(buildId(threadId))
                .setStepByInstruction(byInstruction)
                .build()
        }

    /**
     * 单步跳过（Step Over）
     *
     * 执行一行代码，遇到函数调用时不进入函数内部
     *
     * @param threadId 目标线程 ID
     * @param byInstruction 是否按机器指令单步
     * @return 单步跳过请求消息
     */
    fun stepOver(threadId: Long, byInstruction: Boolean = false): Request =
        buildComposite {
            stepOver = StepOverRequest.newBuilder()
                .setThreadId(buildId(threadId))
                .setStepByInstruction(byInstruction)
                .build()
        }

    /**
     * 单步跳出（Step Out）
     *
     * 执行当前函数的剩余部分，直到返回到调用者
     *
     * @param threadId 目标线程 ID
     * @return 单步跳出请求消息
     */
    fun stepOut(threadId: Long): Request =
        buildComposite {
            stepOut = StepOutRequest.newBuilder()
                .setThreadId(buildId(threadId))
                .build()
        }

    /**
     * 运行到指定地址
     *
     * 使用 SBThread::RunToAddress() 运行到指定内存地址
     *
     * @param threadId 目标线程 ID
     * @param address 目标内存地址
     * @return 运行到地址请求消息
     */
    fun runToAddress(threadId: Long, address: ULong): Request =
        buildComposite {
            runToCursor = RunToCursorRequest.newBuilder()
                .setThreadId(buildId(threadId))
                .setAddress(address.toLong())
                .build()
        }

    /**
     * 运行到指定源代码行
     *
     * 通过临时断点方式运行到指定源代码位置
     *
     * @param threadId 目标线程 ID
     * @param filePath 源文件路径
     * @param line 目标行号（从 1 开始）
     * @return 运行到行请求消息
     */
    fun runToLine(threadId: Long, filePath: String, line: Int): Request =
        buildComposite {
            runToCursor = RunToCursorRequest.newBuilder()
                .setThreadId(buildId(threadId))
                .setSourceLocation(
                    SourceLocation.newBuilder()
                        .setFilePath(filePath)
                        .setLine(line)
                        .build()
                )
                .build()
        }


    /**
     * 跳转到指定源代码行
     *
     * 修改程序计数器，使程序从指定位置继续执行
     *
     * @param threadId 目标线程 ID
     * @param filePath 源文件路径
     * @param line 目标行号
     * @param allowLeavingFunction 是否允许跳出当前函数
     * @return 跳转到行请求消息
     */
    fun jumpToLine(
        threadId: Long,
        filePath: String,
        line: Int,
        allowLeavingFunction: Boolean = false
    ): Request = buildComposite {
//        jumpToLine = JumpToLineRequest.newBuilder()
//            .setThreadId(threadId.toInt())
//            .setFilePath(filePath)
//            .setLine(line)
//            .setAllowLeavingFunction(allowLeavingFunction)
//            .build()
    }

    /**
     * 跳转到指定内存地址
     *
     * @param threadId 目标线程 ID
     * @param address 目标内存地址
     * @param allowLeavingFunction 是否允许跳出当前函数
     * @return 跳转到地址请求消息
     */
    fun jumpToAddress(
        threadId: Long,
        address: Long,
        allowLeavingFunction: Boolean = false
    ): Request = buildComposite {
//        jumpToAddress = JumpToAddressRequest.newBuilder()
//            .setThreadId(threadId.toInt())
//            .setAddress(address)
//            .setAllowLeavingFunction(allowLeavingFunction)
//            .build()
    }

    // ==================== 线程和调用栈管理 ====================

    /**
     * 获取所有线程列表
     */
    fun getThreads(): Request = buildComposite {
        threads = ThreadsRequest.getDefaultInstance()

    }

    /**
     * 冻结线程（暂停调度）
     *
     * @param threadId 目标线程 ID
     * @return 冻结线程请求消息
     */
    fun freezeThread(threadId: Long): Request =
        buildComposite {
//            freezeThread = FreezeThreadRequest.newBuilder()
//                .setThreadId(threadId.toInt())
//                .build()
        }

    /**
     * 解冻线程（恢复调度）
     *
     * @param threadId 目标线程 ID
     * @return 解冻线程请求消息
     */
    fun unfreezeThread(threadId: Long): Request =
        buildComposite {
//            unfreezeThread = UnfreezeThreadRequest.newBuilder()
//                .setThreadId(threadId.toInt())
//                .build()
        }

    /**
     * 获取调用栈帧列表
     *
     * @param threadId 目标线程 ID
     * @param startIndex 起始索引（0 表示最顶层栈帧）
     * @param count 获取数量
     * @param untilValidSourceLine 是否只获取到第一个有效源代码行的栈帧
     * @return 获取栈帧请求消息
     */
    fun getFrames(
        threadId: Long,
        startIndex: Int,
        count: Int,
        untilValidSourceLine: Boolean = false
    ): Request = buildComposite {
        frames = FramesRequest.newBuilder()

            .setThreadId(buildId(threadId))
            .setStartIndex(startIndex)
            .setCount(count)

            .setFirstValidSourceOnly(untilValidSourceLine)

            .build()
    }

    // ==================== 变量和表达式求值 ====================

    /**
     * 获取栈帧中的变量列表
     *
     * @param threadId 目标线程 ID
     * @param frameIndex 栈帧索引（0 表示当前栈帧）
     * @param includeArgument 是否包含参数
     * @param includeStatics 是否包含静态变量
     * @param includeLocals 是否包含本地变量
     * @param inScopeOnly 是否只包含当前作用域内的变量
     * @param includeRuntimeSupportValues 是否包含运行时支持的变量（如 this 指针、返回值等）
     * @param useDynamic 动态值获取选项
     * @param includeRecognizedArguments 是否包含已识别的参数（类型推断的参数）
     * @param includeRegisters 是否获取寄存器
     * @return 获取变量请求消息
     */
    fun getVariables(
        threadId: Long,
        frameIndex: Int,
        includeArgument: Boolean = true,
        includeStatics: Boolean = true,
        includeLocals: Boolean = true,
        inScopeOnly: Boolean = true,
        includeRuntimeSupportValues: Boolean = true,
        useDynamic: DynamicValueType = DynamicValueType.DYNAMIC_VALUE_NONE,
        includeRecognizedArguments: Boolean = true,

    ): Request = buildComposite {
        variables = VariablesRequest.newBuilder()
            .setThreadId(buildId(threadId))
            .setFrameIndex(frameIndex)
            .setIncludeArguments(includeArgument)
            .setIncludeLocals(includeLocals)
            .setIncludeStatics(includeStatics)
            .setInScopeOnly(inScopeOnly)
            .setIncludeRuntimeSupportValues(includeRuntimeSupportValues)
            .setUseDynamic(useDynamic)
            .setIncludeRecognizedArguments(includeRecognizedArguments)
            .build()
    }

    /**
     * 在指定上下文中求值表达式
     *
     * @param threadId 目标线程 ID
     * @param frameIndex 栈帧索引
     * @param expression 要求值的表达式
     * @return 求值表达式请求消息
     */
    fun evaluateExpression(
        threadId: Long,
        frameIndex: Int,
        expression: String
    ): Request = buildComposite {
        evaluate = EvaluateRequest.newBuilder()
            .setThreadId(buildId(threadId))
            .setFrameIndex(frameIndex)
            .setExpression(expression)
            .build()
    }

    /**
     * 获取复合类型值的子元素
     *
     * @param valueId 父值 ID
     * @param offset 起始偏移量
     * @param count 获取数量（Int.MAX_VALUE 表示全部）
     * @return 获取子元素请求消息
     */
    fun getValueChildren(
        valueId: Long,
        offset: Int = 0,
        count: Int = Int.MAX_VALUE
    ): Request = buildComposite {
        getVariablesChildren = VariablesChildrenRequest.newBuilder()
            .setVariableId(buildId(valueId))

            .setOffset(offset)
            .setCount(count)
            .build()
    }

    /**
     * 获取复合类型值的子元素数量
     *
     * @param valueId 值 ID
     * @return 获取子元素数量请求消息
     */
    fun getVariablesChildren(
        variableId: Long,

        offset: Int = 0,
        count: Int = Int.MAX_VALUE
    ): Request = buildComposite {
        getVariablesChildren = VariablesChildrenRequest.newBuilder()
            .setVariableId(buildId(variableId))


            .setOffset(offset)
            .setCount(count)
            .build()
    }

    /**
     * 获取值的详细数据
     *
     * @param valueId 值 ID
     * @param maxDescriptionLength 描述文本最大长度
     * @return 获取值数据请求消息
     */
    fun getValue(
        variableId: Long,

        maxStringLength: Int = 1000
    ): Request = buildComposite {
        getValue = GetValueRequest.newBuilder()
            .setVariableId(buildId(variableId))

            .setMaxStringLength(maxStringLength)
            .build()
    }

    /**
     * 获取值的字符串描述
     *
     * @param valueId 值 ID
     * @param maxLength 描述最大长度
     * @return 获取值描述请求消息
     */
    fun getValueDescription(
        valueId: Int,
        maxLength: Int = 1000
    ): Request = buildComposite {
//        getValueDescription = GetValueDescriptionRequest.newBuilder()
//            .setValueId(valueId)
//            .setMaxLength(maxLength)
//            .build()
    }

    /**
     * 获取值的内存地址
     *
     * @param valueId 值 ID
     * @return 获取值地址请求消息
     */
    fun getValueAddress(valueId: Int): Request =
        buildComposite {
//            getValueAddress = GetValueAddressRequest.newBuilder()
//                .setValueId(valueId)
//                .build()
        }

    /**
     * 获取数组的切片视图
     *
     * @param valueId 数组值 ID
     * @param offset 起始索引
     * @param count 元素数量
     * @return 获取数组切片请求消息
     */
    fun arraySlice(valueId: Int, offset: Int, count: Int): Request =
        buildComposite {
//            getArraySlice = GetArraySliceRequest.newBuilder()
//                .setValueId(valueId)
//                .setOffset(offset)
//                .setCount(count)
//                .build()
        }

    /**
     * 设置变量值
     *
     * @param variableId 变量 ID
     * @param newValue 新的值（字符串形式）
     * @return 设置变量值请求消息
     */
    fun setVariableValue(
        variableId: Long,
        newValue: String
    ): Request = buildComposite {
        setVariableValue = SetVariableValueRequest.newBuilder()
            .setVariableId(buildId(variableId))
            .setValue(newValue)
            .build()
    }

    /**
     * 设置值过滤策略
     *
     * @param enabled 是否启用过滤（过滤掉某些内部或系统变量）
     * @return 设置过滤策略请求消息
     */
    fun setValuesFilteringEnabled(enabled: Boolean): Request =
        buildComposite {
//            setValueFilteringPolicy = SetValueFilteringPolicyRequest.newBuilder()
//                .setFilterEnabled(enabled)
//                .build()
        }

    // ==================== 内存和反汇编操作 ====================

    /**
     * 读取内存区域
     *
     * @param address 起始地址
     * @param size 要读取的字节数
     * @return 读取内存请求消息
     */
    fun readMemory(address: Long, size: Int): Request =
        buildComposite {
            readMemory = ReadMemoryRequest.newBuilder()
                .setAddress(address)
                .setSize(size)
                .build()
        }

    /**
     * 写入内存数据
     *
     * @param address 目标地址
     * @param data 要写入的数据（字符串形式）
     * @return 写入内存请求消息
     */
    fun writeMemory(address: Long, data: String): Request =
        buildComposite {
            writeMemory = WriteMemoryRequest.newBuilder()
                .setAddress(address)
                .setData(data)
                .build()
        }

    /**
     * 反汇编 - 地址范围模式
     *
     * @param startAddress 起始地址
     * @param endAddress 结束地址
     * @param options 反汇编选项
     * @return 反汇编请求消息
     */
    fun disassembleRange(
        startAddress: Long,
        endAddress: Long,
        options: DisassembleOptions? = null
    ): Request =
        buildComposite {
            disassemble = DisassembleRequest.newBuilder()
                .setRange(DisassembleRangeMode.newBuilder()
                    .setStartAddress(startAddress)
                    .setEndAddress(endAddress)
                    .build())
                .applyIfNotNull(options) { setOptions(it) }
                .build()
        }

    /**
     * 反汇编 - 指令数量模式
     *
     * @param startAddress 起始地址
     * @param instructionCount 要反汇编的指令数量
     * @param options 反汇编选项
     * @return 反汇编请求消息
     */
    fun disassembleCount(
        startAddress: Long,
        instructionCount: Int,
        options: DisassembleOptions? = null
    ): Request =
        buildComposite {
            disassemble = DisassembleRequest.newBuilder()
                .setCount(DisassembleCountMode.newBuilder()
                    .setStartAddress(startAddress)
                    .setInstructionCount(instructionCount)
                    .build())
                .applyIfNotNull(options) { setOptions(it) }
                .build()
        }

    /**
     * 反汇编 - 锚点模式
     *
     * @param anchorAddress 锚点地址
     * @param backwardCount 向后反汇编的指令数量
     * @param forwardCount 向前反汇编的指令数量
     * @param options 反汇编选项
     * @return 反汇编请求消息
     */
    fun disassembleAnchor(
        anchorAddress: Long,
        backwardCount: Int,
        forwardCount: Int,
        options: DisassembleOptions? = null
    ): Request =
        buildComposite {
            disassemble = DisassembleRequest.newBuilder()
                .setAnchor(DisassembleAnchorMode.newBuilder()
                    .setAnchorAddress(anchorAddress)
                    .setBackwardCount(backwardCount)
                    .setForwardCount(forwardCount)
                    .build())
                .applyIfNotNull(options) { setOptions(it) }
                .build()
        }

    /**
     * 反汇编 - 反汇编到锚点模式
     *
     * @param startAddress 起始地址
     * @param pivotAddress 锚点地址
     * @param includePivot 是否包含锚点处的指令
     * @param options 反汇编选项
     * @return 反汇编请求消息
     */
    fun disassembleUntilPivot(
        startAddress: Long,
        pivotAddress: Long,
        includePivot: Boolean = false,
        options: DisassembleOptions? = null
    ): Request =
        buildComposite {
            disassemble = DisassembleRequest.newBuilder()
                .setUntilPivot(DisassembleUntilPivotMode.newBuilder()
                    .setStartAddress(startAddress)
                    .setPivotAddress(pivotAddress)
                    .setIncludePivot(includePivot)
                    .build())
                .applyIfNotNull(options) { setOptions(it) }
                .build()
        }

    /**
     * 获取函数信息 - 按地址查询
     *
     * @param address 函数地址
     * @param moduleName 可选：限定模块名
     * @param threadId 可选：线程上下文
     * @return 获取函数信息请求消息
     */
    fun getFunctionInfoByAddress(
        address: Long,
        moduleName: String? = null,
    ): Request =
        buildComposite {
            getFunctionInfo = GetFunctionInfoRequest.newBuilder()
                .setAddress(address)
                .applyIfNotNull(moduleName) { setModuleName(it) }
                .build()
        }

    /**
     * 获取函数信息 - 按名称查询
     *
     * @param name 函数名称或修饰名
     * @param moduleName 可选：限定模块名
     * @param threadId 可选：线程上下文
     * @return 获取函数信息请求消息
     */
    fun getFunctionInfoByName(
        name: String,
        moduleName: String? = null,
    ): Request =
        buildComposite {
            getFunctionInfo = GetFunctionInfoRequest.newBuilder()
                .setName(name)
                .applyIfNotNull(moduleName) { setModuleName(it) }
                .build()
        }




    // ==================== 寄存器操作 ====================

    /**
     * 获取寄存器值
     *
     * @param threadId 目标线程 ID
     * @param frameIndex 栈帧索引
     * @param regNames 要获取的寄存器名称集合（空集合表示获取所有寄存器）
     * @return 获取寄存器请求消息
     */
    fun getRegisters(
        threadId: Long,
        frameIndex: Int,
        regNames: Set<String> = emptySet()
    ): Request = buildComposite {
        registers = RegistersRequest.newBuilder()
            .setThreadId(buildId(threadId))
            .setFrameIndex(frameIndex)
            .addAllRegisterNames(regNames)
            .build()
    }

    /**
     * 获取寄存器组信息
     *
     * 获取寄存器组的元数据信息，不包含具体的寄存器值。
     * 这可以用于了解目标平台的寄存器组织结构。
     *
     * @param threadId 目标线程 ID
     * @param frameIndex 栈帧索引
     * @return 获取寄存器组请求消息
     */
    fun getRegisterGroups(
        threadId: Long,
        frameIndex: Int
    ): Request = buildComposite {
        registerGroups = RegisterGroupsRequest.newBuilder()
            .setThreadId(buildId(threadId))
            .setFrameIndex(frameIndex)
            .build()
    }





    // ==================== 控制台和命令执行 ====================

    /**
     * 执行控制台命令
     *
     * 在调试器上下文中执行命令（类似于 LLDB 或 GDB 命令）
     *
     * @param threadId 目标线程 ID
     * @param frameIndex 栈帧索引
     * @param command 要执行的命令字符串
     * @return 执行控制台命令请求消息
     */
    fun handleConsoleCommand(
        threadId: Int,
        frameIndex: Int,
        command: String
    ): Request = buildComposite {
//        handleConsoleCommand = HandleConsoleCommandRequest.newBuilder()
//            .setThreadId(threadId.toInt())
//            .setFrameIndex(frameIndex)
//            .setCommand(command)
//            .build()
    }

    /**
     * 分发输入到目标
     *
     * 将用户输入发送到被调试程序的标准输入
     *
     * @param input 输入字符串
     * @param target 输入目标（进程或调试器）
     * @return 分发输入请求消息
     */
//    fun dispatchInput(input: String, target:  DispatchTarget): Request =
//        buildComposite {
//            dispatchInput = DispatchInputRequest.newBuilder()
//                .setInput(input)
//                .setTarget(target)
//                .build()
//        }

    /**
     * 处理命令补全
     *
     * 获取控制台命令的自动补全建议
     *
     * @param command 当前命令字符串
     * @param cursorPos 光标位置
     * @return 处理补全请求消息
     */
    fun handleCompletion(command: String, cursorPos: Int): Request =
        buildComposite {
//            handleCompletion = HandleCompletionRequest.newBuilder()
//                .setCommand(command)
//                .setCursorPosition(cursorPos)
//                .build()
        }

    /**
     * 调整控制台大小
     *
     * 通知调试器终端窗口大小变化（用于伪终端模式）
     *
     * @param columns 列数
     * @param rows 行数
     * @return 调整控制台大小请求消息
     */
    fun resizeConsole(columns: Int, rows: Int): Request =
        buildComposite {
//            resizeConsole = ResizeConsoleRequest.newBuilder()
//                .setColumns(columns)
//                .setRows(rows)
//                .build()
        }

    /**
     * 执行 Shell 命令
     *
     * 在远程平台或本地系统执行 Shell 命令
     *
     * @param command Shell 命令字符串
     * @param workingDir 工作目录（null 表示使用默认目录）
     * @param timeoutSecs 超时时间（秒）
     * @return 执行 Shell 命令请求消息
     */
    fun executeShellCommand(
        command: String,
        workingDir: String? = null,
        timeoutSecs: Int = 30
    ): Request = buildComposite {
//        executeShellCommand = ExecuteShellCommandRequest.newBuilder()
//            .setCommand(command)
//            .setTimeoutSeconds(timeoutSecs)
//            .applyIfNotNull(workingDir) { setWorkingDirectory(it) }
//            .build()
    }

    // ==================== 信号和符号管理 ====================

    /**
     * 配置信号处理策略
     *
     * @param signalName 信号名称（如 "SIGINT", "SIGSEGV"）
     * @param stop 接收到信号时是否停止进程
     * @param pass 是否将信号传递给被调试程序
     * @param notify 是否通知调试器前端
     * @return 处理信号请求消息
     */
    fun handleSignal(
        signalName: String,
        stop: Boolean,
        pass: Boolean,
        notify: Boolean
    ): Request = buildComposite {
//        handleSignal = HandleSignalRequest.newBuilder()
//            .setSignalName(signalName)
//            .setShouldStop(stop)
//            .setShouldPass(pass)
//            .setShouldNotify(notify)
//            .build()
    }

    /**
     * 取消符号下载
     *
     * 中止正在进行的调试符号下载操作
     *
     * @param details 取消原因描述
     * @return 取消符号下载请求消息
     */
    fun cancelSymbolsDownload(details: String): Request =
        buildComposite {
//            cancelSymbolsDownload = CancelSymbolsDownloadRequest.newBuilder()
//                .setDetails(details)
//                .build()
        }

    // ==================== 内部构建器方法 ====================

    /**
     * 创建复合请求构建器
     */
    private inline fun buildComposite(
        block: Request.Builder.() -> Unit
    ): Request = Request.newBuilder()
        .setHash(hashGenerator.getAndIncrement())
        .apply(block)
        .build()

    /**
     * 条件应用配置
     *
     * 仅在值非空时应用构建器配置
     */
    private inline fun <T, B> B.applyIfNotNull(value: T?, block: B.(T) -> Unit): B = apply {
        value?.let { block(it) }
    }

    /**
     * 确定控制台模式
     */
    private fun determineConsoleMode(usePseudo: Boolean, useExternal: Boolean): ConsoleMode {
        return when {
            usePseudo -> ConsoleMode.CONSOLE_MODE_PSEUDO
            useExternal -> ConsoleMode.CONSOLE_MODE_EXTERNAL
            else -> ConsoleMode.CONSOLE_MODE_PARENT
        }
    }

    /**
     * 构建断点请求
     */
    private inline fun buildBreakpointRequest(
        block: AddBreakpointRequest.Builder.() -> Unit
    ): AddBreakpointRequest = AddBreakpointRequest.newBuilder().apply(block).build()

    /**
     * 构建进程启动信息
     *
     * 将 GeneralCommandLine 转换为 Protobuf 启动信息对象
     */
    private fun buildProcessLaunchInfo(
        execPath: String,
        cmdLine: GeneralCommandLine,
        stdinPath: String? = null,
        stdoutPath: String? = null,
        stderrPath: String? = null
    ): ProcessLaunchInfo {
        val workingDir = cmdLine.workDirectory ?: File(execPath).parentFile

        return ProcessLaunchInfo.newBuilder()
            .setExecutablePath(execPath)
            .setWorkingDirectory(workingDir.absolutePath)
            .addAllEnv(buildEnvironmentVariables(cmdLine))
            .addAllArgv(cmdLine.parametersList.array.toList())
            .applyIfNotNull(stdinPath) { setStdinPath(it) }
            .applyIfNotNull(stdoutPath) { setStdoutPath(it) }
            .applyIfNotNull(stderrPath) { setStderrPath(it) }
            .build()
    }

    /**
     * 构建环境变量列表
     */
    private fun buildEnvironmentVariables(cmdLine: GeneralCommandLine): List<EnvironmentVariable> {
        return cmdLine.effectiveEnvironment.map { (name, value) ->
            EnvironmentVariable.newBuilder()
                .setName(name)
                .setValue(value)
                .build()
        }
    }
}