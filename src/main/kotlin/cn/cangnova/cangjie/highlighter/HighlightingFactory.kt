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

package cn.cangnova.cangjie.highlighter

import com.intellij.codeInsight.daemon.impl.HighlightInfo
import com.intellij.codeInsight.daemon.impl.HighlightInfoType
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.NlsContexts.DetailedDescription
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement

/**
 * 高亮信息工厂类 - 创建和管理代码高亮信息
 * Highlighting Factory - Creates and manages code highlighting information
 *
 * 这个工厂类负责创建和配置代码高亮信息。它提供了一组静态方法，用于生成不同类型的高亮效果。
 * This factory class is responsible for creating and configuring code highlighting information.
 * It provides a set of static methods for generating different types of highlighting effects.
 *
 * 主要功能：
 * Main Features:
 *
 * 1. 元素高亮 (Element Highlighting)
 *    - 为 PSI 元素创建高亮信息
 *    - 支持自定义高亮类型
 *    - 可以添加描述性提示信息
 *
 * 2. 范围高亮 (Range Highlighting)
 *    - 支持指定文本范围的高亮
 *    - 可以独立于 PSI 元素进行高亮
 *    - 适用于特定文本区域的高亮需求
 *
 * 使用场景：
 * Usage Scenarios:
 *
 * 1. 语法高亮 (Syntax Highlighting)
 *    ```kotlin
 *    // 高亮一个方法名
 *    highlightName(methodElement, CangJieHighlightInfoTypeSemanticNames.FUNCTION_DECLARATION)
 *    ```
 *
 * 2. 错误标记 (Error Marking)
 *    ```kotlin
 *    // 标记语法错误
 *    highlightName(element, CangJieHighlightInfoTypeSemanticNames.ERROR, "Syntax error")
 *    ```
 *
 * 3. 范围高亮 (Range Highlighting)
 *    ```kotlin
 *    // 高亮特定文本范围
 *    highlightName(project, textRange, CangJieHighlightInfoTypeSemanticNames.KEYWORD)
 *    ```
 *
 * 工作原理：
 * How it works:
 *
 * 1. 创建高亮信息
 *    - 检查元素或范围的有效性
 *    - 配置高亮类型和属性
 *    - 生成高亮信息构建器
 *
 * 2. 配置选项
 *    - 设置高亮范围
 *    - 添加提示信息（可选）
 *    - 应用高亮样式
 *
 * 注意事项：
 * Notes:
 * 1. 确保传入的 PSI 元素有效且具有非空的文本范围
 * 2. 高亮类型应该与元素的语义相匹配
 * 3. 提示信息应该简洁明确，帮助用户理解高亮原因
 */
object HighlightingFactory {
    /**
     * 为 PSI 元素创建高亮信息
     * Creates highlighting information for a PSI element
     *
     * @param element 要高亮的 PSI 元素 (The PSI element to highlight)
     * @param highlightInfoType 高亮类型，决定高亮的样式 (Highlight type that determines the style)
     * @param message 可选的提示信息，鼠标悬停时显示 (Optional tooltip message shown on hover)
     * @return 高亮信息构建器，如果元素无效则返回 null (Highlight info builder, or null if element is invalid)
     */
    fun highlightName(element: PsiElement, highlightInfoType: HighlightInfoType, message: @DetailedDescription String? = null): HighlightInfo.Builder? {
        val project = element.project
        if (!element.textRange.isEmpty) {
            return highlightName(project, element.textRange, highlightInfoType, message)
        }
        return null
    }

    /**
     * 为指定文本范围创建高亮信息
     * Creates highlighting information for a specific text range
     *
     * @param project 当前项目实例 (Current project instance)
     * @param textRange 要高亮的文本范围 (Text range to highlight)
     * @param highlightInfoType 高亮类型，决定高亮的样式 (Highlight type that determines the style)
     * @param message 可选的提示信息，鼠标悬停时显示 (Optional tooltip message shown on hover)
     * @return 高亮信息构建器 (Highlight info builder)
     */
    fun highlightName(
        project: Project,
        textRange: TextRange,
        highlightInfoType: HighlightInfoType,
        message: @DetailedDescription String? = null
    ): HighlightInfo.Builder {
        val builder = HighlightInfo.newHighlightInfo(highlightInfoType)
        if (message != null) {
            builder.descriptionAndTooltip(message)
        }
        val annotation = builder
            .range(textRange)
        return annotation
    }
}
