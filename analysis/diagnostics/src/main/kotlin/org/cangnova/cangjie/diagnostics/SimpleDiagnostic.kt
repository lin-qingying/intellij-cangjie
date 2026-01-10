/*
 * Copyright 2026 LinQingYing. and contributors.
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

import com.intellij.psi.PsiElement

/**
 * 无参数诊断的简单实现
 *
 * 用于表示不需要额外参数的诊断信息（如简单的错误或警告）。
 * 由 [DiagnosticFactory0] 创建，适用于固定消息的诊断场景。
 *
 * 与参数化诊断的区别：
 * - SimpleDiagnostic：固定消息，如 "Syntax error"
 * - DiagnosticWithParameters1：带 1 个参数，如 "Unresolved reference: {0}"
 * - DiagnosticWithParameters2：带 2 个参数，如 "Type mismatch: expected {0}, found {1}"
 *
 * 使用示例：
 * ```kotlin
 * val SYNTAX_ERROR = DiagnosticFactory0.create<PsiElement>(Severity.ERROR)
 * val diagnostic = SYNTAX_ERROR.on(element)  // 返回 SimpleDiagnostic
 * ```
 *
 * 设计优势：
 * - 内存效率：不需要存储参数对象
 * - 类型安全：泛型 E 确保元素类型匹配
 * - 简洁性：适合大量简单诊断的场景
 *
 * @param E PSI 元素类型，指定诊断关联的元素类型
 * @param psiElement 诊断关联的 PSI 元素
 * @param factory 无参数诊断工厂
 * @param severity 严重性级别
 */
class SimpleDiagnostic<E : PsiElement>(
    psiElement: E,
    factory: DiagnosticFactory0<E>,
    severity: Severity
) : AbstractDiagnostic<E>(psiElement, factory, severity) {
    /**
     * 诊断工厂
     *
     * 覆写以提供更具体的类型（DiagnosticFactory0 而非 DiagnosticFactory）
     */
    override val factory: DiagnosticFactory0<E>
        get() = super.factory as DiagnosticFactory0<E>
}
