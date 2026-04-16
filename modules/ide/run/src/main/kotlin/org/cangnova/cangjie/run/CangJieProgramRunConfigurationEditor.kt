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

package org.cangnova.cangjie.run

import com.intellij.execution.configuration.EnvironmentVariablesComponent
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComboBox
import com.intellij.ui.components.JBTextField
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.panel
import org.cangnova.cangjie.project.CjProjectBundle
import org.cangnova.cangjie.project.model.CjModule
import org.cangnova.cangjie.project.service.CjProjectsService
import javax.swing.JComponent

/**
 * 仓颉程序运行配置编辑器
 *
 * 提供选择模块和配置程序执行的用户界面
 */
class CangJieProgramRunConfigurationEditor(private val project: Project) : SettingsEditor<CangJieProgramRunConfiguration>() {

    // UI 组件
    private val moduleComboBox = ComboBox<String>()
    private val programArgsField = JBTextField()
    private val workingDirectoryField = com.intellij.openapi.ui.TextFieldWithBrowseButton()
    private val envVarsComponent = EnvironmentVariablesComponent()

    private var mainPanel: JComponent? = null

    init {
        setupUI()
        refreshModules()
    }

    /**
     * 初始化 UI 组件
     */
    private fun setupUI() {
        // 模块选择监听器 - 自动更新工作目录
        moduleComboBox.addActionListener { refreshWorkingDirectory() }

        // 工作目录浏览器配置
        workingDirectoryField.addBrowseFolderListener(
            CjProjectBundle.message("run.configuration.editor.select.working.directory.title"),
            CjProjectBundle.message("run.configuration.editor.select.working.directory.description"),
            project,
            FileChooserDescriptorFactory.createSingleFolderDescriptor()
        )
    }

    /**
     * 创建主面板
     *
     * @return 包含所有配置项的主面板组件
     */
    private fun createMainPanel(): JComponent = panel {
        // 模块选择下拉框
        row(CjProjectBundle.message("run.configuration.editor.module.label")) {
            cell(moduleComboBox)
                .align(AlignX.FILL)
        }

        // 程序参数输入框
        row(CjProjectBundle.message("run.configuration.editor.program.arguments.label")) {
            cell(programArgsField)
                .align(AlignX.FILL)
        }

        // 工作目录选择器
        row(CjProjectBundle.message("run.configuration.editor.working.directory.label")) {
            cell(workingDirectoryField)
                .align(AlignX.FILL)
        }

        // 环境变量配置
        row(envVarsComponent.label) {
            cell(envVarsComponent.component)
                .align(AlignX.FILL)
        }
    }

    /**
     * 刷新可用模块列表
     *
     * 从项目服务中获取所有可用的仓颉模块，并更新下拉框
     */
    private fun refreshModules() {
        val projectsService = CjProjectsService.getInstance(project)
        val cjProject = projectsService.cjProject

        logDebugInfo(cjProject)

        moduleComboBox.removeAllItems()

        // 检查项目有效性
        if (!cjProject.isValid) {
            moduleComboBox.addItem(CjProjectBundle.message("run.configuration.editor.no.valid.project"))
            return
        }

        // 获取模块列表
        val modules = when {
            cjProject.isWorkspace -> {
                // 工作区模式：获取所有子模块
                val workspace = cjProject.workspace
                if (workspace != null) {
                    println("Debug: 使用工作区模块，数量 = ${workspace.modules.size}")
                    workspace.modules
                } else {
                    println("Debug: 未找到工作区")
                    emptyList()
                }
            }
            else -> {
                // 单模块模式
                val module = cjProject.module
                if (module != null) {
                    println("Debug: 使用单模块: ${module.name}")
                    listOf(module)
                } else {
                    println("Debug: 项目中未找到模块")
                    emptyList()
                }
            }
        }

        // 填充下拉框
        if (modules.isEmpty()) {
            println("Debug: 未找到模块，添加占位符")
            moduleComboBox.addItem(CjProjectBundle.message("run.configuration.editor.no.modules.found"))
        } else {
            modules.forEach { module ->
                println("Debug: 添加模块: ${module.name} 位于 ${module.rootDir.path}")
                moduleComboBox.addItem(module.name)
            }
        }
    }

    /**
     * 输出调试信息
     *
     * @param cjProject 仓颉项目实例
     */
    private fun logDebugInfo(cjProject: org.cangnova.cangjie.project.model.CjProject) {
        println("Debug: 项目名称 = ${cjProject.name}")
        println("Debug: 项目是否有效 = ${cjProject.isValid}")
        println("Debug: 是否为工作区 = ${cjProject.isWorkspace}")
        println("Debug: 根目录 = ${cjProject.rootDir.path}")
    }

    /**
     * 根据选中的模块刷新工作目录
     */
    private fun refreshWorkingDirectory() {
        val selectedModule = getSelectedModule()
        if (selectedModule != null) {
            workingDirectoryField.text = selectedModule.rootDir.path
        }
    }

    /**
     * 获取当前选中的模块
     *
     * @return 选中的模块实例，如果未选中或选中无效项则返回 null
     */
    private fun getSelectedModule(): CjModule? {
        val selectedModuleName = moduleComboBox.selectedItem as? String ?: return null

        // 忽略占位符项（支持中英文）
        val invalidProjectText = CjProjectBundle.message("run.configuration.editor.no.valid.project")
        val noModulesText = CjProjectBundle.message("run.configuration.editor.no.modules.found")

        if (selectedModuleName == invalidProjectText || selectedModuleName == noModulesText) {
            return null
        }

        // 向后兼容：忽略旧式占位符项
        if (selectedModuleName.startsWith("<") && selectedModuleName.endsWith(">")) {
            return null
        }

        val projectsService = CjProjectsService.getInstance(project)
        return projectsService.cjProject.findModule(selectedModuleName)
    }

    /**
     * 从配置重置编辑器状态
     *
     * @param configuration 运行配置实例
     */
    override fun resetEditorFrom(configuration: CangJieProgramRunConfiguration) {
        refreshModules()

        // 设置选中的模块
        configuration.moduleName?.let { moduleName ->
            val index = (0 until moduleComboBox.itemCount)
                .find { moduleComboBox.getItemAt(it) == moduleName }
            if (index != null) {
                moduleComboBox.selectedIndex = index
            }
        }

        // 恢复其他配置项
        programArgsField.text = configuration.programArgs.orEmpty()
        workingDirectoryField.text = configuration.workingDirectory?.toString().orEmpty()
        envVarsComponent.envData = configuration.env
    }

    /**
     * 将编辑器状态应用到配置
     *
     * @param configuration 运行配置实例
     */
    override fun applyEditorTo(configuration: CangJieProgramRunConfiguration) {
        val selectedModuleName = moduleComboBox.selectedItem as? String

        // 只有在非占位符项时才保存模块名
        val invalidProjectText = CjProjectBundle.message("run.configuration.editor.no.valid.project")
        val noModulesText = CjProjectBundle.message("run.configuration.editor.no.modules.found")

        configuration.moduleName = when {
            selectedModuleName == invalidProjectText || selectedModuleName == noModulesText -> null
            selectedModuleName?.startsWith("<") == true -> null // 向后兼容
            else -> selectedModuleName
        }

        // 保存其他配置项
        configuration.programArgs = programArgsField.text.takeIf { it.isNotBlank() }
        configuration.workingDirectory = workingDirectoryField.text
            .takeIf { it.isNotBlank() }?.let { java.nio.file.Paths.get(it) }
        configuration.env = envVarsComponent.envData
    }

    /**
     * 创建编辑器组件
     *
     * @return 编辑器的主面板组件
     */
    override fun createEditor(): JComponent {
        return mainPanel ?: createMainPanel().also {
            mainPanel = it
        }
    }
}