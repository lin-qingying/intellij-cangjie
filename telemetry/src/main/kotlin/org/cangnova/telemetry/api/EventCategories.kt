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

package org.cangnova.telemetry.api

/**
 * 遥测事件类别常量
 */
object EventCategories {
    /**
     * IDE打开事件
     */
    const val IDE_OPEN = "ide_open"

    /**
     * 用户界面事件
     */
    const val UI = "ui"

    /**
     * 性能事件
     */
    const val PERFORMANCE = "performance"

    /**
     * 错误事件
     */
    const val ERROR = "error"

    /**
     * 诊断事件
     */
    const val DIAGNOSTICS = "diagnostics"

    /**
     * 功能使用事件
     */
    const val FEATURE_USAGE = "feature_usage"

    /**
     * 编译器事件
     */
    const val COMPILER = "compiler"

    /**
     * 语言服务器事件
     */
    const val LSP = "lsp"

    /**
     * 测试事件
     */
    const val TEST = "test"
} 