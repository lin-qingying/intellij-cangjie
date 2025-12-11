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
 * 带 4 个参数的诊断工厂
 *
 * 用于创建需要 4 个参数的诊断信息。
 * ��承自 [DiagnosticFactoryWithPsiElement]，专门生成 [DiagnosticWithParameters4] 实例。
 *
 * 参数用途：
 * - 在诊断消息中插值，生成包含非常详细上下文的动态消息
 * - 适用于需要大量信息的复杂错误场景
 * - 支持需要多维度信息的诊断
 *
 * 使用示例：
 * ```kotlin
 * val OVERLOAD_RESOLUTION_FAILED = DiagnosticFactory4.create<PsiElement, String, String, Int, String>(
 *     Severity.ERROR,
 *     PositioningStrategies.DEFAULT
 * )
 *
 * // 在分析器中使用
 * val diagnostic = OVERLOAD_RESOLUTION_FAILED.on(
 *     element,
 *     functionName,
 *     fileName,
 *     lineNumber,
 *     reason
 * )
 * // 渲染结果: "Overload resolution failed: foo at Main.cj:42, reason: ambiguous call"
 * diagnosticSink.report(diagnostic)
 * ```
 *
 * 典型应用场景：
 * - "Overload resolution failed: {0} at {1}:{2}, reason: {3}" - 重载解析失败详情
 * - "Type inference failed for {0}: expected {1}, got {2}, context: {3}" - 类型推断失败
 * - "Cannot assign {0} to {1} at {2}: {3}" - 赋值错误详情
 * - "Constraint violation: {0} requires {1} but found {2} in {3}" - 约束违反
 *
 * 设计考虑：
 * - 4 个参数通常是诊断的上限，更多参数会影响可读性
 * - 如需更多信息，考虑将部分参数合并或使用复合对象
 * - 适用于需要定位信息（文件名、行号）的场景
 *
 * 与其他工厂的对比：
 * - DiagnosticFactory1：简单引用错误
 * - DiagnosticFactory2：对比场景
 * - DiagnosticFactory3：复杂语义错误
 * - DiagnosticFactory4：非常详细的错误，包含位置和上下文
 *
 * @param E PSI 元素类型
 * @param A 第一个参数类型，必须是非空类型（Any 的子类型）
 * @param B 第二个参数类型，必须是非空类型（Any 的子类型）
 * @param C 第三个参数类型，必须是非空类型（Any 的子类型）
 * @param D 第四个参数类型，必须是非空类型（Any 的子类型）
 * @param severity 严重性级别
 * @param positioningStrategy 定位策略
 */
class DiagnosticFactory4<E : PsiElement, A : Any, B : Any, C : Any, D : Any> protected constructor(
    severity: Severity,
    positioningStrategy: PositioningStrategy<E>
) :
    DiagnosticFactoryWithPsiElement<E, DiagnosticWithParameters4<E, A, B, C, D>>(severity, positioningStrategy) {

    /**
     * 在指定元素上创建带 4 个参数的诊断
     *
     * @param element 诊断关联的 PSI 元素
     * @param a 第一个参数，将用于消息插值
     * @param b 第二个参数，将用于消息插值
     * @param c 第三个参数，将用于消息插值
     * @param d 第四个参数，将用于消息插值
     * @return 新创建的参数化诊断实例
     */
    fun on(element: E, a: A, b: B, c: C, d: D): ParametrizedDiagnostic<E> {
        return DiagnosticWithParameters4(element, a, b, c, d, this, severity)
    }

    companion object {
        /**
         * 创建使用默认定位策略的诊断工厂
         *
         * @param T PSI 元素类型
         * @param A 第一个参数类型
         * @param B 第二个参数类型
         * @param C 第三个参数类型
         * @param D 第四个参数类型
         * @param severity 严重性级别
         * @return 新的诊断工厂实例
         */
        @JvmStatic
        fun <T : PsiElement, A : Any, B : Any, C : Any, D : Any> create(severity: Severity): DiagnosticFactory4<T, A, B, C, D> {
            return create(severity, PositioningStrategies.DEFAULT)
        }

        /**
         * 创建使用自定义定位策略的诊断工厂
         *
         * @param T PSI 元素类型
         * @param A 第一个参数类型
         * @param B 第二个参数类型
         * @param C 第三个参数类型
         * @param D 第四个参数类型
         * @param severity 严重性级别
         * @param positioningStrategy 定位策略
         * @return 新的诊断工厂实例
         */
        @JvmStatic
        fun <T : PsiElement, A : Any, B : Any, C : Any, D : Any> create(
            severity: Severity,
            positioningStrategy: PositioningStrategy<T>
        ): DiagnosticFactory4<T, A, B, C, D> {
            return DiagnosticFactory4(severity, positioningStrategy)
        }
    }
}
