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

import com.intellij.execution.ExternalizablePath
import com.intellij.execution.configurations.*
import com.intellij.openapi.project.Project
import org.cangnova.cangjie.project.CjProjectBundle
import org.jdom.Element
import java.nio.file.Path
import java.nio.file.Paths

/**
 * Base configuration for CangJie run configurations.
 * This is an abstract class that should be extended by subsystems (e.g., CJPM, CJC)
 * to provide concrete implementations.
 */
abstract class CangJieCommandConfiguration(
    project: Project,
    name: String,
    factory: ConfigurationFactory
) : LocatableConfigurationBase<RunProfileState>(project, factory, name),
    RunConfigurationWithSuppressedDefaultDebugAction {

    /**
     * The command to execute (e.g., "run", "build", "test")
     */
    abstract var command: String

    /**
     * Working directory for command execution
     */
    var workingDirectory: Path? = if (!project.isDefault) {
        project.basePath?.let { Paths.get(it) }
    } else {
        null
    }

    override fun writeExternal(element: Element) {
        super.writeExternal(element)
        element.writeString("command", command)
        element.writePath("workingDirectory", workingDirectory)
    }

    override fun readExternal(element: Element) {
        super.readExternal(element)
        element.readString("command")?.let { command = it }
        element.readPath("workingDirectory")?.let { workingDirectory = it }
    }

    override fun checkConfiguration() {
        if (command.isBlank()) {
            throw RuntimeConfigurationError(CjProjectBundle.message("run.configuration.error.command.empty"))
        }
        if (workingDirectory == null) {
            throw RuntimeConfigurationError(CjProjectBundle.message("run.configuration.error.working.directory.missing"))
        }
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