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
 * 带抑制功能的诊断集合
 *
 * 实现了诊断抑制机制，可以根据抑制缓存过滤诊断信息
 *
 * @param suppressCache 抑制缓存，用于确定哪些诊断应被抑制
 * @param diagnostics 原始诊断集合
 */
class DiagnosticsWithSuppression(
    val suppressCache: CangJieSuppressCache, val diagnostics: Collection<Diagnostic>

) : Diagnostics {
    /**
     * 元素缓存
     *
     * 用于缓存与PSI元素关联的已过滤诊断，提高查询性能
     */
    val elementsCache = DiagnosticsElementsCache(this, suppressCache.filter)

    /**
     * 获取所有未被抑制的诊断
     *
     * @return 未被抑制的诊断集合
     */
    override fun all(): Collection<Diagnostic> {
        return diagnostics.filter(suppressCache.filter)
    }

    /**
     * 获取与指定PSI元素关联的未被抑制的诊断
     *
     * @param psiElement PSI元素
     * @return 与元素关联的未被抑制的诊断集合
     */
    override fun forElement(psiElement: PsiElement): Collection<Diagnostic> {
        return elementsCache.getDiagnostics(psiElement)
    }

    /**
     * 获取不应用抑制机制的诊断集合
     *
     * @return 包含所有原始诊断的集合，不应用抑制规则
     */
    override fun noSuppression(): Diagnostics {
        return SimpleDiagnostics(diagnostics)
    }
}
