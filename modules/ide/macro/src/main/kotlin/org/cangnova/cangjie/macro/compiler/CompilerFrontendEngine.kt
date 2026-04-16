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

package org.cangnova.cangjie.macro.compiler

import org.cangnova.cangjie.macro.engine.EngineProtocol
import org.cangnova.cangjie.macro.engine.ExpansionGranularity
import org.cangnova.cangjie.macro.engine.MacroExpansionEngine
import org.cangnova.cangjie.macro.messages.CangJieMacroBundle

/**
 * `cjc-frontend --debug-macro` 宏展开引擎描述符
 *
 * 描述通过调用 `cjc-frontend --debug-macro` 命令展开宏的引擎特性。
 * 该引擎每次展开都会启动一个新进程，展开整个文件中的所有宏，
 * 并将结果写入 `<source>.macrocall` 文本文件。
 *
 * @see CompilerMacroExpansionProvider
 */
object CompilerFrontendEngine : MacroExpansionEngine {
    override val id: String = "cjc-frontend"

    override val displayName: String = "cjc-frontend"

    override val description: String
        get() = CangJieMacroBundle.message("macro.engine.cjc.frontend.description")

    override val executableName: String = "cjc-frontend"

    override val protocol: EngineProtocol = EngineProtocol.FILE

    override val supportsStreaming: Boolean = false

    override val requiresMacroCompilation: Boolean = true

    override val expansionGranularity: ExpansionGranularity = ExpansionGranularity.FILE

    override fun toString(): String = displayName
}
