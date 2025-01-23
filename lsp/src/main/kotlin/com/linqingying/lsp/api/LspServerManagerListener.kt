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

package com.linqingying.lsp.api

import com.intellij.openapi.vfs.VirtualFile
import java.util.*



/**
 * LspServerManagerListener接口定义了监听LSP（Language Server Protocol）服务器管理事件的监听器需要实现的方法。
 * 这个接口扩展了EventListener，确保实现者可以监听和响应LSP服务器的各种状态变化和事件。
 */
interface LspServerManagerListener : EventListener {

    /**
     * 当LSP服务器的状态发生变化时调用。
     *
     * @param lspServer 发生状态变化的LSP服务器实例。
     */
    fun serverStateChanged(lspServer: LspServer) {}

    /**
     * 当一个文件在LSP服务器的上下文中被打开时调用。
     *
     * @param lspServer 与打开文件事件相关的LSP服务器实例。
     * @param file 被打开的虚拟文件。
     */
    fun fileOpened(lspServer: LspServer, file: VirtualFile) {}

    /**
     * 当从LSP服务器接收到诊断信息时调用。
     * 诊断信息通常包含文件中的错误、警告或其他类型的消息。
     *
     * @param lspServer 发送诊断信息的LSP服务器实例。
     * @param file 与诊断信息相关的虚拟文件。
     */
    fun diagnosticsReceived(lspServer: LspServer, file: VirtualFile) {}
}
