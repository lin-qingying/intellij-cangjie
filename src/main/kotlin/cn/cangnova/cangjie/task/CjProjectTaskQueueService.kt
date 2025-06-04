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

package cn.cangnova.cangjie.task

import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project

@Service
class CjProjectTaskQueueService: Disposable {
    private val queue: CjBackgroundTaskQueue = CjBackgroundTaskQueue()

    /** Submits a task. A task can implement [CangJieTask] */
    fun run(task: Task.Backgroundable) = queue.run(task)

    /** Equivalent to running an empty task with [CangJieTask.taskType] = [taskType] */
    fun cancelTasks(taskType:CangJieTask.TaskType) = queue.cancelTasks(taskType)

    /** @return true if no running or pending tasks */
    val isEmpty: Boolean get() = queue.isEmpty

    override fun dispose() {
        queue.dispose()
    }
}
val Project.taskQueue: CjProjectTaskQueueService get() = service()

interface CangJieTask {
    val taskType: TaskType
        get() = TaskType.INDEPENDENT

    val progressBarShowDelay: Int
        get() = 0

    /** If true, the task will not be run (and progress bar will not be shown) until the smart mode */
    val waitForSmartMode: Boolean
        get() = false

    val runSyncInUnitTests: Boolean
        get() = false


    enum class TaskType(val canBeCanceledByOther: Boolean = true) {
        CJPM_SYNC(canBeCanceledByOther = false),
        MACROS_CLEAR(canBeCanceledByOther = false),
        MACROS_UNPROCESSED,
        MACROS_FULL,

        /** Can't be canceled, cancels nothing. Should be the last variant of the enum. */
        INDEPENDENT(canBeCanceledByOther = false);

        fun canCancelOther(other: TaskType): Boolean =
            other.canBeCanceledByOther && this.ordinal <= other.ordinal
    }
}
