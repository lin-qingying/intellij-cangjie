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

package org.cangnova.cangjie.diagnostics

import com.intellij.openapi.util.TextRange

/**
 * 未绑定诊断信息接口
 *
 * 表示尚未绑定到具体 PSI 元素的诊断信息基础接口。
 * 这是诊断系统的核心抽象，包含了诊断的基本属性：
 * - 诊断工厂：用于创建和识别诊断类型
 * - 严重性级别：错误、警告、信息等
 * - 文本范围：诊断在文件中的位置
 * - 有效性标志：诊断是否仍然有效
 *
 * 设计理念：
 * - 将诊断信息与具体 PSI 元素解耦，允许诊断在元素失效后仍可查询
 * - 支持多个文本范围，适用于需要高亮多处位置的复杂诊断
 * - 通过 factory 属性支持诊断的类型识别和消息渲染
 */
interface UnboundDiagnostic {
    /** 诊断工厂，用于创建和识别此诊断类型 */
    val factory: DiagnosticFactory<*>

    /** 严重性级别（错误、警告、信息等） */
    val severity: Severity

    /** 诊断关联的文本范围列表，支持高亮多处位置 */
    val textRanges: List<TextRange>

    /** 诊断是否仍然有效（PSI 元素未失效） */
    val isValid: Boolean
}