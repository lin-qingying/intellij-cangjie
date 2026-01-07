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

import org.cangnova.cangjie.diagnostics.Diagnostic

/**
 * IDE 错误消息渲染器
 *
 * 为 IntelliJ IDE 环境提供富文本格式的诊断消息渲染。
 *
 * ## 渲染策略
 *
 * - **HTML 富文本**: 支持表格、列表、样式等
 * - **增强可读性**: 使用格式化布局展示复杂错误（如类型不匹配）
 * - **自动回退**: IDE 渲染器找不到时，自动回退到 [DefaultErrorMessages]
 *
 * ## 使用场景
 *
 * 1. 代码编辑器悬浮提示 (Hover Tooltips)
 * 2. 错误高亮的详细信息
 * 3. 快速修复 (Quick Fix) 的说明文本
 * 4. 意图操作 (Intention Actions) 的描述
 *
 * ## HTML 格式示例
 *
 * ### 简单错误
 * ```
 * Unresolved reference: <b>foo</b>
 * ```
 *
 * ### 类型不匹配（表格对比）
 * ```html
 * <table>
 * <tr><td>Required:</td><td><b>String</b></td></tr>
 * <tr><td>Found:</td><td><b>Int</b></td></tr>
 * </table>
 * ```
 *
 * ### 推断错误（嵌套列表）
 * ```html
 * Type inference failed:
 * <ul>
 *   <li>Cannot infer type parameter T</li>
 *   <li>Not enough information to infer parameter T</li>
 * </ul>
 * ```
 *
 * @see DefaultErrorMessages 基础文本渲染器
 * @see DiagnosticRendererRegistry 渲染器注册中心
 */
object IdeErrorMessages {

    /**
     * 渲染诊断消息为 IDE 友好的格式
     *
     * ## 渲染优先级
     *
     * 1. IDE 特定渲染器（使用 [IDECangJieDiagnosisBundle]）
     * 2. 自动回退到默认渲染器（使用 [CangJieDiagnosisBundle]）
     *
     * @param diagnostic 诊断对象
     * @return HTML 格式的错误消息，或纯文本回退消息
     */
    fun render(diagnostic: Diagnostic): String {
        // 先检查 IDECangJieDiagnosisBundle 是否有该诊断的消息
        return if (IDECangJieDiagnosisBundle.containsKey(diagnostic.factory.name)) {
            DiagnosticRendererRegistry.render(diagnostic, DiagnosticRendererRegistry.IDE)
        } else {
            // 回退到默认渲染器
            DiagnosticRendererRegistry.render(diagnostic, DiagnosticRendererRegistry.DEFAULT)
        }
    }
}
