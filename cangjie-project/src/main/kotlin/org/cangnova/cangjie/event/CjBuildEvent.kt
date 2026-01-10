/*
 * Copyright 2026 LinQingYing. and contributors.
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

package org.cangnova.cangjie.event

import com.intellij.openapi.project.Project
import org.cangnova.cangjie.model.CjBuildResult

import org.cangnova.cangjie.model.CjBuildTask

/**
 * 构建事件
 */
sealed class CjBuildEvent(
    /**
     * 项目实例
     */
    val project: Project
) {
    /**
     * 任务开始事件
     */
    data class TaskStarted(
        val task: CjBuildTask,
        val projectInstance: Project
    ) : CjBuildEvent(projectInstance)

    /**
     * 任务完成事件
     */
    data class TaskFinished(
        val task: CjBuildTask,
        val result: CjBuildResult,
        val projectInstance: Project
    ) : CjBuildEvent(projectInstance)

    /**
     * 任务取消事件
     */
    data class TaskCancelled(
        val task: CjBuildTask,
        val projectInstance: Project
    ) : CjBuildEvent(projectInstance)

    /**
     * 任务进度更新事件
     */
    data class TaskProgress(
        val task: CjBuildTask,
        val progress: Double,
        val message: String?,
        val projectInstance: Project
    ) : CjBuildEvent(projectInstance)
}