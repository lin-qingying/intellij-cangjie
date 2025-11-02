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

package org.cangnova.cangjie.model

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile

/**
 * 构建系统接口
 *
 * 表示一个构建系统（如 CJPM、Gradle 等）
 */
interface CjBuildSystem {
    /**
     * 构建系统名称
     */
    val name: String

    /**
     * 构建系统版本
     */
    val version: String?

    /**
     * 项目根目录
     */
    val projectDir: VirtualFile

    /**
     * IntelliJ 项目实例
     */
    val project: Project

    /**
     * 构建配置
     */
    val configuration: CjBuildConfiguration

    /**
     * 执行构建任务
     *
     * @param task 构建任务
     * @param context 构建上下文
     * @return 构建结果
     */
    fun execute(task: CjBuildTask, context: CjBuildContext): CjBuildResult

    /**
     * 获取所有可用的构建任务
     */
    fun getAvailableTasks(): List<CjBuildTask>

    /**
     * 验证构建系统配置是否正确
     */
    fun validate(): Boolean

    /**
     * 刷新构建系统状态
     */
    fun refresh()
}