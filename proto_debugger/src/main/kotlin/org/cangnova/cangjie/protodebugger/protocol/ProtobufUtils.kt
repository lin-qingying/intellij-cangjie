/*
 * Copyright 2025 LinQingYing. and contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * The use of this source code is governed by the Apache License 2.0,
 * which allows users to freely use, modify, and distribute the code,
 * provided they adhere to the terms of the license.
 *
 * The software is provided "as-is", and the authors are not responsible for
 * any damages or issues arising from its use.
 *
 */

package org.cangnova.cangjie.protodebugger.protocol

import com.google.protobuf.Descriptors
import com.google.protobuf.Message
import lldbprotobuf.Model

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
    ): Pair<Long?, Message> {
        var hash: Long? = null

        // 如果当前消息不满足继续解包的条件，直接返回
        return if (!(decomposeFilter(compositeResponse))) {
            hash to compositeResponse
        } else {
            // 获取消息的所有字段
            val allFields: Map<Descriptors.FieldDescriptor, Any> = compositeResponse.allFields
            val values: MutableCollection<Any> = allFields.values.toMutableList()


            if (values.size > 1) {
                // 在 values 中查找类型为 Model.HashId 的字段
                val hashIdValue = values.find { it is Model.HashId } as? Model.HashId
                hash = hashIdValue?.hash

                // 如果找到 HashId，从 values 中移除
                if (hashIdValue != null) {
                    values.remove(hashIdValue)

                }
            }

            // 确保复合消息只包含一个消息字段
            assert(values.size == 1) { "More than 1 message packed in one composite message" }

            // 递归解包内部消息
            val next: Message = values.iterator().next() as Message
            val (recursiveHash, resultMessage) = unpackComposite(next, decomposeFilter)

            // 优先使用当前层的hash，如果没有则使用递归调用返回的hash
            val finalHash = hash ?: recursiveHash
            finalHash to resultMessage
        }
    }
}