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

package org.cangnova.cangjie.cjpm.actions

import com.intellij.execution.Executor
import com.intellij.execution.executors.DefaultRunExecutor
import com.intellij.ide.actions.runAnything.items.RunAnythingItem
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.project.Project
import org.cangnova.cangjie.icon.CangJieIcons


import org.cangnova.cangjie.project.model.CjProject
import org.cangnova.cangjie.project.service.cangjieProjectService
import org.cangnova.cangjie.project.ui.actions.CjRunAnythingProvider
import org.cangnova.cangjie.project.ui.actions.CjCommandCompletionProvider
import org.cangnova.cangjie.project.ui.actions.CjRunAnythingItem
import org.cangnova.cangjie.project.ui.actions.CommandProviderCompletionProvider
import org.cangnova.cangjie.project.ui.toolwindow.CjProjectToolWindow
import java.awt.Component
import java.nio.file.Path
import javax.swing.Icon

/**
 * CJPM RunAnything 提供者
 *
 * 将 CJPM 命令提供者集成到 RunAnything 框架中
 */
class CjpmRunAnythingProvider : CjRunAnythingProvider() {

    override fun getMainListItem(dataContext: DataContext, value: String): RunAnythingItem =
        CjpmRunAnythingItem(getCommand(value), getIcon(value))

    override fun getIcon(value: String): Icon = CangJieIcons.CANGJIE

    override fun run(
        executor: Executor,
        command: String,
        params: List<String>,
        workingDirectory: Path,
        cjProject: CjProject
    ) {
        // 获取 CJPM 命令提供者并执行命令
        val project = cjProject.intellijProject
        val cjpmCommandProvider = CjpmCommandProvider()

        // 查找匹配的命令
        val availableCommands = cjpmCommandProvider.getAvailableCommands(project, cjProject)
        val matchedCommand = availableCommands.find { it.id == command }

        if (matchedCommand != null) {
            cjpmCommandProvider.executeCommand(project, cjProject, matchedCommand, params)
        } else {
            // 作为自定义命令执行
            val customCommand = org.cangnova.cangjie.project.ui.actions.CjCommand(
                id = "custom",
                displayName = "Custom Command",
                description = "Custom command execution",
                requiresArgs = false
            )
            cjpmCommandProvider.executeCommand(project, cjProject, customCommand, listOf(command) + params)
        }
    }

    override fun getCompletionProvider(project: Project, dataContext: DataContext): CjCommandCompletionProvider =
        CommandProviderCompletionProvider(project) {
            getAppropriateCjProject(dataContext)
        }

    override fun getHelpCommand(): String = "cjpm"

    override fun getCommand(value: String): String = value

    /**
     * 获取适合的仓颉项目
     *
     * 在单项目模型中，直接返回当前 IntelliJ 项目对应的仓颉项目。
     * 优先使用工具窗口选中的项目（如果有），否则返回当前项目。
     */
    private fun getAppropriateCjProject(dataContext: DataContext): CjProject? {
        val project = dataContext.getData(com.intellij.openapi.actionSystem.CommonDataKeys.PROJECT) ?: return null
        val projectService = project.cangjieProjectService
        val cjProject = projectService.cjProject

        // 如果项目无效，返回 null
        if (!cjProject.isValid) return null

        // 优先使用工具窗口选中的项目（通常就是当前项目）
        return dataContext.getData(CjProjectToolWindow.SELECTED_CJ_PROJECT) ?: cjProject
    }
}

class CjpmRunAnythingItem(command: String, icon: Icon) : CjRunAnythingItem(command, icon) {
    override val helpCommand: String = "cjpm"

    override val commandDescriptions: Map<String, String> = emptyMap()
//        CjpmCommands.entries.associate { it.presentableName to it.description }

    override fun getOptionsDescriptionsForCommand(commandName: String): Map<String, String>? {
//        val command = CjpmCommands.entries.find { it.presentableName == commandName } ?: return null
//        return command.options.associate { it.longName to it.description }
        return null
    }
}
