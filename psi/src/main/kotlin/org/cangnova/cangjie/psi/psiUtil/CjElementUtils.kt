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

package org.cangnova.cangjie.psi.psiUtil

import org.cangnova.cangjie.lexer.CjTokens
import org.cangnova.cangjie.psi.CjElement
import org.cangnova.cangjie.psi.CjExpression
import org.cangnova.cangjie.psi.CjLambdaExpression
import org.cangnova.cangjie.psi.CjParenthesizedExpression
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiWhiteSpace
import com.intellij.psi.util.PsiTreeUtil

/**
 * 删除元素后的分号
 *
 * ## 使用场景
 * 在代码重构或转换时，需要移除语句末尾的分号。例如：
 * - 将语句转换为表达式时（不再需要分号）
 * - 删除语句时，同时清理分号
 * - 代码风格转换（从 Java 风格转为仓颉风格）
 *
 * ## 处理逻辑
 * 1. 跳过元素后的空白和注释
 * 2. 检查是否是分号
 * 3. 如果是分号，找到分号后的最后一个空白
 * 4. 删除分号及其后的空白（保持代码整洁）
 *
 * ## 为什么要删除分号后的空白？
 * - 避免留下多余的空白行
 * - 保持代码格式的一致性
 *
 * ## 示例
 * ```kotlin
 * // 转换前：
 * val x = 10;
 *
 * // 删除分号后：
 * val x = 10
 * ```
 *
 * @receiver 要删除分号的元素
 */
internal fun CjElement.deleteSemicolon() {
    // 跳过空白和注释，找到第一个有意义的兄弟元素
    val sibling = PsiTreeUtil.skipSiblingsForward(this, PsiWhiteSpace::class.java, PsiComment::class.java)
    if (sibling == null || sibling.node.elementType != CjTokens.SEMICOLON) return

    // 找到分号后的最后一个空白元素
    val lastSiblingToDelete = PsiTreeUtil.skipSiblingsForward(sibling, PsiWhiteSpace::class.java)?.prevSibling ?: sibling

    // 删除从下一个兄弟到最后一个空白的所有元素（包括分号）
    parent?.deleteChildRange(nextSibling, lastSiblingToDelete)
}

/**
 * 解包函数字面量（Lambda 表达式）
 *
 * ## 核心功能
 * 从表达式中提取出 Lambda 表达式，忽略包裹层（如括号、标签、注解）。
 *
 * ## 处理的表达式类型
 *
 * ### 1. 直接的 Lambda 表达式
 * ```kotlin
 * { x -> x * 2 }  // 直接返回
 * ```
 *
 * ### 2. 带标签的 Lambda（已注释）
 * ```kotlin
 * label@ { x -> x * 2 }  // 解包后返回 Lambda
 * ```
 *
 * ### 3. 带注解的 Lambda（已注释）
 * ```kotlin
 * @Anno { x -> x * 2 }  // 解包后返回 Lambda
 * ```
 *
 * ### 4. 括号包裹的 Lambda
 * ```kotlin
 * ({ x -> x * 2 })  // 仅在 allowParentheses = true 时解包
 * ```
 *
 * ### 5. 其他表达式
 * ```kotlin
 * x + 1  // 不是 Lambda，返回 null
 * ```
 *
 * ## 参数说明
 * @param allowParentheses 是否允许解包括号表达式
 *   - true：`({ ... })` 会被解包为 `{ ... }`
 *   - false：`({ ... })` 返回 null（不认为是 Lambda）
 *
 * ## 使用场景
 * - 检查函数调用的最后一个参数是否是 Lambda（尾随 Lambda 语法）
 * - 代码转换：移除不必要的括号
 * - 类型推导：获取 Lambda 的参数和返回类型
 *
 * ## 为什么有 allowParentheses 参数？
 * - 在某些上下文中，括号是必需的（如运算符优先级）
 * - 在其他上下文中，括号是多余的（如函数调用的尾随 Lambda）
 * - 通过参数控制，灵活处理不同场景
 *
 * ## 示例
 * ```kotlin
 * // 场景 1：尾随 Lambda 检查
 * fun foo(f: () -> Unit) { ... }
 * foo { println("Hello") }  // 提取 Lambda
 *
 * // 场景 2：括号处理
 * foo(({ println("Hello") }))  // allowParentheses=true 时提取
 * ```
 *
 * @receiver 要解包的表达式
 * @return 解包出的 Lambda 表达式，如果不是 Lambda 则返回 null
 */
fun CjExpression.unpackFunctionLiteral(allowParentheses: Boolean = false): CjLambdaExpression? {
    return when (this) {
        // 直接的 Lambda 表达式
        is CjLambdaExpression -> this

        // 标签表达式（如 label@ { ... }）- 已注释，未来可能支持
//        is CjLabeledExpression -> baseExpression?.unpackFunctionLiteral(allowParentheses)

        // 注解表达式（如 @Anno { ... }）- 已注释，未来可能支持
//        is CjAnnotatedExpression -> baseExpression?.unpackFunctionLiteral(allowParentheses)

        // 括号表达式（如 ({ ... })）
        is CjParenthesizedExpression -> if (allowParentheses) expression?.unpackFunctionLiteral(allowParentheses) else null

        // 其他表达式，不是 Lambda
        else -> null
    }
}
