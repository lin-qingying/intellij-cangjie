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

/**
 * IDE 和 LSP 服务器之间通信的通道。
 * @see LspServerDescriptor.lspCommunicationChannel
 */
sealed class LspCommunicationChannel {

    data object StdIO : LspCommunicationChannel()

    /**
     * @param startProcess `true` 表示 IDE 应该通过调用 [LspServerDescriptor.startServerProcess] 启动 LSP 服务器，
     * 然后使用指定的 [port] 通过套接字连接到启动的服务器；
     * `false` 表示 LSP 服务器已经运行，
     * 因此 IDE 只需通过套接字连接到它即可。
     */
    data class Socket(val port: Int, val startProcess: Boolean = true) : LspCommunicationChannel()
}
