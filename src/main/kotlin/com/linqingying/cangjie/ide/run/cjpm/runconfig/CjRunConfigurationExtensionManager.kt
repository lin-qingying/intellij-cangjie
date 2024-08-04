package com.linqingying.cangjie.ide.run.cjpm.runconfig

import com.linqingying.cangjie.ide.run.cjpm.CjpmCommandConfiguration
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

//        processEnabledExtensions(configuration, environment.runnerSettings) {
//            it.patchCommandLine(configuration, environment, cmdLine, context)
//        }
        processEnabledExtensions(configuration, environment) {
            it.patchCommandLine(configuration, environment, cmdLine, context)
        }
    }

    fun attachExtensionsToProcess(
        configuration: CjpmCommandConfiguration,
        handler: ProcessHandler,
        environment: ExecutionEnvironment,
        context: ConfigurationExtensionContext
    ) {
//        processEnabledExtensions(configuration, environment.runnerSettings) {
//            it.attachToProcess(configuration, handler, environment, context)
//        }
        processEnabledExtensions(configuration, environment) {
            it.attachToProcess(configuration, handler, environment, context)
        }
    }

    fun patchCommandLineState(
        configuration: CjpmCommandConfiguration,
        environment: ExecutionEnvironment,
        state: CommandLineState,
        context: ConfigurationExtensionContext
    ) {
        processEnabledExtensions(configuration, environment) {
            it.patchCommandLineState(configuration, environment, state, context)
        }
//        processEnabledExtensions(configuration, environment.runnerSettings) {
//            it.patchCommandLineState(configuration, environment, state, context)
//        }
    }

    private fun processEnabledExtensions(
        configuration: CjpmCommandConfiguration,
        environment: ExecutionEnvironment,
        action: (CjpmCommandConfigurationExtension) -> Unit
    ) {

//        TODO 调用processEnabledExtensions方法会在插件兼容性验证中报错使用内部方法
//        processEnabledExtensions(configuration, environment.runnerSettings) {
//            action(it)
//        }
    }


    companion object {
        @JvmStatic
        fun getInstance(): CjRunConfigurationExtensionManager = service()

    }
}
