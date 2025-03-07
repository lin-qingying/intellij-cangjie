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

package cn.cangnova.cangjie.ide.editor

import cn.cangnova.cangjie.CangJieBundle.message
import com.intellij.application.options.editor.CodeFoldingOptionsProvider
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.options.BeanConfigurable
import com.intellij.util.xmlb.XmlSerializerUtil
import org.jetbrains.annotations.NonNls

private val editorOptions get() = CangJieEditorOptions.getInstance()

@NonNls
const val ID = "editor.title.cangjie"

@Service(Service.Level.APP)
@State(name = "CangJieEditorOptions", storages = [Storage("cangjie.editor.codeinsight.xml")])
class CangJieEditorOptions : PersistentStateComponent<CangJieEditorOptions> {
    companion object {
        fun getInstance(): CangJieEditorOptions {
            return ApplicationManager.getApplication().getService(CangJieEditorOptions::class.java)
        }
    }

    override fun getState(): CangJieEditorOptions {
        return this

    }

    override fun loadState(state: CangJieEditorOptions) {
        XmlSerializerUtil.copyBean(state, this)

    }
}

class CangJieEditorOptionsConfigurable : BeanConfigurable<CangJieEditorOptions>(editorOptions, message(ID)) ,
    CodeFoldingOptionsProvider
