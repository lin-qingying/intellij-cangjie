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

package cn.cangnova.cangjie.toolchain.api

import java.nio.file.Path

/**
 * CangJie格式化结果
 */
interface CjFormatResult {
    /**
     * 格式化是否成功
     */
    val success: Boolean

    /**
     * 格式化的文件列表
     */
    val formattedFiles: List<Path>

    /**
     * 格式化工具输出信息
     */
    val output: String

    /**
     * 格式化状态
     */
    val status: FormatStatus

    /**
     * 格式化状态枚举
     */
    enum class FormatStatus {
        /**
         * 成功
         */
        SUCCESS,

        /**
         * 部分成功
         */
        PARTIAL_SUCCESS,

        /**
         * 失败
         */
        FAILURE,

        /**
         * 无变化
         */
        NO_CHANGE
    }
}