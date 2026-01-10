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

package org.cangnova.cangjie.diagnostics

import org.cangnova.cangjie.psi.psiUtil.endOffset
import org.cangnova.cangjie.psi.psiUtil.startOffset
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiErrorElement
import com.intellij.psi.PsiWhiteSpace

/**
 * 诊断定位策略基类
 *
 * 定义了在 IDE 编辑器中标记诊断信息（错误、警告等）的位置策略。
 * 此类决定了编译器诊断信息应该在代码的哪个文本范围内显示（如波浪线、高亮等）。
 *
 * ## 核心职责
 * - **精确定位**：确定诊断信息应该标记的文本范围
 * - **语法验证**：检查元素是否有语法错误
 * - **可扩展性**：支持为不同类型的 PSI 元素自定义定位策略
 *
 * ## 工作原理
 * 1. 接收一个 PSI 元素和诊断标记器
 * 2. 根据元素类型和上下文确定应该标记的文本范围
 * 3. 返回一个或多个文本范围，用于在编辑器中显示诊断提示
 *
 * ## 使用场景
 * - 错误高亮：标记语法错误或类型错误的具体位置
 * - 警告提示：标记潜在问题的代码片段
 * - 快速修复：为快速修复操作确定目标范围
 *
 * @param E PSI 元素类型，使用逆变泛型以支持更灵活的类型匹配
 *
 * @see org.cangnova.cangjie.diagnostics.DiagnosticMarker 诊断标记器，包含待标记的诊断信息
 * @see TextRange IntelliJ 平台的文本范围类
 *
 * @sample
 * ```kotlin
 * // 自定义定位策略：只标记函数名称
 * class FunctionNamePositioningStrategy : PositioningStrategy<CjFunctionDeclaration>() {
 *     override fun mark(element: CjFunctionDeclaration): List<TextRange> {
 *         val nameElement = element.nameIdentifier ?: return emptyList()
 *         return markElement(nameElement)
 *     }
 * }
 * ```
 */
open class PositioningStrategy<in E : PsiElement> {
    /**
     * 标记诊断信息的文本范围
     *
     * 这是主入口方法，接收诊断标记器并返回应该标记的文本范围列表。
     * 默认实现会将诊断标记器中的 PSI 元素转换为类型 `E`，然后调用 [mark] 方法。
     *
     * @param diagnostic 诊断标记器，包含待标记的 PSI 元素和诊断信息
     * @return 应该标记的文本范围列表，通常包含一个范围，特殊情况下可能有多个
     */
    open fun markDiagnostic(diagnostic: DiagnosticMarker): List<TextRange> {
        @Suppress("UNCHECKED_CAST")
        return mark(diagnostic.psiElement as E)
    }

    /**
     * 标记指定 PSI 元素的文本范围
     *
     * 子类应该覆盖此方法以实现自定义的定位逻辑。
     * 默认实现会标记整个元素的范围（去除空白和注释）。
     *
     * @param element 要标记的 PSI 元素
     * @return 应该标记的文本范围列表
     */
    open fun mark(element: E): List<TextRange> {
        return markElement(element)
    }

    /**
     * 检查元素是否有效（无语法错误）
     *
     * 在某些情况下，只有语法正确的元素才应该显示特定的诊断信息。
     * 此方法用于过滤掉已经有语法错误的元素，避免重复报错。
     *
     * @param element 要检查的 PSI 元素
     * @return 如果元素有效（无语法错误）返回 true，否则返回 false
     */
    open fun isValid(element: E): Boolean {
        return !hasSyntaxErrors(element)
    }
}

/**
 * 检查 PSI 元素是否包含语法错误
 *
 * 递归检查元素及其子元素，判断是否存在语法错误节点（PsiErrorElement）。
 * 为了优化性能，只检查最后一个子元素，因为语法错误通常出现在解析的末尾。
 *
 * @param psiElement 要检查的 PSI 元素
 * @return 如果元素或其子元素包含语法错误返回 true，否则返回 false
 */
fun hasSyntaxErrors(psiElement: PsiElement): Boolean {
    if (psiElement is PsiErrorElement) return true

    val children = psiElement.children
    return children.isNotEmpty() && hasSyntaxErrors(children.last())
}

/**
 * 标记指定的文本范围
 *
 * 将单个文本范围包装成列表返回，用于统一接口。
 *
 * @param range 要标记的文本范围
 * @return 包含单个文本范围的列表
 */
fun markRange(range: TextRange): List<TextRange> {
    return listOf(range)
}

/**
 * 标记从一个元素到另一个元素的文本范围
 *
 * 创建一个从起始元素到结束元素的连续文本范围。
 * 自动跳过起始和结束位置的空白和注释。
 *
 * @param from 范围的起始 PSI 元素
 * @param to 范围的结束 PSI 元素
 * @return 包含指定范围的列表
 */
fun markRange(from: PsiElement, to: PsiElement): List<TextRange> {
    return markRange(TextRange(getStartOffset(from), getEndOffset(to)))
}

/**
 * 标记整个 PSI 元素的文本范围
 *
 * 创建覆盖整个元素的文本范围，但会自动排除前后的空白和注释。
 * 这是最常用的标记方法，适用于大多数诊断场景。
 *
 * @param element 要标记的 PSI 元素
 * @return 包含元素有效内容范围的列表
 */
fun markElement(element: PsiElement): List<TextRange> {
    return listOf(TextRange(getStartOffset(element), getEndOffset(element)))
}

/**
 * 获取元素的实际结束偏移量
 *
 * 递归查找元素的最后一个有效子节点，跳过尾部的空白和注释。
 * 这确保了诊断标记不会包含无意义的空白字符。
 *
 * @param element PSI 元素
 * @return 元素有效内容的结束位置（文档偏移量）
 */
private fun getEndOffset(element: PsiElement): Int {
    var child = element.lastChild
    if (child != null) {
        while (child is PsiComment || child is PsiWhiteSpace) {
            child = child.prevSibling
        }
        if (child != null) {
            return getEndOffset(child)
        }
    }

    return element.endOffset
}

/**
 * 获取元素的实际起始偏移量
 *
 * 递归查找元素的第一个有效子节点，跳过前导的空白和注释。
 * 这确保了诊断标记从实际代码内容开始，而不是从空白处开始。
 *
 * @param element PSI 元素
 * @return 元素有效内容的起始位置（文档偏移量）
 */
private fun getStartOffset(element: PsiElement): Int {
    var child = element.firstChild
    if (child != null) {
        while (child is PsiComment || child is PsiWhiteSpace) {
            child = child.nextSibling
        }
        if (child != null) {
            return getStartOffset(child)
        }
    }
    return element.startOffset
}

