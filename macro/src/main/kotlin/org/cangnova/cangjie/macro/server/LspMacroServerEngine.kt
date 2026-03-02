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
 */

package org.cangnova.cangjie.macro.server

import org.cangnova.cangjie.macro.engine.EngineProtocol
import org.cangnova.cangjie.macro.engine.ExpansionGranularity
import org.cangnova.cangjie.macro.engine.MacroExpansionEngine
import org.cangnova.cangjie.macro.messages.CangJieMacroBundle

/**
 * `LSPMacroServer` 宏展开引擎描述符
 *
 * 描述通过 `LSPMacroServer` 常驻进程展开宏的引擎特性。
 * 该引擎启动一次后持续运行，通过匿名管道接收 FlatBuffers 编码的宏调用请求，
 * 并流式返回展开结果，无需每次重新启动进程。
 *
 * ## 通信协议
 *
 * 使用 `MacroMsgFormat.fbs` 定义的 FlatBuffers 消息格式，
 * 通过 length-prefixed 帧协议在管道上传输：
 * `[4字节 uint32_le 长度][N字节 FlatBuffers payload]`
 *
 * ## 启动参数
 *
 * ```
 * LSPMacroServer <read_fd> <write_fd> <enable_parallel> <cjc_folder> [ppid]
 * ```
 *
 * @see LspMacroServerProvider
 * @see LspMacroServerProcess
 */
object LspMacroServerEngine : MacroExpansionEngine {
    override val id: String = "lsp-macro-server"

    override val displayName: String = "LSPMacroServer"

    override val description: String
        get() = CangJieMacroBundle.message("macro.engine.lsp.server.description")

    override val executableName: String = "LSPMacroServer"

    override val protocol: EngineProtocol = EngineProtocol.PIPE_FLATBUFFERS

    override val supportsStreaming: Boolean = true

    override val requiresMacroCompilation: Boolean = true

    override val expansionGranularity: ExpansionGranularity = ExpansionGranularity.MACRO_CALL

    override fun toString(): String = displayName
}
