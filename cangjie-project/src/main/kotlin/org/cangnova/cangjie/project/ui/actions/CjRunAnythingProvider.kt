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

import com.intellij.execution.Executor
import com.intellij.execution.executors.DefaultRunExecutor
import com.intellij.ide.actions.runAnything.RunAnythingAction
import com.intellij.ide.actions.runAnything.RunAnythingContext

import com.intellij.ide.actions.runAnything.activity.RunAnythingProvider
import com.intellij.ide.actions.runAnything.activity.RunAnythingProviderBase
import com.intellij.ide.actions.runAnything.getPath
import com.intellij.ide.actions.runAnything.items.RunAnythingItem
import com.intellij.ide.actions.runAnything.items.RunAnythingItemBase
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.intellij.openapi.util.text.StringUtil
import com.intellij.ui.SimpleColoredComponent
import com.intellij.ui.SimpleTextAttributes
import com.intellij.util.execution.ParametersListUtil

import org.cangnova.cangjie.project.model.CjProject
import org.cangnova.cangjie.project.service.cangjieProjectService
import org.cangnova.cangjie.project.ui.toolwindow.CjProjectToolWindow
import org.cangnova.cangjie.utils.toPath
import java.awt.BorderLayout
import java.awt.Component
import java.nio.file.Path
import javax.swing.Icon
import javax.swing.JPanel

abstract class CjRunAnythingItem(command: String, icon: Icon) : RunAnythingItemBase(command, icon){

    abstract val helpCommand: String

    abstract val commandDescriptions: Map<String, String>

    abstract fun getOptionsDescriptionsForCommand(commandName: String): Map<String, String>?

    override fun createComponent(pattern: String?, isSelected: Boolean, hasFocus: Boolean): Component =
        super.createComponent(pattern, isSelected, hasFocus).also(this::customizeComponent)

    private fun customizeComponent(component: Component) {
        if (component !is JPanel) return

        val params = ParametersListUtil.parse(StringUtil.trimStart(command, helpCommand))
        @Suppress("HardCodedStringLiteral") val description = when (params.size) {
            0 -> null
            1 -> commandDescriptions[params.last()]
            else -> {
                val optionsDescriptions = getOptionsDescriptionsForCommand(params.first())
                optionsDescriptions?.get(params.last())
            }
        } ?: return
        val descriptionComponent = SimpleColoredComponent()
        descriptionComponent.append(
            StringUtil.shortenTextWithEllipsis(" $description.", 200, 0),
            SimpleTextAttributes.GRAYED_ITALIC_ATTRIBUTES
        )
        component.add(descriptionComponent, BorderLayout.EAST)
    }
}

/**
 * 仓颉项目 RunAnything 提供者的抽象基类
 *
 * 提供通用的 Run Anything 集成框架，用于执行仓颉项目相关的命令。
 * 具体的命令实现通过继承此类并实现抽象方法来提供。
 */
abstract class CjRunAnythingProvider : RunAnythingProviderBase<String>() {


    abstract override fun getMainListItem(dataContext: DataContext, value: String): RunAnythingItem

    /**
     * 执行命令的核心逻辑
     *
     * @param executor 命令执行器
     * @param command 主命令
     * @param params 命令参数列表
     * @param workingDirectory 工作目录
     * @param cjProject 仓颉项目实例
     */
    protected abstract fun run(
        executor: Executor,
        command: String,
        params: List<String>,
        workingDirectory: Path,
        cjProject: CjProject
    )

    /**
     * 获取命令补全提供者
     *
     * @param project 当前项目
     * @param dataContext 数据上下文
     * @return 命令补全提供者
     */
    abstract fun getCompletionProvider(project: Project, dataContext: DataContext): CjCommandCompletionProvider

    override fun findMatchingValue(dataContext: DataContext, pattern: String): String? =
        if (pattern.startsWith(helpCommand)) getCommand(pattern) else null

    override fun getValues(dataContext: DataContext, pattern: String): Collection<String> {
        val project = dataContext.getData(CommonDataKeys.PROJECT) ?: return emptyList()
        if (!project.hasCjProject) return emptyList()
        val completionProvider = getCompletionProvider(project, dataContext)

        return when {
            pattern.startsWith(helpCommand) -> {
                val context = StringUtil.trimStart(pattern, helpCommand).substringBeforeLast(' ')
                val prefix = pattern.substringBeforeLast(' ')
                completionProvider.complete(context).map { "$prefix ${it.lookupString}" }
            }
            pattern.isNotBlank() && helpCommand.startsWith(pattern) ->
                completionProvider.complete("").map { "$helpCommand ${it.lookupString}" }
            else -> emptyList()
        }
    }

    override fun execute(dataContext: DataContext, value: String) {
        val project = dataContext.getData(CommonDataKeys.PROJECT) ?: return
        if (!project.hasCjProject) return
        val cjProject = getAppropriateCjProject(dataContext) ?: return
        val params = ParametersListUtil.parse(StringUtil.trimStart(value, helpCommand))
        val executionContext = dataContext.getData(EXECUTING_CONTEXT) ?: RunAnythingContext.ProjectContext(project)
        val path = executionContext.getPath()?.toPath() ?: return
        val executor = dataContext.getData(RunAnythingAction.EXECUTOR_KEY) ?: DefaultRunExecutor.getRunExecutorInstance()
        run(executor, params.firstOrNull() ?: "--help", params.drop(1), path, cjProject)
    }

    abstract override fun getHelpCommand(): String

    /**
     * 获取适合的仓颉项目
     *
     * 在单项目模型中，直接返回当前 IntelliJ 项目对应的仓颉项目。
     * 优先使用工具窗口选中的项目（如果有），否则返回当前项目。
     */
    private fun getAppropriateCjProject(dataContext: DataContext): CjProject? {
        val projectService = dataContext.getData(CommonDataKeys.PROJECT)?.cangjieProjectService ?: return null
        val cjProject = projectService.cjProject

        // 如果项目无效，返回 null
        if (!cjProject.isValid) return null

        // 优先使用工具窗口选中的项目（通常就是当前项目）
        return dataContext.getData(CjProjectToolWindow.SELECTED_CJ_PROJECT) ?: cjProject
    }
}

/**
 * 项目扩展属性：检查项目是否有仓颉项目
 */
private val Project.hasCjProject: Boolean
    get() = this.cangjieProjectService.cjProject.isValid

