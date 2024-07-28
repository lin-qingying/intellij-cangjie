package com.huawei.cangjie.ide.actions.runAnything.cjpm

import com.huawei.cangjie.CangJieBundle
import com.huawei.cangjie.cjpm.project.model.CjpmProject
import com.huawei.cangjie.cjpm.project.model.cjpmProjects
import com.huawei.cangjie.ide.actions.runAnything.CjRunAnythingProvider
import com.huawei.cangjie.ide.actions.runAnything.RunAnythingCjpmItem
import com.huawei.cangjie.ide.actions.runAnything.getAppropriateCjpmProject
import com.huawei.cangjie.icon.CangJieIcons
import com.huawei.cangjie.ide.run.cjpm.CjCommandCompletionProvider
import com.huawei.cangjie.ide.run.cjpm.CjpmCommandCompletionProvider
import com.huawei.cangjie.ide.run.cjpm.CjpmCommandLine
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

    override fun getIcon(value: String): Icon = CangJieIcons.CANGJIE_FILE

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
