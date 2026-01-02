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

package org.cangnova.cangjie.diagnostics.rendering

import org.cangnova.cangjie.diagnostics.DiagnosticFactory
import org.jetbrains.annotations.Nls
import org.jetbrains.annotations.NonNls

/**
 * 诊断消息 Bundle 接口
 *
 * 抽象不同渲染场景使用的消息资源包，支持多 Bundle 架构。
 *
 * ## 使用场景
 *
 * - **默认 Bundle** ([CangJieDiagnosisBundle])：命令行、构建日志等场景
 * - **IDE Bundle** ([IDECangJieDiagnosisBundle])：IDE 悬浮提示、快速修复等场景
 * - **自定义 Bundle**：特定模块或插件的诊断消息
 *
 * ## 实现示例
 *
 * ```kotlin
 * object MyCustomBundle : MessageBundle {
 *     private const val BUNDLE = "messages.MyCustomBundle"
 *     private val bundle = ResourceBundle.getBundle(BUNDLE)
 *
 *     override fun getMessage(key: String): String =
 *         bundle.getString(key)
 *
 *     override fun getMessage(factory: DiagnosticFactory<*>): String =
 *         getMessage(factory.name)
 * }
 * ```
 */
interface MessageBundle {
    /**
     * 获取原始消息模板（不带参数）
     *
     * @param key 消息键
     * @return 消息模板，包含占位符如 {0}, {1} 等
     */
    @Nls
    fun getMessage(@NonNls key: String): String
    /**
     * 获取原始消息模板（通过工厂）
     *
     * @param factory 诊断工厂，使用其名称作为键
     * @return 消息模板，包含占位符如 {0}, {1} 等
     */
    @Nls
    fun getMessage(@NonNls factory: DiagnosticFactory<*>): String = getMessage(factory.name)
}
