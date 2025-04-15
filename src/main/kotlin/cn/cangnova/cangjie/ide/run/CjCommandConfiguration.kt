/*
 * Copyright 2024 LinQingYing. and contributors.
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

package cn.cangnova.cangjie.ide.run


import cn.cangnova.cangjie.cjpm.project.model.cjpmProjects
import cn.cangnova.cangjie.cjpm.project.model.impl.workingDirectory
import cn.cangnova.cangjie.ide.experiments.CjExperiments
import cn.cangnova.cangjie.ide.run.cjpm.CjpmCommandConfiguration
import cn.cangnova.cangjie.ide.run.cjpm.runconfig.buildtool.isHeadlessEnvironment
import cn.cangnova.cangjie.utils.isUnitTestMode
import com.intellij.execution.ExternalizablePath
import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.LocatableConfigurationBase
import com.intellij.execution.configurations.RunConfigurationWithSuppressedDefaultDebugAction
import com.intellij.execution.configurations.RunProfileState
import com.intellij.openapi.application.Experiments
import com.intellij.openapi.project.Project
import org.jdom.Element
import java.nio.file.Path
import java.nio.file.Paths

val CjCommandConfiguration.hasRemoteTarget: Boolean
    get() = if (this is CjpmCommandConfiguration) defaultTargetName != null else false


abstract class CjCommandConfiguration(
    project: Project,
    name: String,
    factory: ConfigurationFactory
) : LocatableConfigurationBase<RunProfileState>(project, factory, name),
    RunConfigurationWithSuppressedDefaultDebugAction {

        companion object{
            val emulateTerminalDefault: Boolean
                get() = isFeatureEnabled(CjExperiments.EMULATE_TERMINAL) && !isUnitTestMode
        }

    abstract var command: String
    var emulateTerminal: Boolean = emulateTerminalDefault

    var workingDirectory: Path? = if (!project.isDefault) {
        project.cjpmProjects.allProjects.firstOrNull()?.workingDirectory
    } else {
        null
    }

    override fun writeExternal(element: Element) {
        super.writeExternal(element)
        element.writeString("command", command)

        element.writePath("workingDirectory", workingDirectory)
        element.writeBool("emulateTerminal", emulateTerminal)

    }

    override fun readExternal(element: Element) {
        super.readExternal(element)

        element.readString("command")?.let { command = it }
        element.readPath("workingDirectory")?.let { workingDirectory = it }
        element.readBool("emulateTerminal")?.let { emulateTerminal = it }
    }
}

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

fun Element.writeBool(name: String, value: Boolean) {
    writeString(name, value.toString())
}

fun Element.readBool(name: String): Boolean? =
    readString(name)?.toBoolean()

fun <E : Enum<*>> Element.writeEnum(name: String, value: E) {
    writeString(name, value.name)
}

inline fun <reified E : Enum<E>> Element.readEnum(name: String): E? {
    val variantName = readString(name) ?: return null
    return try {
        java.lang.Enum.valueOf(E::class.java, variantName)
    } catch (_: IllegalArgumentException) {
        null
    }
}
fun isFeatureEnabled(featureId: String): Boolean {
    // Hack to pass values of experimental features in headless IDE run
    // Should help to configure IDE-based tools like Qodana
    if (isHeadlessEnvironment) {
        val value = System.getProperty(featureId)?.toBooleanStrictOrNull()
        if (value != null) return value
    }

    return Experiments.getInstance().isFeatureEnabled(featureId)
}
