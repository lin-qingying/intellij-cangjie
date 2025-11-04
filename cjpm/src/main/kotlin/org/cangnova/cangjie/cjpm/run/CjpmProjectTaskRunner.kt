/*
 * Copyright 2025 LinQingYing. and contributors.
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

package org.cangnova.cangjie.cjpm.run

import com.intellij.execution.ExecutorRegistry
import com.intellij.execution.RunManager
import com.intellij.execution.executors.DefaultRunExecutor
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.execution.runners.ProgramRunner
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectModelBuildableElement
import com.intellij.openapi.roots.ProjectModelExternalSource
import com.intellij.task.ModuleBuildTask
import com.intellij.task.ProjectModelBuildTask
import com.intellij.task.ProjectTask
import com.intellij.task.ProjectTaskContext
import com.intellij.task.ProjectTaskRunner
import com.intellij.task.TaskRunnerResults
import com.intellij.task.impl.ProjectModelBuildTaskImpl
import org.cangnova.cangjie.cjpm.project.CjpmBuildSystemId
import org.cangnova.cangjie.project.extension.ProjectBuildSystemId
import org.cangnova.cangjie.project.service.CjProjectsService
import org.cangnova.cangjie.run.*
import org.jetbrains.concurrency.AsyncPromise
import org.jetbrains.concurrency.Promise
import org.jetbrains.concurrency.resolvedPromise
import java.nio.file.Paths
import java.util.concurrent.ExecutionException

private val LOG: Logger = logger<CjpmProjectTaskRunner>()

/**
 * CJPM implementation of project task runner.
 * Handles building CJPM projects through the IDE's build system.
 */
class CjpmProjectTaskRunner : CangJieProjectTaskRunner {

    override fun getBuildSystemId(): ProjectBuildSystemId = CjpmBuildSystemId

    override fun canRun(projectTask: ProjectTask): Boolean {
        return when (projectTask) {
            is ModuleBuildTask -> {
                // Check if this is a CJPM project
                val project = projectTask.module.project
                val cjProject = CjProjectsService.getInstance(project).cjProject
                cjProject.isValid
            }

            is ProjectModelBuildTaskImpl<*> -> {
                val buildableElement = projectTask.buildableElement
                buildableElement is CjpmBuildConfiguration
            }

            else -> false
        }
    }

    override fun expandTask(task: ProjectTask): List<ProjectTask> {
        if (task !is ModuleBuildTask) return listOf(task)

        val project = task.module.project
        val cjProject = CjProjectsService.getInstance(project).cjProject

        val executor = ExecutorRegistry.getInstance()
            .getExecutorById(DefaultRunExecutor.EXECUTOR_ID) ?: return emptyList()

        val runner = ProgramRunner.findRunnerById("CangJieProgramRunner") ?: return emptyList()

        val runManager = RunManager.getInstance(project)

        // Create a build configuration for the project
        val factory = CangJieCommandRunConfigurationType.instance.factory
        val settings = runManager.createConfiguration("Build ${cjProject.name}", factory)
        val configuration = settings.configuration as? CangJieCommandRunConfiguration
            ?: return emptyList()

        // Configure for build command
        configuration.command = "build"
        configuration.workingDirectory = Paths.get(cjProject.rootDir.path)

        val environment = ExecutionEnvironment(executor, runner, settings, project)
        val buildableElement = CjpmBuildConfiguration(configuration, environment)

        return listOf(ProjectModelBuildTaskImpl(buildableElement, task.isIncrementalBuild))
    }

    override fun executeTask(
        project: Project,
        context: ProjectTaskContext,
        task: ProjectTask
    ): Promise<ProjectTaskRunner.Result> {

        if (task !is ProjectModelBuildTask<*>) {
            return resolvedPromise(TaskRunnerResults.ABORTED)
        }

        val buildConfiguration = task.buildableElement as? CjpmBuildConfiguration
            ?: return resolvedPromise(TaskRunnerResults.FAILURE)

        return executeBuild(project, buildConfiguration, task.isIncrementalBuild)
    }

    private fun executeBuild(
        project: Project,
        buildConfiguration: CjpmBuildConfiguration,
        isIncremental: Boolean
    ): Promise<ProjectTaskRunner.Result> {
        val promise = AsyncPromise<ProjectTaskRunner.Result>()

        try {
            // If not incremental, run clean first
            if (!isIncremental) {
                val cleanResult = runClean(project, buildConfiguration)
                if (!cleanResult) {
                    promise.setResult(TaskRunnerResults.FAILURE)
                    return promise
                }
            }

            // Run build
            val buildResult = runBuild(buildConfiguration)

            promise.setResult(if (buildResult) TaskRunnerResults.SUCCESS else TaskRunnerResults.FAILURE)
        } catch (e: ExecutionException) {
            LOG.error("Build execution failed", e)
            promise.setResult(TaskRunnerResults.FAILURE)
        } catch (e: Exception) {
            LOG.error("Build failed", e)
            promise.setResult(TaskRunnerResults.FAILURE)
        }

        return promise
    }

    private fun runClean(project: Project, buildConfiguration: CjpmBuildConfiguration): Boolean {
        // TODO: Implement clean logic
        // This should execute "cjpm clean" command
        return true
    }

    private fun runBuild(buildConfiguration: CjpmBuildConfiguration): Boolean {
        // TODO: Implement build logic
        // This should execute the build command through the configuration's environment
        val environment = buildConfiguration.environment
        val state = buildConfiguration.configuration.getState(
            environment.executor,
            environment
        ) as? CangJieCommandRunState ?: return false

        // Execute the build
        try {
            val processHandler = state.startProcess(processColors = false)
            processHandler.startNotify()
            processHandler.waitFor()
            return processHandler.exitCode == 0
        } catch (e: Exception) {
            LOG.error("Failed to execute build", e)
            return false
        }
    }
}

/**
 * Build configuration for CJPM projects
 */
data class CjpmBuildConfiguration(
    val configuration: CangJieCommandRunConfiguration,
    val environment: ExecutionEnvironment
) : ProjectModelBuildableElement {
    override fun getExternalSource(): ProjectModelExternalSource? {
        return null
    }
}