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

import com.intellij.openapi.util.NotNullLazyValue
import com.intellij.psi.PsiElement
import com.intellij.util.containers.Interner
import com.intellij.util.containers.MultiMap

/**
 * 诊断元素缓存
 *
 * 用于加速按 PSI 元素查找诊断的缓存类。
 * 通过构建 PSI 元素到诊断的映射索引，将查找复杂度从 O(n) 降低到 O(1)。
 *
 * 设计原理：
 * - **延迟初始化**：使用 NotNullLazyValue 延迟构建索引，避免不必要的计算
 * - **线程安全**：使用 atomicLazy 确保多线程环境下的安全性
 * - **过滤支持**：通过 filter 参数支持条件过滤（如只缓存非抑制的诊断）
 * - **并发友好**：使用 MultiMap.createConcurrent 支持并发访问
 *
 * 性能优化：
 * - 首次调用 getDiagnostics() 时构建完整索引
 * - 后续查找为 O(1) 复杂度
 * - 适合频繁按元素查询的场景（如 IDE 显示诊断）
 *
 * 使用场景：
 * - SimpleDiagnostics.forElement() 的底层实现
 * - IDE 光标移动时快速查找相关诊断
 * - 代码检查结果的高效访问
 *
 * @param diagnostics 诊断集合
 * @param filter 诊断过滤器，返回 true 的诊断会被缓存
 */
class DiagnosticsElementsCache(val diagnostics: Diagnostics, val filter: (Diagnostic) -> Boolean) {

    companion object {

        /**
         * 构建元素到诊断的映射缓存
         *
         * 遍历所有诊断，将通过过滤器的诊断按其 PSI 元素建立索引。
         *
         * 实现细节：
         * - 使用 MultiMap 支持一个元素对应多个诊断
         * - 使用并发安全的实现 (createConcurrentMultiMap)
         * - 只缓存通过 filter 的诊断
         *
         * @param diagnostics 诊断集合
         * @param filter 诊断过滤器
         * @return PSI 元素到诊断的多值映射
         */
        private fun buildElementToDiagnosticCache(
            diagnostics: Diagnostics,
            filter: (Diagnostic) -> Boolean
        ): MultiMap<PsiElement, Diagnostic> {
            val elementToDiagnostic: MultiMap<PsiElement, Diagnostic> =
                createConcurrentMultiMap()
            for (diagnostic in diagnostics) {
                if (filter.invoke(diagnostic)) {
                    elementToDiagnostic.putValue(diagnostic.psiElement, diagnostic)
                }
            }

            return elementToDiagnostic
        }
    }

    /**
     * 元素到诊断的映射缓存
     *
     * 使用延迟初始化，首次访问时才构建索引。
     * 使用原子操作确保线程安全。
     */
    private val elementToDiagnostic: NotNullLazyValue<MultiMap<PsiElement, Diagnostic>> = NotNullLazyValue.atomicLazy {
        buildElementToDiagnosticCache(
            this.diagnostics,
            this.filter
        )
    }

    /**
     * 获取指定 PSI 元素的所有诊断
     *
     * 首次调用会触发缓存构建，后续调用直接从缓存中获取。
     *
     * @param psiElement 要查找的 PSI 元素
     * @return 该元素关联的诊断集合（可修改，但不影响内部缓存）
     */
    fun getDiagnostics(psiElement: PsiElement): MutableCollection<Diagnostic> {
        return elementToDiagnostic.value.get(psiElement)
    }


}

/**
 * 创建字符串驻留器
 *
 * 用于字符串去重，减少内存占用。
 * 在诊断系统中可用于去重诊断消息和标识符。
 *
 * @return 字符串驻留器实例
 */
fun createStringInterner(): Interner<String> =
    Interner.createStringInterner()

/**
 * 创建并发安全的多值映射
 *
 * 用于构建一对多的映射关系，支持并发访问。
 *
 * @param K 键类型
 * @param V 值类型
 * @return 并发安全的多值映射实例
 */
fun <K, V> createConcurrentMultiMap(): MultiMap<K, V> =
    MultiMap.createConcurrent<K, V>()
