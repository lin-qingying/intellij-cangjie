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

import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement

/**
 * 带 PSI 元素的诊断工厂抽象基类
 *
 * 所有具体诊断工厂的基类，提供与 PSI 元素相关的通用功能。
 * 这是诊断工厂层次结构的核心抽象类。
 *
 * 核心职责：
 * - **定位策略管理**：持有 PositioningStrategy，决定如何计算高亮范围
 * - **文本范围计算**：通过定位策略计算诊断的文本范围
 * - **有效性检查**：检查诊断关联的 PSI 元素是否仍然有效
 * - **类型转换**：提供安全的诊断类型转换
 *
 * 设计模式：
 * - **策略模式**：通过 PositioningStrategy 支持不同的定位策略
 * - **模板方法**：定义诊断工厂的通用结构，子类实现具体参数处理
 *
 * 子类实现：
 * - [DiagnosticFactory0]：无参数诊断工厂
 * - [DiagnosticFactory1]：带 1 个参数的诊断工厂
 * - [DiagnosticFactory2]：带 2 个参数的诊断工厂
 * - [DiagnosticFactory3]：带 3 个参数的诊断工厂
 * - [DiagnosticFactory4]：带 4 个参数的诊断工厂
 *
 * 为什么需要定位策略：
 * - 不同的诊断需要高亮不同的范围
 * - 例如：函数声明错误可能只高亮函数名，而非整个函数体
 * - 支持高亮多个不连续的范围（如参数列表和返回类型）
 *
 * @param E PSI 元素类型
 * @param D 诊断类型
 * @param severity 严重性级别
 * @param positioningStrategy 定位策略，决定如何计算高亮范围
 */
abstract class DiagnosticFactoryWithPsiElement<E : PsiElement, D : Diagnostic>(
    severity: Severity,
    val positioningStrategy: PositioningStrategy<E>
) :
    DiagnosticFactory<D>(severity) {

    /**
     * 获取诊断的文本范围列表
     *
     * 委托给定位策略计算，支持返回多个不连续的范围。
     *
     * 实现细节：
     * - 调用 positioningStrategy.markDiagnostic()
     * - 返回的范围列表将用于 IDE 高亮显示
     *
     * @param diagnostic 参数化诊断实例
     * @return 文本范围列表，用于高亮显示
     */
    fun getTextRanges(diagnostic: ParametrizedDiagnostic<E>): List<TextRange> {
        // TODO: 奇怪的是 Java 需要在这里进行类型转换，因为 ParametrizedDiagnostic<E> 继承自 DiagnosticMarker
        return positioningStrategy.markDiagnostic(diagnostic)
    }

    /**
     * 检查诊断是否有效
     *
     * 检查关联的 PSI 元素是否仍在有效的 PSI 树中。
     * PSI 元素可能因为代码编辑、文件重新解析等原因失效。
     *
     * @param diagnostic 参数化诊断实例
     * @return 如果诊断仍然有效则返回 true
     */
    fun isValid(diagnostic: ParametrizedDiagnostic<E>): Boolean {
        return positioningStrategy.isValid(diagnostic.psiElement)
    }

    /**
     * 类型转换
     *
     * 将通用的 Diagnostic 转换为具体的诊断类型 D。
     * 提供类型安全的转换，由父类 DiagnosticFactory 实现。
     *
     * @param d 要转换的诊断
     * @return 转换后的诊断实例
     */
    fun cast(d: Diagnostic): D {
        return super.cast(d)
    }
}
