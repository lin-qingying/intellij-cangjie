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

package org.cangnova.cangjie.build.service

import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import org.cangnova.cangjie.build.event.CjBuildListener
import org.cangnova.cangjie.build.model.CjBuildContext
import org.cangnova.cangjie.build.model.CjBuildResult
import org.cangnova.cangjie.build.model.CjBuildTask

/**
 * 构建任务管理器服务接口
 */
@Service(Service.Level.PROJECT)
interface CjBuildTaskManager {
    companion object {
        /**
         * 获取服务实例
         */
        fun getInstance(project: Project): CjBuildTaskManager {
            return project.getService(CjBuildTaskManager::class.java)
        }
    }

    /**
     * 执行构建任务
     *
     * @param task 构建任务
     * @param context 构建上下文
     * @return 构建结果
     */
    fun executeTask(task: CjBuildTask, context: CjBuildContext): CjBuildResult

    /**
     * 批量执行构建任务
     *
     * @param tasks 构建任务列表
     * @param context 构建上下文
     * @return 构建结果列表
     */
    fun executeTasks(tasks: List<CjBuildTask>, context: CjBuildContext): List<CjBuildResult>

    /**
     * 取消正在执行的任务
     *
     * @param task 构建任务
     */
    fun cancelTask(task: CjBuildTask)

    /**
     * 取消所有正在执行的任务
     */
    fun cancelAllTasks()

    /**
     * 获取正在执行的任务列表
     */
    fun getRunningTasks(): List<CjBuildTask>

    /**
     * 检查任务是否正在执行
     *
     * @param task 构建任务
     */
    fun isTaskRunning(task: CjBuildTask): Boolean

    /**
     * 添加构建监听器
     *
     * @param listener 构建监听器
     */
    fun addBuildListener(listener: CjBuildListener)

    /**
     * 移除构建监听器
     *
     * @param listener 构建监听器
     */
    fun removeBuildListener(listener: CjBuildListener)
}