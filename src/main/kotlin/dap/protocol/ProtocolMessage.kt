package dap.protocol

import dap.type.MessageType
import kotlinx.serialization.*


@Polymorphic
interface ProtocolMessage {

    /**
     * 消息的序列号（也称为消息ID）。客户端或调试适配器发送的第一条消息的 `seq` 是1，每个
     * 后续消息的 `seq` 比它发送的前一条消息大1。`seq` 可用于对请求、响应和事件进行排序，以及
     * 将请求与其对应的响应关联。对于类型为 `request` 的协议消息，序列号可以用于取消请求。
     */

    val seq: Int

    /**
     * 消息类型
     */
//    @SerialName("type")
    val type: MessageType


}

















