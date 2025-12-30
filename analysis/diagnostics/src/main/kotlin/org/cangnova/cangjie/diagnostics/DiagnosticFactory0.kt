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


import com.intellij.psi.PsiElement


/**
 * 无参数诊断工厂
 *
 * 用于创建不需要额外参数的诊断信息（如简单的语法错误）。
 * 继承自 [DiagnosticFactoryWithPsiElement]，专门生成 [SimpleDiagnostic] 实例。
 *
 * 设计特点：
 * - **零参数**：诊断消息是固定的，不需要插值
 * - **类型安全**：泛型 E 确保元素类型匹配
 * - **轻量级**：不存储参数，内存效率高
 *
 * 与参数化工厂的对比：
 * - DiagnosticFactory0：固定消息，如 "Missing semicolon"
 * - DiagnosticFactory1：带 1 个参数，如 "Unresolved reference: {0}"
 * - DiagnosticFactory2：带 2 个参数，如 "Type mismatch: expected {0}, found {1}"
 *
 * 使用示例：
 * ```kotlin
 * val SYNTAX_ERROR = DiagnosticFactory0.create<PsiElement>(
 *     Severity.ERROR,
 *     PositioningStrategies.DEFAULT
 * )
 *
 * // 在分析器中使用
 * val diagnostic = SYNTAX_ERROR.on(errorElement)
 * diagnosticSink.report(diagnostic)
 * ```
 *
 * @param E PSI 元素类型
 * @param severity 严重性级别（ERROR、WARNING、INFO 等）
 * @param positioningStrategy 定位策略，决定如何计算高亮范围
 */
class DiagnosticFactory0<E : PsiElement>(
    severity: Severity,
    positioningStrategy: PositioningStrategy<E>
) :
    DiagnosticFactoryWithPsiElement<E, SimpleDiagnostic<E>>(severity, positioningStrategy) {

    /**
     * 在指定元素上创建诊断
     *
     * 生成一个绑定到给定 PSI 元素的 [SimpleDiagnostic] 实例。
     *
     * @param element 诊断关联的 PSI 元素
     * @return 新创建的诊断实例
     */
    fun on(element: E): SimpleDiagnostic<E> {
        return SimpleDiagnostic(element, this, severity)
    }

    companion object {
        /**
         * 创建使用默认定位策略的诊断工厂
         *
         * 默认策略会高亮整个 PSI 元素。
         *
         * @param T PSI 元素类型
         * @param severity 严重性级别
         * @return 新的诊断工厂实例
         */

        fun <T : PsiElement> create(severity: Severity): DiagnosticFactory0<T> {
            return create(severity, PositioningStrategies.DEFAULT)
        }

        /**
         * 创建使用自定义定位策略的诊断工厂
         *
         * 定位策略可以自定义高亮范围，例如：
         * - 只高亮标识符而非整个声明
         * - 高亮函数签名而非整个函数体
         * - 高亮多个不连续的范围
         *
         * @param T PSI 元素类型
         * @param severity 严重性级别
         * @param positioningStrategy 定位策略
         * @return 新的诊断工厂实例
         */

        fun <T : PsiElement> create(
            severity: Severity,
            positioningStrategy: PositioningStrategy<T>
        ): DiagnosticFactory0<T> {
            return DiagnosticFactory0(severity, positioningStrategy)
        }
    }
}
