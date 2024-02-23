package com.huawei.cangjie.idea.run.cjpm

import com.huawei.cangjie.idea.run.CjCommandConfiguration
import com.huawei.cangjie.idea.run.cjpm.runconfig.buildtool.createFilters
import com.huawei.cangjie.idea.run.hasRemoteTarget

import com.intellij.execution.filters.TextConsoleBuilderImpl
import com.intellij.execution.impl.ConsoleViewImpl
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.execution.ui.ConsoleView
import com.intellij.openapi.project.Project
import com.intellij.psi.search.ExecutionSearchScopes
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.terminal.TerminalExecutionConsole


class CjpmRunState(
    environment: ExecutionEnvironment,
    configuration: CjpmCommandConfiguration,
    config: CjpmCommandConfiguration.CleanConfiguration.Ok,

) :
    CjpmRunStateBase(environment, configuration, config) {


    init {
        consoleBuilder = CjConsoleBuilder(project, configuration)
        createFilters().forEach { consoleBuilder.addFilter(it) }
    }

    /**
     * 路径转为windows格式
     */
    private fun String.toWindowsPath(): String {
        return this.replace("/", "\\")
    }


}

open class CjConsoleBuilder(
    project: Project,
    val config: CjCommandConfiguration
) : TextConsoleBuilderImpl(project, ExecutionSearchScopes.executionScope(project, config)) {
    override fun createConsole(): ConsoleView =
        if (!config.hasRemoteTarget) {
            TerminalExecutionConsole(project, null)
        } else {
            CjpmConsoleView(project, scope, isViewer, true)
        }
}


class CjpmConsoleView(
    project: Project,
    searchScope: GlobalSearchScope,
    viewer: Boolean,
    usePredefinedMessageFilter: Boolean
) : ConsoleViewImpl(project, searchScope, viewer, usePredefinedMessageFilter)
