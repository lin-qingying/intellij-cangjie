package com.linqingying.cangjie.ide.run.cjpm.runconfig

import com.linqingying.cangjie.ide.run.cjpm.CjpmCommandConfiguration

import com.intellij.execution.configuration.RunConfigurationExtensionBase
import com.intellij.execution.configurations.CommandLineState
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.configurations.RunnerSettings
import com.intellij.execution.process.ProcessHandler
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.util.UserDataHolderBase

class ConfigurationExtensionContext : UserDataHolderBase()
abstract class CjpmCommandConfigurationExtension: RunConfigurationExtensionBase<CjpmCommandConfiguration>() {



    open fun patchCommandLineState(
        configuration: CjpmCommandConfiguration,
        environment: ExecutionEnvironment,
        state: CommandLineState,
        context: ConfigurationExtensionContext
    ) {
    }
    abstract fun attachToProcess(
        configuration: CjpmCommandConfiguration,
        handler: ProcessHandler,
        environment: ExecutionEnvironment,
        context: ConfigurationExtensionContext
    )

    override fun patchCommandLine(
        configuration: CjpmCommandConfiguration,
        runnerSettings: RunnerSettings?,
        cmdLine: GeneralCommandLine,
        runnerId: String
    ) {
        LOG.error("use the other overload of 'patchCommandLine' method")
    }
    abstract fun patchCommandLine(
        configuration: CjpmCommandConfiguration,
        environment: ExecutionEnvironment,
        cmdLine: GeneralCommandLine,
        context: ConfigurationExtensionContext
    )
    companion object{
        val EP_NAME = ExtensionPointName.create<CjpmCommandConfigurationExtension>("com.linqingying.cangjie.runConfigurationExtension")

        private val LOG: Logger = logger<CjpmCommandConfigurationExtension>()
    }
}
