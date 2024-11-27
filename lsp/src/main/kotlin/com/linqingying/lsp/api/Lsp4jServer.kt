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
 * [org.eclipse.lsp4j.services.LanguageServer] 接口的别名，来自 `lsp4j` 库。
 * 它有助于区分 `lsp4j` 库特定的类和 IntelliJ LSP API 类。
 *
 * 例如，[LspServer] 类表示 IntelliJ 模型中的一个已启动的 LSP 服务器，
 * 而 [Lsp4jServer] 是一个底层的库特定接口。
 *
 * 插件只有在需要向 LSP 服务器发送自定义未文档化的通知或请求时，才需要重写 [Lsp4jServer]。
 *
 * @see LspServerDescriptor.lsp4jServerClass
 */
typealias Lsp4jServer = org.eclipse.lsp4j.services.LanguageServer
