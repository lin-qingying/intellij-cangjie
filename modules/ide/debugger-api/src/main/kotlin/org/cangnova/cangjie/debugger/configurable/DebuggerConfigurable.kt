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

package org.cangnova.cangjie.debugger.configurable

import com.intellij.openapi.components.*
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.dsl.builder.MutableProperty
import com.intellij.ui.dsl.builder.bind
import com.intellij.ui.dsl.builder.panel
import com.intellij.util.xmlb.XmlSerializerUtil
import org.cangnova.cangjie.configurable.CjConfigurableBase
import org.cangnova.cangjie.debugger.messages.DebuggerBundle


enum class DebuggerEngine {
    DAP, PROTOBUF
}


@State(name = "CangJieDebuggerEngineServices", storages = [Storage("cangjie.language.debugger.xml")])
@Service(Service.Level.PROJECT)
class CangJieDebuggerEngineServices : PersistentStateComponent<CangJieDebuggerEngineServices> {
    override fun getState(): CangJieDebuggerEngineServices {
        return this
    }

    override fun loadState(state: CangJieDebuggerEngineServices) {
        XmlSerializerUtil.copyBean(state, this)
    }

    var debuggerEngine: DebuggerEngine = DebuggerEngine.DAP


    companion object {
        fun getInstance(project: Project): CangJieDebuggerEngineServices {
            return project.service<CangJieDebuggerEngineServices>()
        }
    }

}

internal class DebuggerConfigurable(override val project: Project) : CjConfigurableBase(
    project, DebuggerBundle.message("debugger.configurable.title")
) {
    val settings get() = CangJieDebuggerEngineServices.getInstance(project)

    override fun createPanel(): DialogPanel = panel {
        row {
            label(DebuggerBundle.message("debugger.engine"))
                .bold()
        }
        buttonsGroup {
            row {
                radioButton(DebuggerBundle.message("debugger.engine.dap.text"), DebuggerEngine.DAP)
                    .comment(DebuggerBundle.message("debugger.engine.dap.description"))
            }
            row {
                radioButton(DebuggerBundle.message("debugger.engine.protobuf.text"), DebuggerEngine.PROTOBUF)
                    .comment(DebuggerBundle.message("debugger.engine.protobuf.description"))
            }
        }.bind(
            object : MutableProperty<DebuggerEngine> {
                override fun get(): DebuggerEngine = settings.debuggerEngine
                override fun set(value: DebuggerEngine) {
                    settings.debuggerEngine = value
                }
            }
        )

        separator()

    }
}

val Project.isDapDebuggerEngine
    get() =
        CangJieDebuggerEngineServices.getInstance(this).debuggerEngine == DebuggerEngine.DAP


val Project.isProtoDebuggerEngine
    get() =
        CangJieDebuggerEngineServices.getInstance(this).debuggerEngine == DebuggerEngine.PROTOBUF

