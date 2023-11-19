package com.huawei.cangjie.idea.run.cjpm

import com.intellij.execution.Executor
import com.intellij.execution.configurations.*
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.options.SettingsEditorGroup
import com.intellij.openapi.project.Project
import org.jdom.Element


class CjpmRunConfiguration(project: Project, factory: ConfigurationFactory, name: String) :
    RunConfigurationBase<CangJieRunConfigurationOptions>(project, factory, name) {


    var args: String? = null


    var command: CjpmCommand? = null


    override fun getState(executor: Executor, environment: ExecutionEnvironment): RunProfileState {


//        判断是否是debug模式
//        if (executor.id == "Debug" && executor is DefaultDebugExecutor) {
//
//            return object :  CjpmCommandLineState(environment, this) {
//
//
//
//            }
//        }


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


}
