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
import com.intellij.openapi.ui.ComboBox
import com.intellij.ui.components.JBTextField
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.panel
import org.cangnova.cangjie.project.CjProjectBundle
import org.cangnova.cangjie.project.model.CjModule
import org.cangnova.cangjie.project.service.CjProjectsService
import javax.swing.JComponent

/**
 * Settings editor for CangJie program run configurations.
 * Provides UI for selecting modules and configuring program execution.
 */
class CangJieProgramRunConfigurationEditor(private val project: Project) : SettingsEditor<CangJieProgramRunConfiguration>() {

    private val moduleComboBox = ComboBox<String>()
    private val programArgsField = JBTextField()
    private val workingDirectoryField = com.intellij.openapi.ui.TextFieldWithBrowseButton()
    private val envVarsComponent = EnvironmentVariablesComponent()

    private var mainPanel: JComponent? = null

    init {
        setupUI()
        refreshModules()
    }

    private fun setupUI() {
        // Module selection
        moduleComboBox.addActionListener { refreshWorkingDirectory() }

        // Working directory browser
        workingDirectoryField.addBrowseFolderListener(
            CjProjectBundle.message("run.configuration.editor.select.working.directory.title"),
            CjProjectBundle.message("run.configuration.editor.select.working.directory.description"),
            project,
            FileChooserDescriptorFactory.createSingleFolderDescriptor()
        )
    }

    private fun createMainPanel(): JComponent = panel {
        row(CjProjectBundle.message("run.configuration.editor.module.label")) {
            cell(moduleComboBox)
                .align(AlignX.FILL)
        }

        row(CjProjectBundle.message("run.configuration.editor.program.arguments.label")) {
            cell(programArgsField)
                .align(AlignX.FILL)
        }

        row(CjProjectBundle.message("run.configuration.editor.working.directory.label")) {
            cell(workingDirectoryField)
                .align(AlignX.FILL)
        }

        row (envVarsComponent.label) {
            cell(envVarsComponent.component)
                .align(AlignX.FILL)
        }
    }/*.withPreferredWidth(600).withPreferredHeight(400)*/

    private fun refreshModules() {
        val projectsService = CjProjectsService.getInstance(project)
        val cjProject = projectsService.cjProject

        logDebugInfo(cjProject)

        moduleComboBox.removeAllItems()

        if (!cjProject.isValid) {
            println("Debug: CangJie project is not valid or not initialized")
            moduleComboBox.addItem(CjProjectBundle.message("run.configuration.editor.no.valid.project"))
            return
        }

        val modules = when {
            cjProject.isWorkspace && cjProject.workspace != null -> {
                println("Debug: Using workspace modules, count = ${cjProject.workspace!!.modules.size}")
                cjProject.workspace!!.modules
            }
            cjProject.module != null -> {
                println("Debug: Using single module: ${cjProject.module!!.name}")
                listOf(cjProject.module!!)
            }
            else -> {
                println("Debug: No modules found in project")
                emptyList()
            }
        }

        if (modules.isEmpty()) {
            println("Debug: No modules found, adding placeholder")
            moduleComboBox.addItem(CjProjectBundle.message("run.configuration.editor.no.modules.found"))
        } else {
            modules.forEach { module ->
                println("Debug: Adding module: ${module.name} at ${module.rootDir.path}")
                moduleComboBox.addItem(module.name)
            }
        }
    }

    private fun logDebugInfo(cjProject: org.cangnova.cangjie.project.model.CjProject) {
        println("Debug: cjProject.name = ${cjProject.name}")
        println("Debug: cjProject.isValid = ${cjProject.isValid}")
        println("Debug: cjProject.isWorkspace = ${cjProject.isWorkspace}")
        println("Debug: cjProject.rootDir = ${cjProject.rootDir.path}")
    }

    private fun refreshWorkingDirectory() {
        val selectedModule = getSelectedModule()
        if (selectedModule != null) {
            workingDirectoryField.text = selectedModule.rootDir.path
        }
    }

    private fun getSelectedModule(): CjModule? {
        val selectedModuleName = moduleComboBox.selectedItem as? String ?: return null

        // Ignore placeholder items (both English and Chinese)
        val invalidProjectText = CjProjectBundle.message("run.configuration.editor.no.valid.project")
        val noModulesText = CjProjectBundle.message("run.configuration.editor.no.modules.found")

        if (selectedModuleName == invalidProjectText || selectedModuleName == noModulesText) {
            return null
        }

        // Also ignore old-style placeholder items for backward compatibility
        if (selectedModuleName.startsWith("<") && selectedModuleName.endsWith(">")) {
            return null
        }

        val projectsService = CjProjectsService.getInstance(project)
        return projectsService.cjProject.findModule(selectedModuleName)
    }

    override fun resetEditorFrom(configuration: CangJieProgramRunConfiguration) {
        refreshModules()

        // Set selected module
        configuration.moduleName?.let { moduleName ->
            val index = (0 until moduleComboBox.itemCount)
                .find { moduleComboBox.getItemAt(it) == moduleName }
            if (index != null) {
                moduleComboBox.selectedIndex = index
            }
        }

        programArgsField.text = configuration.programArgs.orEmpty()
        workingDirectoryField.text = configuration.workingDirectory?.toString().orEmpty()
        envVarsComponent.envData = configuration.env

    }

    override fun applyEditorTo(configuration: CangJieProgramRunConfiguration) {
        val selectedModuleName = moduleComboBox.selectedItem as? String

        // Only save module name if it's not a placeholder
        val invalidProjectText = CjProjectBundle.message("run.configuration.editor.no.valid.project")
        val noModulesText = CjProjectBundle.message("run.configuration.editor.no.modules.found")

        configuration.moduleName = when {
            selectedModuleName == invalidProjectText || selectedModuleName == noModulesText -> null
            selectedModuleName?.startsWith("<") == true -> null // Backward compatibility
            else -> selectedModuleName
        }

        configuration.programArgs = programArgsField.text.takeIf { it.isNotBlank() }
        configuration.workingDirectory = workingDirectoryField.text
            .takeIf { it.isNotBlank() }?.let { java.nio.file.Paths.get(it) }
        configuration.env = envVarsComponent.envData
    }

    override fun createEditor(): JComponent {
        return mainPanel ?: createMainPanel().also {
            mainPanel = it
        }
    }
}