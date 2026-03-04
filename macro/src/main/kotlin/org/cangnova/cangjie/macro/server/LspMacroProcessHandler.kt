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

package org.cangnova.cangjie.macro.server

import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.process.KillableProcessHandler
import com.intellij.execution.process.ProcessEvent
import com.intellij.execution.process.ProcessListener
import com.intellij.execution.process.ProcessOutputType
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.util.Key
import com.intellij.util.io.BaseOutputReader

/**
 * LSPMacroServer 专用进程处理器
 *
 * 基于 IntelliJ `KillableProcessHandler`，
 * 用于接入 IDE 生命周期管理。
 */
class LspMacroProcessHandler(
    commandLine: GeneralCommandLine
) : KillableProcessHandler(commandLine) {

    private val logger = Logger.getInstance(LspMacroProcessHandler::class.java)

    init {
        addProcessListener(object : ProcessListener {

            override fun startNotified(event: ProcessEvent) {
                val pid = runCatching { process.pid() }.getOrElse { -1 }
                logger.info("LSPMacroServer 已启动 (PID=$pid)")
            }

            override fun processTerminated(event: ProcessEvent) {
                logger.info("LSPMacroServer 已退出 (exitCode=${event.exitCode})")
            }

            override fun onTextAvailable(event: ProcessEvent, outputType: Key<*>) {
                if (ProcessOutputType.isStderr(outputType)) {
                    logger.warn("[LSP STDERR] ${event.text.trim()}")
                }
                // stdout 不消费，保留给 LSP 通信线程
            }
        })
    }

    // LSPMacroServer 是长期运行的静默守护进程，使用低 CPU 占用的读取模式
    override fun readerOptions(): BaseOutputReader.Options = BaseOutputReader.Options.forMostlySilentProcess()

    override fun destroyProcessImpl() {
        logger.debug("销毁 LSPMacroServer 进程...")
        // TODO: 发送 LSP shutdown + exit 请求，需在 super 之前完成
        super.destroyProcessImpl()
    }

    override fun onOSProcessTerminated(exitCode: Int) {
        logger.debug("LSPMacroServer 进程终止，exitCode=$exitCode")
        super.onOSProcessTerminated(exitCode)
        // TODO: 如需自动重启，在此处添加逻辑
    }
}