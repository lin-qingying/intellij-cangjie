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

import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import java.util.*

/**
 * 参数化诊断的抽象实现
 * 
 * 提供与PSI元素关联的诊断基本实现，包括文本范围和有效性检查
 *
 * @param E PSI元素类型
 * @param psiElement 与诊断关联的PSI元素
 * @param factory 创建此诊断的工厂
 * @param severity 诊断严重程度
 */
abstract class AbstractDiagnostic<E : PsiElement>(
    override val psiElement: E,
    override val factory: DiagnosticFactoryWithPsiElement<E, *>,
    override val severity: Severity
) :
    ParametrizedDiagnostic<E> {

    /**
     * 获取包含诊断的PSI文件
     * 
     * @return 包含诊断的PSI文件
     */
    override val psiFile: PsiFile
        get() = psiElement.containingFile

    /**
     * 获取诊断相关的文本范围列表
     * 
     * @return 文本范围列表
     */
    override val textRanges: List<TextRange>
        get() = factory.getTextRanges(this)

    /**
     * 检查诊断是否有效
     * 
     * @return 如果诊断有效则返回true，否则返回false
     */
    override val isValid: Boolean
        get() {
            return factory.isValid(this)
        }

    /**
     * 比较两个诊断是否相等
     * 
     * 如果PSI元素、工厂和严重程度都相等，则认为诊断相等
     *
     * @param other 要比较的对象
     * @return 如果相等则返回true，否则返回false
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false
        val that = other as AbstractDiagnostic<*>
        return psiElement == that.psiElement && factory == that.factory && severity == that.severity
    }

    /**
     * 计算诊断的哈希码
     * 
     * 基于PSI元素、工厂和严重程度计算
     *
     * @return 诊断的哈希码
     */
    override fun hashCode(): Int {
        return Objects.hash(psiElement, factory, severity)
    }
}
