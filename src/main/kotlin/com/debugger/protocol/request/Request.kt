package com.debugger.protocol.request


import com.debugger.protocol.ProtocolMessage
import com.debugger.protocol.type.MessageCommand
import com.debugger.protocol.type.MessageType
import com.debugger.protocol.type.arguments.*
import kotlinx.serialization.Serializable

interface Request : ProtocolMessage {
    //    @SerialName("type")
    override val type: MessageType get() = MessageType.request

    /**
     * 要执行的命令
     */

    val command: MessageCommand


    /**
     * 包好命令参数的对象
     */

    val arguments: Arguments? get() = null


}

/**
 * 取消请求
 */
@Serializable
data class CancelRequest(
    override val seq: Int,
    override val command: MessageCommand,
    override val arguments: CancelArguments?


) : Request {
    //    @SerialName("type")
    override val type: MessageType = super.type
}

/**
 * 附加请求
 */
@Serializable
data class AttachRequest(
    override val seq: Int,

    override val arguments: AttachRequestArguments? = null,

    ) : Request {

    override val type: MessageType
        get() = super.type
    override val command: MessageCommand = MessageCommand.attach
}


/**
 * 初始化请求
 */
@Serializable
data class InitializeRequest(


    override val arguments: InitializeRequestArguments? = null,
    override val seq: Int = 1,
) : Request {
    override val command: MessageCommand = MessageCommand.initialize


    override val type: MessageType = super.type
}

/**
 * 启动请求
 */
@Serializable
data class LaunchRequest(
    override val seq: Int,

//    override val type: MessageType = MessageType.request,

    override val arguments: LaunchRequestArguments? = null,
) : Request {
    //
    override val type: MessageType = super.type


    override val command: MessageCommand = MessageCommand.launch
}

/**
 * 为单个源设置多个断点，并清除该源中所有以前的断点
 * 若要清除源的所有断点，请指定一个空数组。
 *
 * 当命中断点时，将生成一个事件（带有原因）。stoppedbreakpoint
 */

@Serializable
data class SetBreakpointsRequest(
    override val seq: Int,

    override val arguments: SetBreakpointsArguments?
) : Request {
    override val type: MessageType = super.type
    override val command: MessageCommand = MessageCommand.setBreakpoints
}

/**
 * ConfigurationDone 请求
 * 此请求指示客户端已完成调试适配器的初始化。
 *
 * 因此，它是配置请求序列中的最后一个请求（由事件启动）。initialized
 *
 * 仅当相应的功能为 true 时，客户端才应调用此请求。supportsConfigurationDoneRequest
 */
@Serializable
data class ConfigurationDoneRequest(
    override val seq: Int,

    override val arguments: ConfigurationDoneArguments? = null
) : Request {

    override val command: MessageCommand = MessageCommand.configurationDone

    override val type: MessageType = super.type

}


/**
 * 断开连接请求
 */
@Serializable
data class DisconnectRequest(
    override val seq: Int,
    override val arguments: DisconnectArguments? = null
) : Request {
    override val command: MessageCommand = MessageCommand.disconnect
    override val type: MessageType = super.type
}


/**
 * 变量请求
 */
@Serializable
data class VariablesRequest(
    override val seq: Int,
    override val arguments: VariablesArguments? = null

) : Request {
    override val type: MessageType = super.type
    override val command: MessageCommand = MessageCommand.variables
}


/**
 *该请求检索所有线程的列表。
 */
@Serializable
data class ThreadsRequest(override val seq: Int) : Request {
    override val type: MessageType = super.type
    override val command: MessageCommand = MessageCommand.threads
}


/**
 * StackTrace 请求
 * 该请求从给定线程的当前执行状态返回堆栈跟踪。
 *
 * 客户端可以通过省略 startFrame 和 levels 参数来请求所有堆栈帧。
 * 对于注重性能的客户端，如果相应的功能为 true，则可以使用 and 参数以分段方式检索堆栈帧。请求的响应可能包含一个属性，
 * 该属性暗示堆栈中的总帧数。如果客户端预先需要此总数，它可以发出单个（第一个）帧的请求，
 * 并根据 的值决定如何继续。在任何情况下，客户端都应该准备好接收比请求更少的帧，这表明堆栈已到达末尾。
 */
@Serializable
data class StackTraceRequest(
    override val seq: Int,
    override val arguments: StackTraceArguments? = null
) : Request {
    override val command: MessageCommand = MessageCommand.stackTrace
    override val type: MessageType = super.type
}

/**
 * SetVariable 请求
 * 将变量容器中具有给定名称的变量设置为新值。仅当相应的功能为 true 时，客户端才应调用此请求。supportsSetVariable
 *
 * 如果调试适配器同时实现 和 ，则客户端将仅在变量具有属性时使用。setVariablesetExpressionsetExpressionevaluateName
 */
@Serializable
data class SetVariableRequest(override val seq: Int, override val arguments: SetVariableArguments? = null) : Request {
    override val command: MessageCommand = MessageCommand.setVariable
    override val type: MessageType = super.type
}

/**
 * 下一个请求
 * 该请求对指定的线程执行一个步骤（在给定的粒度内），并允许所有其他线程通过恢复它们来自由运行。
 *
 * 如果调试适配器支持单线程执行（请参阅 capability ），则将参数设置为 true 可防止其他挂起的线程恢复。supportsSingleThreadExecutionRequestssingleThread
 *
 * 调试适配器首先发送响应，然后在步骤完成后发送事件（带原因）。stoppedstep
 */
//@Serializable
//data class NextRequest(override val seq: Int,
//                       override val arguments: NextArguments? = null) :Request{
//    override val command: MessageCommand = MessageCommand.next
//    override val type: MessageType = super.type
//}
/**
 * StepIn 请求
 * 该请求恢复给定线程以单步执行函数/方法，并允许所有其他线程通过恢复它们自由运行。
 *
 * 如果调试适配器支持单线程执行（请参阅 capability ），则将参数设置为 true 可防止其他挂起的线程恢复。supportsSingleThreadExecutionRequestssingleThread
 *
 * 如果请求无法单步执行目标，则其行为与请求类似。stepInnext
 *
 * 调试适配器首先发送响应，然后在步骤完成后发送事件（带原因）。stoppedstep
 *
 * 如果源代码行上有多个函数/方法调用（或其他目标），
 *
 * 该参数可用于控制应将该操作发送到哪个目标中。targetIdstepIn
 *
 * 可以通过请求检索给定源代码行的可能目标列表。stepInTargets
 */
//@Serializable
//data class StepInRequest(override val seq: Int, override val arguments: StepInArguments? = null) :Request{
//    override val command: MessageCommand = MessageCommand.stepIn
//    override val type: MessageType = super.type
//}


/**
 * StepOut 请求
 * 该请求恢复给定线程以从函数/方法中退出（返回），并允许所有其他线程通过恢复它们自由运行。
 *
 * 如果调试适配器支持单线程执行（请参阅 capability ），则将参数设置为 true 可防止其他挂起的线程恢复。supportsSingleThreadExecutionRequestssingleThread
 *
 * 调试适配器首先发送响应，然后在步骤完成后发送事件（带原因）。stoppedstep
 */
//@Serializable
//data class StepOutRequest(override val seq: Int, override val arguments: StepOutArguments? = null) :Request{
//    override val command: MessageCommand = MessageCommand.stepOut
//    override val type: MessageType = super.type
//}

/**
 * 后退请求
 * 该请求对指定的线程执行一个向后步骤（在给定的粒度中），并允许所有其他线程通过恢复它们自由向后运行。
 *
 * 如果调试适配器支持单线程执行（请参阅 capability ），则将参数设置为 true 可防止其他挂起的线程恢复。supportsSingleThreadExecutionRequestssingleThread
 *
 * 调试适配器首先发送响应，然后在步骤完成后发送事件（带原因）。stoppedstep
 *
 * 仅当相应的功能为 true 时，客户端才应调用此请求。supportsStepBack
 */
//@Serializable
//data class StepBackRequest(override val seq: Int, override val arguments: StepBackArguments? = null) :Request{
//    override val command: MessageCommand = MessageCommand.stepBack
//    override val type: MessageType = super.type
//}


/**
 * 继续请求
 * 请求将恢复所有线程的执行。如果调试适配器支持单线程执行（请参阅 capability ），则将参数设置为 true 仅恢复指定的线程。如果并非所有线程都已恢复，则响应的属性应设置为 false。supportsSingleThreadExecutionRequestssingleThreadallThreadsContinued
 */
//@Serializable
//data class ContinueRequest(override val seq: Int, override val arguments: ContinueArguments? = null) :Request{
//    override val command: MessageCommand = MessageCommand.continue_
//    override val type: MessageType = super.type
//}

/**
 * ReverseContinue 请求
 * 请求恢复所有线程的向后执行。如果调试适配器支持单线程执行（请参阅 capability ），则将参数设置为 true 仅恢复指定的线程。如果并非所有线程都已恢复，则响应的属性应设置为 false。supportsSingleThreadExecutionRequestssingleThreadallThreadsContinued
 *
 * 仅当相应的功能为 true 时，客户端才应调用此请求。supportsStepBack
 */

//@Serializable
//data class ReverseContinueRequest(override val seq: Int, override val arguments: ReverseContinueArguments? = null) :Request{
//    override val command: MessageCommand = MessageCommand.reverseContinue
//    override val type: MessageType = super.type
//}


/**
 * RunInTerminal 请求
 * 此请求从调试适配器发送到客户端，以在终端中运行命令。
 *
 * 这通常用于在客户端提供的终端中启动调试对象。
 *
 * 仅当相应的客户端功能为 true 时，才应调用此请求。supportsRunInTerminalRequest
 *
 * 的客户端实现可以自由地运行命令，无论他们选择什么，包括向命令行解释器（又名“shell”）发出命令。传递给请求的参数字符串必须在要运行的命令中逐字到达。因此，使用 shell 的客户端负责转义参数字符串中的任何特殊 shell 字符，以防止它们被 shell 解释（和修改）。runInTerminalrunInTerminal
 *
 * 一些用户可能希望在参数字符串中利用 shell 处理。对于使用中间 shell 实现的客户端，可以将该属性设置为 true。在这种情况下，要求客户端不要对参数字符串中的任何特殊 shell 字符进行转义。runInTerminalargsCanBeInterpretedByShell
 */
@Serializable

data class RunInTerminalRequest(
    override val seq: Int,

    override val arguments: RunInTerminalRequestArguments? = null
) : Request {
    override val type: MessageType = super.type
    override val command: MessageCommand = MessageCommand.runInTerminal
}


@Serializable
data class DebugInConsoleRequest(
    override val seq: Int,

    override val arguments: DebugInConsoleRequestArguments? = null
) : Request {
    override val type: MessageType = super.type
    override val command: MessageCommand = MessageCommand.debugInConsole
}

/**
 * SetFunctionBreakpoints 请求
 * 将所有现有函数断点替换为新的函数断点。
 *
 * 若要清除所有函数断点，请指定一个空数组。
 *
 * 当命中断点时，将生成一个事件（带有原因）。stoppedfunction breakpoint
 *
 * 仅当相应的功能为 true 时，客户端才应调用此请求。supportsFunctionBreakpoints
 */

@Serializable

data class SetFunctionBreakpointsRequest(
    override val seq: Int,

    override val arguments: SetFunctionBreakpointsArguments? = null
) : Request {
    override val type: MessageType = super.type
    override val command: MessageCommand = MessageCommand.setFunctionBreakpoints
}

/**
 * SetDataBreakpoints 请求
 * 将所有现有数据断点替换为新的数据断点。
 *
 * 若要清除所有数据断点，请指定一个空数组。
 *
 * 当命中断点时，将生成一个事件（带有原因）。stoppeddata breakpoint
 *
 * 仅当相应的功能为 true 时，客户端才应调用此请求。supportsDataBreakpoints
 */
@Serializable
data class SetDataBreakpointsRequest(
    override val seq: Int,

    override val arguments: SetDataBreakpointsArguments? = null
) : Request {
    override val type: MessageType = super.type
    override val command: MessageCommand = MessageCommand.setDataBreakpoints
}

/**
 * SetInstructionBreakpoints 请求
 * 替换所有现有的指令断点。通常，指令断点将从反汇编窗口设置。
 *
 * 若要清除所有指令断点，请指定一个空数组。
 *
 * 当命中指令断点时，将生成一个事件（带有原因）。stoppedinstruction breakpoint
 *
 * 仅当相应的功能为 true 时，客户端才应调用此请求。supportsInstructionBreakpoints
 */
@Serializable
data class SetInstructionBreakpointsRequest(
    override val seq: Int,

    override val arguments: SetInstructionBreakpointsArguments? = null
) : Request {
    override val type: MessageType = super.type
    override val command: MessageCommand = MessageCommand.setInstructionBreakpoints
}


/**
 * 范围请求
 * 该请求返回给定堆栈帧 ID 的变量范围。
 */
@Serializable
data class ScopesRequest(
    override val seq: Int,

    override val arguments: ScopesArguments? = null
) : Request {
    override val type: MessageType = super.type
    override val command: MessageCommand = MessageCommand.scopes
}


/**
 * 下一个请求
 * 该请求对指定的线程执行一个步骤（在给定的粒度内），并允许所有其他线程通过恢复它们来自由运行。
 *
 * 如果调试适配器支持单线程执行（请参阅 capability ），则将参数设置为 true 可防止其他挂起的线程恢复。supportsSingleThreadExecutionRequestssingleThread
 *
 * 调试适配器首先发送响应，然后在步骤完成后发送事件（带原因）。stoppedstep
 */
@Serializable
data class NextRequest(
    override val seq: Int,

    override val arguments: NextArguments? = null
) : Request {
    override val type: MessageType = super.type
    override val command: MessageCommand = MessageCommand.next
}

/**
 * StepIn 请求
 * 该请求恢复给定线程以单步执行函数/方法，并允许所有其他线程通过恢复它们自由运行。
 *
 * 如果调试适配器支持单线程执行（请参阅 capability ），则将参数设置为 true 可防止其他挂起的线程恢复。supportsSingleThreadExecutionRequestssingleThread
 *
 * 如果请求无法单步执行目标，则其行为与请求类似。stepInnext
 *
 * 调试适配器首先发送响应，然后在步骤完成后发送事件（带原因）。stoppedstep
 *
 * 如果源代码行上有多个函数/方法调用（或其他目标），
 *
 * 该参数可用于控制应将该操作发送到哪个目标中。targetIdstepIn
 *
 * 可以通过请求检索给定源代码行的可能目标列表。stepInTargets
 */

@Serializable
data class StepInRequest(
    override val seq: Int,

    override val arguments: StepInArguments? = null
) : Request {
    override val type: MessageType = super.type
    override val command: MessageCommand = MessageCommand.stepIn
}

/**
 * StepOut 请求
 * 该请求恢复给定线程以从函数/方法中退出（返回），并允许所有其他线程通过恢复它们自由运行。
 *
 * 如果调试适配器支持单线程执行（请参阅 capability ），则将参数设置为 true 可防止其他挂起的线程恢复。supportsSingleThreadExecutionRequestssingleThread
 *
 * 调试适配器首先发送响应，然后在步骤完成后发送事件（带原因）。stoppedstep
 */
@Serializable

data class StepOutRequest(
    override val seq: Int,

    override val arguments: StepOutArguments? = null
) : Request {
    override val type: MessageType = super.type
    override val command: MessageCommand = MessageCommand.stepOut
}

/**
 * 继续请求
 * 请求将恢复所有线程的执行。如果调试适配器支持单线程执行（请参阅 capability ），则将参数设置为 true 仅恢复指定的线程。如果并非所有线程都已恢复，则响应的属性应设置为 false。supportsSingleThreadExecutionRequestssingleThreadallThreadsContinued
 */
@Serializable
data class ContinueRequest(
    override val seq: Int,

    override val arguments: ContinueArguments? = null
) : Request {
    override val type: MessageType = super.type
    override val command: MessageCommand = MessageCommand.Continue
}


/**
 * 模块请求
 * 可以使用此请求从调试适配器中检索模块，该请求可以返回所有模块或一系列模块以支持分页。
 *
 * 仅当相应的功能为 true 时，客户端才应调用此请求。supportsModulesRequest
 */
@Serializable

data class ModulesRequest (
    override val seq: Int,


    override val arguments: ModulesArguments
) : Request {
    override val  command: MessageCommand = MessageCommand.modules
    override val type: MessageType = super.type

}
/**
 * 评估请求
 * 在最顶层堆栈帧的上下文中计算给定的表达式。
 *
 * 表达式有权访问作用域内的任何变量和参数。
 */
@Serializable
data class EvaluateRequest(
    override val seq: Int,

    override val arguments: EvaluateArguments? = null
) : Request {
    override val type: MessageType = super.type
    override val command: MessageCommand = MessageCommand.evaluate
}
/**
 * SetVariable 请求
 * 将变量容器中具有给定名称的变量设置为新值。仅当相应的功能为 true 时，客户端才应调用此请求。supportsSetVariable
 *
 * 如果调试适配器同时实现 和 ，则客户端将仅在变量具有属性时使用。setVariablesetExpressionsetExpressionevaluateName
 */


/**
 * 该请求检索给定源引用的源代码
 */
@Serializable
data class SourceRequest(
    override val seq: Int,

    override val arguments: SourceArguments? = null
) : Request {
    override val type: MessageType = super.type
    override val command: MessageCommand = MessageCommand.source
}
