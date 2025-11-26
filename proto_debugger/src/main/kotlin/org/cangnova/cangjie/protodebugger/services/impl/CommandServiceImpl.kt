package org.cangnova.cangjie.protodebugger.services.impl

import lldbprotobuf.ResponseOuterClass
import org.cangnova.cangjie.protodebugger.protocol.ProtobufFactory
import org.cangnova.cangjie.protodebugger.services.CommandCompletionResult
import org.cangnova.cangjie.protodebugger.services.CommandResult
import org.cangnova.cangjie.protodebugger.services.CommandService
import org.cangnova.cangjie.protodebugger.transport.MessageBus

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