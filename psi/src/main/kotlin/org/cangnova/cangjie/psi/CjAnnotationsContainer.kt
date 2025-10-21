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

package org.cangnova.cangjie.psi

/**
 * 注解容器标记接口
 *
 * 该接口是一个标记接口(marker interface),用于标识可以包含注解的 PSI 元素。
 * 它为从 PSI 树或 Stub 中收集注解条目提供了统一的处理机制。
 *
 * ## 设计目的
 *
 * 这是一个内部技术接口,主要用于:
 * 1. **类型标记**: 标识哪些元素可以包含注解
 * 2. **扩展函数支持**: 为收集注解的扩展函数提供统一的接收者类型
 * 3. **抽象层**: 提供注解收集的抽象,隐藏 Stub 和 PSI 的实现细节
 *
 * ## 实现者
 *
 * 主要由以下类型实现:
 * - **[CjAnnotated]**: 所有可被注解修饰的元素接口
 *   - 通过该接口,所有声明和类型引用都自动成为注解容器
 * - **[CjModifierList]**: 修饰符列表(间接实现,作为注解的直接容器)
 * - **[CjAnnotations]**: 注解容器本身(可能间接实现)
 *
 * ## 相关扩展函数
 *
 * 该接口主要配合以下扩展函数使用(定义在 `CjPsiUtil.kt` 中):
 *
 * ### collectAnnotationEntriesFromStubOrPsi()
 * ```kotlin
 * fun CjAnnotationsContainer.collectAnnotationEntriesFromStubOrPsi(): List<CjAnnotation>
 * ```
 * 智能地从 Stub 或 PSI 中收集注解条目:
 * - 如果有 Stub,从 Stub 中读取(高效)
 * - 否则从 PSI 树中遍历收集
 *
 * ### collectAnnotationEntriesFromPsi()
 * ```kotlin
 * private fun CjAnnotationsContainer.collectAnnotationEntriesFromPsi(): List<CjAnnotation>
 * ```
 * 从 PSI 树的子节点中收集所有 [CjAnnotation]
 *
 * ## 使用场景
 *
 * ### 场景 1: 在 CjModifierList 中收集注解
 * ```kotlin
 * class CjModifierList : CjElementImplStub<...> {
 *     val annotationEntries: List<CjAnnotation>
 *         get() = this.collectAnnotationEntriesFromStubOrPsi()
 * }
 * ```
 *
 * ### 场景 2: 在 CjAnnotated 实现中使用
 * ```kotlin
 * interface CjAnnotated : CjElement, CjAnnotationsContainer {
 *     val annotationEntries: List<CjAnnotation>
 *         get() = modifierList?.annotationEntries ?: emptyList()
 * }
 * ```
 *
 * ## 为什么需要这个接口?
 *
 * 1. **统一处理**: 提供统一的方式收集注解,无论元素类型
 * 2. **性能优化**: 支持从 Stub 快速读取,避免构建完整 PSI 树
 * 3. **代码复用**: 扩展函数可以作用于所有实现者,减少重复代码
 * 4. **类型安全**: 编译时确保只有合适的元素可以收集注解
 *
 * ## 与其他注解相关接口的关系
 *
 * ```
 * CjAnnotationsContainer (技术标记接口)
 *   ↑
 *   └─ CjAnnotated (业务接口,可被注解修饰的元素)
 *       └─ 提供 annotations 和 annotationEntries 属性
 * ```
 *
 * - **CjAnnotationsContainer**: 底层技术接口,支持注解收集机制
 * - **CjAnnotated**: 高层业务接口,表示"可被注解修饰"的语义
 *
 * ## 实现注意事项
 *
 * 1. **不要直接实现**: 通常通过实现 [CjAnnotated] 间接获得此接口
 * 2. **扩展函数**: 依赖于 `collectAnnotationEntriesFromStubOrPsi()` 扩展函数
 * 3. **Stub 支持**: 实现者需要正确处理 Stub 情况以获得性能优势
 *
 * ## 示例
 *
 * ### 正确的使用方式
 * ```kotlin
 * // 实现 CjAnnotated 自动获得 CjAnnotationsContainer
 * interface CjModifierListOwner : CjAnnotated {
 *     val modifierList: CjModifierList?
 *
 *     override val annotationEntries: List<CjAnnotation>
 *         get() = modifierList?.annotationEntries ?: emptyList()
 * }
 * ```
 *
 * ### 内部实现示例
 * ```kotlin
 * // 在 CjModifierList 中使用
 * val annotationEntries: List<CjAnnotation>
 *     get() = this.collectAnnotationEntriesFromStubOrPsi()
 * ```
 *
 * @see CjAnnotated 可被注解修饰的元素接口(业务层)
 * @see CjAnnotations 注解容器
 * @see CjAnnotation 注解条目
 * @see CjModifierList 修饰符列表
 */
interface CjAnnotationsContainer : CjElement
