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

package org.cangnova.cangjie.protodebugger.output

import com.intellij.openapi.util.NlsSafe

/**
 * 日志级别类型
 *
 * 表示日志消息的严重程度级别。
 */
enum class LogType(@field:NlsSafe private val typeName: String) {
    /**
     * 信息级别 - 一般运行信息
     */
    INFO("INFO"),

    /**
     * 警告级别 - 潜在问题
     */
    WARNING("WARNING"),

    /**
     * 错误级别 - 可恢复的错误
     */
    ERROR("ERROR"),

    /**
     * 致命级别 - 导致程序终止的错误
     */
    FATAL("FATAL");

    override fun toString(): String = typeName

    companion object {
        /**
         * 从字符串解析日志类型
         */
        fun fromString(name: String): LogType? {
            return values().find { it.typeName.equals(name, ignoreCase = true) }
        }
    }
}