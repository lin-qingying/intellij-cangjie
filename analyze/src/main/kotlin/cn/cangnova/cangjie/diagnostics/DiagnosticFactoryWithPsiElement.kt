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

/**
 * 与PSI元素关联的诊断工厂抽象类
 * 
 * 扩展基本诊断工厂，提供与PSI元素相关的诊断功能，包括文本范围和有效性检查
 *
 * @param E PSI元素类型
 * @param D 诊断类型
 * @param severity 诊断严重程度
 * @param positioningStrategy 用于确定诊断位置的策略
 */
abstract class DiagnosticFactoryWithPsiElement<E : PsiElement, D : Diagnostic>(
    severity: Severity,
    val  positioningStrategy: PositioningStrategy<E>
) :
    DiagnosticFactory<D>(severity) {

    /**
     * 获取诊断相关的文本范围列表
     *
     * @param diagnostic 参数化诊断
     * @return 文本范围列表
     */
    fun getTextRanges(diagnostic: ParametrizedDiagnostic<E>): List<TextRange> {
        // TODO: it's strange that java requires cast here, because ParametrizedDiagnostic<E> inherits DiagnosticMarker
        return positioningStrategy.markDiagnostic(diagnostic)
    }

    /**
     * 检查诊断是否有效
     * 
     * 通过定位策略验证PSI元素的有效性
     *
     * @param diagnostic 参数化诊断
     * @return 如果诊断有效则返回true，否则返回false
     */
    fun isValid(diagnostic: ParametrizedDiagnostic<E>): Boolean {
        return positioningStrategy.isValid(diagnostic.psiElement)
    }

    /**
     * 将通用诊断转换为特定类型
     *
     * @param d 要转换的诊断
     * @return 转换后的特定类型诊断
     */
    fun cast(d: Diagnostic): D {
        return super.cast(d)
    }
}
