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
 * 带 2 个参数的诊断工厂
 *
 * 用于创建需要 2 个参数的诊断信息（如类型不匹配错误）。
 * 继承自 [DiagnosticFactoryWithPsiElement]，专门生成 [DiagnosticWithParameters2] 实例。
 *
 * 参数用途：
 * - 在诊断消息中插值，生成包含多个上下文信息的动态消息
 * - 提供更丰富的错误描述
 * - 支持对比类场景（期望值 vs 实际值）
 *
 * 使用示例：
 * ```kotlin
 * val TYPE_MISMATCH = DiagnosticFactory2.create<PsiElement, CjType, CjType>(
 *     Severity.ERROR,
 *     PositioningStrategies.DEFAULT
 * )
 *
 * // 在分析器中使用
 * val diagnostic = TYPE_MISMATCH.on(element, expectedType, actualType)
 * // 渲���结果: "Type mismatch: expected String, found Int"
 * diagnosticSink.report(diagnostic)
 * ```
 *
 * 典型应用场景：
 * - "Type mismatch: expected {0}, found {1}" - 类型不匹配
 * - "Conflicting declarations: {0} and {1}" - 声明冲突
 * - "Cannot convert {0} to {1}" - 类型转换错误
 * - "Redeclaration: {0} already defined at {1}" - 重复声明
 *
 * 与其他工厂的对比：
 * - DiagnosticFactory0：无参数，如 "Missing semicolon"
 * - DiagnosticFactory1：1 个参数，如 "Unresolved reference: {0}"
 * - DiagnosticFactory2：2 个参数，如 "Type mismatch: expected {0}, found {1}"
 * - DiagnosticFactory3：3 个参数，如 "Cannot call {0} with {1}: {2}"
 *
 * @param E PSI 元素类型
 * @param A 第一个参数类型，必须是非空类型（Any 的子类型）
 * @param B 第二个参数类型，必须是非空类型（Any 的子类型）
 * @param severity 严重性级别
 * @param positioningStrategy 定位策略
 */
class DiagnosticFactory2<E : PsiElement, A : Any, B : Any> private constructor(
    severity: Severity,
    positioningStrategy: PositioningStrategy<E>
) :
    DiagnosticFactoryWithPsiElement<E, DiagnosticWithParameters2<E, A, B>>(severity, positioningStrategy) {

    /**
     * 在指定元素上创建带 2 个参数的诊断
     *
     * @param element 诊断关联的 PSI 元素
     * @param a 第一个参数，将用于消息插值
     * @param b 第二个参数，将用于消息插值
     * @return 新创建的参数化诊断实例
     */
    fun on(element: E, a: A, b: B): ParametrizedDiagnostic<E> {
        return DiagnosticWithParameters2(element, a, b, this, severity)
    }

    companion object {
        /**
         * 创建使用自定义定位策略的诊断工厂
         *
         * @param T PSI 元素类型
         * @param A 第一个参数类型
         * @param B 第二个参数类型
         * @param severity 严重性级别
         * @param positioningStrategy 定位��略
         * @return 新的诊断工厂实例
         */
        @JvmStatic
        fun <T : PsiElement, A : Any, B : Any> create(
            severity: Severity,
            positioningStrategy: PositioningStrategy<T>
        ): DiagnosticFactory2<T, A, B> {
            return DiagnosticFactory2(severity, positioningStrategy)
        }

        /**
         * 创建使用默认定位策略的诊断工厂
         *
         * @param T PSI 元素类型
         * @param A 第一个参数类型
         * @param B 第二个参数类型
         * @param severity 严重性级别
         * @return 新的诊断工厂实例
         */
        @JvmStatic
        fun <T : PsiElement, A : Any, B : Any> create(severity: Severity): DiagnosticFactory2<T, A, B> {
            return DiagnosticFactory2(severity, PositioningStrategies.DEFAULT)
        }
    }
}
