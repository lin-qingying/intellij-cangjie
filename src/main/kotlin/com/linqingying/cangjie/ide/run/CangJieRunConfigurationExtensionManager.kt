package com.linqingying.cangjie.ide.run

import com.intellij.execution.Executor
import com.intellij.execution.configuration.RunConfigurationExtensionsManager
import com.intellij.execution.configurations.RunnerSettings
import com.intellij.execution.ui.ConsoleView
import com.linqingying.cangjie.ide.run.cjpm.CjpmCommandConfiguration
import com.linqingying.cangjie.ide.run.cjpm.runconfig.CjpmCommandConfigurationExtension

object CangJieRunConfigurationExtensionManager :
    RunConfigurationExtensionsManager<CjpmCommandConfiguration, CjpmCommandConfigurationExtension>(
        CjpmCommandConfigurationExtension.EP_NAME
    ) {

    fun decorateExecutionConsole(
        configuration: CjpmCommandConfiguration,
        runnerSettings: RunnerSettings?,
        console: ConsoleView,
        executor: Executor
    ): ConsoleView {
        var result = console
        processEnabledExtensions(configuration, runnerSettings) {
            result = it.decorate(result, configuration, executor)
        }
        return result
    }

}
