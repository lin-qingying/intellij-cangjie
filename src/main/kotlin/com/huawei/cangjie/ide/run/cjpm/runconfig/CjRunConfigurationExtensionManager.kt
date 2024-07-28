package com.huawei.cangjie.ide.run.cjpm.runconfig

import com.huawei.cangjie.ide.run.cjpm.CjpmCommandConfiguration
import com.huawei.cangjie.ide.run.cjpm.ConfigurationExtensionContext
import com.intellij.execution.configuration.RunConfigurationExtensionsManager
import com.intellij.execution.configurations.CommandLineState
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.process.ProcessHandler
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
@Service
class CjRunConfigurationExtensionManager :
    RunConfigurationExtensionsManager<CjpmCommandConfiguration, CjpmCommandConfigurationExtension>(
        CjpmCommandConfigurationExtension.EP_NAME
    ) {

    fun patchCommandLine(
        configuration: CjpmCommandConfiguration,
        environment: ExecutionEnvironment,
        cmdLine: GeneralCommandLine,
        context: ConfigurationExtensionContext
    ) {
        processEnabledExtensions(configuration, environment.runnerSettings) {
            it.patchCommandLine(configuration, environment, cmdLine, context)
        }
    }
    fun attachExtensionsToProcess(
        configuration: CjpmCommandConfiguration,
        handler: ProcessHandler,
        environment: ExecutionEnvironment,
        context: ConfigurationExtensionContext
    ) {
        processEnabledExtensions(configuration, environment.runnerSettings) {
            it.attachToProcess(configuration, handler, environment, context)
        }
    }
    fun patchCommandLineState(
        configuration: CjpmCommandConfiguration,
        environment: ExecutionEnvironment,
        state: CommandLineState,
        context: ConfigurationExtensionContext
    ) {
        processEnabledExtensions(configuration, environment.runnerSettings) {
            it.patchCommandLineState(configuration, environment, state, context)
        }
    }
    companion object {
        @JvmStatic
        fun getInstance(): CjRunConfigurationExtensionManager = service()

    }
}
