package com.huawei.cangjie.idea.cjpm.configurations

import com.huawei.cangjie.idea.icons.CangJieIcons
import com.huawei.cangjie.lang.sdk.CangJieSdkManager
import com.intellij.execution.*
import com.intellij.execution.configurations.*
import com.intellij.execution.filters.TextConsoleBuilderFactory
import com.intellij.execution.impl.RunManagerImpl
import com.intellij.execution.process.OSProcessHandler
import com.intellij.execution.process.ProcessAdapter
import com.intellij.execution.process.ProcessEvent
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.execution.runners.ProgramRunner
import com.intellij.execution.ui.ConsoleViewContentType
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.extensions.ExtensionsArea
import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.options.SettingsEditorGroup
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.NotNullLazyValue
import com.intellij.openapi.util.SystemInfo.isWindows
import java.io.File


class MyBeforeRunTaskProvider : BeforeRunTaskProvider<BeforeRunTask<*>>() {
    private val myId = Key.create<BeforeRunTask<*>>("MyBeforeRunTask")
    override fun getId(): Key<BeforeRunTask<*>> {
        return myId
    }

    override fun getName(): String {
        return "My Before Run Task"
    }

    override fun executeTask(
        context: DataContext, configuration: RunConfiguration,
        env: ExecutionEnvironment, task: BeforeRunTask<*>
    ): Boolean {
        // 在这里执行你的任务
        return true
    }


    override fun createTask(runConfiguration: RunConfiguration): BeforeRunTask<*> {
        return object : BeforeRunTask<BeforeRunTask<*>?>(myId) {
            val isExecutable: Boolean
                get() = true // 返回true使得任务可以被选择
        }
    }
}

class CjpmRunConfigurationType : SimpleConfigurationType(
    "CjpmRunConfigurationType",
    "Cjpm",
    "Cjpm",
    NotNullLazyValue.createValue { CangJieIcons.SMALL_LOGO }

) {


    override fun createTemplateConfiguration(project: Project): CjpmRunConfiguration {

        return CjpmRunConfiguration(project, this, "Cjpm")
    }

    override fun isDumbAware(): Boolean = true

    override fun isEditableInDumbMode(): Boolean = true


    companion object {
        val instance: CjpmRunConfigurationType
            get() = ConfigurationTypeUtil.findConfigurationType(CjpmRunConfigurationType::class.java)
    }


}

//class CjpmConfigurationFactory(configurationType: ConfigurationType) : ConfigurationFactory(configurationType) {
//    override fun createTemplateConfiguration(project: Project): RunConfiguration {
//        return CjpmRunConfiguration(project, this, "Cjpm")
//    }
//}

class CjpmRunConfiguration(project: Project, factory: ConfigurationFactory, name: String) :
    RunConfigurationBase<Any?>(project, factory, name) {
    override fun getState(executor: Executor, environment: ExecutionEnvironment): RunProfileState {

//        运行命令
        return CjpmCommandLineState(environment, this)
    }

    val runConfigEditor = CjpmRunConfigurationEditor(project)
    override fun checkConfiguration() {
        super.checkConfiguration()

        if (runConfigEditor.command.isNullOrEmpty()) {
            throw RuntimeConfigurationException("命令不能为空")
        }
        if (runConfigEditor.cjcPath.isNullOrEmpty() || runConfigEditor.cjpmPath.isNullOrEmpty()) {
            throw RuntimeConfigurationException("请配置仓颉SDK")
        }


    }

//    override fun setBeforeRunTasks(value: MutableList<BeforeRunTask<*>>) {
//        value.add(MyBeforeRunTaskProvider().createTask(this))
//
//    }
//    override fun getBeforeRunTasks(): MutableList<BeforeRunTask<*>> {
//        val myBeforeRunTaskProvider = MyBeforeRunTaskProvider()
//        // 创建并配置你的BeforeRunTask
//        val myBeforeRunTask = myBeforeRunTaskProvider.createTask(this)
//        // 返回一个包含你的BeforeRunTask的列表
//        return listOf(myBeforeRunTask).toMutableList()
//    }

    override fun getConfigurationEditor(): SettingsEditor<out RunConfiguration> {
//        创建
        val group = SettingsEditorGroup<CjpmRunConfiguration>()
        runConfigEditor.resetSdks()
        group.addEditor(
//            ExecutionBundle.message("run.configuration.configuration.tab.title"),
            "cjpm",
            runConfigEditor
        )

//        group.addEditor(ExecutionBundle.message("logs.tab.title"), LogConfigurationPanel())
        return group
    }


    class CjpmCommandLineState(environment: ExecutionEnvironment, configuration: CjpmRunConfiguration) :
        CommandLineState(environment) {
        override fun startProcess(): OSProcessHandler {

            val sdk = CangJieSdkManager.getProjectSdk()
            val sdkHome = sdk?.homePath

            val cjpmPath = if (isWindows) "$sdkHome\\tools\\bin\\cjpm.exe" else "$sdkHome/tools/bin/cjpm"

//            获取工作目录为当前项目的根目录
            val project = environment.project


            val commandLine = GeneralCommandLine(cjpmPath, "update")
            commandLine.workDirectory = project.basePath?.let { File(it) }
            val handler = OSProcessHandler(commandLine)


            handler.startNotify()
            return handler

        }


        override fun execute(executor: Executor, runner: ProgramRunner<*>): ExecutionResult {
            val processHandler = startProcess()
            val consoleView = TextConsoleBuilderFactory.getInstance().createBuilder(environment.project).console
            consoleView.attachToProcess(processHandler)

            // 创建一个StringBuilder用于存储命令的输出
            val output = StringBuilder()

            // 在有新的输出可用时，将其添加到StringBuilder中
            processHandler.addProcessListener(object : ProcessAdapter() {
                override fun onTextAvailable(event: ProcessEvent, outputType: Key<*>) {
                    output.append(event.text)
                }

                override fun processTerminated(event: ProcessEvent) {
                    // 在命令结束后，将命令的输出打印到控制台
                    consoleView.print("Command Output:\n$output\n", ConsoleViewContentType.NORMAL_OUTPUT)
                }
            })

            return DefaultExecutionResult(consoleView, processHandler, *createActions(consoleView, processHandler))
        }


    }

}
