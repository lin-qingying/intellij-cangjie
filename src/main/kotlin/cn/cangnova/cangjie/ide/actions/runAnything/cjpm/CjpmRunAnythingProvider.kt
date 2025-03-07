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

package cn.cangnova.cangjie.ide.actions.runAnything.cjpm

import cn.cangnova.cangjie.CangJieBundle
import cn.cangnova.cangjie.cjpm.project.model.CjpmProject
import cn.cangnova.cangjie.cjpm.project.model.cjpmProjects
import cn.cangnova.cangjie.ide.actions.runAnything.CjRunAnythingProvider
import cn.cangnova.cangjie.ide.actions.runAnything.RunAnythingCjpmItem
import cn.cangnova.cangjie.ide.actions.runAnything.getAppropriateCjpmProject
import cn.cangnova.cangjie.icon.CangJieIcons
import cn.cangnova.cangjie.ide.run.cjpm.CjCommandCompletionProvider
import cn.cangnova.cangjie.ide.run.cjpm.CjpmCommandCompletionProvider
import cn.cangnova.cangjie.ide.run.cjpm.CjpmCommandLine
import com.intellij.execution.Executor
import com.intellij.ide.actions.runAnything.items.RunAnythingItem
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.project.Project
import java.nio.file.Path
import javax.swing.Icon

/**
 * CjpmRunAnythingProvider 类继承自 CjRunAnythingProvider，用于提供与 Cjpm 相关的运行任何命令的功能。
 * 它实现了运行命令、获取命令图标、获取命令补全提供者等功能。
 */
class CjpmRunAnythingProvider : CjRunAnythingProvider() {

    /**
     * 伴生对象用于定义帮助命令的常量。
     */
    companion object {
        const val HELP_COMMAND = "cjpm"
    }

    /**
     * 获取主列表项。
     *
     * @param dataContext 数据上下文，用于获取执行上下文信息。
     * @param value 用户输入的命令值。
     * @return 返回一个 RunAnythingCjpmItem，包含命令和图标信息。
     */
    override fun getMainListItem(dataContext: DataContext, value: String): RunAnythingItem =
        RunAnythingCjpmItem(getCommand(value), getIcon(value))

    /**
     * 获取命令图标。
     *
     * @param value 用户输入的命令值，此处未使用。
     * @return 返回 CangJie 图标，作为命令的视觉标识。
     */
    override fun getIcon(value: String): Icon = CangJieIcons.CANGJIE

    /**
     * 执行命令。
     *
     * @param executor 执行器，用于执行命令。
     * @param command 要执行的命令。
     * @param params 命令的参数列表。
     * @param workingDirectory 命令执行的工作目录。
     * @param cjpmProject Cjpm 项目对象，包含项目相关信息。
     */
    override fun run(
        executor: Executor,
        command: String,
        params: List<String>,
        workingDirectory: Path,
        cjpmProject: CjpmProject
    ) {
        CjpmCommandLine(command, workingDirectory, params).run(cjpmProject, executor = executor)
    }

    /**
     * 获取命令补全提供者。
     *
     * @param project 项目对象，用于获取项目相关信息。
     * @param dataContext 数据上下文，用于获取执行上下文信息。
     * @return 返回一个 CjpmCommandCompletionProvider，用于提供命令补全建议。
     */
    override fun getCompletionProvider(project: Project, dataContext: DataContext): CjCommandCompletionProvider =
        CjpmCommandCompletionProvider(project.cjpmProjects) {
            getAppropriateCjpmProject(dataContext)?.workspace
        }

    /**
     * 获取帮助命令。
     *
     * @return 返回帮助命令字符串。
     */
    override fun getHelpCommand(): String = HELP_COMMAND

    /**
     * 获取帮助组标题。
     *
     * @return 返回帮助组标题，用于在帮助文档中显示。
     */
    override fun getHelpGroupTitle(): String = CangJieBundle.message("build.event.title.cjpm")

    /**
     * 获取命令。
     *
     * @param value 用户输入的命令值。
     * @return 返回经过处理的命令字符串。
     */
    override fun getCommand(value: String): String = value

    /**
     * 获取命令补全组标题。
     *
     * @return 返回命令补全组标题，用于在命令补全界面显示。
     */
    override fun getCompletionGroupTitle(): String = CangJieBundle.message("cjpm.commands")

    /**
     * 获取帮助描述。
     *
     * @return 返回帮助描述字符串，用于在帮助文档中显示。
     */
    override fun getHelpDescription(): String = CangJieBundle.message("runs.cjpm.command")
}
