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
 * @see LspServer.state
 */
enum class LspServerState {
    /**
     * [LspServer] 初始状态为 [Initializing]。
     * 当 IDE 收到对 [initialize](https://microsoft.github.io/language-server-protocol/specification/#initialize)
     * 请求的响应时，状态会变为 [Running]。
     */
    Initializing,

    /**
     * [LspServer] 处于 [Running] 状态时，表示它已准备好处理来自 IDE 的请求和通知。
     * 技术上，这意味着 IDE 已收到对第一个
     * [initialize](https://microsoft.github.io/language-server-protocol/specification/#initialize) 请求的响应。
     */
    Running,

    /**
     * 正常关闭状态。
     */
    ShutdownNormally,

    /**
     * 意外关闭状态。
     */
    ShutdownUnexpectedly,
}
