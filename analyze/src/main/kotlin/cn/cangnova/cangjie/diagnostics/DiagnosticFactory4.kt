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

/**
 * 四参数诊断工厂类
 * 
 * 用于创建需要四个额外参数的诊断信息
 *
 * @param E 目标PSI元素类型
 * @param A 第一个参数类型
 * @param B 第二个参数类型
 * @param C 第三个参数类型
 * @param D 第四个参数类型
 * @param severity 诊断严重程度
 * @param positioningStrategy 定位策略
 */
class DiagnosticFactory4<E : PsiElement, A : Any, B : Any, C : Any, D : Any> protected constructor(
    severity: Severity,
    positioningStrategy: PositioningStrategy<E>
) :
    DiagnosticFactoryWithPsiElement<E, DiagnosticWithParameters4<E, A, B, C, D>>(severity, positioningStrategy) {
    /**
     * 在指定元素上创建带四个参数的诊断
     *
     * @param element 目标元素
     * @param a 第一个参数
     * @param b 第二个参数
     * @param c 第三个参数
     * @param d 第四个参数
     * @return 创建的参数化诊断
     */
    fun on(element: E, a: A, b: B, c: C, d: D): ParametrizedDiagnostic<E> {
        return DiagnosticWithParameters4(element, a, b, c, d, this, severity)
    }

    companion object {
        /**
         * 使用默认定位策略创建诊断工厂
         *
         * @param T 目标PSI元素类型
         * @param A 第一个参数类型
         * @param B 第二个参数类型
         * @param C 第三个参数类型
         * @param D 第四个参数类型
         * @param severity 诊断严重程度
         * @return 新创建的诊断工厂
         */
        @JvmStatic
        fun <T : PsiElement, A : Any, B : Any, C : Any, D : Any> create(severity: Severity): DiagnosticFactory4<T, A, B, C, D> {
            return create(severity, PositioningStrategies.DEFAULT)
        }

        /**
         * 使用指定定位策略创建诊断工厂
         *
         * @param T 目标PSI元素类型
         * @param A 第一个参数类型
         * @param B 第二个参数类型
         * @param C 第三个参数类型
         * @param D 第四个参数类型
         * @param severity 诊断严重程度
         * @param positioningStrategy 定位策略
         * @return 新创建的诊断工厂
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
