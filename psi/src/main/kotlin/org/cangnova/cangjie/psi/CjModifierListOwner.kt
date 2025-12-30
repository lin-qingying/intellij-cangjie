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

import org.cangnova.cangjie.lexer.CjKeywordToken
import com.intellij.psi.PsiElement

/**
 * 拥有修饰符列表的仓颉 PSI 元素接口
 *
 * 该接口表示所有可以使用修饰符(modifier)修饰的 PSI 元素。
 * 修饰符用于改变声明的可见性、行为和特性,如 `public`、`private`、`mut`、`abstract` 等。
 *
 * ## 修饰符类型
 * 仓颉语言中的修饰符包括但不限于:
 * - **可见性修饰符**: `public`, `private`, `protected`, `internal`
 * - **可变性修饰符**: `mut`, `const`
 * - **类特性修饰符**: `abstract`, `sealed`, `open`, `final`
 * - **成员特性修饰符**: `override`, `static`, `native`
 * - **其他修饰符**: `inline`, `operator`, `infix` 等
 *
 * ## 接口继承关系
 * 该接口继承自:
 * - [PsiElement]: IntelliJ 平台的 PSI 元素基础接口
 * - [CjAnnotated]: 可被注解修饰的元素接口
 *
 * 这意味着所有拥有修饰符的元素都可以同时被注解修饰。
 *
 * ## 典型实现类
 * - [CjDeclaration]: 所有声明元素的基类
 * - [CjNamedFunction]: 函数声明
 * - [CjClass]: 类声明
 * - [CjProperty]: 属性声明
 * - [CjParameter]: 参数声明
 *
 * ## 使用示例
 *
 * ### 示例 1: 检查元素是否为公开访问
 * ```kotlin
 * fun isPublic(element: CjModifierListOwner): Boolean {
 *     return element.hasModifier(CjKeywordToken.PUBLIC_KEYWORD)
 * }
 * ```
 *
 * ### 示例 2: 添加 open 修饰符
 * ```kotlin
 * fun makeOpen(element: CjModifierListOwner) {
 *     if (!element.hasModifier(CjKeywordToken.OPEN_KEYWORD)) {
 *         element.addModifier(CjKeywordToken.OPEN_KEYWORD)
 *     }
 * }
 * ```
 *
 * ### 示例 3: 获取所有修饰符
 * ```kotlin
 * fun getModifiers(element: CjModifierListOwner): List<CjKeywordToken> {
 *     val modifierList = element.modifierList ?: return emptyList()
 *     return modifierList.modifierElements.map { it.modifier }
 * }
 * ```
 *
 * ## 源代码示例
 * ```cangjie
 * // public abstract 类
 * public abstract class Shape {
 *     // protected const 属性
 *     protected const static PI: Float64 = 3.14159
 *
 *     // public abstract 方法
 *     public abstract func area(): Float64
 * }
 *
 * // public sealed 类
 * public sealed class Result<T> {
 *     // ...
 * }
 * ```
 *
 * ## 与 CjModifierList 的关系
 * - [CjModifierListOwner] (接口): 表示"拥有"修饰符列表的元素
 * - [CjModifierList] (PSI 元素): 修饰符列表的实际 PSI 节点,包含修饰符和注解
 *
 * ```
 * CjModifierListOwner
 *   └─ modifierList: CjModifierList?
 *       ├─ modifiers: List<CjModifier>
 *       └─ annotations: List<CjAnnotation>
 * ```
 *
 * @see CjModifierList 修饰符列表的 PSI 元素
 * @see CjAnnotated 可被注解修饰的元素接口
 * @see CjKeywordToken 关键字标记枚举
 */
interface CjModifierListOwner : PsiElement, CjAnnotated {

    /**
     * 获取修饰符列表
     *
     * @return 修饰符列表的 PSI 元素,如果没有修饰符则返回 null
     */
    val modifierList: CjModifierList?

    /**
     * 检查是否包含指定修饰符
     *
     * @param modifier 要检查的修饰符关键字标记
     * @return 如果包含该修饰符返回 true,否则返回 false
     */
    fun hasModifier(modifier: CjKeywordToken): Boolean

    /**
     * 添加指定修饰符
     *
     * 如果该修饰符已存在,则不会重复添加。
     * 该方法会修改 PSI 树,通常在代码重构或快速修复时使用。
     *
     * @param modifier 要添加的修饰符关键字标记
     */
    fun addModifier(modifier: CjKeywordToken)

    /**
     * 移除指定修饰符
     *
     * 如果该修饰符不存在,则不进行任何操作。
     * 该方法会修改 PSI 树,通常在代码重构或快速修复时使用。
     *
     * @param modifier 要移除的修饰符关键字标记
     */
    fun removeModifier(modifier: CjKeywordToken)
}
