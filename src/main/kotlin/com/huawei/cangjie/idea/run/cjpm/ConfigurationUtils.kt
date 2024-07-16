package com.huawei.cangjie.idea.run.cjpm

import com.huawei.cangjie.idea.run.CjCommandConfiguration
import com.huawei.cangjie.psi.CjFile
import com.intellij.execution.ExecutionManager
import com.intellij.execution.RunManager
import com.intellij.execution.RunnerAndConfigurationSettings
import com.intellij.execution.actions.ConfigurationContext
import com.intellij.execution.actions.RunConfigurationProducer
import com.intellij.execution.executors.DefaultRunExecutor
import com.intellij.execution.runners.ExecutionEnvironmentBuilder
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys

fun executeActionCjpmCommand(e: AnActionEvent, configName: String, command: String, vararg args: String) {
    val file = e.getData(CommonDataKeys.PSI_FILE) as? CjFile ?: return
    val project = file.project

    val settings = createRunConfig(e) ?: run {
        RunManager.getInstance(project)
            .createConfiguration(configName, CjpmCommandConfigurationType::class.java)
    }


    val runConfiguration = settings.configuration as CjCommandConfiguration

    runConfiguration.command = "$command  ${args.joinToString(" ")}"
    runConfiguration.name = configName


    val executorInstance = DefaultRunExecutor.getRunExecutorInstance()
    val executionEnvironment = ExecutionEnvironmentBuilder
        .createOrNull(executorInstance, runConfiguration)
        ?.build() ?: run {
        throw RuntimeException("cannot create executive environment")
    }

    ExecutionManager.getInstance(project).restartRunProfile(executionEnvironment)
}

private fun createRunConfig(e: AnActionEvent): RunnerAndConfigurationSettings? {
    val context = ConfigurationContext.getFromContext(e.dataContext, e.place)
    val configProducer = RunConfigurationProducer.getInstance(CjpmRunConfigurationProducer::class.java)

    val existingConfiguration = configProducer.findExistingConfiguration(context)
    return existingConfiguration
}
