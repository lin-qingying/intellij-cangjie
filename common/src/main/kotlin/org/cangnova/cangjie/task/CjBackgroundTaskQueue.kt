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

package org.cangnova.cangjie.task


import com.intellij.ide.plugins.PluginManagerCore.isUnitTestMode
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
import kotlin.collections.plusAssign

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
    fun cancelTasks(taskType: CangJieTask.TaskType) {
        cancelableTasks.removeIf { data ->
            if (data.task is CangJieTask && taskType.canCancelOther(data.task.taskType)) {
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
        pm.runProcessWithProgressInCurrentThread(task, EmptyProgressIndicator(), ModalityState.NON_MODAL)
    }

    @Synchronized
    fun run(task: Task.Backgroundable) {
        if (isUnitTestMode && task is CangJieTask && task.runSyncInUnitTests) {
            runTaskInCurrentThread(task)
        } else {
            LOG.debug("Scheduling task $task")
            if (task is CangJieTask) {
                cancelTasks(task.taskType)
            }
            val data = BackgroundableTaskData(task, ::onFinish)

            // Add to cancelable tasks even if the task is not [CjTaskExt] b/c it still can be canceled by [cancelAll]
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
    ) : ContinuableRunnable {
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

            if (task is CangJieTask && task.waitForSmartMode && DumbService.isDumb(task.project)) {
                check(state !is State.WaitForSmartMode)
                state = State.WaitForSmartMode(continuation)
                DumbService.getInstance(task.project).runWhenSmart { run(continuation) }
                return
            }

            val indicator = when {
                isHeadlessEnvironment -> EmptyProgressIndicator()

                task is CangJieTask && task.progressBarShowDelay > 0 ->
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
                ModalityState.NON_MODAL
            )
        }


        private sealed class State {
            object Pending : State()
            data class WaitForSmartMode(val continuation: Runnable) : State()
            object Canceled : State()
            object CanceledContinued : State()
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

val isHeadlessEnvironment: Boolean get() = ApplicationManager.getApplication().isHeadlessEnvironment
