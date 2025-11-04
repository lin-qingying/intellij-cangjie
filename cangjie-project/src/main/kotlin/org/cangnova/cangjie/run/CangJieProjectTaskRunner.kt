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
import com.intellij.task.ProjectTaskRunner.Result

import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.project.Project
import com.intellij.task.ProjectTask
import com.intellij.task.ProjectTaskContext
import org.cangnova.cangjie.project.extension.ProjectBuildSystemId
import org.jetbrains.concurrency.Promise

/**
 * Extension point for subsystem-specific project task runners.
 * Each build system (CJPM, etc.) should provide its own implementation.
 */
interface CangJieProjectTaskRunner {

    companion object {
        private val EP_NAME = ExtensionPointName<CangJieProjectTaskRunner>(
            "org.cangnova.cangjie.run.projectTaskRunner"
        )

        /**
         * Find the appropriate task runner for the current build system
         */
        fun findRunner(): CangJieProjectTaskRunner? {
            val buildSystem = org.cangnova.cangjie.project.service.CjProjectBuildSystemService
                .getInstance()
                .getBuildSystem() ?: return null

            return EP_NAME.extensionList.firstOrNull {
                it.getBuildSystemId().id == buildSystem.id
            }
        }
    }

    /**
     * Get the build system ID this runner handles
     */
    fun getBuildSystemId(): ProjectBuildSystemId

    /**
     * Check if this runner can handle the given task
     */
    fun canRun(projectTask: ProjectTask): Boolean

    /**
     * Expand a task into multiple sub-tasks if needed
     */
    fun expandTask(task: ProjectTask): List<ProjectTask>

    /**
     * Execute a single task
     */
    fun executeTask(
        project: Project,
        context: ProjectTaskContext,
        task: ProjectTask
    ): Promise<Result>
}

