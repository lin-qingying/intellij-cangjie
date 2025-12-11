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


import com.intellij.openapi.util.ModificationTracker
import com.intellij.psi.PsiElement

/**
 * 诊断集合接口
 *
 * 扩展自 [org.cangnova.cangjie.diagnostics.GenericDiagnostics]，提供完整的诊断集合管理功能。
 * 这是诊断系统的核心接口之一，用于存储和查询编译/分析过程中产生的所有诊断信息。
 *
 * 核心功能：
 * - **全量访问**：all() 获取所有诊断
 * - **元素索引**：forElement() 快速查找特定元素的诊断
 * - **抑制支持**：noSuppression() 返回未被抑制的诊断
 * - **回调机制**：支持诊断变化的通知回调
 * - **修改跟踪**：通过 ModificationTracker 追踪诊断变化
 *
 * 设计模式：
 * - **迭代器模式**：实现 Iterable，支持 for-in 循环
 * - **观察者模式**：通过 DiagnosticsCallback 通知诊断变化
 * - **空对象模式**：提供 EMPTY 单例避免空指针检查
 *
 * 实现类：
 * - [SimpleDiagnostics]：基于 ArrayList 的简单实现
 * - BindingContext.Diagnostics：绑定上下文中的诊断集合
 *
 * 使用场景：
 * - 编译器前端：收集语义分析的所有错误和警告
 * - IDE 检查：存储代码检查的结果
 * - 批量处理：一次性获取所有诊断并生成报告
 */
interface Diagnostics : GenericDiagnostics<Diagnostic> {
    /**
     * 修改追踪器
     *
     * 用于追踪诊断集合的变化，支持增量更新和缓存失效。
     * 默认实现抛出异常，具体实现应根据需要覆写。
     *
     * @throws IllegalStateException 如果未实现修改追踪
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
     * 检查是否为空
     *
     * @return 如果没有任何诊断则返回 true
     */
    override fun isEmpty(): Boolean = all().isEmpty()

    /**
     * 返回迭代器
     *
     * 支持 for-in 循环遍历所有诊断。
     *
     * @return 诊断迭代器
     */
    override fun iterator(): Iterator<Diagnostic> = all().iterator()

    /**
     * 获取指定 PSI 元素的所有诊断
     *
     * 这是一个性能关键的方法，实现类通常使用索引加速查找。
     *
     * @param psiElement 要查找的 PSI 元素
     * @return 该元素关联的所有诊断
     */
    fun forElement(psiElement: PsiElement): Collection<Diagnostic>

    /**
     * 返回未被抑制的诊断集合
     *
     * 某些诊断可能被 @Suppress 注解抑制，此方法返回过滤后的集合。
     *
     * @return 未被抑制的诊断集合
     */
    fun noSuppression(): Diagnostics

    /**
     * 设置诊断回调
     *
     * 当诊断集合变化时，会触发回调通知。
     *
     * @param callback 诊断变化的回调
     */
    fun setCallback(callback: DiagnosticSink.DiagnosticsCallback) {
        setCallbackIfNotSet(callback)
    }

    /**
     * 仅在未设置时设置回调
     *
     * @param callback 诊断变化的回调
     * @return 如果成功设置则返回 true，如果已有回调则返回 false
     */
    fun setCallbackIfNotSet(callback: DiagnosticSink.DiagnosticsCallback): Boolean = false

    /**
     * 重置回调
     *
     * 移除之前设置的回调。
     */
    fun resetCallback() {}

    companion object {
        /**
         * 空诊断集合单例
         *
         * 使用空对象模式避免空指针检查，提供一个永远为空的诊断集合。
         *
         * 特性：
         * - 不包含任何诊断
         * - 修改追踪器永不变化（NEVER_CHANGED）
         * - 对所有元素返回空列表
         */
        val EMPTY: Diagnostics = object : Diagnostics {

            override fun noSuppression(): Diagnostics = this
            override val modificationTracker: ModificationTracker = ModificationTracker.NEVER_CHANGED
            override fun all() = listOf<Diagnostic>()
            override fun forElement(psiElement: PsiElement) = listOf<Diagnostic>()
        }
    }
}
