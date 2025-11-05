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
import javax.swing.JPanel

/**
 * Settings editor for CangJie run configuration.
 * Provides fields for command, arguments, working directory, and environment variables.
 */
class CangJieRunConfigurationEditor(private val project: Project) : SettingsEditor<CangJieCommandRunConfiguration>() {

    private val commandField = RawCommandLineEditor()
    private val argsField = JBTextField()
    private val workingDirectoryField = TextFieldWithBrowseButton()
    private val envVarsComponent = EnvironmentVariablesComponent()

    init {
        commandField.dialogCaption = CjProjectBundle.message("run.configuration.editor.default.command.label")

        workingDirectoryField.addBrowseFolderListener(
            CjProjectBundle.message("run.configuration.editor.default.select.working.directory.title"),
            CjProjectBundle.message("run.configuration.editor.default.select.working.directory.description"),
            project,
            FileChooserDescriptorFactory.createSingleFolderDescriptor()
        )
    }

    override fun resetEditorFrom(configuration: CangJieCommandRunConfiguration) {
        commandField.text = configuration.command
        argsField.text = configuration.args
        workingDirectoryField.text = configuration.workingDirectory?.toString() ?: ""
        envVarsComponent.envData = configuration.env
    }

    override fun applyEditorTo(configuration: CangJieCommandRunConfiguration) {
        configuration.command = commandField.text
        configuration.args = argsField.text.takeIf { it.isNotBlank() }.toString()
        configuration.workingDirectory = workingDirectoryField.text.takeIf { it.isNotBlank() }?.let { Paths.get(it) }
        configuration.env = envVarsComponent.envData
    }

    override fun createEditor(): JComponent = panel {
//        row(CjProjectBundle.message("run.configuration.editor.default.build.system.label")) {
//            cell(buildSystemField)
//                .align(AlignX.FILL)
//        }

        row(CjProjectBundle.message("run.configuration.editor.default.command.label")) {
            cell(commandField)
                .align(AlignX.FILL)
        }

        row(CjProjectBundle.message("run.configuration.editor.default.arguments.label")) {
            cell(argsField)
                .align(AlignX.FILL)
        }

        row(CjProjectBundle.message("run.configuration.editor.default.working.directory.label")) {
            cell(workingDirectoryField)
                .align(AlignX.FILL)
        }

        row (envVarsComponent.label) {
            cell(envVarsComponent.component)
                .align(AlignX.FILL)
        }
    }
}