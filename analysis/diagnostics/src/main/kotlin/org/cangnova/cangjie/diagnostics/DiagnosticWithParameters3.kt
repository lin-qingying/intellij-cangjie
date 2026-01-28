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
import java.util.*

/**
 * 带 3 个参数的诊断实现
 *
 * 用于表示需要三个参数的诊断信息。
 * 继承自 [AbstractDiagnostic]，实现 [org.cangnova.cangjie.diagnostics.DiagnosticWithParameters3Marker]。
 *
 * 参数存储：
 * - 存储三个参数 [a]、[b] 和 [c]，用于消息插值和渲染
 * - 参数必须是非空类型（Any 的子类型）
 * - 参数值参与 equals/hashCode 计算
 *
 * 典型使用场景：
 * - "Cannot convert {0} to {1}: {2}" - 类型转换错误及原因
 *   - a: 源类型, b: 目标类型, c: 原因
 * - "Function {0} expects {1} arguments, got {2}" - 参数数量不匹配
 *   - a: 函数名, b: 期望数量, c: 实际数量
 * - "Incompatible types: {0}, {1} and {2}" - 多类型不兼容
 *   - a, b, c: 三个不兼容的类型
 * - "Overload resolution ambiguity between: {0}, {1}, {2}" - 重载解析歧义
 *   - a, b, c: 三个候选重载
 * - "Type mismatch in {0}: expected {1}, found {2}" - 上下文类型错误
 *   - a: 上下文, b: 期望类型, c: 实际类型
 *
 * 与其他参数化诊断的对比：
 * - DiagnosticWithParameters1：简单引用错误
 * - DiagnosticWithParameters2：对比场景（期望 vs 实际）
 * - DiagnosticWithParameters3：复杂语义错误，包含原因或多个实体
 * - DiagnosticWithParameters4：非常详细的错误，包含位置信息
 *
 * @param E PSI 元素类型
 * @param A 第一个参数类型
 * @param B 第二个参数类型
 * @param C 第三个参数类型
 * @param psiElement 诊断关联的 PSI 元素
 * @param a 第一个参数
 * @param b 第二个参数
 * @param c 第三个参数
 * @param factory 诊断工厂
 * @param severity 严重性级别
 */
class DiagnosticWithParameters3<E : PsiElement, A, B, C>(
    psiElement: E,
    override val a: A,
    override val b: B,
    override val c: C,
    factory: DiagnosticFactory3<E, A, B, C>,
    severity: Severity
) : AbstractDiagnostic<E>(psiElement, factory, severity),
    DiagnosticWithParameters3Marker<A, B, C> {

    /**
     * 诊断工厂
     *
     * 覆写以提供更具体的类型（DiagnosticFactory3 而非 DiagnosticFactory）
     */
    override val factory: DiagnosticFactory3<E, A, B, C>
        get() = super.factory as DiagnosticFactory3<E, A, B, C>

    /**
     * 字符串表示
     *
     * 格式: "FactoryName(a = param1, b = param2, c = param3)"
     * 用于调试和日志输出。
     */
    override fun toString(): String {
        return "$factory(a = $a, b = $b, c = $c)"
    }

    /**
     * 相等性比较
     *
     * 两个诊断相等当且仅当：
     * - 基类属性相等（元素、工厂、严重性）
     * - 参数 a、b 和 c 相等
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false
        if (!super.equals(other)) return false
        val that = other as DiagnosticWithParameters3<*, *, *, *>
        return a == that.a &&
                b == that.b &&
                c == that.c
    }

    /**
     * 哈希码计算
     *
     * 基于基类哈希码和参数 a、b、c 计算，确保与 equals 一致。
     */
    override fun hashCode(): Int {
        return Objects.hash(super.hashCode(), a, b, c)
    }
}
