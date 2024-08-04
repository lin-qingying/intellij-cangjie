package com.linqingying.cangjie.ide.actions.runAnything

import com.linqingying.cangjie.cjpm.project.model.CjpmProject
import com.linqingying.cangjie.cjpm.project.model.cjpmProjects
import com.linqingying.cangjie.cjpm.project.toPath
import com.linqingying.cangjie.cjpm.project.toolwindow.CjpmToolWindow
import com.linqingying.cangjie.cjpm.project.toolwindow.hasCjpmProject
import com.linqingying.cangjie.ide.run.cjpm.CjCommandCompletionProvider
import com.intellij.execution.Executor
import com.intellij.execution.executors.DefaultRunExecutor
import com.intellij.ide.actions.runAnything.RunAnythingAction
import com.intellij.ide.actions.runAnything.RunAnythingContext
import com.intellij.ide.actions.runAnything.activity.RunAnythingProviderBase
import com.intellij.ide.actions.runAnything.getPath
import com.intellij.ide.actions.runAnything.items.RunAnythingItem
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.text.StringUtil
import com.intellij.util.execution.ParametersListUtil
import java.nio.file.Path


abstract class CjRunAnythingProvider : RunAnythingProviderBase<String>(){


    abstract override fun getMainListItem(dataContext: DataContext, value: String): RunAnythingItem
    protected abstract fun run(
        executor: Executor,
        command: String,
        params: List<String>,
        workingDirectory: Path,
        cjpmProject: CjpmProject
    )
    abstract fun getCompletionProvider(project: Project, dataContext: DataContext) : CjCommandCompletionProvider
    override fun findMatchingValue(dataContext: DataContext, pattern: String): String? =
        if (pattern.startsWith(helpCommand)) getCommand(pattern) else null
    override fun getValues(dataContext: DataContext, pattern: String): Collection<String> {
        val project = dataContext.getData(CommonDataKeys.PROJECT) ?: return emptyList()
        if (!project.hasCjpmProject) return emptyList()
        val completionProvider = getCompletionProvider(project, dataContext)

        return when {
            pattern.startsWith(helpCommand) -> {
                val context = StringUtil.trimStart(pattern, helpCommand).substringBeforeLast(' ')
                val prefix = pattern.substringBeforeLast(' ')
                completionProvider.complete(context).map { "$prefix ${it.lookupString}" }
            }
            pattern.isNotBlank() && helpCommand.startsWith(pattern) ->
                completionProvider.complete("").map { "$helpCommand ${it.lookupString}" }
            else -> emptyList()
        }
    }

    override fun execute(dataContext: DataContext, value: String) {
        val project = dataContext.getData(CommonDataKeys.PROJECT) ?: return
        if (!project.hasCjpmProject) return
        val cjpmProject = getAppropriateCjpmProject(dataContext) ?: return
        val params = ParametersListUtil.parse(StringUtil.trimStart(value, helpCommand))
        val executionContext = dataContext.getData(EXECUTING_CONTEXT) ?: RunAnythingContext.ProjectContext(project)
        val path = executionContext.getPath()?.toPath() ?: return
        val executor = dataContext.getData(RunAnythingAction.EXECUTOR_KEY) ?: DefaultRunExecutor.getRunExecutorInstance()
        run(executor, params.firstOrNull() ?: "--help", params.drop(1), path, cjpmProject)
    }

    abstract override fun getHelpCommand(): String
}
fun getAppropriateCjpmProject(dataContext: DataContext): CjpmProject? {
    val cjpmProjects = dataContext.getData(CommonDataKeys.PROJECT)?.cjpmProjects ?: return null
    cjpmProjects.allProjects.singleOrNull()?.let { return it }

    dataContext.getData(CommonDataKeys.VIRTUAL_FILE)
        ?.let { cjpmProjects.findProjectForFile(it) }
        ?.let { return it }

    return dataContext.getData(CjpmToolWindow.SELECTED_CJPM_PROJECT)
        ?: cjpmProjects.allProjects.firstOrNull()
}
