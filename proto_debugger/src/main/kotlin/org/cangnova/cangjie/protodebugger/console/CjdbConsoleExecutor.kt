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
 */

package org.cangnova.cangjie.protodebugger.console

import com.intellij.execution.console.LanguageConsoleView
import com.intellij.execution.ui.ConsoleViewContentType
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.diagnostic.Logger
import org.cangnova.cangjie.protodebugger.core.CangJieDebugProcess

/**
 * Cjdb 控制台执行器
 *
 * 处理用户在 cjdb 控制台中输入的命令，通过 CommandService 执行 LLDB 命令
 */
class CjdbConsoleExecutor(
    private val debugProcess: CangJieDebugProcess,
    private val console: LanguageConsoleView
) {

    companion object {
        private val LOG = Logger.getInstance(CjdbConsoleExecutor::class.java)
    }

    /**
     * 获取控制台执行 Action
     */
    fun createExecuteAction(): AnAction {
        return object : AnAction() {
            override fun actionPerformed(e: AnActionEvent) {
                val text = console.consoleEditor.document.text.trim()
                if (text.isNotEmpty()) {
                    execute(text)
                    // 清空输入区域
                    console.consoleEditor.document.setText("")
                }
            }
        }
    }

    /**
     * 执行控制台命令
     *
     * @param command 用户输入的命令文本
     */
    fun execute(command: String) {
        if (command.isEmpty()) {
            return
        }

        // 在控制台中回显命令
//        console.print("$command\n", ConsoleViewContentType.USER_INPUT)

        // 异步执行命令
        debugProcess.executeCommand {
            try {
                // 获取当前停止的线程信息（如果有）
                val stoppedThread = debugProcess.facade.getStoppedThread()

                // 执行命令
                val result = if (stoppedThread != null) {
                    // 在线程上下文中执行
                    debugProcess.facade.commandService.executeCommandInContext(
                        threadId = stoppedThread.id.toLong(),
                        frameIndex = 0,
                        command = command
                    )
                } else {
                    // 在全局上下文中执行
                    debugProcess.facade.commandService.executeCommand(
                        command = command
                    )
                }

                // 在控制台中显示结果
                if (result.success) {
                    if (result.output.isNotEmpty()) {
                        console.print(result.output, ConsoleViewContentType.NORMAL_OUTPUT)
                        if (!result.output.endsWith("\n")) {
                            console.print("\n", ConsoleViewContentType.NORMAL_OUTPUT)
                        }
                    }
                    if (result.error.isNotEmpty()) {
                        console.print(result.error, ConsoleViewContentType.ERROR_OUTPUT)
                        if (!result.error.endsWith("\n")) {
                            console.print("\n", ConsoleViewContentType.ERROR_OUTPUT)
                        }
                    }
                } else {
                    // 显示错误信息
                    console.print("Error: ${result.errorMessage}\n", ConsoleViewContentType.ERROR_OUTPUT)
                    if (result.output.isNotEmpty()) {
                        console.print(result.output, ConsoleViewContentType.NORMAL_OUTPUT)
                    }
                }
            } catch (e: Exception) {
                LOG.error("Failed to execute command: $command", e)
                console.print("Error: ${e.message}\n", ConsoleViewContentType.ERROR_OUTPUT)
            }
        }.exceptionally { error ->
            LOG.error("Command execution failed", error)
            console.print("Error: ${error.message}\n", ConsoleViewContentType.ERROR_OUTPUT)
            null
        }
    }

    /**
     * 获取命令补全建议
     *
     * 同步获取 LLDB 命令补全建议。
     * 这个方法会阻塞调用线程,因为 IntelliJ 的补全 API 是同步的。
     *
     * @param text 当前输入的文本
     * @param offset 光标位置
     * @return 补全建议列表
     */
    fun getCompletions(text: String, offset: Int): List<String> {
        if (text.isEmpty()) {
            return emptyList()
        }

        return try {
            // 使用 CompletableFuture.get() 同步等待结果
            // 注意: 这会阻塞当前线程,但补全操作通常很快
            val future = debugProcess.executeCommand {
                debugProcess.facade.commandService.getCommandCompletions(
                    partialCommand = text,
                    cursorPosition = offset,
                    maxResults = 50
                )
            }

            // 设置超时避免永久阻塞
            val result = future.get(500, java.util.concurrent.TimeUnit.MILLISECONDS)

            if (result.success) {
                result.completions
            } else {
                LOG.warn("Command completion failed: ${result.errorMessage}")
                emptyList()
            }
        } catch (e: java.util.concurrent.TimeoutException) {
            LOG.warn("Command completion timed out")
            emptyList()
        } catch (e: Exception) {
            LOG.error("Failed to get command completions", e)
            emptyList()
        }
    }
}