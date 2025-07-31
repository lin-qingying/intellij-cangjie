/*
 * Copyright 2024 LinQingYing. and contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * The use of this source code is governed by the Apache License 2.0,
 * which allows users to freely use, modify, and distribute the code,
 * provided they adhere to the terms of the license.
 *
 * The software is provided "as-is", and the authors are not responsible for
 * any damages or issues arising from its use.
 *
 */

package cn.cangnova.cangjie.buildsystem.impl.cjpm

import cn.cangnova.cangjie.ide.run.CjCommandConfiguration
import cn.cangnova.cangjie.ide.run.cjpm.runconfig.buildtool.createFilters
import cn.cangnova.cangjie.ide.run.hasRemoteTarget

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
    override fun createConsole(): ConsoleView {
        return  if (!config.hasRemoteTarget) {
            TerminalExecutionConsole(project, null)
        } else {
            CjpmConsoleView(project, scope, isViewer, true)
        }
    }

}


class CjpmConsoleView(
    project: Project,
    searchScope: GlobalSearchScope,
    viewer: Boolean,
    usePredefinedMessageFilter: Boolean
) : ConsoleViewImpl(project, searchScope, viewer, usePredefinedMessageFilter)
