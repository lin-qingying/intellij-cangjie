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
import java.util.*

/**
 * 带 4 个参数的诊断实现
 *
 * 用于表示需要四个参数的诊断信息。
 * 继承自 [AbstractDiagnostic]，实现 [org.cangnova.cangjie.diagnostics.DiagnosticWithParameters4Marker]。
 *
 * 参数存储：
 * - 存储四个参数 [a]、[b]、[c] 和 [d]，用于消息插值和渲染
 * - 参数必须是非空类型（Any 的子类型）
 * - 参数值参与 equals/hashCode 计算
 *
 * 典��使用场景：
 * - "Overload resolution failed: {0} at {1}:{2}, reason: {3}" - 重载解析失败详情
 *   - a: 函数名, b: 文��名, c: 行号, d: 失败原因
 * - "Type inference failed for {0}: expected {1}, got {2}, context: {3}" - 类型推断失败
 *   - a: 变量名, b: 期望类型, c: 推断类型, d: 上下文信息
 * - "Cannot assign {0} to {1} at {2}: {3}" - 赋值错误详情
 *   - a: 源表达式, b: 目标, c: 位置, d: 原因
 * - "Constraint violation: {0} requires {1} but found {2} in {3}" - 约束违反
 *   - a: 约束名, b: 要求, c: 实际值, d: 上下文
 *
 * 设计考虑：
 * - 4 个参数通常是诊断的实用上限
 * - 更多参数会降低消息可读性
 * - 如需更多信息，建议将相关参数合并为复合对象
 * - 适合需要同时提供位置信息（文件名、行号）和语义信息的场景
 *
 * 与其他参数化诊断的对比：
 * - DiagnosticWithParameters1：简单引用错误
 * - DiagnosticWithParameters2：对比场景
 * - DiagnosticWithParameters3：复杂语义错误
 * - DiagnosticWithParameters4：最详细的错误，包含完整上下文和位置信息
 *
 * @param E PSI 元素类型
 * @param A 第一个参数类型
 * @param B 第二个参数类型
 * @param C 第三个参数类型
 * @param D 第四个参数类型
 * @param psiElement 诊断关联�� PSI 元素
 * @param a 第一个参数
 * @param b 第二个参数
 * @param c 第三个参数
 * @param d 第四个参数
 * @param factory 诊断工厂
 * @param severity 严重性级别
 */
class DiagnosticWithParameters4<E : PsiElement, A : Any, B : Any, C : Any, D : Any>(
    psiElement: E,
    override val a: A,
    override val b: B,
    override val c: C,
    override val d: D,
    factory: DiagnosticFactory4<E, A, B, C, D>,
    severity: Severity
) : AbstractDiagnostic<E>(psiElement, factory, severity),
    DiagnosticWithParameters4Marker<A, B, C, D> {

    /**
     * 诊断工厂
     *
     * 覆写以提供更具体的类型（DiagnosticFactory4 而非 DiagnosticFactory）
     */
    override val factory: DiagnosticFactory4<E, A, B, C, D>
        get() = super.factory as DiagnosticFactory4<E, A, B, C, D>

    /**
     * 字符串表示
     *
     * 格式: "FactoryName(a = param1, b = param2, c = param3, d = param4)"
     * 用于调试和日志输出。
     */
    override fun toString(): String {
        return "$factory(a = $a, b = $b, c = $c, d = $d)"
    }

    /**
     * 相等性比较
     *
     * 两个诊断相等当且仅当：
     * - 基类属性相等（元素、工厂、严重性）
     * - 参数 a、b、c 和 d 相等
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false
        if (!super.equals(other)) return false
        val that = other as DiagnosticWithParameters4<*, *, *, *, *>
        return a == that.a &&
                b == that.b &&
                c == that.c &&
                d == that.d
    }

    /**
     * 哈希码计算
     *
     * 基于基类哈希码和参数 a、b、c、d 计算，确保与 equals 一致。
     */
    override fun hashCode(): Int {
        return Objects.hash(super.hashCode(), a, b, c, d)
    }
}
