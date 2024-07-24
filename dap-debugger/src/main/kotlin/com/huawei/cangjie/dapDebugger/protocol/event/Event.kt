package com.huawei.cangjie.dapDebugger.protocol.event



import com.huawei.cangjie.dapDebugger.protocol.ProtocolMessage
import com.huawei.cangjie.dapDebugger.protocol.type.EventType
import com.huawei.cangjie.dapDebugger.protocol.type.MessageType
import com.huawei.cangjie.dapDebugger.protocol.type.body.*


import kotlinx.serialization.Polymorphic
import kotlinx.serialization.Serializable

/**
 * 事件
 */
@Polymorphic
interface Event : ProtocolMessage {
//    @SerialName("type")
//    override val type: MessageType get() = MessageType.event

    /**
     * 事件类型
     */
    val event: EventType

    /**
     * 包好事件参数的对象
     */
    val body: Body? get() = null

    override val type: MessageType
        get() = MessageType.event
}


/**
 * 此事件表示调试适配器已准备好接受配置请求（
 */
@Serializable
data class InitializedEvent(
    override val seq: Int,
    override val event: EventType = EventType.initialized,

    ) : Event {
    override val type: MessageType = super.type
}

/**
 * 该事件指示目标已生成一些输出。
 */
@Serializable
data class OutputEvent(
    override val seq: Int,
    override val event: EventType = EventType.output,
    override val body: OutPutEventBody? = null,
) : Event {
    override val type: MessageType = super.type
}

@Serializable
data class BreakpointEvent(
    override val seq: Int,

    override val body: BreakpointEventBody? = null,
) : Event {
    override val event: EventType = EventType.breakpoint
    override val type: MessageType = super.type
}


@Serializable
data class ThreadEvent(

    override val body: ThreadEventBody,
    override val seq: Int
) : Event {
    override val event: EventType = EventType.thread
}


@Serializable
data class LoadedSourceEvent(
    override val seq: Int,
    override val body: LoadedSourceEventBody? = null
) : Event {

    override val event: EventType = EventType.loadedSource

}

/**
 * Stopped 事件
 * 该事件表示调试对象的执行由于某些情况而停止。
 *
 * 这可能是由先前设置的断点、单步执行请求完成、执行调试器语句等引起的。
 */
@Serializable
data class StoppedEvent(
    override val seq: Int,
    override val body: StoppedEventBody? = null
) : Event {
    override val event: EventType = EventType.stopped
    override val type: MessageType = super.type
}


/**
 * 终止事件
 * 该事件表示调试对象的调试已终止。这并不意味着调试对象本身已退出。
 */
@Serializable
data class TerminatedEvent(
    override val seq: Int,
    override val body: TerminatedEventBody? = null
) : Event {
    override val event: EventType = EventType.terminated
    override val type: MessageType
        get() = super.type
}

/**
 * Exited 事件
 * 该事件指示调试对象已退出并返回其退出代码。
 */
@Serializable
data class ExitedEvent(
    override val seq: Int,
    override val body: ExitedEventBody? = null
) : Event {
    override val event: EventType
        get() = EventType.exited

    override val type: MessageType = super.type
}

@Serializable
data class ContinuedEvent(
    override val seq: Int,
    override val body: ContinuedEventBody? = null
    ) : Event {
    override val type: MessageType
        get() = super.type
    override val event: EventType = EventType.continued
}

@Serializable
data class ModuleEvent (
    override val seq: Int,
    override val body: ModuleEventBody? = null
) : Event {
    override val type: MessageType
        get() = super.type
    override val event: EventType = EventType.module
}
