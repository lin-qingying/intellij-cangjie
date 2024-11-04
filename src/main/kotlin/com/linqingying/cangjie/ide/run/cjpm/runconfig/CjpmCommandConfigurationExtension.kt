package com.linqingying.cangjie.ide.run.cjpm.runconfig

import com.linqingying.cangjie.ide.run.cjpm.CjpmCommandConfiguration
import com.linqingying.cangjie.ide.run.cjpm.ConfigurationExtensionContext
import com.intellij.execution.configuration.RunConfigurationExtensionBase
import com.intellij.execution.configurations.CommandLineState
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.configurations.RunnerSettings
import com.intellij.execution.process.ProcessHandler
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.extensions.ExtensionPointName

abstract class CjpmCommandConfigurationExtension: RunConfigurationExtensionBase<CjpmCommandConfiguration>() {
    override fun isApplicableFor(configuration: CjpmCommandConfiguration): Boolean {
        TODO("Not yet implemented")
    }

    override fun isEnabledFor(applicableConfiguration: CjpmCommandConfiguration, runnerSettings: RunnerSettings?): Boolean {
        TODO("Not yet implemented")
    }
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
        TODO("Not yet implemented")
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
