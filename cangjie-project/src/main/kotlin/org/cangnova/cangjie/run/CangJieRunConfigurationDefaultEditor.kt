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

package org.cangnova.cangjie.run

import com.intellij.execution.configuration.EnvironmentVariablesComponent
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.ui.RawCommandLineEditor
import com.intellij.ui.components.JBTextField
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.panel
import org.cangnova.cangjie.project.CjProjectBundle
import java.nio.file.Paths
import javax.swing.JComponent

/**
 * 仓颉运行配置编辑器
 *
 * 提供命令、参数、工作目录和环境变量的配置字段
 */
class CangJieRunConfigurationEditor(private val project: Project) : SettingsEditor<CangJieCommandRunConfiguration>() {

    // UI 组件
    private val commandField = RawCommandLineEditor()
    private val argsField = JBTextField()
    private val workingDirectoryField = TextFieldWithBrowseButton()
    private val envVarsComponent = EnvironmentVariablesComponent()

    init {
        // 设置命令字段的对话框标题
        commandField.dialogCaption = CjProjectBundle.message("run.configuration.editor.default.command.label")

        // 配置工作目录浏览器
        workingDirectoryField.addBrowseFolderListener(
            CjProjectBundle.message("run.configuration.editor.default.select.working.directory.title"),
            CjProjectBundle.message("run.configuration.editor.default.select.working.directory.description"),
            project,
            FileChooserDescriptorFactory.createSingleFolderDescriptor()
        )
    }

    /**
     * 从配置重置编辑器状态
     *
     * @param configuration 运行配置实例
     */
    override fun resetEditorFrom(configuration: CangJieCommandRunConfiguration) {
        commandField.text = configuration.command
        argsField.text = configuration.args
        workingDirectoryField.text = configuration.workingDirectory?.toString() ?: ""
        envVarsComponent.envData = configuration.env
    }

    /**
     * 将编辑器状态应用到配置
     *
     * @param configuration 运行配置实例
     */
    override fun applyEditorTo(configuration: CangJieCommandRunConfiguration) {
        configuration.command = commandField.text
        configuration.args = argsField.text.takeIf { it.isNotBlank() }.toString()
        configuration.workingDirectory = workingDirectoryField.text
            .takeIf { it.isNotBlank() }?.let { Paths.get(it) }
        configuration.env = envVarsComponent.envData
    }

    /**
     * 创建编辑器 UI 组件
     *
     * @return 包含所有配置字段的面板组件
     */
    override fun createEditor(): JComponent = panel {
        // 注释掉的构建系统选择器 - 保留以备将来使用
//        row(CjProjectBundle.message("run.configuration.editor.default.build.system.label")) {
//            cell(buildSystemField)
//                .align(AlignX.FILL)
//        }

        // 命令输入框
        row(CjProjectBundle.message("run.configuration.editor.default.command.label")) {
            cell(commandField)
                .align(AlignX.FILL)
        }

        // 参数输入框
        row(CjProjectBundle.message("run.configuration.editor.default.arguments.label")) {
            cell(argsField)
                .align(AlignX.FILL)
        }

        // 工作目录选择器
        row(CjProjectBundle.message("run.configuration.editor.default.working.directory.label")) {
            cell(workingDirectoryField)
                .align(AlignX.FILL)
        }

        // 环境变量配置
        row(envVarsComponent.label) {
            cell(envVarsComponent.component)
                .align(AlignX.FILL)
        }
    }
}