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

import com.intellij.openapi.extensions.ExtensionPointName

/**
 * 诊断渲染器提供者
 *
 * 实现此接口以注册诊断渲染器配置。
 * 所有实现类会通过扩展点自动加载并调用其 [register] 方法。
 *
 * ## 可扩展架构
 *
 * 通过 [DiagnosticRendererRegistry.configure] 可以注册任意类型的渲染器：
 * - **DEFAULT**：默认渲染器（纯文本格式）
 * - **IDE**：IDE渲染器（HTML富文本格式）
 * - **自定义类型**：可以注册任意类型的渲染器
 *
 * 扩展渲染器找不到时会自动回退到默认渲染器。
 *
 * ## 使用示例
 *
 * ```kotlin
 * class ErrorRenderers : DiagnosticRendererProvider {
 *     override fun register() {
 *         // 注册默认渲染器
 *         DiagnosticRendererRegistry.configureDefault {
 *             register(INVALID_BINARY_OPERATOR) {
 *                 message { CangJieDiagnosisBundle.rawMessage(it) }
 *                 parameterExtractor { diagnostic ->
 *                     arrayOf(diagnostic.operatorString, diagnostic.leftType, diagnostic.rightType)
 *                 }
 *             }
 *         }
 *
 *         // 注册IDE渲染器
 *         DiagnosticRendererRegistry.configureIde {
 *             register(INVALID_BINARY_OPERATOR) {
 *                 message { IDECangJieDiagnosisBundle.rawMessage(it) }
 *                 parameterExtractor { diagnostic ->
 *                     arrayOf(diagnostic.operatorString, diagnostic.leftType, diagnostic.rightType)
 *                 }
 *             }
 *         }
 *
 *         // 注册自定义类型渲染器（可选）
 *         DiagnosticRendererRegistry.configure("web", MyWebBundle) {
 *             register(SOME_DIAGNOSTIC) { ... }
 *         }
 *     }
 * }
 * ```
 *
 * 然后在 plugin.xml 中注册：
 * ```xml
 * <extensions defaultExtensionNs="org.cangnova.cangjie">
 *     <diagnosticRendererProvider implementation="...ErrorRenderers"/>
 * </extensions>
 * ```
 */
interface DiagnosticRendererProvider {
    /**
     * 注册诊断渲染器
     *
     * 在此方法中调用 [DiagnosticRendererRegistry.configureDefault]、
     * [DiagnosticRendererRegistry.configureIde] 或
     * [DiagnosticRendererRegistry.configure] 来配置渲染器。
     *
     * 如果没有需要特殊处理的诊断，可以保持空实现。
     */
    fun register()

    companion object {
        /**
         * 扩展点名称
         */
        val EP_NAME = ExtensionPointName<DiagnosticRendererProvider>(
            "org.cangnova.cangjie.diagnosticRendererProvider"
        )
    }
}
