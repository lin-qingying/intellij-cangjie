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

/**
 * 仓颉后台任务队列
 *
 * 用于管理和执行后台任务的队列系统，确保任务按顺序执行，并提供任务取消、智能模式等待等功能。
 *
 * ## 核心功能
 *
 * - **顺序执行**：任务按照提交顺序在 AWT（EDT）线程上调度，然后异步执行
 * - **任务取消**：支持按任务类型取消正在等待或运行的任务
 * - **智能模式等待**：可以等待项目退出 Dumb 模式后再执行任务
 * - **进度指示器**：自动管理后台进度条的显示和更新
 * - **单元测试支持**：在测试模式下可以同步执行任务
 *
 * ## 任务生命周期
 *
 * 1. **提交 (Submit)**：通过 [run] 方法提交任务到队列
 * 2. **排队 (Queued)**：任务在 EDT 上等待被处理
 * 3. **智能模式等待 (Wait for Smart Mode)**：如果需要，等待项目退出 Dumb 模式
 * 4. **执行 (Running)**：在后台线程中异步执行任务
 * 5. **完成 (Finished)**：任务执行完成，从可取消列表中移除
 *
 * ## 任务取消
 *
 * - 可以通过 [cancelTasks] 按任务类型取消任务
 * - 取消规则由 [CangJieTask.TaskType.canCancelOther] 定义
 * - 已经在运行的任务会收到取消信号，但可能不会立即停止
 *
 * ## 使用示例
 *
 * ```kotlin
 * val queue = CjBackgroundTaskQueue()
 *
 * // 提交任务
 * queue.run(object : CangJieTask(project, "My Task") {
 *     override fun run(indicator: ProgressIndicator) {
 *         // 执行耗时操作
 *     }
 * })
 *
 * // 取消特定类型的任务
 * queue.cancelTasks(CangJieTask.TaskType.PROJECT_SYNC)
 *
 * // 清理资源
 * queue.dispose()
 * ```
 *
 * ## 线程安全
 *
 * - 所有公共方法都是线程安全的
 * - 内部使用 `@Synchronized` 保护共享状态
 * - 任务调度在 EDT 上进行，但执行在后台线程
 *
 * @see CangJieTask
 * @see Task.Backgroundable
 */
class CjBackgroundTaskQueue {
    companion object {
        private val LOG: Logger = logger<CjBackgroundTaskQueue>()
    }

    /**
     * 队列处理器
     *
     * 使用 IntelliJ 的 QueueProcessor 在 AWT 线程上按顺序处理任务。
     * 每个任务完成后会通过 continuation 触发下一个任务的执行。
     */
    private val processor = QueueProcessor(
        QueueConsumer(),
        true,
        QueueProcessor.ThreadToUse.AWT,
    ) { isDisposed }

    /**
     * 队列是否已被释放
     */
    @Volatile
    private var isDisposed: Boolean = false

    /**
     * 释放队列资源
     *
     * 停止接受新任务，清空队列，并取消所有正在运行或等待的任务。
     * 调用此方法后，队列将不再可用。
     */
    fun dispose() {
        isDisposed = true
        processor.clear()
        cancelAll()
    }

    /**
     * 可取消的任务列表
     *
     * 保存所有正在排队或运行的任务，用于按类型取消或全部取消。
     * 访问此列表必须在 `@Synchronized` 方法中进行。
     */
    // Guarded by self object monitor (@Synchronized)
    private val cancelableTasks: MutableList<BackgroundableTaskData> = mutableListOf()

    /**
     * 队列是否为空
     *
     * @return true 如果队列中没有待处理的任务
     */
    val isEmpty: Boolean get() = processor.isEmpty

    /**
     * 取消指定类型的任务
     *
     * 根据 [CangJieTask.TaskType.canCancelOther] 的规则，取消队列中所有可以被指定类型取消的任务。
     *
     * ## 取消行为
     *
     * - **Pending 状态**：直接标记为已取消，不会执行
     * - **WaitForSmartMode 状态**：取消等待，继续执行下一个任务
     * - **Running 状态**：向 ProgressIndicator 发送取消信号
     *
     * @param taskType 要取消的任务类型
     */
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

    /**
     * 取消所有任务
     *
     * 取消队列中的所有任务，包括正在等待和正在运行的任务。
     * 此方法在 [dispose] 时被调用。
     */
    @Synchronized
    private fun cancelAll() {
        for (task in cancelableTasks) {
            task.cancel()
        }
        cancelableTasks.clear()
    }

    /**
     * 在当前线程同步运行任务（仅用于单元测试）
     *
     * 在单元测试模式下，某些任务需要同步执行以便测试代码能够正确断言结果。
     * 此方法会在当前线程中运行任务，阻塞直到任务完成。
     *
     * @param task 要执行的任务
     * @throws IllegalStateException 如果不在单元测试模式
     */
    private fun runTaskInCurrentThread(task: Task.Backgroundable) {
        check(isUnitTestMode)
        val pm = ProgressManager.getInstance() as ProgressManagerImpl
        pm.runProcessWithProgressInCurrentThread(task, EmptyProgressIndicator(), ModalityState.NON_MODAL)
    }

    /**
     * 提交任务到队列
     *
     * 将任务添加到执行队列。如果任务是 [CangJieTask]，会先取消所有可以被该任务类型取消的其他任务。
     *
     * ## 执行模式
     *
     * - **单元测试模式**：如果 [CangJieTask.runSyncInUnitTests] 为 true，则在当前线程同步执行
     * - **正常模式**：异步执行，任务在后台线程中运行
     *
     * ## 任务取消
     *
     * 如果提交的是 [CangJieTask]，会根据其 [CangJieTask.taskType] 自动取消队列中的相关任务。
     * 例如：提交一个新的项目同步任务会取消之前排队的同步任务。
     *
     * @param task 要执行的后台任务
     */
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

    /**
     * 任务完成回调
     *
     * 当任务执行完成（成功或失败）时调用，从可取消任务列表中移除该任务。
     *
     * @param data 已完成的任务数据
     */
    @Synchronized
    private fun onFinish(data: BackgroundableTaskData) {
        cancelableTasks.remove(data)
    }

    /**
     * 可继续执行的 Runnable
     *
     * 扩展了标准 Runnable，增加了 continuation 参数，用于在任务完成后触发下一个任务。
     * 这是 [QueueProcessor] 要求的接口。
     */
    private interface ContinuableRunnable {
        /**
         * 执行任务，并在完成后调用 continuation 继续执行队列中的下一个任务
         *
         * @param continuation 任务完成后要执行的回调
         */
        fun run(continuation: Runnable)
    }

    /**
     * 后台任务数据包装器
     *
     * 包装 [Task.Backgroundable]，管理任务的生命周期和状态转换。
     *
     * ## 状态机
     *
     * ```
     * Pending -> WaitForSmartMode -> Running -> (Finished)
     *    ↓            ↓                ↓
     * Canceled   CanceledContinued  (Canceled)
     * ```
     *
     * - **Pending**：任务已创建，等待执行
     * - **WaitForSmartMode**：等待项目退出 Dumb 模式
     * - **Running**：任务正在后台线程中执行
     * - **Canceled**：任务在开始前被取消
     * - **CanceledContinued**：任务在等待智能模式时被取消，continuation 已被调用
     *
     * @property task 要执行的后台任务
     * @property onFinish 任务完成时的回调
     */
    private class BackgroundableTaskData(
        val task: Task.Backgroundable,
        val onFinish: (BackgroundableTaskData) -> Unit
    ) : ContinuableRunnable {
        /**
         * 任务当前状态
         */
        private var state: State = State.Pending

        /**
         * 取消任务
         *
         * 根据当前状态执行不同的取消操作：
         * - **Pending**：标记为已取消，任务不会执行
         * - **WaitForSmartMode**：取消等待，调用 continuation 继续下一个任务
         * - **Running**：向进度指示器发送取消信号
         * - **Canceled/CanceledContinued**：无操作（已经取消）
         */
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

        /**
         * 执行任务
         *
         * 必须在 EDT 线程上调用。根据任务状态和配置，执行以下操作：
         *
         * 1. **检查取消状态**：如果任务已被取消，直接调用 continuation
         * 2. **等待智能模式**：如果需要且项目在 Dumb 模式，等待项目变为 Smart 模式
         * 3. **创建进度指示器**：根据环境和任务配置创建合适的进度指示器
         * 4. **异步执行**：在后台线程中异步执行任务
         * 5. **完成回调**：任务完成后调用 [onFinish] 和 continuation
         *
         * @param continuation 任务完成后要调用的回调
         * @throws IllegalStateException 如果任务已经在运行
         * @throws IllegalStateException 如果不在 EDT 线程
         */
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

            // 如果任务需要智能模式且当前在 Dumb 模式，等待项目变为 Smart 模式
            if (task is CangJieTask && task.waitForSmartMode && DumbService.isDumb(task.project)) {
                check(state !is State.WaitForSmartMode)
                state = State.WaitForSmartMode(continuation)
                DumbService.getInstance(task.project).runWhenSmart { run(continuation) }
                return
            }

            // 创建进度指示器
            val indicator = when {
                isHeadlessEnvironment -> EmptyProgressIndicator()

                task is CangJieTask && task.progressBarShowDelay > 0 ->
                    DelayedBackgroundableProcessIndicator(task, task.progressBarShowDelay)

                else -> BackgroundableProcessIndicator(task)
            }

            state = State.Running(indicator)

            // 异步执行任务
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


        /**
         * 任务状态
         *
         * 定义任务在其生命周期中的所有可能状态。
         */
        private sealed class State {
            /**
             * 任务已创建，等待执行
             */
            object Pending : State()

            /**
             * 任务正在等待项目退出 Dumb 模式
             *
             * @property continuation 智能模式就绪后要调用的回调
             */
            data class WaitForSmartMode(val continuation: Runnable) : State()

            /**
             * 任务在开始前被取消
             */
            object Canceled : State()

            /**
             * 任务在等待智能模式时被取消，continuation 已被调用
             */
            object CanceledContinued : State()

            /**
             * 任务正在后台线程中运行
             *
             * @property indicator 任务的进度指示器，可用于取消任务
             */
            data class Running(val indicator: ProgressIndicator) : State()
        }
    }

    /**
     * 队列消费者
     *
     * 实现 [BiConsumer] 接口，将 [ContinuableRunnable] 和 continuation 连接起来。
     * 这是 [QueueProcessor] 要求的消费者实现。
     */
    private class QueueConsumer : BiConsumer<ContinuableRunnable, Runnable> {
        override fun accept(t: ContinuableRunnable, u: Runnable) = t.run(u)
    }
}

/**
 * 检查当前线程是否为 EDT（Event Dispatch Thread）
 *
 * 某些操作（如创建进度指示器）必须在 EDT 上执行。
 * 此方法用于在开发阶段及早发现线程问题。
 *
 * @throws IllegalStateException 如果当前线程不是 EDT
 */
fun checkIsDispatchThread() {
    check(ApplicationManager.getApplication().isDispatchThread) {
        "Should be invoked on the Swing dispatch thread"
    }
}

/**
 * 是否为无头环境
 *
 * 在无头环境（如 CI/CD）中不应该显示 UI 组件。
 * 此属性用于判断是否需要创建可见的进度指示器。
 */
val isHeadlessEnvironment: Boolean get() = ApplicationManager.getApplication().isHeadlessEnvironment
