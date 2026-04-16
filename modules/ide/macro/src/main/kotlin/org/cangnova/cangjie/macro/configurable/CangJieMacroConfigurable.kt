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
 *
 * The use of this source code is governed by the Apache License 2.0,
 * which allows users to freely use, modify, and distribute the code,
 * provided they adhere to the terms of the license.
 *
 * The software is provided "as-is", and the authors are not responsible for
 * any damages or issues arising from its use.
 *
 */

package org.cangnova.cangjie.macro.configurable

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.SimpleListCellRenderer
import com.intellij.ui.dsl.builder.bindItem
import com.intellij.ui.dsl.builder.panel
import org.cangnova.cangjie.configurable.CjConfigurableBase
import org.cangnova.cangjie.macro.messages.CangJieMacroBundle
import org.cangnova.cangjie.macro.service.MacroExpansionProviderFactory
import org.cangnova.cangjie.macro.service.MacroExpansionSettings

/**
 * 宏展开设置页面
 *
 * 提供宏展开引擎选择 UI，通过 `org.cangnova.cangjie.macroExpansionProviderFactory`
 * 扩展点动态发现所有可用引擎。
 *
 * 用户的选择持久化到 [MacroExpansionSettings]（`cangjie-macro-settings.xml`）。
 */
internal class CangJieMacroConfigurable(override val project: Project) : CjConfigurableBase(
    project, CangJieMacroBundle.message("macro.configurable.title")
) {
    /** 表示"自动选择默认引擎"的虚拟条目 ID */
    private val AUTO_ID = ""

    /** 引擎列表条目：engineId + 显示名称 */
    private data class EngineItem(val engineId: String, val displayName: String)

    override fun createPanel(): DialogPanel {
        val settings = MacroExpansionSettings.getInstance(project)
        val items = buildEngineItems()

        return panel {
            group(CangJieMacroBundle.message("macro.configurable.engine.group")) {
                row(CangJieMacroBundle.message("macro.configurable.engine.label") + ":") {
                    comboBox(
                        items,
                        SimpleListCellRenderer.create("") { it.displayName }
                    ).bindItem(
                        getter = {
                            items.firstOrNull { it.engineId == settings.preferredEngineId }
                                ?: items.first()
                        },
                        setter = { item ->
                            settings.preferredEngineId = item?.engineId ?: AUTO_ID
                        }
                    )
                }
                row {
                    comment(CangJieMacroBundle.message("macro.configurable.engine.description"))
                }
            }
        }
    }

    private fun buildEngineItems(): List<EngineItem> = buildList {
        // "自动"选项始终排第一
        add(EngineItem(AUTO_ID, CangJieMacroBundle.message("macro.configurable.engine.auto")))
        MacroExpansionProviderFactory.EP_NAME.extensionList.forEach { factory ->
            add(EngineItem(factory.engineId, factory.engineId))
        }
    }
}
