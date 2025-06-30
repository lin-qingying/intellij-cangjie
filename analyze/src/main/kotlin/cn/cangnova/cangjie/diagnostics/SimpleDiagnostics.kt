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

import cn.cangnova.cangjie.diagnostics.Diagnostic
import cn.cangnova.cangjie.diagnostics.Diagnostics
import cn.cangnova.cangjie.diagnostics.DiagnosticsElementsCache
import com.intellij.psi.PsiElement

/**
 * 简单诊断集合类
 * 
 * 实现了诊断集合接口，提供对诊断的基本管理功能
 *
 * @property diagnostics 诊断集合
 */
class SimpleDiagnostics(diagnostics: Collection<Diagnostic>) : SimpleGenericDiagnostics<Diagnostic>(diagnostics),
    Diagnostics {
    //copy to prevent external change
    private val diagnostics = ArrayList(diagnostics)

    /**
     * 元素缓存
     * 
     * 用于快速查找与特定PSI元素相关的诊断
     */
    @Suppress("UNCHECKED_CAST")
    private val elementsCache = DiagnosticsElementsCache(this) { true }

    /**
     * 获取所有诊断
     *
     * @return 所有诊断的集合
     */
    override fun all() = diagnostics

    /**
     * 获取与指定元素相关的诊断
     *
     * @param psiElement PSI元素
     * @return 与该元素相关的诊断集合
     */
    override fun forElement(psiElement: PsiElement): MutableCollection<Diagnostic> = elementsCache.getDiagnostics(psiElement)

    /**
     * 获取不带抑制的诊断集合
     * 
     * 在此实现中，直接返回自身，因为没有实现抑制功能
     *
     * @return 诊断集合
     */
    override fun noSuppression() = this
}
