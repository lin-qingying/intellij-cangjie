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

package com.linqingying.lsp.api.requests

import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.concurrency.annotations.RequiresBackgroundThread
import com.linqingying.lsp.api.LspServer
import org.eclipse.lsp4j.TextDocumentIdentifier

/**
 * 这个类已经被弃用并计划移除。
 * 替代方法是：
 * - [LspServer.sendNotification]
 * - [LspServer.sendRequest]
 * - [LspServer.sendRequestSync]
 * - [LspServer.getDocumentVersion]
 * - [LspServer.getDocumentIdentifier]
 */
interface LspRequestExecutor {
    @Deprecated("Use LspServer.getDocumentIdentifier")
    fun getDocumentIdentifier(file: VirtualFile): TextDocumentIdentifier

    @Deprecated("Use LspServer.sendNotification")
    fun sendNotification(notification: LspClientNotification)

    @Deprecated("Use LspServer.sendRequest")
    fun <Lsp4jResponse, Result> sendRequestAsync(
        lspRequest: LspRequest<Lsp4jResponse, Result>,
        resultConsumer: (Result?) -> Unit
    )

    @RequiresBackgroundThread
    @Deprecated("Use LspServer.sendRequest")
    fun <Lsp4jResponse, Result> sendRequestSync(lspRequest: LspRequest<Lsp4jResponse, Result>): Result?
}
