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

package org.cangnova.cangjie.run

import com.intellij.build.BuildProgressListener
import com.intellij.build.BuildViewManager
import com.intellij.execution.ExecutorRegistry
import com.intellij.execution.RunManager
import com.intellij.execution.executors.DefaultDebugExecutor
import com.intellij.execution.executors.DefaultRunExecutor
import com.intellij.execution.impl.RunManagerImpl
import com.intellij.execution.impl.RunnerAndConfigurationSettingsImpl
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.execution.runners.ProgramRunner
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.MessageType
import org.cangnova.cangjie.run.CjExecutableRunner.Companion.initializeArtifactsFuture
import java.nio.file.Path
import java.util.concurrent.CompletableFuture

val ExecutionEnvironment?.isActivateToolWindowBeforeRun: Boolean
    get() = this?.runnerAndConfigurationSettings?.isActivateToolWindowBeforeRun != false

/**
 * Manager for CangJie build operations.
 * Provides utilities for executing builds with Build Tool Window integration.
 */
object CangJieBuildManager {

    private const val NOTIFICATION_GROUP_ID = "CangJie Build"

    /**
     * Create a build adapter for a process handler
     */
    fun createBuildAdapter(
        context: CangJieBuildContext,
        buildProgressListener: BuildProgressListener
    ): CangJieBuildAdapter {
        return CangJieBuildAdapter(context, buildProgressListener)
    }

    /**
     * Check if a configuration is a build configuration.
     * A configuration is considered a build configuration if:
     * - It's a CangJieCommandRunConfiguration with command "build"
     * - It's a CangJieProgramRunConfiguration (which requires building the program)
     */
    fun isBuildConfiguration(configuration: CangJieRunConfigurationBase): Boolean {
        return when (configuration) {
            is CangJieCommandRunConfiguration -> configuration.command == "build"
            is CangJieProgramRunConfiguration -> true
            else -> false
        }
    }

    /**
     * Get or create a build configuration for the given run configuration.
     *
     * For CangJieCommandRunConfiguration with command "build", returns the configuration itself.
     * For CangJieProgramRunConfiguration, creates a new build configuration.
     * For other configurations, returns null.
     *
     * @param configuration The run configuration to get build configuration for
     * @return A build configuration, or null if the configuration doesn't require building
     */
    fun getBuildConfiguration(configuration: CangJieRunConfigurationBase): CangJieCommandRunConfiguration? {


        return when (configuration) {
            is CangJieCommandRunConfiguration -> {

                if (configuration.command == "build") {
                    // Already a build configuration
                    configuration
                } else {
                    // Create a build configuration for other commands
                    val buildConfig = CangJieCommandRunConfiguration(
                        configuration.project,
                        configuration.factory!!,
                        "Build ${configuration.name}"
                    )
                    buildConfig.command = "build"
                    buildConfig.workingDirectory = configuration.workingDirectory
                    buildConfig.env = configuration.env
                    buildConfig
                }
            }

            is CangJieProgramRunConfiguration -> {

                // Create a build configuration for the program
                val buildConfig = CangJieCommandRunConfiguration(
                    configuration.project,
                    configuration.factory!!,
                    "Build ${configuration.name}"
                )
                buildConfig.command = "build"
                buildConfig.workingDirectory = configuration.workingDirectory
                buildConfig.env = configuration.env
                buildConfig
            }

            else -> null
        }
    }

    fun createBuildEnvironment(
        buildConfiguration: CangJieRunConfigurationBase,
        environment: ExecutionEnvironment
    ): ExecutionEnvironment? {
        require(isBuildConfiguration(buildConfiguration))
        val project = buildConfiguration.project
        val runManager = RunManager.getInstance(project) as? RunManagerImpl ?: return null
        val executor = ExecutorRegistry.getInstance().getExecutorById(
            if (environment.isDebug)
                DefaultDebugExecutor.EXECUTOR_ID
            else
                DefaultRunExecutor.EXECUTOR_ID


        ) ?: return null
        val runner = ProgramRunner.findRunnerById(CangJieProgramRunner.RUNNER_ID) ?: return null
        val settings = RunnerAndConfigurationSettingsImpl(runManager, buildConfiguration)
        settings.isActivateToolWindowBeforeRun = environment.isActivateToolWindowBeforeRun
        val buildEnvironment = ExecutionEnvironment(executor, runner, settings, project)
        environment.copyUserDataTo(buildEnvironment)
        return buildEnvironment
    }

    /**
     * Attach build adapter to process handler with Build Tool Window integration
     */
    fun attachBuildAdapter(
        project: Project,
        taskName: String,
        workingDirectory: Path,
        environment: ExecutionEnvironment,
        processHandler: com.intellij.execution.process.ProcessHandler,
        buildProfile: BuildProfile = BuildProfile.RELEASE  // Default to RELEASE
    ): CangJieBuildContext {
        val buildViewManager = project.service<BuildViewManager>()
        val buildProgressListener = buildViewManager as BuildProgressListener

        val buildId = Any()
        val context = CangJieBuildContext(
            buildId = buildId,
            parentId = null,
            taskName = taskName,
            workingDirectory = workingDirectory,
            environment = environment,
            buildProfile = buildProfile
        )

        context.processHandler = processHandler

        // Initialize artifacts future for this environment
        // This signals that we expect artifacts to be produced
        environment.initializeArtifactsFuture()

        val adapter = createBuildAdapter(context, buildProgressListener)
        processHandler.addProcessListener(adapter)

        return context
    }

    /**
     * Show a build notification
     */
    fun showBuildNotification(
        project: Project,
        messageType: MessageType,
        message: String,
        details: String?,
        duration: Long
    ) {
        val notificationType = when (messageType) {
            MessageType.ERROR -> NotificationType.ERROR
            MessageType.WARNING -> NotificationType.WARNING
            else -> NotificationType.INFORMATION
        }

        val content = buildString {
            append(message)
            if (details != null) {
                append("\n")
                append(details)
            }
            append("\n")
            append("Duration: ${formatDuration(duration)}")
        }

        try {
            NotificationGroupManager.getInstance()
                .getNotificationGroup(NOTIFICATION_GROUP_ID)
                .createNotification(content, notificationType)
                .notify(project)
        } catch (e: Exception) {
            // Fallback if notification group is not registered
            com.intellij.notification.Notifications.Bus.notify(
                com.intellij.notification.Notification(
                    NOTIFICATION_GROUP_ID,
                    message,
                    content,
                    notificationType
                ),
                project
            )
        }
    }

    private fun formatDuration(millis: Long): String {
        val seconds = millis / 1000
        val minutes = seconds / 60
        val remainingSeconds = seconds % 60

        return when {
            minutes > 0 -> "${minutes}m ${remainingSeconds}s"
            else -> "${seconds}s"
        }
    }
}