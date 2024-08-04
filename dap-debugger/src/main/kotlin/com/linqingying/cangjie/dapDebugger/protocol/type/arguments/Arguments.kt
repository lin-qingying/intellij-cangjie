package com.linqingying.cangjie.dapDebugger.protocol.type.arguments

import com.linqingying.cangjie.dapDebugger.protocol.type.*
import kotlinx.serialization.Polymorphic
import kotlinx.serialization.Serializable

@Polymorphic
interface Arguments

/**
 * 初始化请求参数
 */
@Serializable
data class InitializeRequestArguments
    (
    /**
     * 使用此适配器的客户端的 ID。
     */
    val clientId: String? = null,
    /**
     * 使用此适配器的客户端的人类可读名称。
     */
    val clientName: String? = null,

    /**
     * 调试适配器的 ID。
     */
    val adapterID: String,

    /**
     * 使用此适配器的客户端的 ISO-639 区域设置，例如 en-US 或 de-CH。
     */
    val locale: String? = null,

    /**
     * 如果为 true，则所有行号都是基于 1 的（默认）。
     */
    val linesStartAt1: Boolean? = null,

    /**
     * 如果为 true，则所有列号都是基于 1 的（默认）。
     */
    val columnsStartAt1: Boolean? = null,
    /**
     * 确定以何种格式指定路径。默认为 `path`，这是原生格式。
     * 值：'path'，'uri' 等。
     */
    val pathFormat: PathFormat? = null,
    /**
     * 客户端支持变量的 `type` 属性。
     */
    val supportsVariableType: Boolean? = null,
    /**
     * 客户端支持变量的分页。
     */
    val supportsVariablePaging: Boolean? = null,
    /**
     * 客户端支持 `runInTerminal` 请求。
     */
    val supportsRunInTerminalRequest: Boolean? = null,

    /**
     * 客户端支持内存引用。
     */
    val supportsMemoryReferences: Boolean? = null,


    /**
     * 客户端支持进度报告。
     */
    val supportsProgressReporting: Boolean? = null,

    /**
     * 客户端支持 `invalidated` 事件。
     */
    val supportsInvalidatedEvent: Boolean? = null,

    /**
     * 客户端支持 `memory` 事件。
     */
    val supportsMemoryEvent: Boolean? = null,

    /**
     * 客户端支持 `runInTerminal` 请求上的 `argsCanBeInterpretedByShell` 属性。
     */
    val supportsArgsCanBeInterpretedByShell: Boolean? = null,

    /**
     * 客户端支持 `startDebugging` 请求。
     */

    val supportsStartDebuggingRequest: Boolean? = null
) : Arguments


/**
 * 取消请求参数
 */
@Serializable
data class CancelArguments(
    /**
     * 要取消的请求的ID（属性`seq`）。如果缺失，则不取消任何请求。
     * 一个请求中可以同时指定`requestId`和`progressId`。
     */
    val requestId: Int? = null,

    /**
     * 要取消的进度的ID（属性`progressId`）。如果缺失，则不取消任何进度。
     * 一个请求中可以同时指定`requestId`和`progressId`。
     */
    val progressId: String? = null
) : Arguments


/**
 * 启动请求参数
 */
@Serializable
data class LaunchRequestArguments(
    val __sessionId: String? = null,

    val __configurationTarget: Int? = null,

    /**
     * 如果为 true，则启动请求应在不启用调试的情况下启动程序。
     */
//    val noDebug: Boolean? = null,

    /**
     * 来自之前重启会话的任意数据。
     * 数据作为 `terminated` 事件的 `restart` 属性发送。
     * 客户端应保持数据的完整性。
     */
//    val __restart: String? = null,

    /**
     * 如果为 true，构建过程将在启动前执行。
     */
    val buildBeforeLaunch: Boolean? = null,

    /**
     * 如果为 true，程序将在外部控制台中启动。
     */
    val externalConsole: Boolean? = null,


    val env: Map<String, String>? = null,

    /**
     * 调试会话的名称。
     */
    val name: String? = null,

    /**
     * 要调试的程序的路径。
     */
    val program: String? = null,
    /**
     * 请求类型，对于启动请求应为 "launch"。
     */
    val request: MessageCommand? = MessageCommand.launch,
    /**
     * 调试会话的类型。
     */
    val type: String? = null,
) : Arguments


@Serializable
data class AttachRequestArguments(
    /**
    来自之前重启会话的任意数据。
    数据作为终止事件的重启属性发送。
    客户端应保持数据的完整性。*/
    val __restart: String? = null,
) : Arguments

@Serializable
data class SetBreakpointsArguments(
    /**
     * 断点的源位置；必须指定 `source.path` 或 `source.sourceReference`。
     */
    val source: Source,

    /**
     * 断点的代码位置。
     */
    val breakpoints: List<SourceBreakpoint>? = null,

    /**
     * 已弃用：断点的代码位置。
     */
    val lines: List<Int>? = null,

    /**
     * 如果为 true，则表示底层源已被修改，从而产生新的断点位置。
     */
    val sourceModified: Boolean? = null
) : Arguments

@Serializable
class ConfigurationDoneArguments : Arguments

@Serializable
data class DisconnectArguments(
    /**
     * 如果为true，表示此`disconnect`请求是重启序列的一部分。
     */
    val restart: Boolean? = null,

    /**
     * 指示当调试器断开连接时，是否应终止被调试的程序。
     * 如果未指定，调试适配器可以自由决定最好的做法。
     * 只有当相应的能力`supportTerminateDebuggee`为true时，才会考虑此属性。
     */
    val terminateDebuggee: Boolean? = null,

    /**
     * 指示当调试器断开连接时，被调试的程序是否应保持挂起状态。
     * 如果未指定，被调试的程序应恢复执行。
     * 只有当相应的能力`supportSuspendDebuggee`为true时，才会考虑此属性。
     */
    val suspendDebuggee: Boolean? = null
) : Arguments


@Serializable
data class VariablesArguments(
    /**
     * 要获取其子项的变量。`variablesReference`
     * 必须在当前挂起状态下获得。有关详细信息，请参阅概述部分中的“对象引用的生命周期”。
     */
    val variablesReference: Int,

    /**
     * 用于限制子变量为命名或索引的过滤器。如果省略，
     * 则两种类型都将被获取。
     * 值：'indexed', 'named'
     */
    val filter: VariablesArgumentsFilter? = null,

    /**
     * 返回的第一个变量的索引；如果省略，则子项从0开始。
     * 只有当相应的能力`supportsVariablePaging`为真时，才会考虑此属性。
     */
    val start: Int? = null,

    /**
     * 要返回的变量数量。如果count缺失或为0，则返回所有变量。
     * 只有当相应的能力`supportsVariablePaging`为真时，才会考虑此属性。
     */
    val count: Int? = null,

    /**
     * 指定如何格式化变量值的详细信息。
     * 只有当相应的能力`supportsValueFormattingOptions`为真时，才会考虑此属性。
     */
    val format: ValueFormat? = null
) : Arguments


@Serializable
data class StackTraceArguments(
    /**
     * 获取此线程的堆栈跟踪。
     */
    val threadId: Long,

    /**
     * 返回的第一个帧的索引；如果省略，则帧从0开始。
     */
    val startFrame: Int? = null,

    /**
     * 要返回的帧的最大数量。如果未指定 levels 或为0，则返回所有帧。
     */
    val levels: Int? = null,

    /**
     * 指定如何格式化堆栈帧的详细信息。
     * 只有当相应的能力`supportsValueFormattingOptions`为真时，调试适配器才会考虑此属性。
     */
    val format: StackFrameFormat? = null
) : Arguments


@Serializable
data class SetVariableArguments(
    /**
     * 变量容器的引用。`variablesReference`必须在当前挂起状态下获得。详情请参见概述部分的'对象引用的生命周期'。
     */
    val variablesReference: Int,

    /**
     * 容器中变量的名称。
     */
    val name: String,

    /**
     * 变量的值。
     */
    val value: String,

    /**
     * 指定如何格式化响应值的详细信息。
     */
    val format: ValueFormat? = null
) : Arguments


@Serializable
data class RunInTerminalRequestArguments(


    /**
     * 启动的终端类型。如果未指定，默认为 `integrated`。
     * 可选值：'integrated', 'external'
     */
    val kind: RunInTerminalRequestArgumentsKind? = null,

    /**
     * 终端的标题。
     */
    val title: String? = null,

    /**
     * 命令的工作目录。对于非空的有效路径，这通常会执行更改目录命令。
     */
    val cwd: String? = null,

    /**
     * 参数列表。第一个参数是要运行的命令。
     */
    val args: List<String>? = null,

    /**
     * 环境键值对，将被添加到或从默认环境中删除。
     */
    val env: Map<String, String?>? = null,

    /**
     * 只有当相应的功能 `supportsArgsCanBeInterpretedByShell` 为 true 时，才应设置此属性。
     * 如果客户端使用中间 shell 来启动应用程序，那么客户端不必尝试转义 shell 的特殊含义字符。
     * 用户完全负责根据需要进行转义，使用特殊字符的参数可能无法在 shell 之间移植。
     */
    val argsCanBeInterpretedByShell: Boolean? = null

) : Arguments


@Serializable
data class DebugInConsoleRequestArguments(
    /**
     *  要在控制台中执行的代码。
     */
    val debugCommand: String? = null,

    ) : Arguments


@Serializable
data class SetFunctionBreakpointsArguments(
    /**
     * 要设置的断点。
     */
    val breakpoints: List<FunctionBreakpoint>? = null
) : Arguments

/**
 * 数据断点
 * 传递给请求的数据断点的属性。setDataBreakpoints
 */
/**
 * 数据断点
 */
@Serializable
data class DataBreakpoint(
    /**
     * 表示数据的id。此id从`dataBreakpointInfo`请求返回。
     */
    val dataId: String,

    /**
     * 数据的访问类型。
     */
    val accessType: DataBreakpointAccessType? = null,

    /**
     * 条件断点的表达式。
     */
    val condition: String? = null,

    /**
     * 控制忽略断点命中次数的表达式。
     * 预期调试适配器根据需要解释表达式。
     */
    val hitCondition: String? = null
)


@Serializable
data class SetDataBreakpointsArguments(
    /**
     * 要设置的数据断点。
     */
    val breakpoints: List<DataBreakpoint>? = null
) : Arguments


@Serializable
data class SetInstructionBreakpointsArguments(
    /**
     * 要设置的指令断点。
     */
    val breakpoints: List<InstructionBreakpoint>? = null
) : Arguments


@Serializable
data class ScopesArguments(
    /**
     * 通过 `frameId` 获取堆栈帧的作用域。
     * `frameId` 必须在当前的暂停状态下获取。有关详细信息，请参阅概述部分的 '对象引用的生命周期'。
     */
    val frameId: Int
) : Arguments


@Serializable
data class NextArguments(
    /**
     * 指定要为其恢复执行一步（给定粒度）的线程。
     */
    val threadId: Long,

    /**
     * 如果此标志为 true，则不会恢复所有其他挂起的线程。
     */
    val singleThread: Boolean? = null,

    /**
     * 步进粒度。如果未指定粒度，则假定粒度为 `statement`。
     */
    val granularity: SteppingGranularity? = null
) : Arguments

@Serializable
data class StepOutArguments(
    /**
     * 指定要为其恢复执行一步（给定粒度）的线程。
     */
    val threadId: Long,

    /**
     * 如果此标志为 true，则不会恢复所有其他挂起的线程。
     */
    val singleThread: Boolean? = null,

    /**
     * 步进粒度。如果未指定粒度，则假定粒度为 `statement`。
     */
    val granularity: SteppingGranularity? = null
) : Arguments

@Serializable
data class StepInArguments(
    /**
     * 指定要为其恢复执行一步（给定粒度）的线程。
     */
    val threadId: Long,

    /**
     * 如果此标志为 true，则不会恢复所有其他挂起的线程。
     */
    val singleThread: Boolean? = null,

    /**
     * 步进粒度。如果未指定粒度，则假定粒度为 `statement`。
     */
    val granularity: SteppingGranularity? = null
) : Arguments


/**
 * 继续执行的参数
 */
@Serializable
data class ContinueArguments(
    /**
     * 指定活动线程。如果调试适配器支持单线程执行（参见 `supportsSingleThreadExecutionRequests`）并且参数 `singleThread` 为 true，
     * 则只有此 ID 的线程会被恢复执行。
     */
    val threadId: Int,

    /**
     * 如果此标志为 true，只有给定 `threadId` 的线程会恢复执行。
     */
    val singleThread: Boolean? = null
) : Arguments


/**
 * EvaluateArguments
 */
@Serializable
data class EvaluateArguments(
    /**
     * 需要计算的表达式。
     */
    val expression: String,

    /**
     * 在此堆栈帧的范围内计算表达式。如果未指定，
     * 则在全局范围内计算表达式。
     */
    val frameId: Int? = null,

    /**
     * 使用evaluate请求的上下文。
     * 可能的值：
     * 'watch': 从watch视图上下文中调用evaluate。
     * 'repl': 从REPL上下文中调用evaluate。
     * 'hover': 调用evaluate生成debug悬停内容。
     * 只有在相应的能力`supportsEvaluateForHovers`为true时，才应使用此值。
     * 'clipboard': 调用evaluate生成剪贴板内容。
     * 只有在相应的能力`supportsClipboardContext`为true时，才应使用此值。
     * 'variables': 从variables视图上下文中调用evaluate。
     * 等等。
     */
    val context: EvaluateArgumentsContext? = null,

    /**
     * 指定如何格式化结果的详细信息。
     * 只有在相应的能力`supportsValueFormattingOptions`为true时，
     * 才会由debug适配器尊重此属性。
     */
    val format: ValueFormat? = null
) : Arguments


@Serializable
data class SourceArguments(
    /**
     * 指定要加载的源内容。必须指定`source.path`或`source.sourceReference`之一。
     */
    val source: Source? = null,

    /**
     * 源的引用。与`source.sourceReference`相同。
     * 由于旧客户端不理解`source`属性，因此提供此属性以实现向后兼容性。
     */
    val sourceReference: Int
) : Arguments

@Serializable
data class ModulesArguments(
    val startModule: Int? = null, val moduleCount: Int? = null
) : Arguments
