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

package org.cangnova.cangjie.project.ui.actions

import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.project.Project
import org.cangnova.cangjie.project.model.CjProject

/**
 * 仓颉命令提供者接口
 *
 * 扩展此接口可以为仓颉项目提供自定义的命令执行功能。
 * 每个提供者可以定义一组可执行的命令以及它们的执行逻辑。
 */
interface CjCommandProvider {

    companion object {
        /**
         * 扩展点名称，用于注册命令提供者
         */
        val EP_NAME: ExtensionPointName<CjCommandProvider> =
            ExtensionPointName.create("org.cangnova.cangjie.commandProvider")
    }

    /**
     * 获取提供者的名称
     * @return 提供者的显示名称
     */
    fun getName(): String

    /**
     * 获取帮助命令
     * @return 帮助命令字符串，用于 RunAnything 的前缀
     */
    fun getHelpCommand(): String

    /**
     * 获取可用的命令列表
     * @param project 当前项目
     * @param cjProject 选中的仓颉项目（可选）
     * @return 可执行的命令列表
     */
    fun getAvailableCommands(project: Project, cjProject: CjProject? = null): List<CjCommand>

    /**
     * 执行指定的命令
     * @param project 当前项目
     * @param cjProject 仓颉项目
     * @param command 要执行的命令
     * @param args 命令参数
     */
    fun executeCommand(project: Project, cjProject: CjProject, command: CjCommand, args: List<String> = emptyList())
}

/**
 * 仓颉命令定义
 *
 * 描述一个可执行的仓颉命令的基本信息
 */
data class CjCommand(
    /**
     * 命令标识符
     */
    val id: String,

    /**
     * 命令显示名称
     */
    val displayName: String,

    /**
     * 命令描述
     */
    val description: String,

    /**
     * 命令图标资源路径（可选）
     */
    val icon: String? = null,

    /**
     * 是否需要参数
     */
    val requiresArgs: Boolean = false,

    /**
     * 参数提示文本（当需要参数时）
     */
    val argPrompt: String? = null
)