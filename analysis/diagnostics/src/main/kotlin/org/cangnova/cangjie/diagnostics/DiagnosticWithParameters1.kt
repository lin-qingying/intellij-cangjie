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
 * 带 1 个参数的诊断实现
 *
 * 用于表示需要一个参数的诊断信息。
 * 继承自 [AbstractDiagnostic]，实现 [org.cangnova.cangjie.diagnostics.DiagnosticWithParameters1Marker]。
 *
 * 参数存储：
 * - 存储单个参数 [a]，用于消息插值和渲染
 * - 参数必须是非空类型（Any 的子类型）
 * - 参数值参与 equals/hashCode 计算
 *
 * 典型使用场景：
 * - "Unresolved reference: {0}" - 参数是未解析的名称
 * - "Deprecated API: {0}" - 参数是废弃的 API 名称
 * - "Variable '{0}' is never used" - 参数是未使用的变量名
 * - "Cannot find module: {0}" - 参数是模块名
 *
 * 与其他参数化诊断的对比：
 * - DiagnosticWithParameters1：1 个参数
 * - DiagnosticWithParameters2：2 个参数，如 "Type mismatch: expected {0}, found {1}"
 * - DiagnosticWithParameters3：3 个参数
 * - DiagnosticWithParameters4：4 个参数
 *
 * @param E PSI 元素类型
 * @param A 参数类型
 * @param psiElement 诊断关联的 PSI 元素
 * @param a 诊断参数
 * @param factory 诊断工厂
 * @param severity 严重性级别
 */
class DiagnosticWithParameters1<E : PsiElement, A>(
    psiElement: E,
    override val a: A,
    factory: DiagnosticFactory1<E, A>,
    severity: Severity
) : AbstractDiagnostic<E>(psiElement, factory, severity),
    DiagnosticWithParameters1Marker<A> {

    /**
     * 诊断工厂
     *
     * 覆写以提供更具体的类型（DiagnosticFactory1 而非 DiagnosticFactory）
     */
    override val factory: DiagnosticFactory1<E, A>
        get() = super.factory as DiagnosticFactory1<E, A>

    /**
     * 字符串表示
     *
     * 格式: "FactoryName(a = parameterValue)"
     * 用于调试和日志输出。
     */
    override fun toString(): String {
        return "$factory(a = $a)"
    }

    /**
     * 相等性比较
     *
     * 两个诊断相等当且仅当：
     * - 基类属性相等（元素、工厂、严重性）
     * - 参数 a 相等
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false
        if (!super.equals(other)) return false
        val that = other as DiagnosticWithParameters1<*, *>
        return a == that.a
    }

    /**
     * 哈希码计算
     *
     * 基于基类哈希码和参数 a 计算，确保与 equals 一致。
     */
    override fun hashCode(): Int {
        return Objects.hash(super.hashCode(), a)
    }
}
