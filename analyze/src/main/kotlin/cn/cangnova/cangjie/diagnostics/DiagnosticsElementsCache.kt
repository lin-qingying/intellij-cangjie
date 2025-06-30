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

import com.intellij.openapi.util.NotNullLazyValue
import com.intellij.psi.PsiElement
import com.intellij.util.containers.Interner
import com.intellij.util.containers.MultiMap

/**
 * 诊断元素缓存类
 * 
 * 用于缓存诊断与PSI元素之间的关联，提高查询性能
 *
 * @param diagnostics 诊断集合
 * @param filter 诊断过滤器
 */
class DiagnosticsElementsCache(val diagnostics: Diagnostics, val filter: (Diagnostic) -> Boolean) {

    companion object {
        /**
         * 构建元素到诊断的缓存映射
         *
         * @param diagnostics 诊断集合
         * @param filter 诊断过滤器
         * @return 元素到诊断的多值映射
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
     * 元素到诊断的懒加载映射
     * 
     * 首次访问时才会构建缓存
     */
    private val elementToDiagnostic: NotNullLazyValue<MultiMap<PsiElement, Diagnostic>> = NotNullLazyValue.atomicLazy {
        buildElementToDiagnosticCache(
            this.diagnostics,
            this.filter
        )
    }

    /**
     * 获取与指定PSI元素关联的诊断
     *
     * @param psiElement PSI元素
     * @return 与元素关联的诊断集合
     */
    fun getDiagnostics(psiElement: PsiElement): MutableCollection<Diagnostic> {
        return elementToDiagnostic.value.get(psiElement)
    }
}

/**
 * 创建字符串内部化器
 * 
 * 用于减少重复字符串的内存占用
 *
 * @return 字符串内部化器
 */
fun createStringInterner(): Interner<String> =
    Interner.createStringInterner()

/**
 * 创建并发多值映射
 * 
 * 用于线程安全地存储一对多的映射关系
 *
 * @param K 键类型
 * @param V 值类型
 * @return 并发多值映射
 */
fun <K, V> createConcurrentMultiMap(): MultiMap<K, V> =
    MultiMap.createConcurrent<K, V>()
