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

import java.nio.file.Path
import java.time.Duration

/**
 * 构建状态
 */
enum class CjBuildStatus {
    /**
     * 成功
     */
    SUCCESS,

    /**
     * 失败
     */
    FAILURE,

    /**
     * 已取消
     */
    CANCELLED,

    /**
     * 部分成功
     */
    PARTIAL_SUCCESS
}

/**
 * 构建消息级别
 */
enum class CjBuildMessageLevel {
    /**
     * 信息
     */
    INFO,

    /**
     * 警告
     */
    WARNING,

    /**
     * 错误
     */
    ERROR
}

/**
 * 构建消息
 */
data class CjBuildMessage(
    /**
     * 消息级别
     */
    val level: CjBuildMessageLevel,

    /**
     * 消息内容
     */
    val message: String,

    /**
     * 源文件
     */
    val sourceFile: Path? = null,

    /**
     * 行号
     */
    val line: Int? = null,

    /**
     * 列号
     */
    val column: Int? = null
)

/**
 * 构建结果接口
 */
interface CjBuildResult {
    /**
     * 构建状态
     */
    val status: CjBuildStatus

    /**
     * 构建任务
     */
    val task: CjBuildTask

    /**
     * 构建消息列表
     */
    val messages: List<CjBuildMessage>

    /**
     * 构建耗时
     */
    val duration: Duration

    /**
     * 输出文件列表
     */
    val outputFiles: List<Path>

    /**
     * 是否成功
     */
    val isSuccess: Boolean
        get() = status == CjBuildStatus.SUCCESS

    /**
     * 是否失败
     */
    val isFailure: Boolean
        get() = status == CjBuildStatus.FAILURE

    /**
     * 是否被取消
     */
    val isCancelled: Boolean
        get() = status == CjBuildStatus.CANCELLED

    /**
     * 获取错误消息
     */
    fun getErrors(): List<CjBuildMessage> =
        messages.filter { it.level == CjBuildMessageLevel.ERROR }

    /**
     * 获取警告消息
     */
    fun getWarnings(): List<CjBuildMessage> =
        messages.filter { it.level == CjBuildMessageLevel.WARNING }
}