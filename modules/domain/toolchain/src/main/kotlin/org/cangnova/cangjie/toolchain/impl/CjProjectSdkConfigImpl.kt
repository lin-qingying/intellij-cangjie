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

package org.cangnova.cangjie.toolchain.impl

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.project.Project
import org.cangnova.cangjie.toolchain.api.CjProjectSdkConfig
import org.cangnova.cangjie.toolchain.api.CjSdk
import org.cangnova.cangjie.toolchain.api.CjSdkRegistry

/**
 * 项目SDK配置的持久化状态
 */
internal data class CjProjectSdkConfigState(
    /**
     * 项目使用的SDK ID
     */
    var sdkId: String? = null
)

/**
 * 项目SDK配置的默认实现
 *
 * 使用IntelliJ Platform的持久化机制存储项目SDK配置
 */
@Service(Service.Level.PROJECT)
@State(
    name = "CangJieProjectSdkConfig",
    storages = [Storage("cangjie-project-sdk.xml")]
)
internal class CjProjectSdkConfigImpl(
    private val project: Project
) : CjProjectSdkConfig, PersistentStateComponent<CjProjectSdkConfigState> {

    private var state = CjProjectSdkConfigState()

    override fun getState(): CjProjectSdkConfigState = state

    override fun loadState(state: CjProjectSdkConfigState) {
        this.state = state
    }

    override fun getProjectSdkId(): String? {
        return state.sdkId
    }

    override fun setProjectSdkId(sdkId: String?) {
        state.sdkId = sdkId
    }

    override fun getProjectSdk(): CjSdk? {
        val sdkId = state.sdkId ?: return null
        return CjSdkRegistry.getInstance().getSdk(sdkId)
    }

    override fun hasProjectSdk(): Boolean {
        return state.sdkId != null
    }
}
