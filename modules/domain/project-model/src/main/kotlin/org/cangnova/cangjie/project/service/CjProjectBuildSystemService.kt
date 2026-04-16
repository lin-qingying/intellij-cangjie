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

package org.cangnova.cangjie.project.service

import com.intellij.openapi.components.*
import com.intellij.util.application
import org.cangnova.cangjie.project.extension.ProjectBuildSystemId

/**
 * 项目构建系统服务
 *
 * 存储和管理项目使用的构建系统标识符
 */
@Service(Service.Level.APP)
@State(
    name = "CjProjectBuildSystem",
    storages = [Storage("cangjie.build.system.xml")]
)
class CjProjectBuildSystemService : PersistentStateComponent<CjProjectBuildSystemService.State> {

    private var myState = State()

    /**
     * 状态数据类
     */
    data class State(
        /**
         * 当前项目使用的构建系统 ID
         * 如果为 null，表示尚未确定或使用默认构建系统
         */
        var buildSystemId: String? = null
    )

    override fun getState(): State = myState

    override fun loadState(state: State) {
        myState = state
    }

    /**
     * 获取当前项目的构建系统 ID
     */
   private fun getBuildSystemId(): String? = myState.buildSystemId

    /**
     * 设置当前项目的构建系统 ID
     */
    fun setBuildSystemId(id: String?) {
        myState.buildSystemId = id
    }

    /**
     * 获取当前项目的构建系统实例
     *
     * @return 如果找到对应的构建系统则返回，否则返回第一个注册的构建系统，如果没有注册任何构建系统则返回 null
     */
    fun getBuildSystem(): ProjectBuildSystemId? {
        val buildSystemId = getBuildSystemId()
        val extensionList = ProjectBuildSystemId.EP_NAME.extensionList

        return if (buildSystemId != null) {
            extensionList.firstOrNull { it.id == buildSystemId }
                ?: extensionList.firstOrNull()
        } else {
            extensionList.firstOrNull()
        }
    }

    /**
     * 检查是否使用指定的构建系统
     */
    fun isBuildSystem(id: String): Boolean = getBuildSystemId() == id

    companion object {
        /**
         * 获取服务实例
         */
        @JvmStatic
        fun getInstance(): CjProjectBuildSystemService =
            application.service<CjProjectBuildSystemService>()
    }
}
