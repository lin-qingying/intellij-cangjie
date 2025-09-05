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

package org.cangnova.cangjie.toolchain.api

import java.nio.file.Path

/**
 * CangJie编译结果
 */
interface CjCompileResult {
    /**
     * 编译是否成功
     */
    val success: Boolean

    /**
     * 编译输出的文件路径
     */
    val outputFiles: List<Path>

    /**
     * 编译过程中的诊断信息
     */
    val diagnostics: List<CjDiagnostic>

    /**
     * 编译器输出信息
     */
    val output: String

    /**
     * 编译结果状态
     */
    val status: CompilationStatus

    /**
     * 编译结果状态枚举
     */
    enum class CompilationStatus {
        /**
         * 成功
         */
        SUCCESS,

        /**
         * 有警告
         */
        WARNING,

        /**
         * 失败
         */
        FAILURE,

        /**
         * 取消
         */
        CANCELLED,

        /**
         * 超时
         */
        TIMEOUT
    }
}
