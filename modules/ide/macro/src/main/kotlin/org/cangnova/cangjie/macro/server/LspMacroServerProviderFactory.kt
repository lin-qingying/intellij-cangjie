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

import com.intellij.openapi.project.Project
import org.cangnova.cangjie.macro.service.MacroExpansionProvider
import org.cangnova.cangjie.macro.service.MacroExpansionProviderFactory

/**
 * `LSPMacroServer` 引擎工厂
 *
 * 通过扩展点注册到 `org.cangnova.cangjie.macroExpansionProviderFactory`，
 * 提供基于常驻进程的高性能宏展开能力。
 *
 * @see LspMacroServerProvider 实际提供者实现
 * @see LspMacroServerEngine 引擎元信息
 */
internal class LspMacroServerProviderFactory : MacroExpansionProviderFactory {

    override val engineId: String = LspMacroServerEngine.id

    /** LSPMacroServer 非默认引擎，需用户主动选择或系统检测到时启用 */
    override val isDefault: Boolean = false

    override fun createProvider(project: Project): MacroExpansionProvider =
        LspMacroServerProvider(project)
}
