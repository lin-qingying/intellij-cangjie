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

package org.cangnova.cangjie.chir

import com.intellij.lang.LighterASTNode
import com.intellij.psi.tree.IElementType
import com.intellij.util.diff.FlyweightCapableTreeStructure

/**
 * 仓颉源代码元素的抽象表示。
 *
 * 此类封装了 CHIR 元素到源代码位置的映射关系，用于：
 * - 错误报告时定位到具体源码位置
 * - IDE 功能（导航、高亮、代码折叠等）
 * - 调试器断点和单步执行
 * - 代码分析和重构工具
 *
 * ## 设计考虑
 * - 使用轻量级 AST 节点 ([LighterASTNode]) 而非完整 PSI 树，减少内存占用
 * - 通过 [FlyweightCapableTreeStructure] 高效访问 AST 树结构
 * - 基于位置的相等性比较，用于快速去重和查找
 *
 * ## 实现要求
 * - 子类必须正确实现 [hashCode] 和 [equals]，基于源元素内容计算
 * - 相同源位置的元素应被视为相等
 *
 * @see AbstractCjSourceElement 基类，提供位置信息
 * @see ChirElement.source 使用此类型关联源码位置
 */
sealed class CjSourceElement : AbstractCjSourceElement() {
    /**
     * 元素类型标识符。
     *
     * 对应于 IntelliJ PSI 的 [IElementType]，用于识别源代码元素的语法类别
     * （如关键字、标识符、运算符等）。
     *
     * @return 元素类型，如果无法确定则为 null
     */
    abstract val elementType: IElementType?

    /**
     * 轻量级 AST 节点引用。
     *
     * 使用 [LighterASTNode] 而非完整 PSI 节点可以显著降低内存使用，
     * 特别适合大型文件的快速解析和索引构建。
     *
     * @return 对应的轻量级 AST 节点
     */
    abstract val lighterASTNode: LighterASTNode

    /**
     * AST 树结构访问器。
     *
     * 提供对完整 AST 树的访问能力，用于查询父节点、子节点等结构信息。
     * 使用享元模式 ([FlyweightCapableTreeStructure]) 优化内存使用。
     *
     * @return 树结构访问器
     */
    abstract val treeStructure: FlyweightCapableTreeStructure<LighterASTNode>

    /**
     * 获取此元素在上下文中的文本表示（仅用于调试）。
     *
     * 返回包含上下文信息的源代码片段，帮助开发者在调试时快速定位问题。
     * 不应用于生产代码的逻辑判断。
     *
     * @return 调试用的文本表示，通常包含周围的代码上下文
     */
    abstract fun getElementTextInContextForDebug(): String

    /**
     * 计算哈希码。
     *
     * **实现要求**：必须基于源元素的内容计算哈希码，而非对象身份。
     * 相同源位置的元素应产生相同的哈希码。
     *
     * @return 基于源元素内容的哈希码
     */
    abstract override fun hashCode(): Int

    /**
     * 判断与另一个对象是否相等。
     *
     * **实现要求**：相同源位置的元素应被视为相等。
     * 通常基于 [startOffset]、[endOffset] 和文件路径进行比较。
     *
     * @param other 待比较的对象
     * @return 如果表示相同的源位置则返回 true
     */
    abstract override fun equals(other: Any?): Boolean
}

/**
 * 源代码元素的抽象基类，提供基本的位置信息。
 *
 * 此类定义了所有源代码元素共有的位置属性和比较逻辑。
 * 位置信息使用字符偏移量表示，从文件开头计算。
 *
 * ## 位置计算
 * - [startOffset]: 元素起始位置（包含）
 * - [endOffset]: 元素结束位置（不包含）
 * - 范围: `[startOffset, endOffset)`
 *
 * ## 相等性语义
 * 两个源元素如果具有相同的起始和结束位置，则被视为相等。
 * 这种比较方式适用于快速去重和位置查找。
 *
 * @see CjSourceElement 扩展此类的具体实现
 */
sealed class AbstractCjSourceElement {
    /**
     * 元素在源文件中的起始字符偏移量（包含）。
     *
     * 偏移量从文件开头（位置 0）开始计算。
     * 例如，文件第一个字符的 startOffset 为 0。
     *
     * @return 起始位置（从 0 开始）
     */
    abstract val startOffset: Int

    /**
     * 元素在源文件中的结束字符偏移量（不包含）。
     *
     * 偏移量指向元素最后一个字符之后的位置。
     * 元素的长度为 `endOffset - startOffset`。
     *
     * 示例：
     * ```
     * 源码: "hello"
     * startOffset: 0
     * endOffset: 5
     * 长度: 5
     * ```
     *
     * @return 结束位置（不包含此位置的字符）
     */
    abstract val endOffset: Int

    /**
     * 判断与另一个源元素是否相等。
     *
     * 相等性基于位置信息：如果两个元素的 [startOffset] 和 [endOffset] 都相同，
     * 则认为它们相等。
     *
     * @param other 待比较的对象
     * @return 如果位置相同则返回 true
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is AbstractCjSourceElement) return false

        if (startOffset != other.startOffset) return false
        if (endOffset != other.endOffset) return false

        return true
    }

    /**
     * 计算哈希码。
     *
     * 哈希码基于 [startOffset] 和 [endOffset] 计算，
     * 确保相等的元素具有相同的哈希码。
     *
     * @return 基于位置信息的哈希码
     */
    override fun hashCode(): Int {
        var result = startOffset
        result = 31 * result + endOffset
        return result
    }
}