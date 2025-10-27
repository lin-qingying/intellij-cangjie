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

package org.cangnova.cangjie.cjpm.build

import org.cangnova.cangjie.build.extension.CjBuildTaskExecutor
import org.cangnova.cangjie.build.model.CjBuildContext
import org.cangnova.cangjie.build.model.CjBuildResult
import org.cangnova.cangjie.build.model.CjBuildTask

/**
 * CJPM 构建任务执行器
 */
class CjpmBuildTaskExecutor : CjBuildTaskExecutor {

    override val executorName: String = "CJPM Task Executor"

    override val priority: Int = 100

    override fun canExecute(task: CjBuildTask): Boolean {
        // CJPM 执行器可以执行所有 CJPM 任务
        return task is CjpmBuildTaskImpl
    }

    override fun execute(task: CjBuildTask, context: CjBuildContext): CjBuildResult {
        // TODO: 实现任务执行逻辑
        // 1. 构建命令行
        // 2. 执行 cjpm 命令
        // 3. 解析输出
        // 4. 返回结果
        throw NotImplementedError("Task execution not implemented yet")
    }

    override fun cancel(task: CjBuildTask) {
        // TODO: 取消任务执行
    }
}