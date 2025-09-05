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

package org.cangnova.cangjie.ide.run.cjpm.runconfig

import com.intellij.execution.target.LanguageRuntimeConfiguration
import com.intellij.openapi.components.BaseState
import com.intellij.openapi.components.PersistentStateComponent

class CjLanguageRuntimeConfiguration: LanguageRuntimeConfiguration(CjLanguageRuntimeType.TYPE_ID),
    PersistentStateComponent<CjLanguageRuntimeConfiguration.MyState> {
    var cjcPath: String = ""
    var cjcVersion: String = ""

    var cjpmPath: String = ""
    var cjpmVersion: String = ""

    var localBuildArgs: String = ""
    class MyState : BaseState() {
        var cjcPath by string()
        var cjcVersion by string()

        var cjpmPath by string()
        var cjpmVersion by string()

        var localBuildArgs by string()
    }

    override fun getState(): MyState = MyState().also {
        it.cjcPath = this.cjcPath
        it.cjcVersion = this.cjcVersion

        it.cjpmPath = this.cjpmPath
        it.cjpmVersion = this.cjpmVersion

        it.localBuildArgs = this.localBuildArgs
    }

    override fun loadState(state: MyState) {
        this.cjcPath = state.cjcPath.orEmpty()
        this.cjcVersion = state.cjcVersion.orEmpty()

        this.cjpmPath = state.cjpmPath.orEmpty()
        this.cjpmVersion = state.cjpmVersion.orEmpty()

        this.localBuildArgs = state.localBuildArgs.orEmpty()
    }
}
