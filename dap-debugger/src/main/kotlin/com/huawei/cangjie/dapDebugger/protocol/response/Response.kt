package com.huawei.cangjie.dapDebugger.protocol.response


import com.huawei.cangjie.dapDebugger.protocol.ProtocolMessage
import com.huawei.cangjie.dapDebugger.protocol.type.MessageCommand
import com.huawei.cangjie.dapDebugger.protocol.type.MessageType
import com.huawei.cangjie.dapDebugger.protocol.type.ResponseMessage
import com.huawei.cangjie.dapDebugger.protocol.type.body.*

import kotlinx.serialization.Polymorphic
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 响应
 */
@Polymorphic
interface Response : ProtocolMessage {

    /**
     * 请求的序列号
     */
    val request_seq: Int

    /**
     * 响应的结果
     */
    val success: Boolean

    /**
     * 响应的结果
     */
    val command: MessageCommand

    /**
     * 如果 `success` 为 false，则包含简短形式的原始错误。
     * 客户端可能会解析这个原始错误，并且它不会在用户界面中显示。
     * 存在一些预定义的值。
     * 值：
     * 'cancelled': 请求被取消。
     * 'notStopped': 一旦适配器处于 'stopped' 状态，请求可能会被重试。
     * 等等。
     */
    val message: ResponseMessage? get() = null

    /**
     * 如果 `success` 为 true，则包含请求结果；如果 `success` 为 false，则包含错误详情。
     */
    val body: Body? get() = null

    //    @SerialName("type")
    override val type: MessageType
        get() = MessageType.response
}

/**
 * 对请求的响应。这只是一个确认，所以不需要正文字段。
 */
@Serializable
data class LaunchResponse(
    override val seq: Int,

    override val request_seq: Int,
    override val success: Boolean,
    override val command: MessageCommand,
    override var message: ResponseMessage? = null

    ) : Response {
    override val type: MessageType = super.type
}

@Serializable
data class SetBreakpointsResponse(
    override val seq: Int,
    override val request_seq: Int,
    override val success: Boolean,


    override val body: SetBreakpointsResponseBody? = null,

    ) : Response {
    override val type: MessageType = super.type
    override val command: MessageCommand = MessageCommand.setBreakpoints

}

@Serializable

data class ConfigurationDoneResponse(
    override val seq: Int,
    override val request_seq: Int,
    override val success: Boolean,

    ) : Response {
    override val type: MessageType = super.type

    override val command: MessageCommand = MessageCommand.configurationDone
}

/**
 * 附加请求响应
 * 对请求的响应。这只是一个确认，所以不需要正文字段
 */
@Serializable
data class AttachResponse(
    override val seq: Int,
    override val type: MessageType,
    override val request_seq: Int,
    override val success: Boolean,
    override val command: MessageCommand,

    ) : Response

/**
 * 初始化响应
 */
@SerialName("initialize")
@Serializable
data class InitializeResponse(
    override val request_seq: Int,
    override val seq: Int,
    override val success: Boolean,
    override val command: MessageCommand,
    override val message: ResponseMessage? = null,
    override val body: Capabilities?,
//        @SerialName("type")
    override val type: MessageType,
) : Response

/**
 * 错误响应
 */
@Serializable
@SerialName("error")
data class ErrorResponse
    (
    override val request_seq: Int,
    override val seq: Int,
    override val success: Boolean,
    override val command: MessageCommand,
    override val message: ResponseMessage? = null,
    override val body: ErrorBody?,

    override val type: MessageType,
) : Response

/**
 * 对请求的响应。这只是一个确认，所以不需要正文字段。cancel
 */
@Serializable
@SerialName("cancel")
data class CancelResponse(
    override val seq: Int,
    override val request_seq: Int,
    override val success: Boolean,
    override val command: MessageCommand,
    override val message: ResponseMessage? = null,
    override val body: Body?,
//        @SerialName("type")
    override val type: MessageType
) : Response

/**
 * 变量请求响应
 */
@Serializable
data class VariablesResponse(
    /**
     * 给定变量引用的所有（或一部分）变量。
     */
    override val body: VariablesResponseBody,
    override val seq: Int,
    override val request_seq: Int,
    override val success: Boolean,
) : Response {
    override val command: MessageCommand = MessageCommand.variables

}


@Serializable
data class ThreadsResponse(
    override val seq: Int,
    override val request_seq: Int,
    override val success: Boolean,

    override val body: ThreadsResponseBody,
) : Response {
    override val type: MessageType = super.type
    override val command: MessageCommand
        get() = MessageCommand.threads
}


@Serializable
data class RunInTerminalResponse(
    override val seq: Int,
    override val request_seq: Int,
    override val success: Boolean,
    override val body: RunInTerminalResponseBody? = null
) : Response {
    override val type: MessageType = super.type

    override val command: MessageCommand = MessageCommand.runInTerminal
}

@Serializable
data class DebugInConsoleResponse(
    override val seq: Int, override val request_seq: Int,
    override val success: Boolean,
    override val body: DebugInConsoleResponseBody? = null
) : Response {
    override val type: MessageType = super.type

    override val command: MessageCommand = MessageCommand.debugInConsole
}

@Serializable
data class SetFunctionBreakpointsResponse(
    override val seq: Int,
    override val request_seq: Int,
    override val success: Boolean,
    override val body: SetFunctionBreakpointsResponseBody? = null,
) : Response {
    override val type: MessageType = super.type

    override val command: MessageCommand = MessageCommand.setFunctionBreakpoints
}


@Serializable
data class SetDataBreakpointsResponse(
    override val seq: Int,
    override val request_seq: Int,
    override val success: Boolean,
    override val body: SetDataBreakpointsResponseBody? = null,
) : Response {
    override val type: MessageType = super.type

    override val command: MessageCommand = MessageCommand.setDataBreakpoints
}

@Serializable
data class SetInstructionBreakpointsResponse(
    override val seq: Int,
    override val request_seq: Int,
    override val success: Boolean,
    override val body: SetInstructionBreakpointsResponseBody? = null,
) : Response {
    override val type: MessageType = super.type

    override val command: MessageCommand = MessageCommand.setInstructionBreakpoints
}


@Serializable
data class StackTraceResponse(
    override val seq: Int,
    override val request_seq: Int,
    override val success: Boolean,
    override val body: StackTraceResponseBody? = null,
) : Response {
    override val type: MessageType = super.type

    override val command: MessageCommand = MessageCommand.stackTrace
}

@Serializable
data class ScopesResponse(
    override val seq: Int,
    override val request_seq: Int,
    override val success: Boolean,
    override val body: ScopesResponseBody? = null,
) : Response {
    override val type: MessageType = super.type

    override val command: MessageCommand = MessageCommand.scopes
}

@Serializable
data class NextResponse(
    override val seq: Int,
    override val request_seq: Int,
    override val success: Boolean,

    ) : Response {
    override val type: MessageType = super.type

    override val command: MessageCommand = MessageCommand.next
}

@Serializable
data class StepOutResponse(
    override val seq: Int,
    override val request_seq: Int,
    override val success: Boolean,

    ) : Response {
    override val type: MessageType = super.type

    override val command: MessageCommand = MessageCommand.stepOut
}

@Serializable
data class StepInResponse(
    override val seq: Int,
    override val request_seq: Int,
    override val success: Boolean,

    ) : Response {
    override val type: MessageType = super.type

    override val command: MessageCommand = MessageCommand.stepIn
}

@Serializable
data class ContinueResponse(
    override val seq: Int,
    override val request_seq: Int,
    override val success: Boolean,
    override val body: ContinueResponseBody? = null,
) : Response {
    override val type: MessageType = super.type

    override val command: MessageCommand = MessageCommand.Continue
}

@Serializable
data class DisconnectResponse(

    override val seq: Int,
    override val request_seq: Int,
    override val success: Boolean,
    override val body: Body? = null,
    override val message: ResponseMessage? = null,
) : Response {
    override val command: MessageCommand = MessageCommand.disconnect
    override val type: MessageType = super.type
}

@Serializable
data class EvaluateResponse(

    override val seq: Int,
    override val request_seq: Int,
    override val success: Boolean,
    override val body: EvaluateResponseBody? = null,
    override val message: ResponseMessage? = null,
) : Response {
    override val command: MessageCommand = MessageCommand.evaluate
    override val type: MessageType = super.type
}

/**
 * SetVariableResponse
 */
@Serializable
data class SetVariableResponse(
    override val seq: Int,
    override val request_seq: Int,
    override val success: Boolean,
    override val body: SetVariableResponseBody? = null,
    override val message: ResponseMessage? = null,
) : Response {
    override val type: MessageType = super.type


    override val command: MessageCommand = MessageCommand.setVariable
}

@Serializable
data class SourceResponse(
    override val seq: Int,
    override val request_seq: Int,
    override val success: Boolean,
    override val body: SourceResponseBody? = null,
    override val message: ResponseMessage? = null,
) : Response {
    override val type: MessageType = super.type

    override val command: MessageCommand = MessageCommand.source
}

@Serializable
data class ModulesResponse (
    override val seq: Int,
    override val request_seq: Int,
    override val success: Boolean,
    override val body: ModulesResponseBody? = null,
    override val message: ResponseMessage? = null,
) : Response {
    override val type: MessageType = super.type

    override val command: MessageCommand = MessageCommand.modules
}



