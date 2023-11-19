package com.huawei.cangjie.idea.run.modulejson


import com.huawei.cangjie.idea.run.cjpm.CjpmCommand
import com.huawei.cangjie.idea.run.cjpm.CjpmRunConfiguration
import com.huawei.cangjie.idea.run.cjpm.CjpmRunConfigurationType
import com.huawei.cangjie.lang.sdk.CangJieSdkManager
import com.huawei.cangjie.lang.sdk.CangJieSdkType
import com.intellij.execution.Executor
import com.intellij.execution.ProgramRunnerUtil
import com.intellij.execution.RunManager
import com.intellij.execution.executors.DefaultRunExecutor


import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.util.SystemInfo.isWindows


class CjpmUpdateAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {

        val project = e.project ?: return

        // 获取 RunManager
        val runManager = RunManager.getInstance(project)

// 查找 command 为 UPDATE 的运行配置
        var runConfiguration = runManager.allSettings.find {
            it.configuration is CjpmRunConfiguration && (it.configuration as CjpmRunConfiguration).command == CjpmCommand.UPDATE
        }

        // 如果没有找到，就创建一个新的运行配置
        if (runConfiguration == null) {
            runConfiguration = runManager.createConfiguration("update", CjpmRunConfigurationType.instance)
            (runConfiguration.configuration as CjpmRunConfiguration).command = CjpmCommand.UPDATE
            runManager.addConfiguration(runConfiguration)
        }

        // 设置新的运行配置为默认运行配置
//        runManager.selectedConfiguration = runConfiguration

        // 执行运行配置
        val executor: Executor = DefaultRunExecutor.getRunExecutorInstance()
        ProgramRunnerUtil.executeConfiguration(runConfiguration, executor)


    }


    override fun update(e: AnActionEvent) {

        val file = e.getData(PlatformDataKeys.VIRTUAL_FILE)


        val fileName = file?.name

        val fileType = file?.fileType

        val isCjpmModuleJson = fileName?.startsWith("module") == true && fileType?.name == "JSON"


        e.presentation.isEnabledAndVisible = isCjpmModuleJson


    }
}
