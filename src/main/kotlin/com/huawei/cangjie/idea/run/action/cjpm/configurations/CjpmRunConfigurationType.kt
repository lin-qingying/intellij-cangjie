package com.huawei.cangjie.idea.run.action.cjpm.configurations

import com.huawei.cangjie.CangJieBundle
import com.huawei.cangjie.idea.icons.CangJieIcons
import com.huawei.cangjie.lang.sdk.CangJieSdkManager
import com.huawei.cangjie.lang.sdk.CangJieSdkType
import com.intellij.execution.Executor
import com.intellij.execution.configurations.*
import com.intellij.execution.configurations.ConfigurationTypeUtil.findConfigurationType
import com.intellij.execution.process.OSProcessHandler
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.options.SettingsEditorGroup
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.NotNullLazyValue
import com.intellij.openapi.util.SystemInfo.isWindows
import org.jdom.Element


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
            get() = findConfigurationType(CjpmRunConfigurationType::class.java)
    }


}



class CjpmRunConfiguration(project: Project, factory: ConfigurationFactory, name: String) :
    RunConfigurationBase<CangJieRunConfigurationOptions>(project, factory, name) {


    var args: String? = null

    //    var commandSelectIndex: Int? = null
//    var commandStr: String? = null
    var cjcPath: String? = null
    var cjpmPath: String? = null
    var cjpmVersion: String? = null
    var cjcVersion: String? = null
    var moudleJsonPath: String? = null


    var command: CjpmCommand? = null


    override fun getState(executor: Executor, environment: ExecutionEnvironment): RunProfileState {

//        运行命令
        return CjpmCommandLineState(environment, this)
    }

    override fun writeExternal(element: Element) {
//        将配置写入xml
        if (command != null) {
//            element.setAttribute("commandIndex", command?.index.toString())
//element.setAttribute("commandStr", command?.command  )
            element.setAttribute("command", command!!.index.toString())
        }

        if (args != null) {
            element.setAttribute("args", args)
        }

        super.writeExternal(element)
    }


    override fun readExternal(element: Element) {
        super.readExternal(element)
//        从xml中读取配置

        args = element.getAttributeValue("args")

        command = CjpmCommand.fromInt(element.getAttributeValue("command")?.toInt() ?: 0)
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


            val sdk = CangJieSdkManager.getProjectSdk() ?: throw RuntimeConfigurationException(CangJieBundle.message("cangjie.sdk.please.configure"))

            //            获取工作目录为当前项目的根目录
            val project = environment.project


            val commandLine = GeneralCommandLine()

            val commandStrbf = StringBuilder()

            commandLine.setWorkDirectory(project.basePath)
            commandLine.charset = Charsets.UTF_8

            if (configuration.command == CjpmCommand.RUN) {
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
                configuration.command?.command?.let { commandLine.addParameter(it) }
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


//internal class CjpmCommandItem(val name: String, val command: String, val description: String) {
//
//    override fun toString(): String {
//        return name
//    }
//}

enum class CjpmCommand(val index: Int?, val command: String, val description: String) {

    INIT(null, "init", "初始化"),
    RUN(0, "run", "运行模块"),
    BUILD(1, "build", "编译模块"),
    UPDATE(2, "update", "更新模块"),
    CLEAN(3, "clean", "清理模块"),
    CHECK(4, "check", "检查依赖"),
    TEST(5, "test", "单元测试");


    companion object {
        @OptIn(ExperimentalStdlibApi::class)
        @JvmStatic
        fun toArray(): Array<CjpmCommand> {

//            去掉INIT

            val arr = CjpmCommand.entries.toMutableList()
            arr.removeAt(0)
            return arr.toTypedArray()


        }

        //        序列化和反序列化
        @OptIn(ExperimentalStdlibApi::class)
        @JvmStatic
        fun fromInt(index: Int): CjpmCommand {
            val arr = CjpmCommand.entries.toMutableList()
            arr.removeAt(0)

            return arr[index]

//            return when (index) {
//                0 -> INIT
//                1 -> RUN
//                2 -> BUILD
//                3 -> UPDATE
//                4 -> CLEAN
//                5 -> CHECK
//                6 -> TEST
//                else -> throw IllegalArgumentException("Invalid ordinal $index")
//            }
        }


    }

    override fun toString(): String {

        return command
    }
}
