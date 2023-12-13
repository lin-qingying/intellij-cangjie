package dap.type.body

import dap.type.*
import dap.type.Thread
import kotlinx.serialization.Contextual
import kotlinx.serialization.Polymorphic
import kotlinx.serialization.Serializable


@Polymorphic
interface Body

@Serializable
data class SetBreakpointsResponseBody(
    /**
     * 关于断点的信息。
     * 数组元素的顺序与参数中的 `breakpoints` 数组元素的顺序相同。
     */
    val breakpoints: List<Breakpoint>
) : Body

/**
 * InitializedEvent Body
 */
@Serializable
data class OutPutEventBody(
    /**
     * 输出类别。如果未指定或客户端不理解类别，则假定为`console`。
     * 值：
     * 'console': 在客户端的默认消息 UI 中显示输出，例如 'debug console'。此类别仅用于来自调试器的信息性输出（而不是被调试程序）。
     * 'important': 提示客户端在其 UI 中显示输出，用于重要且高度可见的信息，例如弹出通知。此类别仅用于来自调试器的重要消息（而不是被调试程序）。由于此类别值是提示，客户端可能会忽略该提示并假定 `console` 类别。
     * 'stdout': 将输出显示为被调试程序的正常程序输出。
     * 'stderr': 将输出显示为被调试程序的错误程序输出。
     * 'telemetry': 将输出发送到遥测，而不是显示给用户。
     * 等等。
     */
    val category: Category? = null,
    /**
     * 输出通道
     */
    val outputChannel: String? = null,
    /**
     * 要报告的输出。
     */
    val output: String? = null,

    /**
     * 通过将相关消息分组来保持输出日志的组织。值：
     * 'start': 以展开模式开始新的组。后续的输出事件是组的成员，应显示为缩进。`output` 属性成为组的名称，不会缩进。
     * 'startCollapsed': 以折叠模式开始新的组。后续的输出事件是组的成员，应显示为缩进（只要组被展开）。
     * 'end': 结束当前组并减少后续输出事件的缩进。非空的 `output` 属性显示为组的未缩进的结束。
     */
    val group: Group? = null,


    /**
     * 如果存在属性 `variablesReference` 并且其值大于 0，则输出包含可以通过将 `variablesReference` 传递给 `variables` 请求来检索的对象，只要执行保持挂起。有关详细信息，请参阅概述部分中的“对象引用的生命周期”。
     */
    val variablesReference: Int? = null,

    /**
     * 产生输出的源位置。
     */
    val source: Source? = null,

    /**
     * 产生输出的源位置的行。
     */
    val line: Int? = null,

    /**
     * 在 `line` 中产生输出的位置。它以 UTF-16 代码单元进行测量，客户端能力 `columnsStartAt1` 决定它是基于 0 还是基于 1。
     */
    val column: Int? = null,

    /**
     * 要报告的附加数据。对于 `telemetry` 类别，数据发送到遥测，对于其他类别，数据以 JSON 格式显示。
     */
    @Contextual
    val data: Any? = null

) : Body

/**
 * dap能力
 */
@Serializable
data class Capabilities(
    /**
     * 调试适配器支持 `configurationDone` 请求。
     */
    val supportsConfigurationDoneRequest: Boolean? = null,

    /**
     * 调试适配器支持函数断点。
     */
    val supportsFunctionBreakpoints: Boolean? = null,

    /**
     * 调试适配器支持条件断点。
     */
    val supportsConditionalBreakpoints: Boolean? = null,

    /**
     * 调试适配器支持在指定次数的命中后中断执行的断点。
     */
    val supportsHitConditionalBreakpoints: Boolean? = null,

    /**
     * 调试适配器支持对数据悬停进行（无副作用的） `evaluate` 请求。
     */
    val supportsEvaluateForHovers: Boolean? = null,

    /**
     * `setExceptionBreakpoints` 请求的可用异常过滤器选项。
     */
    val exceptionBreakpointFilters: List<ExceptionBreakpointsFilter>? = null,

    /**
     * 调试适配器支持通过 `stepBack` 和 `reverseContinue` 请求进行回退。
     */
    val supportsStepBack: Boolean? = null,

    /**
     * 调试适配器支持将变量设置为值。
     */
    val supportsSetVariable: Boolean? = null,

    /**
     * 调试适配器支持重新启动帧。
     */
    val supportsRestartFrame: Boolean? = null,

    /**
     * 调试适配器支持 `gotoTargets` 请求。
     */
    val supportsGotoTargetsRequest: Boolean? = null,

    /**
     * 调试适配器支持 `stepInTargets` 请求。
     */
    val supportsStepInTargetsRequest: Boolean? = null,

    /**
     * 调试适配器支持 `completions` 请求。
     */
    val supportsCompletionsRequest: Boolean? = null,

    /**
     * 应在 REPL 中触发完成的字符集。如果未指定，UI 应假定 `.` 字符。
     */
    val completionTriggerCharacters: List<String>? = null,

    /**
     * 调试适配器支持 `modules` 请求。
     */
    val supportsModulesRequest: Boolean? = null,

    /**
     * 调试适配器公开的其他模块信息集。
     */
    val additionalModuleColumns: List<ColumnDescriptor>? = null,

    /**
     * 调试适配器支持的校验和算法。
     */
    val supportedChecksumAlgorithms: List<ChecksumAlgorithm>? = null,

    /**
     * 调试适配器支持 `restart` 请求。在这种情况下，客户端不应通过终止和重新启动适配器来实现 `restart`，而应通过调用 `restart` 请求来实现。
     */
    val supportsRestartRequest: Boolean? = null,

    /**
     * 调试适配器支持在 `setExceptionBreakpoints` 请求上的 `exceptionOptions`。
     */
    val supportsExceptionOptions: Boolean? = null,

    /**
     * 调试适配器支持在 `stackTraces`、`variables` 和 `evaluate` 请求上的 `format` 属性。
     */
    val supportsValueFormattingOptions: Boolean? = null,

    /**
     * 调试适配器支持 `exceptionInfo` 请求。
     */
    val supportsExceptionInfoRequest: Boolean? = null,

    /**
     * 调试适配器支持在 `disconnect` 请求上的 `terminateDebuggee` 属性。
     */
    val supportTerminateDebuggee: Boolean? = null,

    /**
     * 调试适配器支持在 `disconnect` 请求上的 `suspendDebuggee` 属性。
     */
    val supportSuspendDebuggee: Boolean? = null,

    /**
     * 调试适配器支持延迟加载堆栈的部分，这需要支持 `stackTraces` 请求的 `startFrame` 和 `levels` 参数以及结果的 `totalFrames`。
     */
    val supportsDelayedStackTraceLoading: Boolean? = null,

    /**
     * 调试适配器支持 `loadedSources` 请求。
     */
    val supportsLoadedSourcesRequest: Boolean? = null,

    /**
     * 调试适配器通过解释 `SourceBreakpoint` 的 `logMessage` 属性来支持日志点。
     */
    val supportsLogPoints: Boolean? = null,

    /**
     * 调试适配器支持 `terminateThreads` 请求。
     */
    val supportsTerminateThreadsRequest: Boolean? = null,

    /**
     * 调试适配器支持 `setExpression` 请求。
     */
    val supportsSetExpression: Boolean? = null,

    /**
     * 调试适配器支持 `terminate` 请求。
     */
    val supportsTerminateRequest: Boolean? = null,

    /**
     * 调试适配器支持数据断点。
     */
    val supportsDataBreakpoints: Boolean? = null,

    /**
     * 调试适配器支持 `readMemory` 请求。
     */
    val supportsReadMemoryRequest: Boolean? = null,

    /**
     * 调试适配器支持 `writeMemory` 请求。
     */
    val supportsWriteMemoryRequest: Boolean? = null,

    /**
     * 调试适配器支持 `disassemble` 请求。
     */
    val supportsDisassembleRequest: Boolean? = null,

    /**
     * 调试适配器支持 `cancel` 请求。
     */
    val supportsCancelRequest: Boolean? = null,

    /**
     * 调试适配器支持 `breakpointLocations` 请求。
     */
    val supportsBreakpointLocationsRequest: Boolean? = null,

    /**
     * 调试适配器支持在 `evaluate` 请求中的 `clipboard` 上下文值。
     */
    val supportsClipboardContext: Boolean? = null,

    /**
     * 调试适配器支持步进粒度（参数 `granularity`）对于步进请求。
     */
    val supportsSteppingGranularity: Boolean? = null,

    /**
     * 调试适配器支持基于指令引用添加断点。
     */
    val supportsInstructionBreakpoints: Boolean? = null,

    /**
     * 调试适配器支持在 `setExceptionBreakpoints` 请求上的 `filterOptions` 作为参数。
     */
    val supportsExceptionFilterOptions: Boolean? = null,

    /**
     * 调试适配器支持在执行请求（`continue`、`next`、`stepIn`、`stepOut`、`reverseContinue`、`stepBack`）上的 `singleThread` 属性。
     */
    val supportsSingleThreadExecutionRequests: Boolean? = null,

    val supportedTimeTravelRecordPointTypes: List<String>? = null,

    ) : Body

/**
 * 错误响应的正文
 */
@Serializable
data class ErrorBody(
    /**
     * 错误的消息。
     */
    val error: String? = null
) : Body

@Serializable
data class BreakpointEventBody(
    /**
     * 事件的原因。
     * 可能的值：'changed', 'new', 'removed'等。
     */
    val reason: BreakpointEventReason? = null,

    /**
     * `id`属性用于查找目标断点，其他属性用作新值。
     */
    val breakpoint: Breakpoint? = null,


    val hitCount: Int? = null,
    val ignoreCount: Int? = null,
) : Body


@Serializable
data class ThreadEventBody(
    /**
     * 事件的原因。
     * 值：'started', 'exited' 等。
     */
    val reason: ThreadEventReason,

    /**
     * 线程的标识符。
     */
    val threadId: Int
) : Body


@Serializable
data class LoadedSourceEventBody(val reason: Reason, val source: Source) : Body

@Serializable
data class VariablesResponseBody(
    /**
     * 给定变量引用的所有（或一部分）变量。
     */
    val variables: List<Variable>? = null,
) : Body


@Serializable
data class StoppedEventBody(
    /**
     * 事件的原因。
     * 为了向后兼容，如果缺少 `description` 属性，此字符串将在 UI 中显示（但不必翻译）。
     * 值：'step', 'breakpoint', 'exception', 'pause', 'entry', 'goto',
     * 'function breakpoint', 'data breakpoint', 'instruction breakpoint'等。
     */
    val reason: StoppedEventReason,

    /**
     * 事件的完整原因，例如 'Paused on exception'。此字符串将按原样显示在 UI 中，并可以翻译。
     */
    val description: String? = null,

    /**
     * 停止的线程。
     */
    val threadId: Int? = null,

    /**
     * 值为 true 的提示给客户端，此事件不应改变焦点。
     */
    val preserveFocusHint: Boolean? = null,

    /**
     * 附加信息。例如，如果原因是 `exception`，文本包含异常名称。此字符串将显示在 UI 中。
     */
    val text: String? = null,


    /**
     * 如果 `allThreadsStopped` 为 true，调试适配器可以声明所有线程已停止。
     * - 客户端应使用此信息来启用所有线程，以便访问它们的堆栈跟踪。
     * - 如果属性缺失或为 false，只有具有给定 `threadId` 的线程可以展开。
     */
    val allThreadsStopped: Boolean? = null,

    /**
     * 触发事件的断点的 ID。在大多数情况下，只有一个断点，但这里有一些多个断点的示例：
     * - 不同类型的断点映射到同一位置。
     * - 编译器/运行时将多个源断点折叠到同一指令。
     * - 具有不同函数名称的多个函数断点映射到同一位置。
     */
    val hitBreakpointIds: List<Int>? = null

) : Body


@Serializable
data class ThreadsResponseBody(
    /**
     * 所有线程的列表。
     */
    val threads: List<Thread>
) : Body


@Serializable
data class TerminatedEventBody(
    /**。
     *调试适配器可以将`restart`设置为True(或设置为任意对象)以。
     *请求客户端重新启动会话。
     *客户端不会解释该值，并将其作为未修改的。
     *`Launch`和`attach`请求的属性为`__restart`。
     */
    @Contextual
    val restart: Any? = null
) : Body

@Serializable
data class ExitedEventBody(
    /**
     * 退出代码
     */
    val exitCode: Int? = null
) : Body


@Serializable
data class ContinuedEventBody(

    /**
     * 如果 `allThreadsContinued` 为真，调试适配器可以宣布所有线程都已继续。
     */
    val allThreadsContinued: Boolean? = null,

    /**
     * 线程id
     */
    val threadId: Int? = null,
) : Body

@Serializable
data class RunInTerminalResponseBody(
    /**
     * 进程ID。该值应小于或等于2147483647（2^31-1）。
     */
//    val processId: Long? = null,

    /**
     * 终端shell的进程ID。该值应小于或等于2147483647（2^31-1）。
     */
    val shellProcessId: Long? = null
) : Body


@Serializable
data class DebugInConsoleResponseBody(
    /**
     *  输出
     */
    val output: String? = null
) : Body

@Serializable
data class SetFunctionBreakpointsResponseBody(
    /**
     * 关于断点的信息。
     * 数组元素的顺序与参数中的 `breakpoints` 数组元素的顺序相同。
     */
    val breakpoints: List<Breakpoint>
) : Body

@Serializable
data class SetDataBreakpointsResponseBody(
    /**
     * 关于断点的信息。
     * 数组元素的顺序与参数中的 `breakpoints` 数组元素的顺序相同。
     */
    val breakpoints: List<Breakpoint>
) : Body

@Serializable
data class SetInstructionBreakpointsResponseBody(
    /**
     * 关于断点的信息。
     * 数组元素的顺序与参数中的 `breakpoints` 数组元素的顺序相同。
     */
    val breakpoints: List<Breakpoint>
) : Body


@Serializable
data class StackTraceResponseBody(
    /**
     * 可用堆栈中的帧总数。如果省略或者
     * `totalFrames` 大于可用帧数，客户端预期
     * 请求帧，直到一个请求返回的帧数少于请求的
     * （这表示堆栈的结束）。对于后续请求返回单调增加的
     * `totalFrames` 值可以用来在客户端强制分页。
     */

    val totalFrames: Int,
    /**
     * 堆栈帧的帧。如果数组长度为零，则没有可用的堆栈帧。
     * 这意味着没有可用的位置信息。
     */
    val stackFrames: List<StackFrame>
) : Body

@Serializable
data class ScopesResponseBody(


    /**
     * 堆栈帧的作用域。如果数组长度为零，则没有可用的作用域。
     */
    val scopes: List<Scope>
) : Body

@Serializable
data class ContinueResponseBody(
    /**
     * 如果 `allThreadsContinued` 为真，调试适配器可以宣布所有线程都已继续。
     */
    val allThreadsContinued: Boolean? = null
) : Body