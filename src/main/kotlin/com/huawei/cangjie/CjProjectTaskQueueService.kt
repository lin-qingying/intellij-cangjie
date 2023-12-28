package com.huawei.cangjie

import com.huawei.cangjie.utils.CjBackgroundTaskQueue
import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project

@Service
class CjProjectTaskQueueService: Disposable {
    private val queue: CjBackgroundTaskQueue = CjBackgroundTaskQueue()

    /** Submits a task. A task can implement [CjTask] */
    fun run(task: Task.Backgroundable) = queue.run(task)

    /** Equivalent to running an empty task with [CjTask.taskType] = [taskType] */
    fun cancelTasks(taskType:CjTask.TaskType) = queue.cancelTasks(taskType)

    /** @return true if no running or pending tasks */
    val isEmpty: Boolean get() = queue.isEmpty

    override fun dispose() {
        queue.dispose()
    }
}
val Project.taskQueue: CjProjectTaskQueueService get() = service()

interface CjTask {
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