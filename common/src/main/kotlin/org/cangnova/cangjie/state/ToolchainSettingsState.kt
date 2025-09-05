/*
 * Copyright 2025 LinQingYing. and contributors.
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

package org.cangnova.cangjie.state

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.StoragePathMacros
import com.intellij.openapi.components.State
import com.intellij.util.application

@Service(Service.Level.APP)
@State(name = "CangJieToolchainSettings", storages = [Storage(StoragePathMacros.NON_ROAMABLE_FILE)])

class ToolchainSettingsState : PersistentStateComponent<ToolchainSettingsState.State> {
    private var state = State()

    override fun getState(): State = state
    override fun loadState(state: State) {
        this.state = state
    }

    var path: String
        get() = state.path
        set(value) {
            state.path = value
        }

    companion object {
        fun getInstance(): ToolchainSettingsState  =
            application.getService(ToolchainSettingsState::class.java) ?: error("ToolchainSettingsState not initialized")
    }

    data class State(var path: String = "")


}