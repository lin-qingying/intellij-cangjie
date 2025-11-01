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

package org.cangnova.cangjie.build.extension

import com.intellij.openapi.extensions.ExtensionPointName
import org.cangnova.cangjie.build.model.CjBuildContext
import org.cangnova.cangjie.build.model.CjBuildResult
import org.cangnova.cangjie.build.model.CjBuildTask
import org.cangnova.cangjie.project.extension.ProjectBuildSystemId

/**
 * 构建任务执行器扩展点
 *
 * 用于执行不同类型的构建任务
 */
interface CjBuildTaskExecutor {
    companion object {
        val EP_NAME = ExtensionPointName<CjBuildTaskExecutor>(
            "org.cangnova.cangjie.build.buildTaskExecutor"
        )
    }

    /**
     * 获取此解析器关联的构建系统 ID
     *
     * @return 构建系统 ID
     */
    fun getBuildSystemId(): ProjectBuildSystemId

    /**
     * 执行器名称
     */
    val executorName: String


    /**
     * 检查是否可以执行指定的任务
     *
     * @param task 构建任务
     * @return 如果可以执行返回 true
     */
    fun canExecute(task: CjBuildTask): Boolean

    /**
     * 执行构建任务
     *
     * @param task 构建任务
     * @param context 构建上下文
     * @return 构建结果
     */
    fun execute(task: CjBuildTask, context: CjBuildContext): CjBuildResult

    /**
     * 取消正在执行的任务
     *
     * @param task 构建任务
     */
    fun cancel(task: CjBuildTask)
}