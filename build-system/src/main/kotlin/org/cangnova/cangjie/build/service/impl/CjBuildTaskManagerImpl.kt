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

package org.cangnova.cangjie.build.service.impl

import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import org.cangnova.cangjie.build.event.CjBuildEvent
import org.cangnova.cangjie.build.event.CjBuildListener
import org.cangnova.cangjie.build.extension.CjBuildTaskExecutor
import org.cangnova.cangjie.build.model.CjBuildContext
import org.cangnova.cangjie.build.model.CjBuildResult
import org.cangnova.cangjie.build.model.CjBuildTask
import org.cangnova.cangjie.build.service.CjBuildTaskManager
import org.cangnova.cangjie.project.service.CjProjectBuildSystemService
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

/**
 * 构建任务管理器服务实现
 */
@Service(Service.Level.PROJECT)
class CjBuildTaskManagerImpl(
    private val project: Project
) : CjBuildTaskManager {

    private val log = logger<CjBuildTaskManagerImpl>()

    /**
     * 正在执行的任务集合
     */
    private val runningTasks = ConcurrentHashMap.newKeySet<CjBuildTask>()

    /**
     * 构建监听器列表
     */
    private val buildListeners = CopyOnWriteArrayList<CjBuildListener>()

    override fun executeTask(task: CjBuildTask, context: CjBuildContext): CjBuildResult {
        log.info("Executing build task: ${task.name}")

        // 标记任务为运行状态
        runningTasks.add(task)

        // 触发任务开始事件
        notifyTaskStarted(task)

        try {
            // 获取任务执行器
            val executor = findExecutor(task)
            if (executor == null) {
                log.warn("No executor found for task: ${task.name}")
                throw IllegalStateException("No executor found for task: ${task.name}")
            }

            log.info("Using executor: ${executor.executorName} for task: ${task.name}")

            // 执行任务
            val result = executor.execute(task, context)

            // 触发任务完成事件
            notifyTaskFinished(task, result)

            return result
        } catch (e: Exception) {
            log.error("Failed to execute task: ${task.name}", e)
            throw e
        } finally {
            // 移除运行状态标记
            runningTasks.remove(task)
        }
    }

    override fun executeTasks(tasks: List<CjBuildTask>, context: CjBuildContext): List<CjBuildResult> {
        log.info("Executing ${tasks.size} build tasks")
        return tasks.map { executeTask(it, context) }
    }

    override fun cancelTask(task: CjBuildTask) {
        if (!runningTasks.contains(task)) {
            log.warn("Task is not running: ${task.name}")
            return
        }

        log.info("Cancelling task: ${task.name}")

        // 获取执行器并取消任务
        val executor = findExecutor(task)
        executor?.cancel(task)

        // 移除运行状态标记
        runningTasks.remove(task)

        // 触发任务取消事件
        notifyTaskCancelled(task)
    }

    override fun cancelAllTasks() {
        log.info("Cancelling all running tasks")
        runningTasks.forEach { cancelTask(it) }
    }

    override fun getRunningTasks(): List<CjBuildTask> {
        return runningTasks.toList()
    }

    override fun isTaskRunning(task: CjBuildTask): Boolean {
        return runningTasks.contains(task)
    }

    override fun addBuildListener(listener: CjBuildListener) {
        buildListeners.add(listener)
    }

    override fun removeBuildListener(listener: CjBuildListener) {
        buildListeners.remove(listener)
    }

    /**
     * 查找任务执行器
     */
    private fun findExecutor(task: CjBuildTask): CjBuildTaskExecutor? {
        val buildSystemService = CjProjectBuildSystemService.getInstance()
        val buildSystemId = buildSystemService.getBuildSystemId()

        return CjBuildTaskExecutor.EP_NAME.extensionList
            .filter { executor ->
                buildSystemId == null || executor.getBuildSystemId().id == buildSystemId
            }

            .firstOrNull { it.canExecute(task) }
    }

    /**
     * 通知任务开始
     */
    private fun notifyTaskStarted(task: CjBuildTask) {
        val event = CjBuildEvent.TaskStarted(task, project)
        buildListeners.forEach { it.onBuildEvent(event) }
    }

    /**
     * 通知任务完成
     */
    private fun notifyTaskFinished(task: CjBuildTask, result: CjBuildResult) {
        val event = CjBuildEvent.TaskFinished(task, result, project)
        buildListeners.forEach { it.onBuildEvent(event) }
    }

    /**
     * 通知任务取消
     */
    private fun notifyTaskCancelled(task: CjBuildTask) {
        val event = CjBuildEvent.TaskCancelled(task, project)
        buildListeners.forEach { it.onBuildEvent(event) }
    }
}