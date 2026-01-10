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

/**
 * 诊断集合的完整实现
 *
 * 继承自 [org.cangnova.cangjie.diagnostics.SimpleGenericDiagnostics]，实现了 [Diagnostics] 接口，
 * 提供了按 PSI 元素快速查找诊断的能力。
 *
 * 核心特性：
 * - **元素索引缓存**：使用 DiagnosticsElementsCache 加速按元素查找
 * - **不可变集合**：通过防御性复制确保线程安全
 * - **无抑制语义**：noSuppression() 返回自身，表示不支持抑制过滤
 *
 * 性能优化：
 * - 首次调用 forElement() 时构建元素到诊断的映射缓存
 * - 后续查找复杂度为 O(1)，适合频繁按元素查询的场景
 *
 * 使用场景：
 * - 编译/分析完成后，存储所有诊断结果
 * - IDE 需要根据光标位置快速查找相关诊断
 * - 批量处理和展示诊断信息
 *
 * @param diagnostics 诊断集合，将被复制并建立索引
 */
class SimpleDiagnostics(diagnostics: Collection<Diagnostic>) : SimpleGenericDiagnostics<Diagnostic>(diagnostics),
    Diagnostics {
    // 复制集合以防止外部修改
    private val diagnostics = ArrayList(diagnostics)

    /** 按 PSI 元素索引的诊断缓存，提升查找性能 */
    @Suppress("UNCHECKED_CAST")
    private val elementsCache = DiagnosticsElementsCache(this) { true }

    /** 返回所有诊断信息 */
    override fun all() = diagnostics

    /**
     * 获取指定 PSI 元素的所有诊断
     *
     * 通过缓存索引实现高效查找，首次调用时构建映射。
     *
     * @param psiElement 要查找诊断的 PSI 元素
     * @return 该元素关联的诊断集合（可修改，但不影响内部存储）
     */
    override fun forElement(psiElement: PsiElement): MutableCollection<Diagnostic> =
        elementsCache.getDiagnostics(psiElement)

    /**
     * 返回无抑制的诊断集合
     *
     * 当前实现不支持诊断抑制，因此直接返回自身。
     *
     * @return 自身（无抑制过滤）
     */
    override fun noSuppression() = this
}
