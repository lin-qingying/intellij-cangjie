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
 * 带 1 个参数的诊断工厂
 *
 * 用于创建需要 1 个参数的诊断信息（如引用未解析错误）。
 * 继承自 [DiagnosticFactoryWithPsiElement]，专门生成 [DiagnosticWithParameters1] 实例。
 *
 * 参数用途：
 * - 在诊断消息中插值，生成动态消息
 * - 提供额外的上下文信息用于诊断渲染
 * - 支持更精确的错误描述
 *
 * 使用示例：
 * ```kotlin
 * val UNRESOLVED_REFERENCE = DiagnosticFactory1.create<PsiElement, String>(
 *     Severity.ERROR,
 *     PositioningStrategies.DEFAULT
 * )
 *
 * // 在分析器中使用
 * val diagnostic = UNRESOLVED_REFERENCE.on(element, "unknownIdentifier")
 * // 渲染结果: "Unresolved reference: unknownIdentifier"
 * diagnosticSink.report(diagnostic)
 * ```
 *
 * 与其他工厂的对比：
 * - DiagnosticFactory0：无参数，如 "Missing semicolon"
 * - DiagnosticFactory1：1 个参数，如 "Unresolved reference: {0}"
 * - DiagnosticFactory2：2 个参数，如 "Type mismatch: expected {0}, found {1}"
 *
 * @param E PSI 元素类型
 * @param A 参数类型，必须是非空类型（Any 的子类型）
 * @param severity 严重性级别
 * @param positioningStrategy 定位策略
 */
class DiagnosticFactory1<E : PsiElement, A : Any>(
    severity: Severity,
    positioningStrategy: PositioningStrategy<E>
) :
    DiagnosticFactoryWithPsiElement<E, DiagnosticWithParameters1<E, A>>(severity, positioningStrategy) {

    /**
     * 在指定元素上创建带参数的诊断
     *
     * @param element 诊断关联的 PSI 元素
     * @param argument 诊断参数，将用于消息插值
     * @return 新创建的参数化诊断实例
     */
    fun on(element: E, argument: A): ParametrizedDiagnostic<E> {
        return DiagnosticWithParameters1(element, argument, this, severity)
    }

    companion object {
        /**
         * 创建使用自定义定位策略的诊断工厂
         *
         * @param T PSI 元素类型
         * @param A 参数类型
         * @param severity 严重性级别
         * @param positioningStrategy 定位策略
         * @return 新的诊断工厂实例
         */
        @JvmStatic
        fun <T : PsiElement, A : Any> create(
            severity: Severity,
            positioningStrategy: PositioningStrategy<T>
        ): DiagnosticFactory1<T, A> {
            return DiagnosticFactory1(severity, positioningStrategy)
        }

        /**
         * 创建使用默认定位策略的诊断工厂
         *
         * @param T PSI 元素类型
         * @param A 参数类型
         * @param severity 严重性级别
         * @return 新的诊断工厂实例
         */
        @JvmStatic
        fun <T : PsiElement, A : Any> create(severity: Severity): DiagnosticFactory1<T, A> {
            return create(severity, PositioningStrategies.DEFAULT)
        }
    }
}
