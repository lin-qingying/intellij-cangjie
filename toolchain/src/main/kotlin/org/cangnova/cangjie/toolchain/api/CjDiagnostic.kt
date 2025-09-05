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
 * CangJie诊断信息
 */
interface CjDiagnostic {
    /**
     * 诊断级别
     */
    val level: Level

    /**
     * 诊断消息
     */
    val message: String

    /**
     * 源文件路径
     */
    val filePath: Path?

    /**
     * 行号
     */
    val line: Int?

    /**
     * 列号
     */
    val column: Int?

    /**
     * 诊断类型
     */
    val type: DiagnosticType

    /**
     * 诊断级别枚举
     */
    enum class Level {
        /**
         * 错误
         */
        ERROR,

        /**
         * 警告
         */
        WARNING,

        /**
         * 信息
         */
        INFO,

        /**
         * 提示
         */
        HINT
    }

    /**
     * 诊断类型枚举
     */
    enum class DiagnosticType {
        /**
         * 语法错误
         */
        SYNTAX,

        /**
         * 类型错误
         */
        TYPE,

        /**
         * 名称解析错误
         */
        NAME_RESOLUTION,

        /**
         * 编译器内部错误
         */
        INTERNAL,

        /**
         * 其他错误
         */
        OTHER
    }
}
