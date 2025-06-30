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

import com.intellij.openapi.util.ModificationTracker
import com.intellij.psi.PsiElement

/**
 * 诊断集合接口
 * 
 * 表示一组诊断信息，提供访问和管理诊断的方法
 */
interface Diagnostics: GenericDiagnostics<Diagnostic> {
    /**
     * 获取修改跟踪器
     * 
     * 用于跟踪诊断集合的修改
     * 
     * @throws IllegalStateException 如果尝试获取未实现此功能的诊断集合的修改跟踪器
     */
    val modificationTracker: ModificationTracker
        get() = throw IllegalStateException("Trying to obtain modification tracker for Diagnostics object of class ${this::class.java}")

    /**
     * 获取所有诊断
     * 
     * @return 所有诊断的集合
     */
    override fun all(): Collection<Diagnostic>

    /**
     * 检查诊断集合是否为空
     * 
     * @return 如果集合为空则返回true，否则返回false
     */
    override fun isEmpty(): Boolean = all().isEmpty()

    /**
     * 获取诊断迭代器
     * 
     * @return 诊断迭代器
     */
    override fun iterator(): Iterator<Diagnostic> = all().iterator()

    /**
     * 获取与指定PSI元素关联的诊断
     * 
     * @param psiElement PSI元素
     * @return 与元素关联的诊断集合
     */
    fun forElement(psiElement: PsiElement): Collection<Diagnostic>

    /**
     * 获取未被抑制的诊断集合
     * 
     * @return 未被抑制的诊断集合
     */
    fun noSuppression(): Diagnostics

    /**
     * 设置诊断回调
     * 
     * @param callback 诊断回调
     */
    fun setCallback(callback: DiagnosticSink.DiagnosticsCallback) {
        setCallbackIfNotSet(callback)
    }

    /**
     * 如果未设置回调，则设置诊断回调
     * 
     * @param callback 诊断回调
     * @return 如果成功设置回调则返回true，否则返回false
     */
    fun setCallbackIfNotSet(callback: DiagnosticSink.DiagnosticsCallback): Boolean = false

    /**
     * 重置诊断回调
     */
    fun resetCallback() {}

    companion object {
        /**
         * 空诊断集合
         * 
         * 不包含任何诊断的集合实现
         */
        val EMPTY: Diagnostics = object : Diagnostics {

            override fun noSuppression(): Diagnostics = this
            override val modificationTracker: ModificationTracker = ModificationTracker.NEVER_CHANGED
            override fun all() = listOf<Diagnostic>()
            override fun forElement(psiElement: PsiElement) = listOf<Diagnostic>()
        }
    }
}
