/*
 * Copyright 2024 LinQingYing. and contributors.
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

package cn.cangnova.cangjie.diagnostics

import com.intellij.psi.PsiElement
import java.util.*

/**
 * 带三个参数的诊断类
 * 
 * 实现了包含三个附加参数的诊断，用于提供更丰富的诊断信息
 *
 * @param E PSI元素类型
 * @param A 第一个参数类型
 * @param B 第二个参数类型
 * @param C 第三个参数类型
 * @param psiElement 与诊断关联的PSI元素
 * @param a 第一个诊断参数
 * @param b 第二个诊断参数
 * @param c 第三个诊断参数
 * @param factory 创建此诊断的工厂
 * @param severity 诊断严重程度
 */
class DiagnosticWithParameters3<E : PsiElement , A:Any, B:Any, C:Any>(
    psiElement: E,
    override val a: A,
    override val b: B,
    override val c: C,
    factory: DiagnosticFactory3<E, A, B, C>,
    severity: Severity
) : AbstractDiagnostic<E>(psiElement, factory, severity),
    DiagnosticWithParameters3Marker<A, B, C> {
    /**
     * 获取诊断工厂
     * 
     * @return 带三个参数的诊断工厂
     */
    override val factory: DiagnosticFactory3<E, A, B, C>
        get() = super.factory as DiagnosticFactory3<E, A, B, C>

    /**
     * 获取诊断的字符串表示
     * 
     * @return 诊断的字符串表示，包含三个参数信息
     */
    override fun toString(): String {
        return "$factory(a = $a, b = $b, c = $c)"
    }

    /**
     * 比较两个诊断是否相等
     * 
     * 如果PSI元素、工厂、严重程度和三个参数都相等，则认为诊断相等
     *
     * @param other 要比较的对象
     * @return 如果相等则返回true，否则返回false
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
     * 计算诊断的哈希码
     * 
     * 基于父类哈希码和三个参数计算
     *
     * @return 诊断的哈希码
     */
    override fun hashCode(): Int {
        return Objects.hash(super.hashCode(), a, b, c)
    }
}
