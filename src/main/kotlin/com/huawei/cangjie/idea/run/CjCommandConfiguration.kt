package com.huawei.cangjie.idea.run


import com.huawei.cangjie.cjpm.project.model.cjpmProjects
import com.huawei.cangjie.cjpm.project.model.impl.workingDirectory
import com.huawei.cangjie.idea.experiments.CjExperiments

import com.huawei.cangjie.idea.run.cjpm.CjpmCommandConfiguration
import com.huawei.cangjie.idea.run.cjpm.isUnitTestMode
import com.huawei.cangjie.idea.run.cjpm.runconfig.buildtool.isHeadlessEnvironment
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