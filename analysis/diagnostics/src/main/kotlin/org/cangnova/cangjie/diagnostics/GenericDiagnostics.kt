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

/**
 * 通用诊断集合接口
 *
 * 这是一个泛型接口，用于管理和访问编译器诊断信息的集合。
 * 它扩展了 [Iterable] 接口，使得诊断集合可以像标准集合一样进行迭代操作。
 *
 * ## 主要特性
 * - **类型安全**：通过泛型参数 `T` 确保集合中只包含特定类型的诊断
 * - **可迭代**：支持 for-each 循环和集合操作
 * - **统一接口**：为不同类型的诊断集合提供统一的访问方式
 *
 * ## 使用场景
 * 1. **收集编译错误和警告**：在编译过程中累积所有诊断信息
 * 2. **批量处理诊断**：对所有诊断信息执行统一操作（如过滤、转换等）
 * 3. **IDE 集成**：将诊断信息传递给 IDE 显示在编辑器中
 * 4. **报告生成**：生成编译报告或错误摘要
 *
 * @param T 诊断类型，必须是 [UnboundDiagnostic] 或其子类型
 *
 * @see UnboundDiagnostic 未绑定的诊断基类
 * @see Diagnostic 已绑定的诊断接口
 *
 * @sample
 * ```kotlin
 * // 示例：遍历所有诊断信息
 * fun processDiagnostics(diagnostics: GenericDiagnostics<SomeDiagnostic>) {
 *     if (diagnostics.isEmpty()) {
 *         println("没有诊断信息")
 *         return
 *     }
 *
 *     for (diagnostic in diagnostics) {
 *         println("发现问题: ${diagnostic.factoryName}")
 *     }
 * }
 * ```
 */
interface GenericDiagnostics<T : UnboundDiagnostic> : Iterable<T> {
    /**
     * 获取所有诊断信息
     *
     * 返回集合中包含的所有诊断对象。
     * 实现类应该确保返回的集合是不可变的或安全的副本，以防止外部修改。
     *
     * @return 包含所有诊断信息的集合，不会返回 null，但可能为空集合
     */
    fun all(): Collection<T>

    /**
     * 检查诊断集合是否为空
     *
     * 这是一个便利方法，用于快速判断是否存在任何诊断信息。
     * 默认实现通过检查 [all] 返回的集合是否为空来实现。
     *
     * @return 如果集合中没有任何诊断信息返回 true，否则返回 false
     */
    fun isEmpty(): Boolean = all().isEmpty()

    /**
     * 返回诊断集合的迭代器
     *
     * 允许使用 for-each 循环遍历所有诊断信息。
     * 默认实现返回 [all] 集合的迭代器。
     *
     * @return 遍历所有诊断信息的迭代器
     *
     * @sample
     * ```kotlin
     * for (diagnostic in diagnostics) {
     *     // 处理每个诊断
     * }
     * ```
     */
    override fun iterator(): Iterator<T> = all().iterator()
}