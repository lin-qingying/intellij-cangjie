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

package org.cangnova.cangjie.resolve.qualified

import com.intellij.util.SmartList
import org.cangnova.cangjie.psi.*
import org.cangnova.cangjie.types.expressions.isWithoutValueArguments

/**
 * 限定表达式扩展函数
 *
 * 本文件提供将 PSI 表达式转换为限定符部分列表的扩展函数。
 */

/**
 * 将表达式转换为限定符部分列表
 *
 * 将嵌套的限定表达式展开为线性的名称路径。
 * 支持普通限定表达式（`a.b.c`）和双冒号左侧表达式（`a.b.C::method`）。
 *
 * ## 处理逻辑
 *
 * ### 普通模式（doubleColonLHS = false）
 * - 只处理简单名称表达式
 * - 从右到左递归展开限定表达式
 * - 最终反转结果得到从左到右的顺序
 *
 * ### 双冒号模式（doubleColonLHS = true）
 * - 支持简单名称表达式
 * - **额外支持无值参数的调用表达式**（如 `Foo<T>()`）
 * - 提取调用表达式的被调用者和类型参数
 *
 * ## 示例
 *
 * ### 普通模式
 * ```kotlin
 * a.b.c -> [ExpressionQualifierPart(a), ExpressionQualifierPart(b), ExpressionQualifierPart(c)]
 * ```
 *
 * ### 双冒号模式
 * ```kotlin
 * com.example.Foo<T> -> [
 *     ExpressionQualifierPart(com),
 *     ExpressionQualifierPart(example),
 *     ExpressionQualifierPart(Foo, typeArgs=<T>)
 * ]
 * ```
 *
 * ## 实现细节
 * - 使用 `SmartList` 优化单元素列表的内存使用
 * - 从右到左构建，最后反转（避免在列表头部插入的性能问题）
 * - 内部嵌套函数 `addQualifierPart` 处理单个表达式的添加
 *
 * @param doubleColonLHS 是否为双冒号左侧（影响调用表达式的处理）
 * @return 限定符部分列表，从左到右的顺序
 */
fun CjExpression.asQualifierPartList(doubleColonLHS: Boolean = false): List<ExpressionQualifierPart> {
    val result = SmartList<ExpressionQualifierPart>()

    /**
     * 尝试将表达式添加为限定符部分
     *
     * @return true 表示成功添加且应该停止递归，false 表示继续处理父表达式
     */
    fun addQualifierPart(expression: CjExpression?): Boolean {
        // 简单名称表达式：直接添加
        if (expression is CjSimpleNameExpression) {
            result.add(ExpressionQualifierPart(expression))
            return true
        }

        // 双冒号模式：支持无值参数的调用表达式（如 Foo<T>()）
        if (doubleColonLHS && expression is CjCallExpression && expression.isWithoutValueArguments) {
            val simpleName = expression.calleeExpression
            if (simpleName is CjSimpleNameExpression) {
                result.add(
                    ExpressionQualifierPart(
                        simpleName.referencedNameAsName,
                        simpleName,
                        expression.typeArgumentList  // 提取类型参数
                    )
                )
                return true
            }
        }

        // 无法处理的表达式类型
        return false
    }

    // 从右到左展开限定表达式
    var expression: CjExpression? = this
    while (true) {
        // 尝试添加当前表达式
        if (addQualifierPart(expression)) break

        // 如果不是限定表达式，无法继续展开
        if (expression !is CjQualifiedExpression) break

        // 添加选择器部分
        addQualifierPart(expression.selectorExpression)

        // 继续处理接收器
        expression = expression.receiverExpression
    }

    // 反转结果（从右到左构建 -> 从左到右）
    return result.asReversed()
}

/**
 * 将导入内容转换为限定符部分列表
 *
 * 根据导入内容的类型（基于表达式或基于完全限定名），
 * 将其转换为统一的限定符部分列表格式。
 *
 * ## 处理逻辑
 *
 * ### 基于表达式的导入（ExpressionBased）
 * ```kotlin
 * import a.b.c  // expression 是一个 CjQualifiedExpression
 * ```
 * 委托给 `CjExpression.asQualifierPartList()` 处理。
 *
 * ### 基于完全限定名的导入（FqNameBased）
 * ```kotlin
 * // 某些情况下导入内容直接存储为 FqName
 * ```
 * 将 `FqName` 的每个段转换为 `QualifierPart`。
 *
 * ## 使用场景
 * - 解析 `import` 语句
 * - 处理默认导入
 * - 构建导入作用域
 *
 * @return 限定符部分列表
 */
fun CjImportInfo.ImportContent.asQualifierPartList(): List<QualifierPart> =
    when (this) {
        // 基于表达式：使用表达式的限定符部分列表
        is CjImportInfo.ImportContent.ExpressionBased -> expression.asQualifierPartList()

        // 基于完全限定名：直接从 FqName 构建
        is CjImportInfo.ImportContent.FqNameBased -> fqName.pathSegments().map { QualifierPart(it) }
    }
