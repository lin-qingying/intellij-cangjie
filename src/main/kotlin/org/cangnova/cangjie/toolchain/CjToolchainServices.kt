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

package org.cangnova.cangjie.toolchain

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.*


/**
 * 该类存储历史所有可用的工具链路径
 */
@Service
@State(name = "cangjie-toolchains-state", reloadable = false, storages = [Storage("cangjie-toolchains-state.xml")])
class CjToolchainServices : PersistentStateComponent<CjToolchainServices.State> {
    companion object {
        fun getInstance(): CjToolchainServices =
            ApplicationManager.getApplication().getService(CjToolchainServices::class.java)
    }

    class State() : BaseState() {

        constructor(toolchains: MutableList<String>) : this() {
            this.toolchains = toolchains
        }


        var toolchains by list<String>()

    }


    var myState = State(mutableListOf())


    fun putToolchainPath(path: String) {
        state.toolchains.map {

            if (it == path) {
                return
            }

        }


        state.toolchains.add(path)
    }

    fun getToolchainPaths(): MutableList<String> {
        return state.toolchains
    }

    fun removeToolchain(path: String) {

        state.toolchains.removeIf {
            it == path
        }


    }

    override fun getState(): State {
        return myState
    }

    override fun loadState(state: State) {
        this.myState = state

    }


}
