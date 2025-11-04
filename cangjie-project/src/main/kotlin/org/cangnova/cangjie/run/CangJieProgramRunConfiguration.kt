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

import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.RuntimeConfigurationError
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project
import org.cangnova.cangjie.project.CjProjectBundle
import org.cangnova.cangjie.project.model.CjModule
import org.cangnova.cangjie.project.service.CjProjectsService
import org.jdom.Element

/**
 * Run configuration for executing CangJie modules.
 * Used for running compiled CangJie modules directly.
 */
class CangJieProgramRunConfiguration(
    project: Project,
    factory: ConfigurationFactory,
    name: String
) : CangJieRunConfigurationBase(project, factory, name) {

    /**
     * Target module to run
     */
    var moduleName: String? = null

    /**
     * Program arguments
     */
    var programArgs: String? = null

    /**
     * Gets the target CjModule for this configuration
     */
    fun getCjModule(): CjModule? {
        val moduleName = this.moduleName ?: return null

        val projectsService = CjProjectsService.getInstance(project)
        val cjProject = projectsService.cjProject

        // Debug information
        println("Debug: Looking for module '$moduleName' in project '${cjProject.name}'")

        if (!cjProject.isValid) {
            println("Debug: CangJie project is not valid")
            return null
        }

        val module = cjProject.findModule(moduleName)
        if (module != null) {
            println("Debug: Found module '${module.name}' at ${module.rootDir.path}")
        } else {
            println("Debug: Module '$moduleName' not found")
            // List available modules for debugging
            val availableModules = if (cjProject.isWorkspace && cjProject.workspace != null) {
                cjProject.workspace!!.modules
            } else {
                cjProject.module?.let { listOf(it) } ?: emptyList()
            }
            println("Debug: Available modules: ${availableModules.map { it.name }}")
        }

        return module
    }

    /**
     * Gets the target IntelliJ Module for this configuration
     */
    fun getIntellijModule(): Module? {
        val moduleName = this.moduleName ?: return null
        return ModuleManager.getInstance(project).findModuleByName(moduleName)
    }

    override fun createRunState(
        environment: ExecutionEnvironment,
        commandExecutor: CangJieCommandExecutor
    ) = CangJieProgramRunState(environment, this)

    override fun getConfigurationEditor() = CangJieProgramRunConfigurationEditor(project)

    override fun suggestedName(): String {
        val module = getCjModule()
        return module?.name ?: project.name
    }

    override fun checkConfiguration() {
        super.checkConfiguration()

        val module = getCjModule()
        if (module == null) {
            throw RuntimeConfigurationError(CjProjectBundle.message("run.configuration.error.no.module.selected"))
        }

        val systemId = org.cangnova.cangjie.project.service.CjProjectBuildSystemService.getInstance().getBuildSystem()
            ?: throw RuntimeConfigurationError(CjProjectBundle.message("run.configuration.error.cannot.detect.build.system"))

        val executor = CangJieCommandExecutor.findExecutor()
            ?: throw RuntimeConfigurationError(
                CjProjectBundle.message(
                    "run.configuration.error.no.command.executor",
                    systemId.id
                )
            )

        // Validate using subsystem executor
        val error = executor.validateConfiguration(this)
        if (error != null) {
            throw RuntimeConfigurationError(error)
        }
    }

    override fun writeExternal(element: Element) {
        super.writeExternal(element)
        moduleName?.let { element.writeString("moduleName", it) }
        programArgs?.let { element.writeString("programArgs", it) }
    }

    override fun readExternal(element: Element) {
        super.readExternal(element)
        element.readString("moduleName")?.let { moduleName = it }
        element.readString("programArgs")?.let { programArgs = it }
    }
}