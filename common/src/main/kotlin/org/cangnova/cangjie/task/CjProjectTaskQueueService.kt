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
 * 这是一个项目级别的服务，每个 IntelliJ 项目都有自己独立的任务队列实例。
 *
 * ## 核心职责
 *
 * - **任务调度**：提交后台任务到队列，按顺序执行
 * - **任务取消**：根据任务类型取消相关任务
 * - **生命周期管理**：随项目关闭自动清理资源
 *
 * ## 架构设计
 *
 * ```
 * CjProjectTaskQueueService (项目级服务)
 *          ↓
 * CjBackgroundTaskQueue (队列实现)
 *          ↓
 * QueueProcessor (IntelliJ 队列处理器)
 * ```
 *
 * ## 使用示例
 *
 * ```kotlin
 * // 通过扩展属性访问
 * val taskQueue = project.taskQueue
 *
 * // 提交任务
 * taskQueue.run(object : Task.Backgroundable(project, "My Task") {
 *     override fun run(indicator: ProgressIndicator) {
 *         // 执行任务
 *     }
 * })
 *
 * // 取消特定类型的任务
 * taskQueue.cancelTasks(CangJieTask.TaskType.CANGJIE_SYNC)
 *
 * // 检查队列状态
 * if (taskQueue.isEmpty) {
 *     // 队列空闲
 * }
 * ```
 *
 * ## 任务类型系统
 *
 * 如果任务实现了 [CangJieTask] 接口，可以通过 [CangJieTask.TaskType] 定义任务之间的取消关系。
 * 这允许新任务自动取消旧任务，避免重复执行。
 *
 * ## 生命周期
 *
 * 服务随项目创建而创建，随项目关闭而销毁。
 * 在 [dispose] 时会自动取消所有正在运行和等待的任务。
 *
 * @see CjBackgroundTaskQueue
 * @see CangJieTask
 */
@Service(Service.Level.PROJECT)
class CjProjectTaskQueueService : Disposable {
    /**
     * 底层任务队列实现
     *
     * 负责实际的任务调度和执行。
     */
    private val queue: CjBackgroundTaskQueue = CjBackgroundTaskQueue()

    /**
     * 提交一个任务到队列
     *
     * 任务会被添加到队列末尾，按照 FIFO（先进先出）顺序执行。
     * 如果任务实现了 [CangJieTask] 接口，会根据 [CangJieTask.taskType] 自动取消相关的旧任务。
     *
     * ## 执行模式
     *
     * - **正常模式**：任务在后台线程异步执行
     * - **测试模式**：如果 [CangJieTask.runSyncInUnitTests] 为 true，在当前线程同步执行
     *
     * @param task 要执行的后台任务
     * @see CjBackgroundTaskQueue.run
     */
    fun run(task: Task.Backgroundable) = queue.run(task)

    /**
     * 取消指定类型的所有任务
     *
     * 根据 [CangJieTask.TaskType.canCancelOther] 的规则，取消队列中所有可以被指定类型取消的任务。
     *
     * ## 取消规则
     *
     * - 只有 `canBeCanceledByOther = true` 的任务可以被取消
     * - ordinal 较小的任务可以取消 ordinal 较大的任务
     * - [TaskType.INDEPENDENT] 类型的任务不会被取消，也不会取消其他任务
     *
     * @param taskType 要取消的任务类型
     * @see CjBackgroundTaskQueue.cancelTasks
     */
    fun cancelTasks(taskType: CangJieTask.TaskType) = queue.cancelTasks(taskType)

    /**
     * 检查队列是否为空
     *
     * @return 如果没有正在运行或待执行的任务，返回 true
     */
    val isEmpty: Boolean get() = queue.isEmpty

    /**
     * 释放服务资源
     *
     * 在项目关闭时自动调用，会取消所有正在运行和等待的任务。
     */
    override fun dispose() {
        queue.dispose()
    }
}
/**
 * 项目任务队列的便捷访问扩展属性
 *
 * 提供了简洁的方式来访问项目的任务队列服务。
 *
 * ## 使用示例
 *
 * ```kotlin
 * // 直接通过项目实例访问
 * project.taskQueue.run(myTask)
 *
 * // 而不需要写
 * project.service<CjProjectTaskQueueService>().run(myTask)
 * ```
 */
val Project.taskQueue: CjProjectTaskQueueService get() = service()

/**
 * 仓颉任务接口
 *
 * 为后台任务提供额外的配置选项和任务类型管理。
 * 后台任务可以实现此接口来定义任务的行为特征。
 *
 * ## 核心功能
 *
 * - **任务类型系统**：通过 [TaskType] 定义任务之间的取消关系
 * - **进度条控制**：配置进度条的显示延迟
 * - **智能模式等待**：可以等待项目退出 Dumb 模式再执行
 * - **测试支持**：控制在单元测试中的执行方式
 *
 * ## 任务取消规则
 *
 * 任务类型定义了任务之间的优先级和取消关系：
 * - 新提交的任务可以取消队列中的旧任务
 * - 取消规则由 [TaskType.canCancelOther] 决定
 * - [TaskType.INDEPENDENT] 类型的任务完全独立，不参与取消机制
 *
 * ## 实现示例
 *
 * ```kotlin
 * class MySyncTask(project: Project) : Task.Backgroundable(project, "Syncing..."), CangJieTask {
 *     override val taskType = CangJieTask.TaskType.CANGJIE_SYNC
 *     override val waitForSmartMode = true
 *     override val progressBarShowDelay = 300
 *
 *     override fun run(indicator: ProgressIndicator) {
 *         // 执行同步操作
 *     }
 * }
 * ```
 *
 * @see TaskType
 * @see CjBackgroundTaskQueue
 */
interface CangJieTask {
    /**
     * 任务类型
     *
     * 定义任务在队列中的行为和优先级。
     * 默认为 [TaskType.INDEPENDENT]，表示独立任务，不参与取消机制。
     *
     * @return 任务类型
     */
    val taskType: TaskType
        get() = TaskType.INDEPENDENT

    /**
     * 进度条显示延迟（毫秒）
     *
     * 控制进度条何时开始显示。对于快速完成的任务，延迟显示进度条可以避免闪烁。
     *
     * ## 使用场景
     *
     * - **0（默认）**：立即显示进度条
     * - **300-500**：适合大多数任务，如果任务在这个时间内完成，不会显示进度条
     * - **1000+**：适合经常很快完成的任务
     *
     * @return 延迟时间（毫秒），默认为 0
     */
    val progressBarShowDelay: Int
        get() = 0

    /**
     * 是否等待智能模式
     *
     * 如果为 true，任务会在项目进入智能模式（退出 Dumb 模式）后才开始执行。
     *
     * ## Dumb 模式和智能模式
     *
     * - **Dumb 模式**：项目正在建立索引，许多 IDE 功能不可用
     * - **智能模式**：索引构建完成，所有功能可用
     *
     * ## 何时使用
     *
     * - 任务需要访问项目索引时设置为 true
     * - 任务需要解析代码、查找引用等操作时设置为 true
     * - 纯文件操作、网络请求等不依赖索引的任务可以设置为 false
     *
     * @return 是否等待智能模式，默认为 false
     */
    val waitForSmartMode: Boolean
        get() = false

    /**
     * 在单元测试中是否同步运行
     *
     * 如果为 true，在单元测试模式下任务会在当前线程同步执行，而不是异步执行。
     * 这使得测试代码可以直接断言任务的执行结果。
     *
     * ## 使用场景
     *
     * - **true**：测试代码需要立即验证任务结果
     * - **false**：任务本身就是异步的，测试需要处理异步场景
     *
     * @return 是否同步运行，默认为 false
     */
    val runSyncInUnitTests: Boolean
        get() = false

    /**
     * 任务类型枚举
     *
     * 定义了所有可能的任务类型及其取消关系。
     * 任务类型通过 ordinal（声明顺序）定义了优先级。
     *
     * ## 取消规则
     *
     * 1. 只有 `canBeCanceledByOther = true` 的任务可以被其他任务取消
     * 2. ordinal 较小（声明靠前）的任务可以取消 ordinal 较大的任务
     * 3. [INDEPENDENT] 类型不参与任何取消机制
     *
     * ## 优先级顺序（从高到低）
     *
     * ```
     * CANGJIE_SYNC (最高优先级，不能被取消)
     * MACROS_CLEAR (高优先级，不能被取消)
     * MACROS_UNPROCESSED (可以被 CANGJIE_SYNC 和 MACROS_CLEAR 取消)
     * MACROS_FULL (可以被上述所有任务取消)
     * INDEPENDENT (独立任务，不参与取消机制)
     * ```
     *
     * ## 使用示例
     *
     * ```kotlin
     * // 提交一个高优先级同步任务，会取消所有可取消的宏任务
     * project.taskQueue.run(SyncTask(project)) // taskType = CANGJIE_SYNC
     *
     * // 提交一个宏处理任务，可能会被新的同步任务取消
     * project.taskQueue.run(MacrosTask(project)) // taskType = MACROS_FULL
     *
     * // 提交一个独立任务，不会被取消也不会取消其他任务
     * project.taskQueue.run(LogTask(project)) // taskType = INDEPENDENT
     * ```
     *
     * @property canBeCanceledByOther 是否可以被其他任务取消
     */
    enum class TaskType(val canBeCanceledByOther: Boolean = true) {
        /**
         * 仓颉项目同步任务
         *
         * 最高优先级任务，用于同步项目配置和依赖。
         * 不能被其他任务取消，但可以取消所有可取消的任务。
         */
        CANGJIE_SYNC(canBeCanceledByOther = false),

        /**
         * 宏清理任务
         *
         * 高优先级任务，用于清理宏缓存。
         * 不能被其他任务取消，但可以取消除 CANGJIE_SYNC 外的所有可取消任务。
         */
        MACROS_CLEAR(canBeCanceledByOther = false),

        /**
         * 未处理宏任务
         *
         * 处理新增或修改的宏。
         * 可以被 CANGJIE_SYNC 和 MACROS_CLEAR 取消。
         */
        MACROS_UNPROCESSED,

        /**
         * 完整宏处理任务
         *
         * 处理所有宏，包括已处理的。
         * 可以被上述所有任务类型取消。
         */
        MACROS_FULL,

        /**
         * 独立任务
         *
         * 不参与任务取消机制的独立任务。
         * - 不能被其他任务取消
         * - 不会取消其他任务
         * - 适用于日志、通知等辅助性任务
         *
         * **注意**：必须是枚举的最后一个值，以确保其 ordinal 最大。
         */
        INDEPENDENT(canBeCanceledByOther = false);

        /**
         * 判断当前任务类型是否可以取消另一个任务类型
         *
         * ## 取消规则
         *
         * 返回 true 当且仅当：
         * 1. 目标任务允许被取消（`other.canBeCanceledByOther == true`）
         * 2. 当前任务的优先级不低于目标任务（`this.ordinal <= other.ordinal`）
         *
         * ## 示例
         *
         * ```kotlin
         * CANGJIE_SYNC.canCancelOther(MACROS_FULL) // true
         * MACROS_FULL.canCancelOther(CANGJIE_SYNC) // false (优先级不够)
         * MACROS_FULL.canCancelOther(INDEPENDENT)  // false (INDEPENDENT 不能被取消)
         * INDEPENDENT.canCancelOther(MACROS_FULL)  // false (INDEPENDENT 不取消其他任务)
         * ```
         *
         * @param other 要检查的目标任务类型
         * @return true 如果当前类型可以取消目标类型
         */
        fun canCancelOther(other: TaskType): Boolean =
            other.canBeCanceledByOther && this.ordinal <= other.ordinal
    }
}
