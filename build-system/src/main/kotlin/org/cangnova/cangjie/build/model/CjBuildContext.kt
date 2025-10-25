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

package org.cangnova.cangjie.build.model

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import java.nio.file.Path

/**
 * 构建模式
 */
enum class CjBuildMode {
    /**
     * Debug 模式
     */
    DEBUG,

    /**
     * Release 模式
     */
    RELEASE,

    /**
     * 自定义模式
     */
    CUSTOM
}

/**
 * 构建上下文接口
 *
 * 包含构建过程中的上下文信息
 */
interface CjBuildContext {
    /**
     * 项目实例
     */
    val project: Project

    /**
     * 项目根目录
     */
    val projectDir: VirtualFile

    /**
     * 构建输出目录
     */
    val outputDir: Path

    /**
     * 构建模式
     */
    val buildMode: CjBuildMode

    /**
     * 构建参数
     */
    val parameters: Map<String, Any>

    /**
     * 环境变量
     */
    val environmentVariables: Map<String, String>

    /**
     * 是否启用并行构建
     */
    val parallelBuild: Boolean
        get() = false

    /**
     * 最大并行任务数
     */
    val maxParallelTasks: Int
        get() = Runtime.getRuntime().availableProcessors()

    /**
     * 是否启用增量构建
     */
    val incrementalBuild: Boolean
        get() = true

    /**
     * 获取参数值
     */
    fun getParameter(key: String): Any?

    /**
     * 获取环境变量值
     */
    fun getEnvironmentVariable(key: String): String?
}