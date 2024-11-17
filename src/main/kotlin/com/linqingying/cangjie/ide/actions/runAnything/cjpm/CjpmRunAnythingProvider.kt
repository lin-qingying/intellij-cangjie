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

package com.linqingying.cangjie.ide.actions.runAnything.cjpm

import com.linqingying.cangjie.CangJieBundle
import com.linqingying.cangjie.cjpm.project.model.CjpmProject
import com.linqingying.cangjie.cjpm.project.model.cjpmProjects
import com.linqingying.cangjie.ide.actions.runAnything.CjRunAnythingProvider
import com.linqingying.cangjie.ide.actions.runAnything.RunAnythingCjpmItem
import com.linqingying.cangjie.ide.actions.runAnything.getAppropriateCjpmProject
import com.linqingying.cangjie.icon.CangJieIcons
import com.linqingying.cangjie.ide.run.cjpm.CjCommandCompletionProvider
import com.linqingying.cangjie.ide.run.cjpm.CjpmCommandCompletionProvider
import com.linqingying.cangjie.ide.run.cjpm.CjpmCommandLine
import com.intellij.execution.Executor
import com.intellij.ide.actions.runAnything.items.RunAnythingItem
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.project.Project
import java.nio.file.Path
import javax.swing.Icon

class CjpmRunAnythingProvider : CjRunAnythingProvider() {


    companion object {
        const val HELP_COMMAND = "cjpm"
    }

    override fun getMainListItem(dataContext: DataContext, value: String): RunAnythingItem =
        RunAnythingCjpmItem(getCommand(value), getIcon(value))

    override fun getIcon(value: String): Icon = CangJieIcons.CANGJIE

    override fun run(
        executor: Executor,
        command: String,
        params: List<String>,
        workingDirectory: Path,
        cjpmProject: CjpmProject
    ) {
        CjpmCommandLine(command, workingDirectory, params).run(cjpmProject, executor = executor)
    }

    override fun getCompletionProvider(project: Project, dataContext: DataContext): CjCommandCompletionProvider =
        CjpmCommandCompletionProvider(project.cjpmProjects) {
            getAppropriateCjpmProject(dataContext)?.workspace
        }

    override fun getHelpCommand(): String = HELP_COMMAND

    override fun getHelpGroupTitle(): String = CangJieBundle.message("build.event.title.cjpm")

    override fun getCommand(value: String): String = value
    override fun getCompletionGroupTitle(): String = CangJieBundle.message("cjpm.commands")
    override fun getHelpDescription(): String = CangJieBundle.message("runs.cjpm.command")

}
