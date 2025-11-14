package org.cangnova.cangjie.protodebugger.ipc

import com.google.protobuf.Descriptors
import com.google.protobuf.Message


/**
 * Protocol Buffers 工具对象
 *
 * 该对象提供了处理 Protocol Buffers 消息的实用工具方法，
 * 主要用于调试器与后端服务之间的通信消息处理。
 *
 * 使用场景：
 * - 调试器协议消息的解析和处理
 * - 复合消息的解包和过滤
 * - Protocol Buffers 消息的递归处理
 * - 调试器通信协议的实现
 *
 * 主要功能：
 * - 递归解包复合消息
 * - 基于过滤器提取目标消息
 * - 处理嵌套的消息结构
 * - 验证消息格式的正确性
 */
object ProtobufUtils {

    /**
     * 解包复合消息
     *
     * 递归地从复合消息中提取目标消息。复合消息是指包含其他消息作为字段的消息。
     * 该方法会根据提供的过滤器函数，逐层解包直到找到满足条件的消息。
     *
     * 工作原理：
     * 1. 检查当前消息是否满足过滤条件
     * 2. 如果不满足，则检查是否为复合消息（只有一个字段且为消息类型）
     * 3. 如果是复合消息，则递归解包内部消息
     * 4. 重复此过程直到找到满足条件的消息或达到最底层
     *
     * 使用场景：
     * - 从嵌套的调试器响应中提取实际数据
     * - 处理协议层包装的消息
     * - 过滤不需要的协议层消息
     * - 提取调试器关心的核心消息内容
     *
     * @param compositeResponse 要解包的复合消息
     * @param decomposeFilter 过滤器函数，用于判断是否需要继续解包
     * @return 解包后的目标消息
     * @throws AssertionError 当复合消息包含多个消息字段时抛出
     */
    fun unpackComposite(
        compositeResponse: Message,
        decomposeFilter: (Message) -> Boolean
    ): Message {
        // 如果当前消息不满足继续解包的条件，直接返回
        return if (!(decomposeFilter(compositeResponse))) {
            compositeResponse
        } else {
            // 获取消息的所有字段
            val allFields: Map<Descriptors.FieldDescriptor, Any> = compositeResponse.allFields
            val values: Collection<Any> = allFields.values

            // 确保复合消息只包含一个消息字段
            assert(values.size == 1) { "More than 1 message packed in one composite message" }

            // 递归解包内部消息
            val next: Message = values.iterator().next() as Message
            unpackComposite(next, decomposeFilter)
        }
    }
}
