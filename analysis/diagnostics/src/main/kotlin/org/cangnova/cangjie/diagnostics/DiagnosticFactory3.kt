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
 * 带 3 个参数的诊断工厂
 *
 * 用于创建需要 3 个参数的诊断信息。
 * 继承自 [DiagnosticFactoryWithPsiElement]，专门生成 [DiagnosticWithParameters3] 实例。
 *
 * 参数用途：
 * - 在诊断消息中插值，生成包含详细上下文的动态消息
 * - 提供更丰富的错误描述，适用于复杂的语义错误
 * - 支持需要多个对比信息的场景
 *
 * 使用示例：
 * ```kotlin
 * val CANNOT_CONVERT = DiagnosticFactory3.create<PsiElement, CjType, CjType, String>(
 *     Severity.ERROR,
 *     PositioningStrategies.DEFAULT
 * )
 *
 * // 在分析器中使用
 * val diagnostic = CANNOT_CONVERT.on(element, sourceType, targetType, reason)
 * // 渲染结果: "Cannot convert Int to String: implicit conversion not allowed"
 * diagnosticSink.report(diagnostic)
 * ```
 *
 * 典型应用场景：
 * - "Cannot convert {0} to {1}: {2}" - 类型转换错误及原因
 * - "Function {0} expects {1} arguments, got {2}" - 参数数量不匹配
 * - "Incompatible types: {0}, {1} and {2}" - 多类型不兼容
 * - "Overload resolution ambiguity: {0}, {1}, {2}" - 重载解析歧义
 *
 * 与其他工厂的对比：
 * - DiagnosticFactory1：1 个参数，简单引用错误
 * - DiagnosticFactory2：2 个参数，对比场景
 * - DiagnosticFactory3：3 个参数，复杂语义错误
 * - DiagnosticFactory4：4 个参数，非常详细的错误
 *
 * @param E PSI 元素类型
 * @param A 第一个参数类型，必须是非空类型（Any 的子类型）
 * @param B 第二个参数类型，必须是非空类型（Any 的子类型）
 * @param C 第三个参数类型，必须是非空类型（Any 的子类型）
 * @param severity 严重性级别
 * @param positioningStrategy 定位策略
 */
class DiagnosticFactory3<E : PsiElement, A, B, C> protected constructor(
    severity: Severity,
    positioningStrategy: PositioningStrategy<E>
) :
    DiagnosticFactoryWithPsiElement<E, DiagnosticWithParameters3<E, A, B, C>>(severity, positioningStrategy) {

    /**
     * 在指定元素上创建带 3 个参数的诊断
     *
     * @param element 诊断关联的 PSI 元素
     * @param a 第一个参数，将用于消息插值
     * @param b 第二个参数，将用于消息插值
     * @param c 第三个参数，将用于消息插值
     * @return 新创建的参数化诊断实例
     */
    fun on(element: E, a: A, b: B, c: C): ParametrizedDiagnostic<E> {
        return DiagnosticWithParameters3(element, a, b, c, this, severity)
    }

    companion object {
        /**
         * 创建使用默认定位策略的诊断工厂
         *
         * @param T PSI 元素类型
         * @param A 第一个参数类型
         * @param B 第二个参数类型
         * @param C 第三个参数类型
         * @param severity 严重性级别
         * @return 新的诊断工厂实例
         */

        fun <T : PsiElement, A, B, C> create(severity: Severity): DiagnosticFactory3<T, A, B, C> {
            return create(severity, PositioningStrategies.DEFAULT)
        }

        /**
         * 创建使用自定义定位策略的诊断工厂
         *
         * @param T PSI 元素类型
         * @param A 第一个参数类型
         * @param B 第二个参数类型
         * @param C 第三个参数类型
         * @param severity 严重性级别
         * @param positioningStrategy 定位策略
         * @return 新的诊断工厂实例
         */

        fun <T : PsiElement, A, B, C> create(
            severity: Severity,
            positioningStrategy: PositioningStrategy<T>
        ): DiagnosticFactory3<T, A, B, C> {
            return DiagnosticFactory3(severity, positioningStrategy)
        }
    }
}
