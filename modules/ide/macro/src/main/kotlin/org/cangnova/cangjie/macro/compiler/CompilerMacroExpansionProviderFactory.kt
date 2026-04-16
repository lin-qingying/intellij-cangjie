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

import com.intellij.openapi.project.Project
import org.cangnova.cangjie.macro.service.MacroExpansionProvider
import org.cangnova.cangjie.macro.service.MacroExpansionProviderFactory

/**
 * `cjc-frontend --debug-macro` 引擎工厂
 *
 * 通过扩展点注册到 `org.cangnova.cangjie.macroExpansionProviderFactory`，
 * 作为系统的**默认引擎**和**最终回退方案**。
 *
 * @see CompilerMacroExpansionProvider 实际提供者实现
 * @see CompilerFrontendEngine 引擎元信息
 */
internal class CompilerMacroExpansionProviderFactory : MacroExpansionProviderFactory {

    override val engineId: String = CompilerFrontendEngine.id

    /** cjc-frontend 是默认引擎，当无配置或其他引擎不可用时使用 */
    override val isDefault: Boolean = true

    override fun createProvider(project: Project): MacroExpansionProvider =
        CompilerMacroExpansionProvider(project)
}
