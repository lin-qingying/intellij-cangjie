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

package org.cangnova.cangjie.task

import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project

/**
 * 仓颉项目任务队列服务
 *
 * 管理项目级别的后台任务队列，提供任务调度和取消功能。
 *
 * ## 主要功能
 * - 提交后台任务到队列
 * - 根据任务类型取消任务
 * - 检查队列状态
 *
 * ## 使用示例
 * ```kotlin
 * val taskQueue = project.taskQueue
 * taskQueue.run(MyBackgroundTask(project))
 * ```
 *
 * @see CjBackgroundTaskQueue
 * @see CangJieTask
 */
@Service
class CjProjectTaskQueueService: Disposable {
    private val queue: CjBackgroundTaskQueue = CjBackgroundTaskQueue()

    /**
     * 提交一个任务到队列
     *
     * 任务可以实现 [CangJieTask] 接口以提供额外的配置选项
     *
     * @param task 要执行的后台任务
     */
    fun run(task: Task.Backgroundable) = queue.run(task)

    /**
     * 取消指定类型的所有任务
     *
     * 相当于运行一个空任务，其 [CangJieTask.taskType] = [taskType]
     *
     * @param taskType 要取消的任务类型
     */
    fun cancelTasks(taskType:CangJieTask.TaskType) = queue.cancelTasks(taskType)

    /**
     * 检查队列是否为空
     *
     * @return 如果没有正在运行或待执行的任务，返回 true
     */
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
        CANGJIE_SYNC(canBeCanceledByOther = false),
        MACROS_CLEAR(canBeCanceledByOther = false),
        MACROS_UNPROCESSED,
        MACROS_FULL,

        /** Can't be canceled, cancels nothing. Should be the last variant of the enum. */
        INDEPENDENT(canBeCanceledByOther = false);

        fun canCancelOther(other: TaskType): Boolean =
            other.canBeCanceledByOther && this.ordinal <= other.ordinal
    }
}
