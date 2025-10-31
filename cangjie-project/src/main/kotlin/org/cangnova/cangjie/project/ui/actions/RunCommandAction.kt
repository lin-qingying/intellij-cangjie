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

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.Presentation
import com.intellij.openapi.project.Project
import org.cangnova.cangjie.project.service.cangjieProjectService

/**
 * 运行仓颉命令的动作
 *
 * 该动作使用 IntelliJ 的 Run Anything 功能来执行仓颉项目相关的命令。
 * 提供统一的命令输入界面，支持自动补全和命令历史。
 */
class RunCommandAction : CangJieProjectActionBase() {

    init {
        templatePresentation.text = "Run Command"
        templatePresentation.description = "Run a command using Run Anything"
        templatePresentation.icon = AllIcons.Actions.Execute
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return

        // 获取第一个可用的帮助命令
        val helpCommand = getAvailableHelpCommand(project)
        if (helpCommand == null) {
            // 如果没有可用的命令提供者，显示提示
            return
        }

        // 打开 Run Anything 界面
        openRunAnything(project, helpCommand, e)
    }

    /**
     * 获取可用的帮助命令
     *
     * @param project 当前项目
     * @return 第一个可用的帮助命令，如果没有则返回 null
     */
    private fun getAvailableHelpCommand(project: Project): String? {
        // 通过扩展点获取所有可用的命令提供者
        val providers = CjCommandProvider.EP_NAME.extensionList
        if (providers.isEmpty()) {
            return null
        }

        // 返回第一个提供者的帮助命令
        // 也可以根据优先级或其他逻辑选择提供者
        return providers.first().getHelpCommand()
    }

    /**
     * 打开 Run Anything 界面
     *
     * @param project 当前项目
     * @param helpCommand 帮助命令
     * @param e 动作事件
     */
    private fun openRunAnything(project: Project, helpCommand: String, e: AnActionEvent) {
        try {
            // 尝试获取 RunAnythingManager
            val runAnythingManagerClass = Class.forName("com.intellij.ide.actions.runAnything.RunAnythingManager")
            val getInstanceMethod = runAnythingManagerClass.getMethod("getInstance", Project::class.java)
            val runAnythingManager = getInstanceMethod.invoke(null, project)

            // 调用 show 方法
            val showMethod = runAnythingManagerClass.getMethod("show", String::class.java, Boolean::class.java, AnActionEvent::class.java)
            showMethod.invoke(runAnythingManager, "$helpCommand ", false, e)

        } catch (e: Exception) {
            // 如果 RunAnything 不可用，静默处理
            // 可以考虑在这里添加回退方案
        }
    }

    override fun updatePresentation(e: AnActionEvent, presentation: Presentation) {
        val project = e.project

        // 只要有项目就启用
        presentation.isEnabled = project != null && project.cangjieProjectService.cjProject.isValid
    }
}