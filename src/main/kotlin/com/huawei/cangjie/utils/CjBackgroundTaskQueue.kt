package com.huawei.cangjie.utils

import com.huawei.cangjie.CjTask
import com.huawei.cangjie.ide.run.cjpm.isUnitTestMode
import com.huawei.cangjie.ide.run.cjpm.runconfig.buildtool.isHeadlessEnvironment

import com.huawei.cangjie.openapiext.DelayedBackgroundableProcessIndicator
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.progress.EmptyProgressIndicator
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.progress.impl.BackgroundableProcessIndicator
import com.intellij.openapi.progress.impl.ProgressManagerImpl
import com.intellij.openapi.project.DumbService
import com.intellij.util.concurrency.QueueProcessor
import java.util.function.BiConsumer

class CjBackgroundTaskQueue {
    companion object {
        private val LOG: Logger = logger<CjBackgroundTaskQueue>()
    }
    private val processor = QueueProcessor(
        QueueConsumer(),
        true,
        QueueProcessor.ThreadToUse.AWT,
    ) { isDisposed }

    @Volatile
    private var isDisposed: Boolean = false
    fun dispose() {
        isDisposed = true
        processor.clear()
        cancelAll()
    }
    // Guarded by self object monitor (@Synchronized)
    private val cancelableTasks: MutableList<BackgroundableTaskData> = mutableListOf()

    val isEmpty: Boolean get() = processor.isEmpty
    @Synchronized
    fun cancelTasks(taskType:CjTask.TaskType) {
        cancelableTasks.removeIf { data ->
            if (data.task is CjTask && taskType.canCancelOther(data.task.taskType)) {
                data.cancel()
                true
            } else {
                false
            }
        }
    }
    @Synchronized
    private fun cancelAll() {
        for (task in cancelableTasks) {
            task.cancel()
        }
        cancelableTasks.clear()
    }
    private fun runTaskInCurrentThread(task: Task.Backgroundable) {
        check(isUnitTestMode)
        val pm = ProgressManager.getInstance() as ProgressManagerImpl
        pm.runProcessWithProgressInCurrentThread(task, EmptyProgressIndicator(), ModalityState. nonModal())
    }
    @Synchronized
    fun run(task: Task.Backgroundable) {
        if (isUnitTestMode && task is CjTask && task.runSyncInUnitTests) {
            runTaskInCurrentThread(task)
        } else {
            LOG.debug("Scheduling task $task")
            if (task is CjTask) {
                cancelTasks(task.taskType)
            }
            val data = BackgroundableTaskData(task, ::onFinish)

            // Add to cancelable tasks even if the task is not [RsTaskExt] b/c it still can be canceled by [cancelAll]
            cancelableTasks += data

            processor.add(data)
        }
    }

    @Synchronized
    private fun onFinish(data: BackgroundableTaskData) {
        cancelableTasks.remove(data)
    }

    private interface ContinuableRunnable {
        fun run(continuation: Runnable)
    }
    private class BackgroundableTaskData(
        val task: Task.Backgroundable,
        val onFinish: (BackgroundableTaskData) -> Unit
    ) : ContinuableRunnable{
        private var state: State = State.Pending


        @Synchronized
        fun cancel() {
            when (val state = state) {
                State.Pending -> this.state = State.Canceled
                is State.Running -> state.indicator.cancel()
                is State.WaitForSmartMode -> {
                    this.state = State.CanceledContinued
                    state.continuation.run()
                }
                State.Canceled -> Unit
                State.CanceledContinued -> Unit
            }
        }
        @Synchronized
        override fun run(continuation: Runnable) {
            // BackgroundableProcessIndicator should be created from EDT
            checkIsDispatchThread()

            when (state) {
                State.CanceledContinued -> {
                    // continuation is already invoked, do nothing
                    return
                }
                State.Canceled -> {
                    continuation.run()
                    return
                }
                is State.Running -> error("Trying to re-run already running task")
                else -> Unit
            }

            if (task is CjTask && task.waitForSmartMode && DumbService.isDumb(task.project)) {
                check(state !is State.WaitForSmartMode)
                state = State.WaitForSmartMode(continuation)
                DumbService.getInstance(task.project).runWhenSmart { run(continuation) }
                return
            }

            val indicator = when {
                isHeadlessEnvironment -> EmptyProgressIndicator()

                task is CjTask && task.progressBarShowDelay > 0 ->
                    DelayedBackgroundableProcessIndicator(task, task.progressBarShowDelay)

                else -> BackgroundableProcessIndicator(task)
            }

            state = State.Running(indicator)

            val pm = ProgressManager.getInstance() as ProgressManagerImpl
            pm.runProcessWithProgressAsynchronously(
                task,
                indicator,
                {
                    onFinish(this)
                    continuation.run()
                },
                ModalityState. nonModal()
            )
        }



        private sealed class State {
            data object Pending : State()
            data class WaitForSmartMode(val continuation: Runnable) : State()
            data object Canceled : State()
            data object CanceledContinued : State()
            data class Running(val indicator: ProgressIndicator) : State()
        }


    }
    private class QueueConsumer : BiConsumer<ContinuableRunnable, Runnable> {
        override fun accept(t: ContinuableRunnable, u: Runnable) = t.run(u)
    }
}

fun checkIsDispatchThread() {
    check(ApplicationManager.getApplication().isDispatchThread) {
        "Should be invoked on the Swing dispatch thread"
    }
}
