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

import com.intellij.execution.Executor
import com.intellij.execution.ExternalizablePath
import com.intellij.execution.configuration.EnvironmentVariablesData
import com.intellij.execution.configurations.*
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.execution.target.RunTargetsEnabled
import com.intellij.execution.target.TargetEnvironmentAwareRunProfile
import com.intellij.execution.target.TargetEnvironmentConfiguration
import com.intellij.execution.target.TargetEnvironmentsManager
import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.project.Project
import org.cangnova.cangjie.project.CjProjectBundle
import org.cangnova.cangjie.project.service.CjProjectBuildSystemService
import org.jdom.Element
import java.nio.file.Path
import java.nio.file.Paths
val CangJieRunConfigurationBase.targetEnvironment: TargetEnvironmentConfiguration?
    get() {
        if (!RunTargetsEnabled.get()) return null
        val targetName = defaultTargetName ?: return null
        return TargetEnvironmentsManager.getInstance(project).targets.findByName(targetName)
    }
/**
 * Base class for CangJie run configurations.
 * Provides common functionality for both command and program run configurations.
 */
abstract class CangJieRunConfigurationBase(
    project: Project,
    factory: ConfigurationFactory,
    name: String
) : LocatableConfigurationBase<RunProfileState>(project, factory, name),
    RunConfigurationWithSuppressedDefaultDebugAction ,
    TargetEnvironmentAwareRunProfile{

    /**
     * Working directory for command execution
     */
    var workingDirectory: Path? = if (!project.isDefault) {
        project.basePath?.let { Paths.get(it) }
    } else {
        null
    }
    override fun getDefaultTargetName(): String? = options.remoteTarget

    override fun setDefaultTargetName(targetName: String?) {
        options.remoteTarget = targetName
    }

    override fun canRunOn(target: TargetEnvironmentConfiguration): Boolean {
        // CangJie configurations can run on any target environment
        return true
    }

    override fun getDefaultLanguageRuntimeType(): com.intellij.execution.target.LanguageRuntimeType<*>? {
        return org.cangnova.cangjie.run.target.CjLanguageRuntimeType()
    }

    /**
     * Environment variables
     */
    var env: EnvironmentVariablesData = EnvironmentVariablesData.DEFAULT

    override fun getState(executor: Executor, environment: ExecutionEnvironment): RunProfileState? {
        val commandExecutor = CangJieCommandExecutor.findExecutor() ?: return null
        return createRunState(environment, commandExecutor)
    }

    /**
     * Create the run state for this configuration.
     * Subclasses should override this to provide their specific run state.
     */
    protected abstract fun createRunState(
        environment: ExecutionEnvironment,
        commandExecutor: CangJieCommandExecutor
    ): RunProfileState


    override fun checkConfiguration() {
        super.checkConfiguration()

        if (workingDirectory == null) {
            throw RuntimeConfigurationError(CjProjectBundle.message("run.configuration.error.working.directory.missing"))
        }
    }

    override fun writeExternal(element: Element) {
        super.writeExternal(element)
        element.writePath("workingDirectory", workingDirectory)
        env.writeExternal(element)
    }

    override fun readExternal(element: Element) {
        super.readExternal(element)
        element.readPath("workingDirectory")?.let { workingDirectory = it }
        env = EnvironmentVariablesData.readExternal(element)
    }
}

// XML serialization helpers
fun Element.writeString(name: String, value: String) {
    val opt = Element("option")
    opt.setAttribute("name", name)
    opt.setAttribute("value", value)
    addContent(opt)
}

fun Element.readString(name: String): String? =
    children
        .find { it.name == "option" && it.getAttributeValue("name") == name }
        ?.getAttributeValue("value")

fun Element.writePath(name: String, value: Path?) {
    if (value != null) {
        val s = ExternalizablePath.urlValue(value.toString())
        writeString(name, s)
    }
}

fun Element.readPath(name: String): Path? {
    return readString(name)?.let { Paths.get(ExternalizablePath.localPathValue(it)) }
}