package com.huawei.cangjie.idea.run.cjpm

import com.huawei.cangjie.CangJieBundle
import com.huawei.cangjie.idea.project.CangJieProjectManager

import com.huawei.cangjie.idea.run.CjCommandConfiguration

import com.huawei.cangjie.idea.run.cjpm.runconfig.CjLanguageRuntimeConfiguration
import com.huawei.cangjie.idea.run.cjpm.runconfig.CjLanguageRuntimeType
import com.huawei.cangjie.idea.run.cjpm.runconfig.isUnitTestMode

import com.intellij.execution.Executor
import com.intellij.execution.InputRedirectAware
import com.intellij.execution.configuration.EnvironmentVariablesData
import com.intellij.execution.configurations.*
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.execution.target.LanguageRuntimeType
import com.intellij.execution.target.TargetEnvironmentAwareRunProfile
import com.intellij.execution.target.TargetEnvironmentConfiguration
import com.intellij.execution.testframework.actions.ConsolePropertiesProvider
import com.intellij.execution.util.ProgramParametersUtil
import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.options.SettingsEditorGroup
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.NlsContexts
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.util.execution.ParametersListUtil
import org.jdom.Element
import java.io.File
import java.nio.file.Path
import java.nio.file.Paths

enum class BuildTarget {
    LOCAL, REMOTE;

    val isLocal: Boolean get() = this == LOCAL
    val isRemote: Boolean get() = this == REMOTE
}

class CjpmCommandConfiguration(project: Project, factory: ConfigurationFactory, name: String) :
    CjCommandConfiguration(project, name, factory),
    InputRedirectAware.InputRedirectOptions,
    ConsolePropertiesProvider,
    TargetEnvironmentAwareRunProfile {



    private val redirectInputFile: File?
        get() {
            if (!isRedirectInput) return null
            if (redirectInputPath?.isNotEmpty() != true) return null
            val redirectInputPath = FileUtil.toSystemDependentName(
                ProgramParametersUtil.expandPathAndMacros(
                    redirectInputPath,
                    null,
                    project
                )
            )
            var file = File(redirectInputPath)

            return file
        }

    var buildTarget: BuildTarget = BuildTarget.REMOTE

    sealed class CleanConfiguration {
        //        class Ok(
//            val cmd: CjpmCommandLine,
//            val toolchain: CjToolchainBase
//        ) : CleanConfiguration()
        class Ok(
            val cmd: CjpmCommandLine,
//            val toolchain: CjToolchainBase
        ) : CleanConfiguration()

        class Err(val error: RuntimeConfigurationError) : CleanConfiguration()

        val ok: Ok? get() = this as? Ok

        companion object {
            fun error(@Suppress("UnstableApiUsage") @NlsContexts.DialogMessage message: String) =
                Err(RuntimeConfigurationError(message))
        }
    }

    var args: String? = null


    override var command: CjpmCommand? = CjpmCommand.RUN

    var requiredFeatures: Boolean = true
    var allFeatures: Boolean = false
    var withSudo: Boolean = false


    var env: EnvironmentVariablesData = EnvironmentVariablesData.DEFAULT

    fun setFromCmd(cmd: CjpmCommandLine) {
        command = cmd.command
        requiredFeatures = cmd.requiredFeatures
        allFeatures = cmd.allFeatures

        withSudo = cmd.withSudo

        env = cmd.environmentVariables

    }


    var executorId:String  = "Run"

    override fun getState(executor: Executor, environment: ExecutionEnvironment): RunProfileState? {

        executorId = executor.id


        val config = clean().ok ?: return null
//        运行命令

        return CjpmRunState(environment, this, config)

    }

    fun clean(): CleanConfiguration {
//        val workingDirectory = workingDirectory
//            ?: return CleanConfiguration.error(CangJieBundle.message("dialog.message.no.working.directory.specified"))
        val workingDirectory = project.basePath?.let { Paths.get(it) }
            ?: return CleanConfiguration.error(CangJieBundle.message("dialog.message.no.working.directory.specified"))
        val cmd = run {
            val parsed = command?.let { ParsedCommand.parse(it) }
                ?: return CleanConfiguration.error(CangJieBundle.message("dialog.message.no.command.specified"))

            CjpmCommandLine(
                parsed.command,
                workingDirectory,
                parsed.additionalArguments,
                redirectInputFile,

                env,
                requiredFeatures,
                allFeatures,
                withSudo
            )
        }

//
//        val toolchain = project.toolchain
//            ?: return CleanConfiguration.error(CangJieBundle.message("dialog.message.no.cangjie.toolchain.specified"))
//


        return CleanConfiguration.Ok(cmd)
    }

    override fun canRunOn(target: TargetEnvironmentConfiguration): Boolean =
        target.runtimes.findByType(CjLanguageRuntimeConfiguration::class.java) != null

    override fun getDefaultLanguageRuntimeType(): LanguageRuntimeType<*>? =
        LanguageRuntimeType.EXTENSION_NAME.findExtension(
            CjLanguageRuntimeType::class.java
        )

    override fun getDefaultTargetName(): String? = options.remoteTarget

    override fun setDefaultTargetName(targetName: String?) {
        options.remoteTarget = targetName
    }


    override fun getConfigurationEditor(): SettingsEditor<out RunConfiguration> {
//        创建
        val group = SettingsEditorGroup<CjpmCommandConfiguration>()
//        runConfigEditor.resetSdks()
        group.addEditor(
//            ExecutionBundle.message("run.configuration.configuration.tab.title"),
            "cjpm", CjpmCommandConfigurationEditor(project)
        )

//        group.addEditor(ExecutionBundle.message("logs.tab.title"), LogConfigurationPanel())
        return group
    }

    private var isRedirectInput: Boolean = false
    private var redirectInputPath: String? = null
    override fun isRedirectInput(): Boolean = isRedirectInput

    override fun setRedirectInput(value: Boolean) {
        isRedirectInput = value
    }

    override fun getRedirectInputPath(): String? = redirectInputPath

    override fun setRedirectInputPath(value: String?) {
        redirectInputPath = value
    }


    override fun writeExternal(element: Element) {
        super.writeExternal(element)
        env.writeExternal(element)
    }

    override fun readExternal(element: Element) {
        super.readExternal(element)
        env = EnvironmentVariablesData.readExternal(element)

    }


}

data class ParsedCommand(val command: CjpmCommand, val additionalArguments: MutableList<String>) {
    companion object {
//        fun parse(rawCommand: String): ParsedCommand? {
//            val args = ParametersListUtil.parse(rawCommand)
//            val command = args.firstOrNull { !it.startsWith("+") } ?: return null
//
//            val additionalArguments = args.drop(args.indexOf(command) + 1)
//            return ParsedCommand(command, additionalArguments)
//        }

        fun parse(rawCommand: CjpmCommand): ParsedCommand? {

            val args = rawCommand.executeCommand.let { it.let { it1 -> ParametersListUtil.parse(it1) } }
            val command = args.firstOrNull { !it.startsWith("+") } ?: return null

            val additionalArguments = args.drop(args.indexOf(command) + 1)
            return ParsedCommand(rawCommand, additionalArguments.toMutableList())

        }
    }
}
