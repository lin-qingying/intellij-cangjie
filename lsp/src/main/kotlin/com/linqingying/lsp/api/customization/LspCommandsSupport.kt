/*
 * Copyright 2024 LinQingYing. and contributors.
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

package com.linqingying.lsp.api.customization

import com.intellij.openapi.application.Application
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.concurrency.annotations.RequiresEdt
import com.linqingying.lsp.api.LspServer
import org.eclipse.lsp4j.Command
import org.eclipse.lsp4j.ExecuteCommandParams
import org.jetbrains.annotations.ApiStatus


/**
 * 处理从 LSP 服务器接收到的 [Command](https://microsoft.github.io/language-server-protocol/specification#command) 对象。
 */

open class LspCommandsSupport {

    /**
     * 处理从 LSP 服务器接收到的 [Command](https://microsoft.github.io/language-server-protocol/specification#command) 对象。
     *
     * 此函数的默认实现只是向 LSP 服务器发送
     * [workspace/executeCommand](https://microsoft.github.io/language-server-protocol/#workspace_executeCommand) 请求。
     * 插件可以重写此函数以实现某些命令的特定插件处理。
     *
     * 在 LSP 规范 v.3.17 中，[Command] 对象可以包含在以下四种响应中：
     * - [CodeAction](https://microsoft.github.io/language-server-protocol/specification#codeAction)，例如快速修复、意图操作或重构
     * - [CompletionItem](https://microsoft.github.io/language-server-protocol/specification#completionItem)
     * - [InlayHintLabelPart](https://microsoft.github.io/language-server-protocol/specification#inlayHintLabelPart)
     * - [CodeLens](https://microsoft.github.io/language-server-protocol/specification#codeLens)
     *
     * 请注意，此函数在事件分派线程（Event Dispatch Thread，EDT）中调用。
     * 如果实现需要执行耗时操作，应该切换到后台线程，例如使用 [Application.executeOnPooledThread]。
     */
    @RequiresEdt
    open fun executeCommand(server: LspServer, contextFile: VirtualFile, command: Command) {
        server.sendNotification {
            it.workspaceService.executeCommand(
                ExecuteCommandParams(
                    command.command,
                    command.arguments
                )
            )
        }
    }
}
