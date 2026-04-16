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

package org.cangnova.cangjie.run

import com.intellij.task.ProjectTaskRunner.Result
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.project.Project
import com.intellij.task.ProjectTask
import com.intellij.task.ProjectTaskContext
import org.cangnova.cangjie.project.extension.ProjectBuildSystemId
import org.jetbrains.concurrency.Promise

/**
 * 仓颉项目任务运行器接口
 *
 * 为子系统特定的项目任务运行器提供扩展点。
 * 每个构建系统（CJPM 等）都应提供自己的实现。
 */
interface CangJieProjectTaskRunner {

    companion object {
        /**
         * 扩展点名称
         */
        private val EP_NAME = ExtensionPointName<CangJieProjectTaskRunner>(
            "org.cangnova.cangjie.run.projectTaskRunner"
        )

        /**
         * 为当前构建系统查找合适的任务运行器
         *
         * @return 匹配当前构建系统的任务运行器，如果未找到则返回 null
         */
        fun findRunner(): CangJieProjectTaskRunner? {
            // 获取当前构建系统
            val buildSystem = org.cangnova.cangjie.project.service.CjProjectBuildSystemService
                .getInstance()
                .getBuildSystem() ?: return null

            // 查找匹配的运行器
            return EP_NAME.extensionList.firstOrNull {
                it.getBuildSystemId().id == buildSystem.id
            }
        }
    }

    /**
     * 获取此运行器处理的构建系统 ID
     *
     * @return 构建系统标识符
     */
    fun getBuildSystemId(): ProjectBuildSystemId

    /**
     * 检查此运行器是否可以处理给定的任务
     *
     * @param projectTask 要检查的项目任务
     * @return 如果可以处理该任务则返回 true
     */
    fun canRun(projectTask: ProjectTask): Boolean

    /**
     * 将任务展开为多个子任务（如果需要）
     *
     * @param task 要展开的任务
     * @return 子任务列表，如果不需要展开则返回包含原任务的列表
     */
    fun expandTask(task: ProjectTask): List<ProjectTask>

    /**
     * 执行单个任务
     *
     * @param project 当前项目
     * @param context 项目任务上下文
     * @param task 要执行的任务
     * @return 包含执行结果的 Promise
     */
    fun executeTask(
        project: Project,
        context: ProjectTaskContext,
        task: ProjectTask
    ): Promise<Result>
}