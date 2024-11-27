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

import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.concurrency.annotations.RequiresEdt
import org.jetbrains.annotations.ApiStatus


open class LspFormattingSupport {
    /**
     * 此函数在 `Reformat Code` 操作时被调用，可能是显式的（例如，使用快捷键）或隐式的（例如，在保存时，如果启用了保存时格式化）。
     *
     * 如果此函数返回 `true`，则 IDE 将发送 [textDocument/formatting](https://microsoft.github.io/language-server-protocol/specification/#textDocument_formatting)
     * 请求到服务器，并应用接收到的结果。
     * IDE 的内部格式化器（如果存在）将不会被使用。
     *
     * 如果此函数返回 `false`，则 IDE 将使用其内部格式化器（如果存在）来格式化此文件。
     *
     * 实现可能如下所示：
     *
     *      override fun shouldFormatThisFileExclusivelyByServer(file: VirtualFile,
     *                                                           ideCanFormatThisFileItself: Boolean,
     *                                                           serverExplicitlyWantsToFormatThisFile: Boolean) =
     *        file.extension == "foo"
     *
     * @param file 要格式化的文件
     *
     * @param ideCanFormatThisFileItself 从技术上讲，这个参数表示 [com.intellij.lang.LanguageFormatting.forContext]
     * 是否为该 [file] 返回非空的 [com.intellij.formatting.FormattingModelBuilder]
     *
     * @param serverExplicitlyWantsToFormatThisFile `true` 表示服务器已发送
     * [client/registerCapability](https://microsoft.github.io/language-server-protocol/specification/#client_registerCapability)
     * 请求到 IDE，注册其 `textDocument/formatting` 功能，并且该 [file] 匹配提供的
     * [DocumentSelector](https://microsoft.github.io/language-server-protocol/specification/#documentSelector)。
     * 换句话说，服务器明确表示它希望格式化此特定的 [file]。
     * 仅有静态的 `documentFormattingProvider`
     * [服务器能力](https://microsoft.github.io/language-server-protocol/specification/#serverCapabilities)
     * （在 [InitializeResult](https://microsoft.github.io/language-server-protocol/specification/#initializeResult) 中）不算作明确希望格式化此特定 [file]
     */
    @RequiresEdt
    open fun shouldFormatThisFileExclusivelyByServer(
        file: VirtualFile,
        ideCanFormatThisFileItself: Boolean,
        serverExplicitlyWantsToFormatThisFile: Boolean
    ): Boolean =
        !ideCanFormatThisFileItself && serverExplicitlyWantsToFormatThisFile
}
