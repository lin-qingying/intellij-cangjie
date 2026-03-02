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

package org.cangnova.cangjie.macro.service

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.project.Project
import com.intellij.util.xmlb.annotations.Attribute

/**
 * 宏展开引擎持久化设置
 *
 * 保存用户选择的宏展开引擎 ID，在项目重启后恢复。
 * 存储于项目级工作空间文件，不纳入版本控制。
 *
 * ## 引擎 ID 来源
 *
 * 引擎 ID 对应 [org.cangnova.cangjie.macro.service.MacroExpansionProviderFactory.engineId]，
 * 也与 [org.cangnova.cangjie.macro.engine.MacroExpansionEngine.id] 一致。
 *
 * ## 默认行为
 *
 * - [preferredEngineId] 为空字符串 → 使用标记了 [org.cangnova.cangjie.macro.service.MacroExpansionProviderFactory.isDefault] 的工厂
 * - [preferredEngineId] 非空但对应引擎不可用 → 回退到默认引擎
 */
@Service(Service.Level.PROJECT)
@State(
    name = "CangJieMacroExpansionSettings",
    storages = [Storage("cangjie-macro-settings.xml")]
)
class MacroExpansionSettings : PersistentStateComponent<MacroExpansionSettings.State> {

    /**
     * 持久化状态数据
     */
    data class State(
        @get:Attribute
        var preferredEngineId: String = "",
    )

    private var myState = State()

    override fun getState(): State = myState

    override fun loadState(state: State) {
        myState = state
    }

    /**
     * 用户选择的引擎 ID
     *
     * 空字符串表示使用默认引擎。
     * 赋值后立即生效，无需重启。
     */
    var preferredEngineId: String
        get() = myState.preferredEngineId
        set(value) {
            myState = myState.copy(preferredEngineId = value)
        }

    companion object {
        fun getInstance(project: Project): MacroExpansionSettings =
            project.getService(MacroExpansionSettings::class.java)
    }
}
