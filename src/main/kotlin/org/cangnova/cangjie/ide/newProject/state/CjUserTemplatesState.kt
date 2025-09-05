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

package org.cangnova.cangjie.ide.newProject.state

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.service
import com.intellij.openapi.util.NlsContexts
import com.intellij.util.xmlb.XmlSerializerUtil


@State(name = "CjUserTemplatesState", storages = [Storage("cangjie.usertemplates.xml")])
class CjUserTemplatesState : PersistentStateComponent<CjUserTemplatesState> {

    var templates = mutableListOf<CjUserTemplate>()

    override fun getState(): CjUserTemplatesState = this

    override fun loadState(state: CjUserTemplatesState) = XmlSerializerUtil.copyBean(state, this)

    companion object {
        fun getInstance(): CjUserTemplatesState = service()
    }
}

data class CjUserTemplate(@NlsContexts.ListItem var name: String = "", var url: String = "")
