/*
 * Copyright 2026 LinQingYing. and contributors.
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

package org.cangnova.cangjie.debugger.protobuf.services.impl

import lldbprotobuf.ResponseOuterClass
import org.cangnova.cangjie.debugger.protobuf.protocol.ProtobufFactory
import org.cangnova.cangjie.debugger.protobuf.services.CommandCompletionResult
import org.cangnova.cangjie.debugger.protobuf.services.CommandResult
import org.cangnova.cangjie.debugger.protobuf.services.CommandService
import org.cangnova.cangjie.debugger.protobuf.transport.MessageBus

/**
 * 命令服务实现
 *
 * 负责执行 LLDB 控制台命令和命令补全
 */
class CommandServiceImpl(
    private val messageBus: MessageBus
) : CommandService {

    override suspend fun executeCommand(
        command: String,
        echoCommand: Boolean,
        asyncExecution: Boolean,
        threadId: Long?,
        frameIndex: Int?
    ): CommandResult {
        // 构建请求
        val request = ProtobufFactory.executeCommand(
            command = command,
            echoCommand = echoCommand,
            asyncExecution = asyncExecution,
            threadId = threadId,
            frameIndex = frameIndex
        )

        // 发送请求并获取响应
        val response = messageBus.request(
            request,
            ResponseOuterClass.ExecuteCommandResponse::class.java
        )

        // 解析响应
        return if (response.status.success) {
            CommandResult.success(
                output = response.output,
                error = response.errorOutput
            )
        } else {
            CommandResult.failure(
                errorMessage = response.status.message,
                output = response.output,
                error = response.errorOutput
            )
        }
    }

    override suspend fun getCommandCompletions(
        partialCommand: String,
        cursorPosition: Int,
        maxResults: Int
    ): CommandCompletionResult {
        // 构建补全请求
        val request = ProtobufFactory.commandCompletion(
            partialCommand = partialCommand,
            cursorPosition = if (cursorPosition == 0) partialCommand.length else cursorPosition,
            maxResults = maxResults
        )

        // 发送请求并获取响应
        val response = messageBus.request(
            request,
            ResponseOuterClass.CommandCompletionResponse::class.java
        )

        // 解析响应
        return if (response.status.success) {
            CommandCompletionResult.success(
                completions = response.completionsList,
                commonPrefix = response.commonPrefix,
                completionStart = response.completionStart
            )
        } else {
            CommandCompletionResult.failure(
                errorMessage = response.status.message
            )
        }
    }
}