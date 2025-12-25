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
 * 可被注解修饰的元素接口
 *
 * 该接口定义了所有能够被注解修饰的 PSI 元素的通用行为。
 * 任何可以使用 `@注解名` 语法进行修饰的语言元素都应该实现此接口。
 *
 * ## 适用范围
 * 在仓颉语言中,以下元素可以被注解修饰:
 * - **声明元素** (通过 [CjDeclaration]/[CjModifierListOwner]):
 *   - 函数声明 (`func`)
 *   - 类声明 (`class`)
 *   - 结构体声明 (`struct`)
 *   - 接口声明 (`interface`)
 *   - 枚举声明 (`enum`)
 *   - 属性声明 (`let`/`var`/`prop`)
 *   - 扩展声明 (`extend`)
 *   - 全局变量
 * - **类型引用** ([CjTypeReference]):
 *   - 类型注解 (如 `let x: @Nullable String`)
 *
 * ## 接口属性
 *
 * ### [annotations]
 * 返回 [CjAnnotations] 容器列表。每个容器可能包含一个或多个连续的注解条目。
 * 这个属性返回的是 PSI 树中的组织节点。
 *
 * **使用场景**: 需要操作注解容器本身时使用,如批量删除、重组注解等。
 *
 * ### [annotationEntries]
 * 返回所有 [CjAnnotation] 的扁平列表,即实际使用的注解条目。
 * 这是最常用的属性,直接提供对所有注解使用的访问。
 *
 * **使用场景**:
 * - 检查元素是否有特定注解
 * - 获取注解参数
 * - 进行注解分析和处理
 *
 * ## 使用示例
 *
 * ### 示例 1: 检查是否有废弃注解
 * ```kotlin
 * fun isDeprecated(element: CjAnnotated): Boolean {
 *     return element.annotationEntries.any {
 *         it.shortName?.asString() == "Deprecated"
 *     }
 * }
 * ```
 *
 * ### 示例 2: 获取所有 FFI 相关注解
 * ```kotlin
 * fun getFfiAnnotations(element: CjAnnotated): List<CjAnnotation> {
 *     return element.annotationEntries.filter { constructor ->
 *         constructor.shortName?.asString() in setOf("C", "Java", "ForeignName")
 *     }
 * }
 * ```
 *
 * ### 示例 3: 获取注解参数
 * ```kotlin
 * fun getForeignName(function: CjNamedFunction): String? {
 *     val foreignNameAnnotation = function.annotationEntries
 *         .find { it.shortName?.asString() == "ForeignName" }
 *     return foreignNameAnnotation?.valueArguments
 *         ?.firstOrNull { it.getArgumentName()?.asString() == "name" }
 *         ?.getArgumentExpression()?.text
 * }
 * ```
 *
 * ## 对应源代码示例
 * ```cangjie
 * @Deprecated
 * @ForeignName(name: "native_function")
 * @C
 * func oldNativeFunc(): Int64 {
 *     return 0
 * }
 * ```
 *
 * 对于上述函数:
 * - `annotations`: 返回包含这些注解条目的 [CjAnnotations] 容器列表
 * - `annotationEntries`: 返回 3 个 [CjAnnotation] 的列表
 *   - `@Deprecated`
 *   - `@ForeignName(name: "native_function")`
 *   - `@C`
 *
 * ## 实现层次
 * ```
 * CjAnnotated (接口)
 *   ├─ CjModifierListOwner (接口, 带修饰符列表的元素)
 *   │   ├─ CjDeclaration (抽象类, 所有声明)
 *   │   │   ├─ CjNamedFunction (类)
 *   │   │   ├─ CjClass (类)
 *   │   │   ├─ CjStruct (类)
 *   │   │   └─ ...
 *   │   └─ CjModifierListOwnerStub (抽象类, 支持 stub)
 *   │       └─ CjTypeReference (类)
 *   └─ [其他直接实现]
 * ```
 *
 * ## 相关接口和类
 * @see CjAnnotations 注解容器,用于在 PSI 树中组织多个注解条目
 * @see CjAnnotation 单个注解使用,表示实际的注解应用
 * @see CjModifierListOwner 带修饰符列表的元素接口
 * @see CjModifierList 修饰符列表,包含修饰符和注解
 * @see CjBuiltInAnnotation 内置注解枚举定义
 *
 * ## 注意事项
 * - 优先使用 [annotationEntries] 而不是 [annotations],除非需要操作容器本身
 * - 注解条目的顺序与源代码中的声明顺序一致
 * - 空列表表示元素没有任何注解修饰
 */
interface CjAnnotated : CjElement , CjAnnotationsContainer {
    /**
     * 获取注解容器列表
     *
     * 返回 PSI 树中的 [CjAnnotations] 容器节点列表。
     * 每个容器可能包含一个或多个连续的注解条目。
     *
     * @return 注解容器列表,如果没有注解则返回空列表
     * @see CjAnnotations
     */
    val annotations: CjAnnotations?

    /**
     * 获取所有注解条目的扁平列表
     *
     * 返回元素上应用的所有 [CjAnnotation] 的列表,
     * 这是实际源代码中使用的注解。这是最常用的属性。
     *
     * 示例:
     * ```cangjie
     * @Deprecated
     * @Frozen
     * func example() {}
     * ```
     * 对于上述函数,此属性返回包含 2 个注解条目的列表。
     *
     * @return 注解条目列表,如果没有注解则返回空列表
     * @see CjAnnotation
     */
    val annotationEntries: List<CjAnnotation>
}
