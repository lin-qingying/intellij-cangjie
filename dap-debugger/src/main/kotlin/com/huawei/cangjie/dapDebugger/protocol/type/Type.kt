package com.linqingying.cangjie.dapDebugger.protocol.type

import com.linqingying.cangjie.dapDebugger.protocol.type.serializer.*
import kotlinx.serialization.Contextual
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable


@Serializable
enum class MessageCommand {
    modules,
    initialize,
    launch,
    attach,
    cancel,
    runInTerminal,
    setBreakpoints,
    configurationDone,
    disconnect,
    variables,
    threads,
    stackTrace,
    setVariable,
    scopes,
    debugInConsole,
    evaluate,
    source,

    setFunctionBreakpoints,
    setDataBreakpoints,
    setInstructionBreakpoints,

    //步骤
    @SerialName("continue")
    Continue,
    next,
    stepIn,
    stepOut,
    pause,

}


@Serializable
enum class EventType {
    thread,
    loadedSource,
    breakpoint,
    output,
    module,
    initialized,
    stopped,
    terminated,
    exited,
    continued
}


@Serializable
sealed class ResponseMessage {
    data object cancelled : ResponseMessage()
    data object notStopped : ResponseMessage()
    data class Other(val value: String) : ResponseMessage() {
        override fun toString(): String {
            return value
        }
    }

    override fun toString(): String {
        return when (this) {
            is cancelled -> "cancelled"
            is notStopped -> "notStopped"
            is Other -> value
        }
    }
}


@Serializable
sealed class Reason {
    data object changed : Reason()
    data object new : Reason()
    data object removed : Reason()
    data class Other(val value: String) : Reason()
}


/**
 * 校验和算法
 */
@Serializable
enum class ChecksumAlgorithm {
    /**
     * MD5 校验和算法。
     */
    MD5,

    /**
     * SHA1 校验和算法。
     */
    SHA1,

    /**
     * SHA256 校验和算法。
     */
    SHA256,

    /**
     * timestamp 校验和算法。
     */
    timestamp
}


/**
 * PathFormat
 */
@Serializable(PathFormatSerializer::class)
sealed class PathFormat {
    @Serializable
    data object Path : PathFormat()

    @Serializable
    data object Uri : PathFormat()

    @Serializable
    data class Other(val value: String) : PathFormat()
}


/**
 * 消息类型 可能为 request response event，或其他自定义类型
 */
@Serializable(MessageTypeSerializer::class)
sealed class MessageType {
    @Serializable
    data object request : MessageType()

    @Serializable
    data object response : MessageType()

    @Serializable
    data object event : MessageType()

    @Serializable
    data class Other(val value: String) : MessageType()
}


/**
 * 在 UI 中显示为筛选器选项，用于配置如何处理异常。
 */
@Serializable
data class ExceptionBreakpointsFilter(
    /**
     * 过滤选项的内部 ID。此值传递给 `setExceptionBreakpoints` 请求。
     */
    val filter: String,

    /**
     * 过滤选项的名称。这将在 UI 中显示。
     */
    val label: String,

    /**
     * 提供有关异常过滤器的附加信息的帮助文本。
     * 该字符串通常显示为悬停，并可以翻译。
     */
    val description: String? = null,

    /**
     * 过滤选项的初始值。如果未指定，则假定值为 false。
     */
    val default: Boolean? = null,

    /**
     * 控制是否可以为此过滤选项指定条件。如果为 false 或缺失，则不能设置条件。
     */
    val supportsCondition: Boolean? = null,

    /**
     * 提供有关条件的信息的帮助文本。该字符串显示为文本框的占位符文本，并可以翻译。
     */
    val conditionDescription: String? = null
)


/**
 * 列数据类型
 */
@Serializable(ColumnDataTypeSerializer::class)
sealed class ColumnDataType {
    @Serializable
    data object string : ColumnDataType()

    @Serializable
    data object number : ColumnDataType()

    @Serializable
    data object boolean : ColumnDataType()

    @Serializable
    data object usnixTimestampUTC : ColumnDataType()

    @Serializable
    data class Other(val value: String) : ColumnDataType()
}

/**
 * 列描述符
 */
@Serializable
data class ColumnDescriptor(
    /**
     * 在此列中渲染的属性的名称。
     */
    val attributeName: String,

    /**
     * 列的头部 UI 标签。
     */
    val label: String,

    /**
     * 用于在此列中渲染值的格式。TBD 格式字符串的样式。
     */
    val format: String? = null,

    /**
     * 此列中值的数据类型。如果未指定，默认为 String`。
     * 值：'String'，'number'，'Boolean'，'unixTimestampUTC'
     */
    val type: ColumnDataType? = null,

    /**
     * 此列的宽度（仅为提示）。
     */
    val width: Int? = null
)

/**
 * 源断点
 */
@Serializable
data class SourceBreakpoint(
    /**
     * 断点或日志点的源代码行。
     */
    val line: Int,

    /**
     * 断点或日志点在源代码行内的起始位置。它以UTF-16代码单元为单位进行测量，
     * 客户端能力 `columnsStartAt1` 决定它是基于0还是基于1。
     */
    val column: Int? = null,

    /**
     * 条件断点的表达式。
     * 只有当相应的能力 `supportsConditionalBreakpoints` 为真时，调试适配器才会考虑它。
     */
    val condition: String? = null,

    /**
     * 控制忽略断点命中次数的表达式。
     * 预期调试适配器根据需要解释表达式。
     * 只有当相应的能力 `supportsHitConditionalBreakpoints` 为真时，该属性才会被调试适配器考虑。
     * 如果指定了此属性和 `condition`，则应仅在满足 `condition` 时评估 `hitCondition`，
     * 并且调试适配器应仅在满足两个条件时停止。
     */
    val hitCondition: String? = null,

    /**
     * 如果此属性存在且非空，则调试适配器必须不'break'（停止）
     * 而是记录消息。`{}` 内的表达式将被插值。
     * 只有当相应的能力 `supportsLogPoints` 为真时，调试适配器才会考虑该属性。
     * 如果指定了 `hitCondition` 或 `condition`，则应仅在满足这些条件时记录消息。
     */
    val logMessage: String? = null
)


@Serializable
enum class PresentationHint {
    normal, emphasize, deemphasize
}


@Serializable
data class Checksum(
    /**
     * 用于计算此校验和的算法。
     */
    val algorithm: ChecksumAlgorithm,

    /**
     * 校验和的值，编码为十六进制值。
     */
    val checksum: String
)

/**
 * 源
 */
@Serializable
data class Source(
    /**
     * 源的简短名称。从调试适配器返回的每个源都有一个名称。
     * 向调试适配器发送源时，此名称是可选的。
     */
    val name: String? = null,

    /**
     * 要在 UI 中显示的源的路径。
     * 只有在未指定 `sourceReference`（或其值为 0）时，才用于定位和加载源的内容。
     */
    val path: String? = null,

    /**
     * 如果值 > 0，则必须通过 `source` 请求检索源的内容（即使指定了路径）。
     * 由于 `sourceReference` 仅对会话有效，因此不能用于持久化源。
     * 该值应小于或等于 2147483647 (2^31-1)。
     */
    val sourceReference: Int? = null,

    /**
     * 如何在 UI 中呈现源的提示。
     * 可以使用 `deemphasize` 值来表示源不可用或在步进时跳过。
     * 值：'normal', 'emphasize', 'deemphasize'
     */
    val presentationHint: PresentationHint? = null,

    /**
     * 此源的起源。例如，'internal module', 'inlined content from source map' 等。
     */
    val origin: String? = null,

    /**
     * 与此源相关的源列表。这可能是生成此源的源。
     */
    val sources: List<Source>? = null,

    /**
     * 调试适配器可能希望通过客户端循环的附加数据。
     * 客户端应保持数据完整，并在会话之间保持持久性。客户端不应解释数据。
     */
    @Contextual
    val adapterData: Any? = null,

    /**
     * 与此文件关联的校验和。
     */
    val checksums: List<Checksum>? = null
)


@Serializable
data class Breakpoint(
    /**
     * 断点的标识符。如果使用断点事件来更新或删除断点，则需要它。
     */
    val id: Int? = null,

    /**
     * 如果为true，断点可以设置（但不一定在期望的位置）。
     */
    val verified: Boolean,

    /**
     * 关于断点状态的消息。
     * 这将显示给用户，并可以用来解释为什么断点无法验证。
     */
    val message: String? = null,

    /**
     * 断点所在的源。
     */
    val source: Source? = null,

    /**
     * 实际范围的起始行，该范围由断点覆盖。
     */
    val line: Int? = null,

    /**
     * 断点覆盖的源范围的起始位置。它以UTF-16代码单元为单位进行测量，客户端能力`columnsStartAt1`决定它是基于0还是基于1。
     */
    val column: Int? = null,

    /**
     * 实际范围的结束行，该范围由断点覆盖。
     */
    val endLine: Int? = null,

    /**
     * 断点覆盖的源范围的结束位置。它以UTF-16代码单元为单位进行测量，客户端能力`columnsStartAt1`决定它是基于0还是基于1。
     * 如果没有给出结束行，则假定结束列在开始行中。
     */
    val endColumn: Int? = null,

    /**
     * 设置断点的内存引用。
     */
    val instructionReference: String? = null,

    /**
     * 从指令引用的偏移量。
     * 这可以是负数。
     */
    val offset: Int? = null,

    /**
     * 为什么断点可能无法验证的机器可读解释。如果断点已验证或未知具体原因，适配器应省略此属性。可能的值包括：
     *
     * - `pending`：表示断点可能在将来被验证，但适配器在当前状态下无法验证它。
     * - `failed`：表示无法验证断点，并且适配器认为在没有干预的情况下无法验证它。
     * 值：'pending'，'failed'
     */
    val reason: String? = null


) {

    var condition: String? = null
}


@Serializable(EventReasonSerializer::class)
sealed class EventReason {
    @Serializable
    data object changed : EventReason()

    @Serializable
    data object new : EventReason()

    @Serializable
    data object removed : EventReason()

    @Serializable
    data class Other(val value: String) : EventReason()
}


@Serializable(CategorySerializer::class)
sealed class Category {
    @Serializable
    data object console : Category()

    @Serializable
    data object important : Category()

    @Serializable
    data object stdout : Category()

    @Serializable
    data object stderr : Category()

    @Serializable
    data object telemetry : Category()

    @Serializable
    data class Other(val value: String) : Category()
}

@Serializable
enum class Group {
    start,
    startCollapsed,
    end
}

@Serializable(ThreadEventReasonSerializer::class)
sealed class ThreadEventReason {
    @Serializable
    data object started : ThreadEventReason()

    @Serializable
    data object exited : ThreadEventReason()

    @Serializable
    data class Other(val value: String) : ThreadEventReason()
}


@Serializable
enum class VariablesArgumentsFilter {
    indexed,
    named
}


/**
 * 值格式
 */
@Serializable
open class ValueFormat(
    /**
     * 以十六进制显示值。
     */
    @Serializable
    open val hex: Boolean? = null
)

@Serializable
data class Variable(
    /**
     * 变量的名称。
     */
    val name: String,

    /**
     * 变量的值。
     * 这可以是多行文本，例如对于函数，是函数的主体。
     * 对于结构化变量（没有简单值的变量），建议提供结构化对象的一行表示。
     * 这有助于在折叠状态下识别结构化对象，当其子对象还不可见时。
     * 如果不应在UI中显示任何值，可以使用空字符串。
     */
    var value: String,

    /**
     * 变量值的类型。通常在悬停在值上时在UI中显示。
     * 只有当相应的能力`supportsVariableType`为真时，调试适配器才应返回此属性。
     */
    var type: String? = null,

    /**
     * 可用于确定如何在UI中呈现变量的变量的属性。
     */
    val presentationHint: VariablePresentationHint? = null,

    /**
     * 此变量的可评估名称，可以传递给`evaluate`请求以获取变量的值。
     */
    val evaluateName: String? = null,

    /**
     * 如果`variablesReference` > 0，变量是结构化的，只要执行保持挂起，就可以通过将`variablesReference`传递给`variables`请求来获取其子项。有关详细信息，请参阅概述部分中的“对象引用的生命周期”。
     */
    val variablesReference: Int,

    val declaredLine: Int? = null,

    val declaredSource: Source? = null,

    val memoryAddress: String? = null,

    val isVarCreated: Boolean? = null,
    /**
     * 命名子变量的数量。
     * 客户端可以使用此信息在分页UI中呈现子项，并分块获取它们。
     */
    val namedVariables: Int? = null,

    /**
     * 索引子变量的数量。
     * 客户端可以使用此信息在分页UI中呈现子项，并分块获取它们。
     */
    val indexedVariables: Int? = null,

    /**
     * 与此变量关联的内存引用。
     * 对于指针类型的变量，这通常是指针中包含的内存地址的引用。
     * 对于可执行数据，此引用可能稍后在`disassemble`请求中使用。
     * 如果相应的能力`supportsMemoryReferences`为真，调试适配器可能会返回此属性。
     */
    val memoryReference: String? = null
)


/**
 * 变量的属性，可用于确定如何在 UI 中呈现变量。
 */
@Serializable
data class VariablePresentationHint(
    /**
     * 变量的种类。在引入其他值之前，尽量使用列出的值。
     * 值：
     * 'property': 表示对象是属性。
     * 'method': 表示对象是方法。
     * 'class': 表示对象是类。
     * 'data': 表示对象是数据。
     * 'event': 表示对象是事件。
     * 'baseClass': 表示对象是基类。
     * 'innerClass': 表示对象是内部类。
     * 'interface': 表示对象是接口。
     * 'mostDerivedClass': 表示对象是最派生的类。
     * 'virtual': 表示对象是虚拟的，也就是说，它是适配器为了渲染目的引入的合成对象，例如，大数组的索引范围。
     * 'dataBreakpoint': 已弃用：表示为对象注册了数据断点。通常应使用`hasDataBreakpoint`属性代替。
     * 等等。
     */
    val kind: String? = null,

    /**
     * 一组表示为字符串数组的属性。在引入其他值之前，尽量使用列出的值。
     * 值：
     * 'static': 表示对象是静态的。
     * 'constant': 表示对象是常量。
     * 'readOnly': 表示对象是只读的。
     * 'rawString': 表示对象是原始字符串。
     * 'hasObjectId': 表示对象可以为其创建对象ID。这是一些客户端使用的遗留属性；'对象ID'在协议中没有指定。
     * 'canHaveObjectId': 表示对象有一个与之关联的对象ID。这是一些客户端使用的遗留属性；'对象ID'在协议中没有指定。
     * 'hasSideEffects': 表示评估有副作用。
     * 'hasDataBreakpoint': 表示对象的值由数据断点跟踪。
     * 等等。
     */
    val attributes: List<String>? = null,

    /**
     * 变量的可见性。在引入其他值之前，尽量使用列出的值。
     * 值：'public', 'private', 'protected', 'internal', 'final'等等。
     */
    val visibility: String? = null,

    /**
     * 如果为true，客户端可以使用支持特定手势触发其评估的UI来呈现变量。
     * 这种机制可用于需要在获取其值时执行代码的属性，其中代码执行可能会昂贵和/或产生副作用。一个典型的例子是基于getter函数的属性。
     * 请注意，除了`lazy`标志外，变量的`variablesReference`应该引用一个将通过另一个`variable`请求提供值的变量。
     */
    val lazy: Boolean? = null
)

@Serializable(VariablePresentationHintKindSerializer::class)
sealed class VariablePresentationHintKind {
    @Serializable
    data object Property : VariablePresentationHintKind()

    @Serializable
    data object Method : VariablePresentationHintKind()

    @Serializable
    data object Class : VariablePresentationHintKind()

    @Serializable
    data object Data : VariablePresentationHintKind()

    @Serializable
    data object Event : VariablePresentationHintKind()

    @Serializable
    data object BaseClass : VariablePresentationHintKind()

    @Serializable
    data object InnerClass : VariablePresentationHintKind()

    @Serializable
    data object Interface : VariablePresentationHintKind()

    @Serializable
    data object MostDerivedClass : VariablePresentationHintKind()

    @Serializable
    data object Virtual : VariablePresentationHintKind()

    @Serializable
    data object DataBreakpoint : VariablePresentationHintKind()

    @Serializable
    data class Other(val value: String) : VariablePresentationHintKind()
}

@Serializable(VariablePresentationHintAttributesSerializer::class)
sealed class VariablePresentationHintAttributes {
    data object Static : VariablePresentationHintAttributes()
    data object Constant : VariablePresentationHintAttributes()
    data object ReadOnly : VariablePresentationHintAttributes()
    data object RawString : VariablePresentationHintAttributes()
    data object HasObjectId : VariablePresentationHintAttributes()
    data object CanHaveObjectId : VariablePresentationHintAttributes()
    data object HasSideEffects : VariablePresentationHintAttributes()
    data object HasDataBreakpoint : VariablePresentationHintAttributes()
    data class Other(val value: String) : VariablePresentationHintAttributes()
}

@Serializable(VariablePresentationHintVisibilitySerializer::class)
sealed class VariablePresentationHintVisibility {
    data object Public : VariablePresentationHintVisibility()
    data object Private : VariablePresentationHintVisibility()
    data object Protected : VariablePresentationHintVisibility()
    data object Internal : VariablePresentationHintVisibility()
    data object Final : VariablePresentationHintVisibility()
    data class Other(val value: String) : VariablePresentationHintVisibility()
}

@Serializable(StoppedEventReasonSerializer::class)
sealed class StoppedEventReason {
    data object Step : StoppedEventReason()
    data object Breakpoint : StoppedEventReason()
    data object Exception : StoppedEventReason()
    data object Pause : StoppedEventReason()
    data object Entry : StoppedEventReason()
    data object Goto : StoppedEventReason()
    data object FunctionBreakpoint : StoppedEventReason()
    data object DataBreakpoint : StoppedEventReason()
    data object InstructionBreakpoint : StoppedEventReason()
    data class Other(val value: String) : StoppedEventReason()
}


@Serializable
data class Thread(
    /**
     * 线程的唯一标识符。
     */
    val id: Long,

    /**
     * 线程的名称。
     */
    val name: String,


    )

@Serializable
data class StackFrameFormat(
    /**
     * 显示堆栈帧的参数。
     */
    val parameters: Boolean? = null,

    /**
     * 显示堆栈帧参数的类型。
     */
    val parameterTypes: Boolean? = null,

    /**
     * 显示堆栈帧参数的名称。
     */
    val parameterNames: Boolean? = null,

    /**
     * 显示堆栈帧参数的值。
     */
    val parameterValues: Boolean? = null,

    /**
     * 显示堆栈帧的行号。
     */
    val line: Boolean? = null,

    /**
     * 显示堆栈帧的模块。
     */
    val module: Boolean? = null,

    /**
     * 包括所有堆栈帧，包括调试适配器可能会隐藏的那些。
     */
    val includeAll: Boolean? = null,

    ) : ValueFormat()


@Serializable
enum class RunInTerminalRequestArgumentsKind {
    /**
     * 运行终端并将其附加到调试器。
     */
    integrated,

    /**
     * 运行终端并将其附加到调试器。
     */
    external
}

@Serializable
/**
 * 函数断点
 */
data class FunctionBreakpoint(
    /**
     * 函数的名称。
     */
    val name: String,

    /**
     * 条件断点的表达式。
     * 只有当相应的能力 `supportsConditionalBreakpoints` 为真时，调试适配器才会考虑它。
     */
    val condition: String? = null,

    /**
     * 控制忽略断点命中次数的表达式。
     * 预期调试适配器根据需要解释表达式。
     * 只有当相应的能力 `supportsHitConditionalBreakpoints` 为真时，该属性才会被调试适配器考虑。
     */
    val hitCondition: String? = null
)

@Serializable
enum class DataBreakpointAccessType {
    read,
    write,
    readWrite
}

/**
 * 指令断点
 * 传递给请求的断点的属性setInstructionBreakpoints
 */

@Serializable
data class InstructionBreakpoint(
    /**
     * 断点的指令引用。
     * 这应该是来自 `EvaluateResponse`、`Variable`、`StackFrame`、`GotoTarget` 或
     * `Breakpoint` 的内存或指令指针引用。
     */
    val instructionReference: String,

    /**
     * 从指令引用的偏移量（以字节为单位）。
     * 这可以是负数。
     */
    val offset: Int? = null,

    /**
     * 条件断点的表达式。
     * 只有当相应的能力 `supportsConditionalBreakpoints` 为真时，调试适配器才会考虑它。
     */
    val condition: String? = null,

    /**
     * 控制忽略断点命中次数的表达式。
     * 预期调试适配器根据需要解释表达式。
     * 只有当相应的能力 `supportsHitConditionalBreakpoints` 为真时，该属性才会被调试适配器考虑。
     */
    val hitCondition: String? = null
)


@Serializable
data class StackFrame(
    /**
     * 堆栈帧的标识符。它必须在所有线程中唯一。
     * 可以使用此id来使用`scopes`请求检索帧的范围，
     * 或者重新启动堆栈帧的执行。
     */
    val id: Int,

    /**
     * 堆栈帧的名称，通常是方法名称。
     */
    val name: String,

    /**
     * 帧的源。
     */
    val source: Source? = null,

    /**
     * 帧在源中的行。如果源属性丢失或不存在，
     * `line`为0，客户端应忽略它。
     */
    val line: Int,

    /**
     * 堆栈帧覆盖的范围的起始位置。它以UTF-16代码单元进行测量，
     * 客户端能力`columnsStartAt1`决定它是基于0还是基于1。
     * 如果属性`source`丢失或不存在，`column`为0，客户端应忽略它。
     */
    val column: Int,

    /**
     * 堆栈帧覆盖的范围的结束行。
     */
    val endLine: Int? = null,

    /**
     * 堆栈帧覆盖的范围的结束位置。它以UTF-16代码单元进行测量，
     * 客户端能力`columnsStartAt1`决定它是基于0还是基于1。
     */
    val endColumn: Int? = null,

    /**
     * 表示是否可以使用`restart`请求重新启动此帧。
     * 客户端只应在调试适配器支持`restart`请求并且相应的能力
     * `supportsRestartRequest`为true时使用此属性。
     * 如果调试适配器具有此能力，则如果属性不存在，`canRestart`默认为`true`。
     */
    val canRestart: Boolean? = null,

    /**
     * 此帧中当前指令指针的内存引用。
     */
    val instructionPointerReference: String? = null,

    /**
     * 与此帧关联的模块（如果有）。
     */
    val moduleId: String? = null,

    /**
     * 如何在UI中呈现此帧的提示。
     * `label`的值可以用来表示帧是用作视觉标签或分隔符的人工帧。
     * `subtle`的值可以用来以“微妙”的方式改变帧的外观。
     * 值：'normal', 'label', 'subtle'
     */
    val presentationHint: StackFramePresentationHint? = null
)


@Serializable
enum class StackFramePresentationHint {
    normal, label, subtle
}

@Serializable(ScopePresentationHintSerializer::class)
sealed class ScopePresentationHint {
    data object Arguments : ScopePresentationHint()
    data object Locals : ScopePresentationHint()
    data object Registers : ScopePresentationHint()
    data object This : ScopePresentationHint()
    data object Statics : ScopePresentationHint()
    data object Globals : ScopePresentationHint()
    data object All : ScopePresentationHint()
    data class Other(val value: String) : ScopePresentationHint()

}

/**
 * 范围
 * A 是变量的命名容器。（可选）作用域可以映射到源或源中的区域。Scope
 */
@Serializable
data class Scope(
    /**
     * 作用域的名称，如 'Arguments', 'Locals', 或 'Registers'。此字符串将按原样显示在 UI 中，并可以翻译。
     */
    val name: String,

    /**
     * 如何在 UI 中呈现此作用域的提示。如果此属性缺失，将使用通用的 UI 显示作用域。
     * 值：
     * 'arguments': 作用域包含方法参数。
     * 'locals': 作用域包含局部变量。
     * 'registers': 作用域包含寄存器。只有一个 `registers` 作用域应该从 `scopes` 请求返回。
     * 等等。
     */
    val presentationHint: ScopePresentationHint? = null,

    /**
     * 可以通过将 `variablesReference` 的值传递给 `variables` 请求来获取此作用域的变量，只要执行保持挂起。有关详细信息，请参阅概述部分的“对象引用的生命周期”。
     */
    val variablesReference: Int,

    /**
     * 此作用域中命名变量的数量。
     * 客户端可以使用此信息在分页 UI 中呈现变量，并分块获取它们。
     */
    val namedVariables: Int? = null,

    /**
     * 此作用域中索引变量的数量。
     * 客户端可以使用此信息在分页 UI 中呈现变量，并分块获取它们。
     */
    val indexedVariables: Int? = null,

    /**
     * 如果为 true，此作用域中的变量数量较大或获取代价较高。
     */
    val expensive: Boolean,

    /**
     * 此作用域的源。
     */
    val source: Source? = null,

    /**
     * 此作用域覆盖的范围的起始行。
     */
    val line: Int? = null,

    /**
     * 作用域覆盖的范围的起始位置。它以UTF-16代码单元进行测量，客户端能力`columnsStartAt1`决定它是基于0还是基于1。
     */
    val column: Int? = null,

    /**
     * 此作用域覆盖的范围的结束行。
     */
    val endLine: Int? = null,

    /**
     * 作用域覆盖的范围的结束位置。它以UTF-16代码单元进行测量，客户端能力`columnsStartAt1`决定它是基于0还是基于1。
     */
    val endColumn: Int? = null
)

/**
 * 步进粒度
 * 单步执行请求 、 、 和 中一个“步骤”的粒度。 值：nextstepInstepOutstepBack
 *
 * “statement”：该步骤应允许程序运行，直到当前语句完成执行。 语句的含义由适配器确定，可以认为它等同于一行。 例如，'for（int i = 0, i < 10, i++）' 可以认为有 3 个语句 'int i = 0'， 'i < 10' 和 'i++'。
 * “line”：该步骤应允许程序运行，直到当前源代码行执行完毕。
 * “instruction”：该步骤应允许执行一条指令（例如一条 x86 指令）。
 */
@Serializable
enum class SteppingGranularity {
    statement,
    line,
    instruction
}

@Serializable(EvaluateArgumentsContextSerializer::class)
sealed class EvaluateArgumentsContext {
    data object Watch : EvaluateArgumentsContext()
    data object Hover : EvaluateArgumentsContext()
    data object Repl : EvaluateArgumentsContext()
    data object Clipboard : EvaluateArgumentsContext()
    data object Variables : EvaluateArgumentsContext()
    data class Other(val value: String) : EvaluateArgumentsContext()
}


/**
 * 模块
 * Module 对象表示模块视图中的一行。
 *
 * 该属性标识模块视图中的模块，并在事件中用于标识要添加、更新或删除的模块。idmodule
 *
 * 该属性用于在 UI 中以最小方式呈现模块。name
 *
 * 可以将其他属性添加到模块中。如果它们具有相应的 .ColumnDescriptor
 *
 * 为了避免语义相似但名称不同的其他属性的不必要扩散，我们建议首先重用下面“推荐”列表中的属性，只有在找不到合适的属性时才引入新属性。
 */
@Serializable
data class Module(

    /**
     * 模块的唯一标识符
     */
    val id: String,

    /**
     * 模块名称
     */

    val name: String,

    /**
     * Logical full path to the module. The exact definition is implementation
     * defined, but usually this would be a full path to the on-disk file for the
     * module.
     */
    val path: String? = null,

    /**
     * True if the module is optimized.
     */
    val isOptimized: Boolean? = null,

    /**
     * True if the module is considered 'user code' by a debugger that supports
     * 'Just My Code'.
     */
    val isUserCode: Boolean? = null,

    /**
     * Version of Module.
     */
    val version: String? = null,

    /**
     * User-understandable description of if symbols were found for the module
     * (ex: 'Symbols Loaded', 'Symbols not found', etc.)
     */
    val symbolStatus: String? = null,

    /**
     * Logical full path to the symbol file. The exact definition is
     * implementation defined.
     */
    val symbolFilePath: String? = null,

    /**
     * Module created or modified, encoded as a RFC 3339 timestamp.
     */
    val dateTimeStamp: String? = null,

    /**
     * Address range covered by this module.
     */
    val addressRange: String? = null,
)
