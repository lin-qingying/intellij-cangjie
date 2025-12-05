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

@file:Suppress("UnstableApiUsage")

package org.cangnova.cangjie.run.target

import com.intellij.execution.target.LanguageRuntimeConfiguration
import com.intellij.openapi.components.BaseState
import com.intellij.openapi.components.PersistentStateComponent

/**
 * CangJie language runtime configuration for targetPlatform environments
 */
class CjLanguageRuntimeConfiguration :
    LanguageRuntimeConfiguration(CjLanguageRuntimeType.TYPE_ID),
    PersistentStateComponent<CjLanguageRuntimeConfiguration.MyState> {

    /**
     * SDK ID reference
     */
    var sdkId: String = ""

    /**
     * Additional build arguments
     */
    var localBuildArgs: String = ""

    class MyState : BaseState() {
        var sdkId by string()
        var localBuildArgs by string()
    }

    override fun getState(): MyState = MyState().also {
        it.sdkId = this.sdkId
        it.localBuildArgs = this.localBuildArgs
    }

    override fun loadState(state: MyState) {
        this.sdkId = state.sdkId.orEmpty()
        this.localBuildArgs = state.localBuildArgs.orEmpty()
    }

    /**
     * Get the CjSdk instance for this runtime configuration
     */
    fun getSdk(): org.cangnova.cangjie.toolchain.api.CjSdk? {
        if (sdkId.isEmpty()) return null
        return org.cangnova.cangjie.toolchain.api.CjSdkRegistry.getInstance().getSdk(sdkId)
    }
}

/**
 * Extension property to get CangJie language runtime from targetPlatform configuration
 */
val com.intellij.execution.target.TargetEnvironmentConfiguration.languageRuntime: CjLanguageRuntimeConfiguration?
    get() = runtimes.findByType()