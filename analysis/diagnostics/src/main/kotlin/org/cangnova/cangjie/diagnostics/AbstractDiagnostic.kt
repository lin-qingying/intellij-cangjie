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

import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import java.util.*

/**
 * 诊断信息的抽象基类
 *
 * 为所有具体诊断实现提供通用功能，包括：
 * - PSI 元素和文件关联
 * - 文本范围计算（委托给 factory 的定位策略）
 * - 有效性检查（PSI 元素是否仍然有效）
 * - equals/hashCode 实现
 *
 * 设计模式：
 * - **模板方法**：定义诊断的通用结构，具体子类实现参数存储
 * - **委托模式**：将定位逻辑委托给 DiagnosticFactory 的 PositioningStrategy
 *
 * 子类实现：
 * - [SimpleDiagnostic]：无参数诊断
 * - [DiagnosticWithParameters1]：带 1 个参数
 * - [DiagnosticWithParameters2]：带 2 个参数
 * - [DiagnosticWithParameters3]：带 3 个参数
 * - [DiagnosticWithParameters4]：带 4 个参数
 *
 * 为什么是抽象类而非接口：
 * - 提供 equals/hashCode 的通用实现
 * - 减少子类重复代码
 * - 统一文本范围和有效性的计算逻辑
 *
 * @param E PSI 元素类型
 * @param psiElement 诊断关联的 PSI 元素
 * @param factory 诊断工厂，包含定位策略和渲染信息
 * @param severity 严重性级别
 */
abstract class AbstractDiagnostic<E : PsiElement>(
    override val psiElement: E,
    override val factory: DiagnosticFactoryWithPsiElement<E, *>,
    override val severity: Severity
) :
    ParametrizedDiagnostic<E> {

    /** 诊断所在的文件，从 PSI 元素获取 */
    override val psiFile: PsiFile
        get() = psiElement.containingFile

    /**
     * 诊断的文本范围列表
     *
     * 委托给 factory 的 PositioningStrategy 计算，支持：
     * - 单个范围：通常是整个元素
     * - 多个范围：用于复杂诊断（如高亮函数签名和返回类型）
     */
    override val textRanges: List<TextRange>
        get() = factory.getTextRanges(this)

    /**
     * 诊断是否仍然有效
     *
     * 检查关联的 PSI 元素是否仍在有效的 PSI 树中。
     * PSI 元素可能因为代码编辑、文件重新解析等原因失效。
     */
    override val isValid: Boolean
        get() {
            return factory.isValid(this)
        }

    /**
     * 相等性比较
     *
     * 两个诊断相等当且仅当：
     * - PSI 元素相同（引用相等）
     * - 诊断工厂相同（同一类型的诊断）
     * - 严重性级别相同
     *
     * 注意：不比较参数，因为同一元素上的同一诊断应视为重复
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false
        val that = other as AbstractDiagnostic<*>
        return psiElement == that.psiElement && factory == that.factory && severity == that.severity
    }

    /**
     * 哈希码计算
     *
     * 基于 psiElement、factory 和 severity 计算，
     * 确保与 equals 一致。
     */
    override fun hashCode(): Int {
        return Objects.hash(psiElement, factory, severity)
    }
}
