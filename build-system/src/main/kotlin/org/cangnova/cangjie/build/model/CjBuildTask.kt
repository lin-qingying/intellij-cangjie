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

/**
 * 构建任务类型
 */
enum class CjBuildTaskType {
    /**
     * 编译任务
     */
    COMPILE,

    /**
     * 测试任务
     */
    TEST,

    /**
     * 打包任务
     */
    PACKAGE,

    /**
     * 清理任务
     */
    CLEAN,

    /**
     * 安装任务
     */
    INSTALL,

    /**
     * 部署任务
     */
    DEPLOY,

    /**
     * 自定义任务
     */
    CUSTOM
}

/**
 * 构建任务接口
 *
 * 表示一个构建任务
 */
interface CjBuildTask {
    /**
     * 任务名称
     */
    val name: String

    /**
     * 任务类型
     */
    val type: CjBuildTaskType

    /**
     * 任务描述
     */
    val description: String?

    /**
     * 任务依赖
     */
    val dependencies: List<CjBuildTask>

    /**
     * 任务是否启用
     */
    val enabled: Boolean
        get() = true

    /**
     * 任务参数
     */
    val parameters: Map<String, Any>
        get() = emptyMap()

    /**
     * 任务执行超时时间（毫秒）
     */
    val timeout: Long?
        get() = null
}