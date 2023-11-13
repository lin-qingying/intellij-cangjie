package com.huawei.cangjie.idea.cjpm.configurations

import com.huawei.cangjie.idea.icons.CangJieIcons
import com.huawei.cangjie.lang.sdk.CangJieSdkManager
import com.huawei.cangjie.lang.sdk.CangJieSdkType
import com.intellij.execution.BeforeRunTask
import com.intellij.execution.BeforeRunTaskProvider
import com.intellij.execution.Executor
import com.intellij.execution.configurations.*
import com.intellij.execution.process.OSProcessHandler
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.options.SettingsEditorGroup
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.NotNullLazyValue
import com.intellij.openapi.util.SystemInfo.isWindows
import org.jdom.Element


class MyBeforeRunTaskProvider : BeforeRunTaskProvider<BeforeRunTask<*>>() {
    private val myId = Key.create<BeforeRunTask<*>>("MyBeforeRunTask")
    override fun getId(): Key<BeforeRunTask<*>> {
        return myId
    }

    override fun getName(): String {
        return "My Before Run Task"
    }

    override fun executeTask(
        context: DataContext, configuration: RunConfiguration, env: ExecutionEnvironment, task: BeforeRunTask<*>
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

class CjpmRunConfigurationType : SimpleConfigurationType("CjpmRunConfigurationType",
    "Cjpm",
    "Cjpm",
    NotNullLazyValue.createValue { CangJieIcons.CANGJIE_FILE }

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
    RunConfigurationBase<CangJieRunConfigurationOptions>(project, factory, name) {


    var args: String? = null
    var commandSelectIndex: Int? = null
    var commandStr: String? = null
    var cjcPath: String? = null
    var cjpmPath: String? = null
    var cjpmVersion: String? = null
    var cjcVersion: String? = null
    var moudleJsonPath: String? = null


    override fun getState(executor: Executor, environment: ExecutionEnvironment): RunProfileState {

//        运行命令
        return CjpmCommandLineState(environment, this)
    }

    override fun writeExternal(element: Element) {
//        将配置写入xml
        if (commandSelectIndex != null) {
            element.setAttribute("commandIndex", commandSelectIndex.toString())

        }
        if (commandStr != null) {
            element.setAttribute("commandStr", commandStr)
        }
        if (args != null) {
            element.setAttribute("args", args)
        }

        super.writeExternal(element)
    }


    override fun readExternal(element: Element) {
        super.readExternal(element)
//        从xml中读取配置
        commandSelectIndex = element.getAttributeValue("commandIndex")?.toInt()
        args = element.getAttributeValue("args")
        commandStr = element.getAttributeValue("commandStr")
    }


    override fun getConfigurationEditor(): SettingsEditor<out RunConfiguration> {
//        创建
        val group = SettingsEditorGroup<CjpmRunConfiguration>()
//        runConfigEditor.resetSdks()
        group.addEditor(
//            ExecutionBundle.message("run.configuration.configuration.tab.title"),
            "cjpm", CjpmRunConfigurationEditor(project)
        )

//        group.addEditor(ExecutionBundle.message("logs.tab.title"), LogConfigurationPanel())
        return group
    }


    class CjpmCommandLineState(environment: ExecutionEnvironment, val configuration: CjpmRunConfiguration) :
        CommandLineState(environment) {
        override fun startProcess(): OSProcessHandler {


            val sdk = CangJieSdkManager.getProjectSdk( ) ?: throw RuntimeConfigurationException("请配置仓颉SDK")

            //            获取工作目录为当前项目的根目录
            val project = environment.project


            val commandLine = GeneralCommandLine()

            val commandStrbf = StringBuilder()

            commandLine.setWorkDirectory(project.basePath)
            commandLine.charset = Charsets.UTF_8

            if (configuration.commandStr.equals("run")) {
                if (isWindows) {

                    commandLine.exePath = "cmd"
                    commandLine.addParameter("/c")
                    commandStrbf.append(
                        (sdk.sdkType as CangJieSdkType).sdkAdditionalData.cjpmPath?.toWindowsPath() ?: ""
                    )
                } else {
                    commandLine.exePath = "/bin/bash"
                    commandLine.addParameter("-c")
                    commandStrbf.append((sdk.sdkType as CangJieSdkType).sdkAdditionalData.cjpmPath ?: "")
                }

                commandStrbf.append(" ")
                commandStrbf.append("build")
                commandStrbf.append(" ")
                commandStrbf.append("&&")
                commandStrbf.append(" ")
                commandStrbf.append("${project.basePath}${if (isWindows) "\\build\\bin\\main.exe" else "/build/bin/main"}")

                commandLine.addParameter(commandStrbf.toString())
            } else {
                commandLine.exePath = (sdk.sdkType as CangJieSdkType).sdkAdditionalData.cjpmPath.toString()
                configuration.commandStr?.let { commandLine.addParameter(it) }
                //            TODO cjpm参数
                configuration.args?.let {
                    if (it.isNotEmpty())
                        commandLine.addParameters(it)
                }
            }


//
//            commandLine.addParameter(if (configuration.runConfigEditor.command.equals("run")) "build" else configuration.runConfigEditor.command)
//
//            if (configuration.runConfigEditor.command.equals("run")) {
//                commandLine.addParameter("&&")
//                commandLine.addParameter("${project.basePath}${if (isWindows) "\\build\\bin\\main.exe" else "/build/bin/main"}")
//            }


            val handler = OSProcessHandler(commandLine)


            handler.startNotify()


            return handler

        }

        /**
         * 路径转为windows格式
         */
        private fun String.toWindowsPath(): String {
            return this.replace("/", "\\")
        }

//        override fun execute(executor: Executor, runner: ProgramRunner<*>): ExecutionResult {
//            val processHandler = startProcess()
//            val consoleView = TextConsoleBuilderFactory.getInstance().createBuilder(environment.project).console
//            consoleView.attachToProcess(processHandler)
//
//            // 创建一个StringBuilder用于存储命令的输出
//            val output = StringBuilder()
//
//            // 在有新的输出可用时，将其添加到StringBuilder中
//            processHandler.addProcessListener(object : ProcessAdapter() {
//                override fun onTextAvailable(event: ProcessEvent, outputType: Key<*>) {
//                    output.append(event.text)
//                }
//
//                override fun processTerminated(event: ProcessEvent) {
//                    // 在命令结束后，将命令的输出打印到控制台
//                    consoleView.print("Command Output:\n$output\n", ConsoleViewContentType.NORMAL_OUTPUT)
//                }
//            })
//
//            return DefaultExecutionResult(consoleView, processHandler, *createActions(consoleView, processHandler))
//        }


    }

}


class CangJieRunConfigurationOptions : RunConfigurationOptions() {
    var commandSelectIndex: Int? = null
    var cjcPath: String? = null
    var cjpmPath: String? = null
    var cjpmVersion: String? = null
    var cjcVersion: String? = null
    var moudleJsonPath: String? = null
}
