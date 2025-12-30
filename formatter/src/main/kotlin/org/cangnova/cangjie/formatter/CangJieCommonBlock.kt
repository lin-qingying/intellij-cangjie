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

/**
 * 仓颉语言代码格式化系统的核心实现
 *
 * ## 架构概述
 *
 * 本文件是 IntelliJ Platform 代码格式化系统在仓颉语言上的具体实现，负责将抽象语法树（AST）
 * 转换为格式化的代码块（Block），并根据用户配置的代码风格规则进行排版。
 *
 * ### 格式化系统架构
 *
 * ```
 * PSI 树（源代码）
 *   ↓
 * AST Node
 *   ↓
 * CangJieCommonBlock（本文件）
 *   ├─ 构建子块（buildSubBlocks）
 *   ├─ 计算缩进（createChildIndent）
 *   ├─ 计算换行（getWrappingStrategy）
 *   ├─ 计算对齐（getChildrenAlignmentStrategy）
 *   └─ 计算间距（spacingBuilder）
 *   ↓
 * 格式化后的代码
 * ```
 *
 * ## 核心功能模块
 *
 * ### 1. 代码块构建（Block Building）
 *
 * **职责**：将 AST 节点递归地转换为格式化块
 *
 * **核心方法**：
 * - `buildChildren()` - 构建当前节点的所有子块
 * - `buildSubBlocks()` - 实际的子块构建逻辑
 * - `buildSubBlock()` - 为单个子节点创建块
 *
 * **特殊处理**：
 * - 限定表达式（Qualified Expressions）- 在 `.` 操作符处分割
 * - Elvis 表达式 - 在 `?:` 操作符处分割
 * - 二元表达式 - 扁平化嵌套的二元表达式树
 *
 * ### 2. 缩进计算（Indentation）
 *
 * **职责**：为每个代码块计算正确的缩进级别
 *
 * **核心方法**：
 * - `createChildIndent()` - 根据父子关系和代码风格计算缩进
 * - `getChildAttributes()` - 获取新插入元素的缩进和对齐
 *
 * **缩进类型**：
 * - `Indent.getNoneIndent()` - 无缩进
 * - `Indent.getNormalIndent()` - 标准缩进（通常是 4 个空格）
 * - `Indent.getContinuationIndent()` - 延续缩进（通常是 8 个空格）
 * - `Indent.getContinuationWithoutFirstIndent()` - 除第一个元素外的延续缩进
 *
 * **缩进规则示例**：
 * ```cangjie
 * class Foo {           // CLASS_BODY 内部使用 NORMAL 缩进
 *     func bar() {      // 成员函数使用 NORMAL 缩进
 *         let x = 1     // BLOCK 内部使用 NORMAL 缩进
 *         let y = some
 *             .very     // 链式调用使用 CONTINUATION 缩进
 *             .long
 *             .call()
 *     }
 * }
 * ```
 *
 * ### 3. 换行策略（Wrapping Strategy）
 *
 * **职责**：决定何时在元素之间插入换行符
 *
 * **核心方法**：
 * - `getWrappingStrategy()` - 根据节点类型返回换行策略
 * - `getWrappingStrategyForItemList()` - 列表元素的通用换行策略
 * - `trailingCommaWrappingStrategy()` - 支持尾随逗号的换行策略
 *
 * **换行模式**（对应 IntelliJ 设置）：
 * - `DO_NOT_WRAP (0)` - 不换行
 * - `WRAP_AS_NEEDED (1)` - 必要时换行（超过行宽限制）
 * - `WRAP_ON_EVERY_ITEM (2)` - 每个元素都换行
 * - `WRAP_ALWAYS (3)` - 总是换行
 *
 * **特殊处理**：
 * - **尾随逗号**：支持参数列表、类型参数等的尾随逗号格式化
 * - **链式调用**：`.method().method()` 形式的换行控制
 * - **枚举常量**：`|` 分隔符的特殊换行逻辑
 *
 * ### 4. 对齐策略（Alignment Strategy）
 *
 * **职责**：在多行代码中对齐相关元素
 *
 * **核心方法**：
 * - `getChildrenAlignmentStrategy()` - 为子元素获取对齐策略
 * - `getAlignmentForChildInParenthesis()` - 括号内元素的对齐
 *
 * **对齐示例**：
 * ```cangjie
 * // 参数对齐
 * func foo(param1: Int,
 *          param2: String,  // 与 param1 对齐
 *          param3: Bool)
 *
 * // 二元表达式对齐
 * let result = value1 +
 *              value2 +     // 与 value1 对齐
 *              value3
 * ```
 *
 * ### 5. 格式化控制（Formatting Control）
 *
 * **职责**：支持选择性禁用格式化
 *
 * **核心方法**：
 * - `shouldFormat()` - 判断节点是否应该被格式化
 *
 * **控制机制**：
 * ```cangjie
 * // @formatter:off
 * let unformatted = poorly    formatted   code
 * // @formatter:on
 * ```
 *
 * ### 6. 特殊语法处理
 *
 * #### 限定表达式（Qualified Expressions）
 *
 * **问题**：链式调用需要特殊的换行和缩进处理
 * ```cangjie
 * object.method1()
 *     .method2()
 *     .method3()
 * ```
 *
 * **解决方案**：
 * - `splitSubBlocksOnDot()` - 在 `.` 处分割子块
 * - `anyCallInCallChainIsWrapped()` - 检测链中是否有换行
 * - `createWrapForQualifierExpression()` - 为链式调用创建换行
 *
 * #### 二元表达式（Binary Expressions）
 *
 * **问题**：嵌套的二元表达式树需要扁平化以便统一处理
 * ```cangjie
 * let result = a + b + c + d
 * ```
 *
 * **解决方案**：
 * - `collectBinaryExpressionChildren()` - 递归收集所有操作数和操作符
 * - `splitSubBlocksOnElvis()` - Elvis 操作符的特殊处理
 *
 * #### 枚举定义（Enum Definitions）
 *
 * **问题**：枚举使用 `|` 分隔符，需要特殊换行逻辑
 * ```cangjie
 * enum Color {
 *     RED
 *     | GREEN
 *     | BLUE
 * }
 * ```
 *
 * **解决方案**：
 * - `getWrappingStrategyForEnum()` - 枚举专用换行策略
 *
 * ## 关键数据结构
 *
 * ### ASTBlock
 *
 * IntelliJ Platform 的格式化块接口，每个块代表一个需要格式化的代码片段。
 *
 * **核心属性**：
 * - `node: ASTNode` - 对应的 AST 节点
 * - `indent: Indent` - 缩进信息
 * - `wrap: Wrap` - 换行信息
 * - `alignment: Alignment` - 对齐信息
 * - `subBlocks: List<Block>` - 子块列表
 *
 * ### CommonAlignmentStrategy
 *
 * 对齐策略的抽象接口，根据节点类型返回对应的对齐对象。
 *
 * ### WrappingStrategy
 *
 * 换行策略的类型别名：`(ASTNode) -> Wrap?`
 *
 * ## 配置项支持
 *
 * 本文件支持以下代码风格配置项（来自 `CodeStyleSettings`）：
 *
 * ### 通用设置（CommonCodeStyleSettings）
 *
 * - `ALIGN_MULTILINE_PARAMETERS` - 对齐多行参数
 * - `ALIGN_MULTILINE_PARAMETERS_IN_CALLS` - 对齐多行调用参数
 * - `ALIGN_MULTILINE_BINARY_OPERATION` - 对齐多行二元表达式
 * - `ALIGN_MULTILINE_EXTENDS_LIST` - 对齐多行继承列表
 * - `METHOD_CALL_CHAIN_WRAP` - 方法链换行模式
 * - `CALL_PARAMETERS_WRAP` - 调用参数换行模式
 * - `METHOD_PARAMETERS_WRAP` - 方法参数换行模式
 * - `ASSIGNMENT_WRAP` - 赋值表达式换行模式
 * - `*_ANNOTATION_WRAP` - 各类注解换行模式
 *
 * ### 自定义设置（CangJieCodeStyleSettings）
 *
 * - `CONTINUATION_INDENT_FOR_CHAINED_CALLS` - 链式调用使用延续缩进
 * - `CONTINUATION_INDENT_IN_ARGUMENT_LISTS` - 参数列表使用延续缩进
 * - `CONTINUATION_INDENT_IN_PARAMETER_LISTS` - 形参列表使用延续缩进
 * - `CONTINUATION_INDENT_IN_ELVIS` - Elvis 表达式使用延续缩进
 * - `CONTINUATION_INDENT_FOR_EXPRESSION_BODIES` - 表达式体使用延续缩进
 * - `WRAP_EXPRESSION_BODY_FUNCTIONS` - 表达式体函数换行模式
 * - `WRAP_FIRST_METHOD_IN_CALL_CHAIN` - 链中第一个方法也换行
 * - `ALIGN_IN_COLUMNS_CASE_BRANCH` - Match 分支列对齐
 * - `BLANK_LINES_AROUND_BLOCK_MATCH_BRANCHES` - Match 分支间空行数
 *
 * ## 性能优化
 *
 * ### 缓存机制
 *
 * - `@Volatile private var mySubBlocks` - 子块列表缓存，避免重复构建
 *
 * ### 惰性计算
 *
 * - 子块按需构建，不会一次性构建整个树
 * - 对齐和换行策略按需计算
 *
 * ### 序列操作
 *
 * - 使用 Kotlin `Sequence` 进行惰性遍历，减少中间集合创建
 *
 * ## 扩展点
 *
 * ### 抽象方法（需要子类实现）
 *
 * - `createBlock()` - 创建特定类型的格式化块
 * - `createSyntheticSpacingNodeBlock()` - 创建合成间距节点块
 * - `getSubBlocks()` - 获取子块列表
 * - `getSuperChildAttributes()` - 获取父类的子元素属性
 * - `isIncompleteInSuper()` - 判断父类是否未完成
 * - `getAlignmentForCaseBranch()` - 获取 case 分支对齐策略
 * - `getAlignment()` - 获取当前块的对齐对象
 * - `createAlignmentStrategy()` - 创建对齐策略
 * - `getNullAlignmentStrategy()` - 获取空对齐策略
 *
 * ## 已知限制
 *
 * 1. **格式化区域标记**：目前 `// @formatter:off/on` 的检测基于简单的文本匹配，
 *    可能在复杂嵌套结构中出现误判
 *
 * 2. **枚举格式化**：枚举的 `|` 分隔符格式化逻辑较为复杂，存在一些注释掉的实验性代码
 *
 * 3. **尾随逗号**：尾随逗号的支持依赖于多个条件判断，可能在边缘情况下行为不一致
 *
 * ## 调试建议
 *
 * ### 启用格式化调试
 *
 * 在 IntelliJ IDEA 中：
 * 1. Help → Diagnostic Tools → Debug Log Settings
 * 2. 添加：`#org.cangnova.cangjie.formatter`
 *
 * ### 常见问题排查
 *
 * - **缩进错误**：检查 `createChildIndent()` 和相关的 `IndentRules`
 * - **换行不符合预期**：检查 `getWrappingStrategy()` 返回的 `Wrap` 对象
 * - **对齐失效**：检查 `getChildrenAlignmentStrategy()` 和 `Alignment` 对象
 * - **格式化被跳过**：检查 `shouldFormat()` 和 `NO_FORMAT_NODES`
 *
 * ## 参考资料
 *
 * - [IntelliJ Platform SDK - Code Formatting](https://plugins.jetbrains.com/docs/intellij/code-formatting.html)
 * - [Kotlin Plugin Formatter 实现](https://github.com/JetBrains/intellij-community/tree/master/plugins/kotlin/formatter)
 *
 * @see CangJieSpacingBuilder 间距计算
 * @see IndentRules 缩进规则集合
 * @see CommonAlignmentStrategy 对齐策略
 * @see cangjieSpacingRules 间距规则定义
 */
package org.cangnova.cangjie.formatter

import org.cangnova.cangjie.formatter.indent.IndentRules
import org.cangnova.cangjie.formatter.util.TrailingCommaHelper.trailingCommaExistsOrCanExist
import org.cangnova.cangjie.formatter.util.addTrailingCommaIsAllowedFor
import org.cangnova.cangjie.lexer.CjTokens.*
import org.cangnova.cangjie.psi.*
import org.cangnova.cangjie.psi.CjNodeTypes.*
import org.cangnova.cangjie.lexer.cdoc.lexer.CDocTokens
import org.cangnova.cangjie.lexer.cdoc.parser.CDocElementTypes
import org.cangnova.cangjie.psi.psiUtil.*
import org.cangnova.cangjie.utils.safeAs
import com.intellij.formatting.*
import com.intellij.lang.ASTNode
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiElement
import com.intellij.psi.TokenType
import com.intellij.psi.codeStyle.CodeStyleSettings
import com.intellij.psi.codeStyle.CommonCodeStyleSettings
import com.intellij.psi.impl.source.tree.TreeUtil
import com.intellij.psi.tree.IElementType
import com.intellij.psi.tree.TokenSet


internal val QUALIFIED_OPERATION = TokenSet.create(DOT)
internal val QUALIFIED_EXPRESSIONS = TokenSet.create(DOT_QUALIFIED_EXPRESSION)
internal val ELVIS_SET = TokenSet.create()
internal val QUALIFIED_EXPRESSIONS_WITHOUT_WRAP = TokenSet.create(IMPORT_DIRECTIVE, PACKAGE_DIRECTIVE)
internal val COMMENTS = TokenSet.create(BLOCK_COMMENT, DOC_COMMENT)

internal const val CDOC_COMMENT_INDENT = 1

internal val BINARY_EXPRESSIONS = TokenSet.create(BINARY_EXPRESSION, BINARY_WITH_TYPE, IS_EXPRESSION)
internal val CDOC_CONTENT = TokenSet.create(CDocTokens.CDOC, CDocElementTypes.CDOC_SECTION, CDocElementTypes.CDOC_TAG)

internal val CODE_BLOCKS = TokenSet.create(BLOCK, CLASS_BODY, FUNCTION_LITERAL, PROPERTY_BODY)

internal val ALIGN_FOR_BINARY_OPERATIONS =
    TokenSet.create(MUL, DIV, PERC, PLUS, MINUS, LT, GT, LTEQ, GTEQ, ANDAND, OROR)
internal val ANNOTATIONS = TokenSet.create()

typealias WrappingStrategy = (childElement: ASTNode) -> Wrap?


internal val NO_FORMAT_NODES = listOf(

    QUOTE_EXPRESSION
)

fun noWrapping(@Suppress("UNUSED_PARAMETER") childElement: ASTNode): Wrap? = null

abstract class CangJieCommonBlock(
    private val node: ASTNode,
    private val settings: CodeStyleSettings,
    private val spacingBuilder: CangJieSpacingBuilder,
    private val alignmentStrategy: CommonAlignmentStrategy,
    private val overrideChildren: Sequence<ASTNode>? = null,
) {
    @Volatile
    private var mySubBlocks: List<ASTBlock>? = null

    fun getTextRange(): TextRange {
        if (overrideChildren != null) {
            return TextRange(overrideChildren.first().startOffset, overrideChildren.last().textRange.endOffset)
        }
        return node.textRange
    }

    protected abstract fun createBlock(
        node: ASTNode,
        alignmentStrategy: CommonAlignmentStrategy,
        indent: Indent?,
        wrap: Wrap?,
        settings: CodeStyleSettings,
        spacingBuilder: CangJieSpacingBuilder,
        overrideChildren: Sequence<ASTNode>? = null,
    ): ASTBlock

    protected abstract fun createSyntheticSpacingNodeBlock(node: ASTNode): ASTBlock

    protected abstract fun getSubBlocks(): List<Block>

    protected abstract fun getSuperChildAttributes(newChildIndex: Int): ChildAttributes

    protected abstract fun isIncompleteInSuper(): Boolean

    protected abstract fun getAlignmentForCaseBranch(shouldAlignInColumns: Boolean): CommonAlignmentStrategy

    protected abstract fun getAlignment(): Alignment?

    protected abstract fun createAlignmentStrategy(
        alignOption: Boolean, defaultAlignment: Alignment?
    ): CommonAlignmentStrategy

    protected abstract fun getNullAlignmentStrategy(): CommonAlignmentStrategy

    fun isLeaf(): Boolean = node.firstChildNode == null

    fun isIncomplete(): Boolean {
        if (isIncompleteInSuper()) {
            return true
        }


        return node.elementType == MODIFIER_LIST && node.treeParent?.elementType == CLASS_BODY
    }

    fun buildChildren(): List<Block> {
        // Check if this node should be formatted
        if (!shouldFormat(node)) {
            return emptyList()
        }

        if (node.elementType == QUOTE_EXPRESSION) {
            return emptyList()
        }

        if (mySubBlocks != null) {
            return mySubBlocks!!
        }

        var nodeSubBlocks = buildSubBlocks()

        if (node.elementType in QUALIFIED_EXPRESSIONS) {
            nodeSubBlocks = splitSubBlocksOnDot(nodeSubBlocks)
        } else {
            val psi = node.psi
            if (psi is CjBinaryExpression) {
                nodeSubBlocks = splitSubBlocksOnElvis(nodeSubBlocks)
            }
        }

        mySubBlocks = nodeSubBlocks

        return nodeSubBlocks
    }

    private fun shouldFormat(node: ASTNode): Boolean {
        // Check for special comments that indicate no formatting
        val prevComment = getPrevWithoutWhitespace(node)?.takeIf { it.elementType in COMMENTS }
        if (prevComment?.text?.trim()?.startsWith("// @formatter:off") == true) {
            return false
        }

        // Check if node is within a no-format region
        var current = node
        while (current.treeParent != null) {
            val parent = current.treeParent!!
            val parentComments = parent.getChildren(COMMENTS)
            for (comment in parentComments) {
                if (comment.text.trim().startsWith("// @formatter:off")) {
                    val offOffset = comment.startOffset
                    val onComment = parent.getChildren(COMMENTS).find { 
                        it.startOffset > offOffset && it.text.trim().startsWith("// @formatter:on")
                    }
                    val onOffset = onComment?.startOffset ?: Int.MAX_VALUE

                    if (node.startOffset in offOffset..onOffset) {
                        return false
                    }
                }
            }
            current = parent
        }

        // 检查父节点是否在不格式化列表中
        var parentNode = node.treeParent
        while (parentNode != null) {
            if (parentNode.elementType in NO_FORMAT_NODES) {
                return false
            }
            parentNode = parentNode.treeParent
        }

        return true
    }

    private fun splitSubBlocksOnDot(nodeSubBlocks: List<ASTBlock>): List<ASTBlock> {
        if (node.treeParent?.isQualifier == true || node.isCallChainWithoutWrap) return nodeSubBlocks

        val operationBlockIndex = nodeSubBlocks.indexOfBlockWithType(QUALIFIED_OPERATION)
        if (operationBlockIndex == -1) return nodeSubBlocks

        val block = nodeSubBlocks.first()
        val wrap = createWrapForQualifierExpression(node)
        val enforceIndentToChildren = anyCallInCallChainIsWrapped(node)
        val indent = createIndentForQualifierExpression(enforceIndentToChildren)
        val newBlock = block.processBlock(wrap, enforceIndentToChildren)
        return nodeSubBlocks.replaceBlock(newBlock, 0).splitAtIndex(operationBlockIndex, indent, wrap)
    }

    private fun ASTBlock.processBlock(wrap: Wrap?, enforceIndentToChildren: Boolean): ASTBlock {
        val currentNode = requireNode()
        val enforceIndent = enforceIndentToChildren && anyCallInCallChainIsWrapped(currentNode)
        val indent = createIndentForQualifierExpression(enforceIndent)

        @Suppress("UNCHECKED_CAST") val subBlocks = subBlocks as List<ASTBlock>
        val elementType = currentNode.elementType
        if (elementType != POSTFIX_EXPRESSION && elementType !in QUALIFIED_EXPRESSIONS) return this

        val index = 0
        val resultWrap =
            if (currentNode.wrapForFirstCallInChainIsAllowed) wrap ?: createWrapForQualifierExpression(currentNode)
            else null

        val newBlock = subBlocks.elementAt(index).processBlock(resultWrap, enforceIndent)
        return subBlocks.replaceBlock(newBlock, index).let {
            val operationIndex = subBlocks.indexOfBlockWithType(QUALIFIED_OPERATION)
            if (operationIndex != -1) it.splitAtIndex(operationIndex, indent, resultWrap)
            else it
        }.wrapToBlock(currentNode, this)
    }

    private fun List<ASTBlock>.replaceBlock(block: ASTBlock, index: Int = 0): List<ASTBlock> =
        toMutableList().apply { this[index] = block }

    private val ASTNode.wrapForFirstCallInChainIsAllowed: Boolean
        get() {
            if (unwrapQualifier()?.isCall != true) return false
            return settings.cangjieCommonSettings.WRAP_FIRST_METHOD_IN_CALL_CHAIN || receiverIsCall()
        }

    private fun createWrapForQualifierExpression(node: ASTNode): Wrap? =
        if (node.wrapForFirstCallInChainIsAllowed && node.receiverIsCall()) Wrap.createWrap(
            settings.cangjieCommonSettings.METHOD_CALL_CHAIN_WRAP,
            true,
        )
        else null


    private fun createIndentForQualifierExpression(enforceIndentToChildren: Boolean): Indent {
        val indentType = if (settings.cangjieCustomSettings.CONTINUATION_INDENT_FOR_CHAINED_CALLS) {
            if (enforceIndentToChildren) Indent.Type.CONTINUATION else Indent.Type.CONTINUATION_WITHOUT_FIRST
        } else {
            Indent.Type.NORMAL
        }

        return Indent.getIndent(
            indentType, false,
            enforceIndentToChildren,
        )
    }

    private fun List<ASTBlock>.wrapToBlock(
        anchor: ASTNode?,
        parentBlock: ASTBlock?,
    ): ASTBlock = splitAtIndex(0, null, null, anchor, parentBlock).single()

    private fun List<ASTBlock>.splitAtIndex(
        index: Int,
        indent: Indent?,
        wrap: Wrap?,
        anchor: ASTNode? = null,
        parentBlock: ASTBlock? = null,
    ): List<ASTBlock> {
        val operationBlock = this[index]
        val createParentSyntheticSpacingBlock: (ASTNode) -> ASTBlock = if (parentBlock != null) {
            { parentBlock }
        } else {
            {
                val parent = it.treeParent ?: node
                val skipOperationNodeParent = if (parent.elementType === OPERATION_REFERENCE) {
                    parent.treeParent ?: parent
                } else {
                    parent
                }
                createSyntheticSpacingNodeBlock(skipOperationNodeParent)
            }
        }

        val operationSyntheticBlock = SyntheticCangJieBlock(
            anchor ?: operationBlock.requireNode(),
            subList(index, size),
            null, indent, wrap, spacingBuilder, createParentSyntheticSpacingBlock,
        )

        return subList(0, index) + operationSyntheticBlock
    }

    private fun splitSubBlocksOnElvis(nodeSubBlocks: List<ASTBlock>): List<ASTBlock> {
        val elvisIndex = nodeSubBlocks.indexOfBlockWithType(ELVIS_SET)
        if (elvisIndex >= 0) {
            val indent = if (settings.cangjieCustomSettings.CONTINUATION_INDENT_IN_ELVIS) {
                Indent.getContinuationIndent()
            } else {
                Indent.getNormalIndent()
            }

            return nodeSubBlocks.splitAtIndex(
                elvisIndex,
                indent,
                null,
            )
        }

        return nodeSubBlocks
    }

    private fun createChildIndent(child: ASTNode): Indent? {
        val childParent = child.treeParent
        val childType = child.elementType

        if (childParent != null && isInCodeChunk(childParent)) {
            return Indent.getNoneIndent()
        }


        if (childParent != null && childParent.psi is CjDeclaration) {
            val prev = getPrevWithoutWhitespace(child)
            if (prev != null && COMMENTS.contains(prev.elementType) && getSiblingWithoutWhitespaceAndComments(prev) == null) {
                return Indent.getNoneIndent()
            }
        }

        // 使用IndentRules获取缩进规则
        for (strategy in IndentRules.getIndentRules()) {
            val indent = strategy.getIndent(child, settings)
            if (indent != null) {
                return indent
            }
        }


        if (childParent != null) {
            val parentType = childParent.elementType

            if (parentType === VALUE_PARAMETER_LIST || parentType === VALUE_ARGUMENT_LIST) {
                val prev = getPrevWithoutWhitespace(child)
                if (childType === RPAR && (prev == null || prev.elementType !== COMMA || !hasDoubleLineBreakBefore(child))) {
                    return Indent.getNoneIndent()
                }

                return if (settings.cangjieCustomSettings.CONTINUATION_INDENT_IN_ARGUMENT_LISTS) Indent.getContinuationWithoutFirstIndent()
                else Indent.getNormalIndent()
//                else Indent.getNoneIndent()
            }

            if (parentType === TYPE_PARAMETER_LIST || parentType === TYPE_ARGUMENT_LIST) {
                return Indent.getContinuationWithoutFirstIndent()
            }
        }

        return Indent.getNoneIndent()
    }

    private fun isInCodeChunk(node: ASTNode): Boolean {
        val parent = node.treeParent ?: return false

        if (node.elementType != BLOCK) {
            return false
        }

        val parentType = parent.elementType
        return parentType == EXPRESSION_CODE_FRAGMENT ||
                parentType == TYPE_CODE_FRAGMENT
//       || parentType == BLOCK_CODE_FRAGMENT
    }

    fun getChildAttributes(newChildIndex: Int): ChildAttributes {
        val type = node.elementType

        if (isInCodeChunk(node)) {
            return ChildAttributes(Indent.getNoneIndent(), null)
        }

        if (type == IF) {
            val elseBlock = mySubBlocks?.getOrNull(newChildIndex)
            if (elseBlock != null && elseBlock.requireNode().elementType == ELSE_KEYWORD) {
                return ChildAttributes.DELEGATE_TO_NEXT_CHILD
            }
        }

        if (newChildIndex > 0) {
            val prevBlock = mySubBlocks?.get(newChildIndex - 1)
            if (prevBlock?.node?.elementType == MODIFIER_LIST) {
                return ChildAttributes(Indent.getNoneIndent(), null)
            }
        }

        return when (type) {
            in CODE_BLOCKS, MATCH, IF, FOR, WHILE, DO_WHILE, MATCH_ENTRY -> ChildAttributes(
                Indent.getNormalIndent(),
                null,
            )

            TRY, CATCH, FINALLY -> ChildAttributes(Indent.getNoneIndent(), null)

            in QUALIFIED_EXPRESSIONS -> ChildAttributes(Indent.getContinuationWithoutFirstIndent(), null)

            VALUE_PARAMETER_LIST, VALUE_ARGUMENT_LIST -> {
                val subBlocks = getSubBlocks()
                if (newChildIndex != 1 && newChildIndex != 0 && newChildIndex < subBlocks.size) {
                    val block = subBlocks[newChildIndex]
                    ChildAttributes(block.indent, block.alignment)
                } else {
                    val indent =
                        if ((type == VALUE_PARAMETER_LIST && !settings.cangjieCustomSettings.CONTINUATION_INDENT_IN_PARAMETER_LISTS) || (type == VALUE_ARGUMENT_LIST && !settings.cangjieCustomSettings.CONTINUATION_INDENT_IN_ARGUMENT_LISTS)) {
                            Indent.getNormalIndent()
                        } else {
                            Indent.getContinuationIndent()
                        }

                    ChildAttributes(indent, null)
                }
            }

            DOC_COMMENT -> ChildAttributes(Indent.getSpaceIndent(CDOC_COMMENT_INDENT), null)

            PARENTHESIZED -> getSuperChildAttributes(newChildIndex)

            else -> {
                val blocks = getSubBlocks()
                if (newChildIndex != 0) {
                    val isIncomplete =
                        if (newChildIndex < blocks.size) blocks[newChildIndex - 1].isIncomplete else isIncompleteInSuper()
                    if (isIncomplete) {
                        if (blocks.size == newChildIndex && !settings.cangjieCustomSettings.CONTINUATION_INDENT_FOR_EXPRESSION_BODIES) {
                            val lastInParent = blocks.last()
                            if (lastInParent is ASTBlock && lastInParent.node?.elementType in ALL_ASSIGNMENTS) {
                                return ChildAttributes(Indent.getNormalIndent(), null)
                            }
                        }

                        return getSuperChildAttributes(newChildIndex)
                    }
                }

                if (blocks.size > newChildIndex) {
                    val block = blocks[newChildIndex]
                    return ChildAttributes(block.indent, block.alignment)
                }

                ChildAttributes(Indent.getNoneIndent(), null)
            }
        }
    }

    private fun getChildrenAlignmentStrategy(): CommonAlignmentStrategy {
        val cangjieCommonSettings = settings.cangjieCommonSettings
        val cangjieCustomSettings = settings.cangjieCustomSettings
        val parentType = node.elementType
        return when {
            parentType === VALUE_PARAMETER_LIST -> getAlignmentForChildInParenthesis(
                cangjieCommonSettings.ALIGN_MULTILINE_PARAMETERS,
                VALUE_PARAMETER,
                cangjieCommonSettings.ALIGN_MULTILINE_METHOD_BRACKETS,
            )

            parentType === VALUE_ARGUMENT_LIST -> getAlignmentForChildInParenthesis(
                cangjieCommonSettings.ALIGN_MULTILINE_PARAMETERS_IN_CALLS,
                VALUE_ARGUMENT,
                cangjieCommonSettings.ALIGN_MULTILINE_METHOD_BRACKETS,
            )

            parentType === MATCH -> getAlignmentForCaseBranch(cangjieCustomSettings.ALIGN_IN_COLUMNS_CASE_BRANCH)

            parentType === MATCH_ENTRY -> alignmentStrategy

            parentType in BINARY_EXPRESSIONS && getOperationType(node) in ALIGN_FOR_BINARY_OPERATIONS -> createAlignmentStrategy(
                cangjieCommonSettings.ALIGN_MULTILINE_BINARY_OPERATION,
                getAlignment()
            )

            parentType === SUPER_TYPE_LIST -> createAlignmentStrategy(
                cangjieCommonSettings.ALIGN_MULTILINE_EXTENDS_LIST,
                getAlignment()
            )

            parentType === PARENTHESIZED -> object : CommonAlignmentStrategy() {
                private var bracketsAlignment: Alignment? =
                    if (cangjieCommonSettings.ALIGN_MULTILINE_BINARY_OPERATION) Alignment.createAlignment() else null

                override fun getAlignment(node: ASTNode): Alignment? {
                    val childNodeType = node.elementType
                    val prev = getPrevWithoutWhitespace(node)

                    if (prev != null && prev.elementType === TokenType.ERROR_ELEMENT || childNodeType === TokenType.ERROR_ELEMENT) {
                        return bracketsAlignment
                    }

                    if (childNodeType === LPAR || childNodeType === RPAR) {
                        return bracketsAlignment
                    }

                    return null
                }
            }

            parentType == TYPE_CONSTRAINT_LIST -> createAlignmentStrategy(true, getAlignment())

            else -> getNullAlignmentStrategy()
        }
    }


    private fun buildSubBlock(
        child: ASTNode,
        alignmentStrategy: CommonAlignmentStrategy,
        wrappingStrategy: WrappingStrategy,
        overrideChildren: Sequence<ASTNode>? = null,
    ): ASTBlock {
        val childWrap = wrappingStrategy(child)


        if (child.elementType === OPERATION_REFERENCE) {
            val operationNode = child.firstChildNode

            if (operationNode != null) {
                return createBlock(
                    operationNode,
                    alignmentStrategy,
                    createChildIndent(child),
                    childWrap,
                    settings,
                    spacingBuilder,
                    overrideChildren,
                )
            }
        }

        return createBlock(
            child, alignmentStrategy, createChildIndent(child), childWrap, settings, spacingBuilder, overrideChildren
        )
    }

    private fun buildSubBlocks(): List<ASTBlock> {
        val childrenAlignmentStrategy = getChildrenAlignmentStrategy()
        val wrappingStrategy = getWrappingStrategy()

        val childNodes = when {
            overrideChildren != null -> overrideChildren
            node.elementType == BINARY_EXPRESSION -> {
                val binaryExpression = node.psi as? CjBinaryExpression
                if (binaryExpression != null && ALL_ASSIGNMENTS.contains(binaryExpression.operationToken)) {
                    node.children()
                } else {
                    val binaryExpressionChildren = mutableListOf<ASTNode>()
                    collectBinaryExpressionChildren(node, binaryExpressionChildren)
                    binaryExpressionChildren.asSequence()
                }
            }

            else -> node.children()
        }

        return childNodes.filter { it.textRange.length > 0 && it.elementType != TokenType.WHITE_SPACE }
            .flatMap { buildSubBlocksForChildNode(it, childrenAlignmentStrategy, wrappingStrategy) }.toList()
    }

    private fun buildSubBlocksForChildNode(
        node: ASTNode,
        childrenAlignmentStrategy: CommonAlignmentStrategy,
        wrappingStrategy: WrappingStrategy,
    ): Sequence<ASTBlock> {
        if (node.elementType == FUNC) {
            val filteredChildren = node.children().filter {
                it.textRange.length > 0 && it.elementType != TokenType.WHITE_SPACE
            }
            val significantChildren = filteredChildren.dropWhile { it.elementType == EOL_COMMENT }
            val funIndent = extractIndent(significantChildren.first())
            val eolComments = filteredChildren.takeWhile {
                it.elementType == EOL_COMMENT && extractIndent(it) != funIndent
            }.toList()
            val remainingChildren = filteredChildren.drop(eolComments.size)

            val blocks =
                eolComments.map { buildSubBlock(it, childrenAlignmentStrategy, wrappingStrategy) } + sequenceOf(
                    buildSubBlock(node, childrenAlignmentStrategy, wrappingStrategy, remainingChildren)
                )
            val blockList = blocks.toList()
            return blockList.asSequence()
        }

        return sequenceOf(buildSubBlock(node, childrenAlignmentStrategy, wrappingStrategy))
    }

    private fun collectBinaryExpressionChildren(node: ASTNode, result: MutableList<ASTNode>) {
        for (child in node.children()) {
            if (child.elementType == BINARY_EXPRESSION) {
                collectBinaryExpressionChildren(child, result)
            } else {
                result.add(child)
            }
        }
    }

    private fun getWrappingStrategy(): WrappingStrategy {
        val commonSettings = settings.cangjieCommonSettings
        val elementType = node.elementType
        val parentElementType = node.treeParent?.elementType
        val nodePsi = node.psi

        when {
            elementType === VALUE_ARGUMENT_LIST -> {
                val wrapSetting = commonSettings.CALL_PARAMETERS_WRAP
                if (!node.addTrailingComma && (wrapSetting == CommonCodeStyleSettings.WRAP_AS_NEEDED || wrapSetting == CommonCodeStyleSettings.WRAP_ON_EVERY_ITEM)

                ) {
                    return ::noWrapping
                }

                return getWrappingStrategyForItemList(
                    wrapSetting,
                    VALUE_ARGUMENT,
                    node.addTrailingComma,
                    additionalWrap = trailingCommaWrappingStrategyWithMultiLineCheck(LPAR, RPAR),
                )
            }

            elementType === VALUE_PARAMETER_LIST -> {
                when (parentElementType) {
                    FUNC, PRIMARY_CONSTRUCTOR, SECONDARY_CONSTRUCTOR -> return getWrappingStrategyForItemList(
                        commonSettings.METHOD_PARAMETERS_WRAP,
                        VALUE_PARAMETER,
                        node.addTrailingComma,
                        additionalWrap = trailingCommaWrappingStrategyWithMultiLineCheck(LPAR, RPAR),
                    )

                    FUNCTION_TYPE -> return defaultTrailingCommaWrappingStrategy(LPAR, RPAR)
                    FUNCTION_LITERAL -> if (trailingCommaExistsOrCanExist(nodePsi.parent, settings)) {
                        val check = thisOrPrevIsMultiLineElement(LBRACE, ARROW)
                        return { childElement ->
                            createWrapAlwaysIf(
                                getSiblingWithoutWhitespaceAndComments(childElement) == null || check(
                                    childElement
                                )
                            )
                        }
                    }
                }
            }

            elementType === FUNCTION_LITERAL -> if (trailingCommaExistsOrCanExist(nodePsi, settings)) {
                return trailingCommaWrappingStrategy(leftAnchor = LBRACE, rightAnchor = ARROW)
            }


            elementType === MATCH_ENTRY -> if (trailingCommaExistsOrCanExist(nodePsi, settings)) {
                val check = thisOrPrevIsMultiLineElement(LBRACE, ARROW)
                return trailingCommaWrappingStrategy(rightAnchor = ARROW) {
                    getSiblingWithoutWhitespaceAndComments(it, true) != null && check(it)
                }
            }

            // 解构声明已移除，统一使用模式匹配变量

            elementType === INDICES -> return defaultTrailingCommaWrappingStrategy(LBRACKET, RBRACKET)

            elementType === TYPE_PARAMETER_LIST -> return defaultTrailingCommaWrappingStrategy(LT, GT)

            elementType === TYPE_ARGUMENT_LIST -> return defaultTrailingCommaWrappingStrategy(LT, GT)

            elementType === COLLECTION_LITERAL_EXPRESSION -> return defaultTrailingCommaWrappingStrategy(
                LBRACKET, RBRACKET
            )

            elementType === SUPER_TYPE_LIST -> {
                val wrap = Wrap.createWrap(commonSettings.EXTENDS_LIST_WRAP, false)
                return { childElement -> if (childElement.psi is CjSuperTypeListEntry) wrap else null }
            }



            elementType === MODIFIER_LIST -> {
                when (val parent = node.treeParent.psi) {
                    is CjParameter -> return getWrappingStrategyForItemList(
                        commonSettings.PARAMETER_ANNOTATION_WRAP,
                        ANNOTATIONS,
                        !node.treeParent.isFirstParameter(),
                    )

                    is CjTypeStatement -> return getWrappingStrategyForItemList(
                        commonSettings.CLASS_ANNOTATION_WRAP,
                        ANNOTATIONS,
                    )

                    is CjNamedFunction, is CjSecondaryConstructor -> return getWrappingStrategyForItemList(
                        commonSettings.METHOD_ANNOTATION_WRAP,
                        ANNOTATIONS,
                    )

                    is CjVariable<*> -> return getWrappingStrategyForItemList(
                        when (parent) {
                            is CjPatternVariable -> if (parent.isLocal) commonSettings.VARIABLE_ANNOTATION_WRAP else commonSettings.FIELD_ANNOTATION_WRAP
                            else -> commonSettings.FIELD_ANNOTATION_WRAP
                        },
                        ANNOTATIONS,
                    )
                }
            }

            elementType === VALUE_PARAMETER -> return wrapAfterAnnotation(commonSettings.PARAMETER_ANNOTATION_WRAP)

            nodePsi is CjEnumBody -> return getWrappingStrategyForEnum(commonSettings.ENUM_CONSTANTS_WRAP)
//                return getWrappingStrategyForItemList(commonSettings.ENUM_CONSTANTS_WRAP, ENUM_CONSTRUCTOR)

            //TODO 别名
            nodePsi is CjTypeStatement -> return wrapAfterAnnotation(commonSettings.CLASS_ANNOTATION_WRAP)

            nodePsi is CjNamedFunction || nodePsi is CjSecondaryConstructor -> return wrap@{ childElement ->
                getWrapAfterAnnotation(childElement, commonSettings.METHOD_ANNOTATION_WRAP)?.let {
                    return@wrap it
                }

                if (getSiblingWithoutWhitespaceAndComments(childElement)?.elementType == EQ) {
                    Wrap.createWrap(settings.cangjieCustomSettings.WRAP_EXPRESSION_BODY_FUNCTIONS, true)
                } else {
                    null
                }
            }

            nodePsi is CjVariable<*> -> return wrap@{ childElement ->
                val wrapSetting = when (nodePsi) {
                    is CjPatternVariable -> if (nodePsi.isLocal) commonSettings.VARIABLE_ANNOTATION_WRAP else commonSettings.FIELD_ANNOTATION_WRAP
                    else -> commonSettings.FIELD_ANNOTATION_WRAP
                }
                getWrapAfterAnnotation(childElement, wrapSetting)?.let {
                    return@wrap it
                }

                if (getSiblingWithoutWhitespaceAndComments(childElement)?.elementType == EQ) {
                    Wrap.createWrap(settings.cangjieCommonSettings.ASSIGNMENT_WRAP, true)
                } else {
                    null
                }
            }

            nodePsi is CjProperty -> return wrap@{ childElement ->
                val wrapSetting = commonSettings.FIELD_ANNOTATION_WRAP
                getWrapAfterAnnotation(childElement, wrapSetting)?.let {
                    return@wrap it
                }
                if (getSiblingWithoutWhitespaceAndComments(childElement)?.elementType == EQ) {
                    return@wrap Wrap.createWrap(settings.cangjieCommonSettings.ASSIGNMENT_WRAP, true)
                }
                null
            }

            nodePsi is CjBinaryExpression -> {
                if (nodePsi.operationToken == EQ) {
                    return { childElement ->
                        if (getSiblingWithoutWhitespaceAndComments(childElement)?.elementType == OPERATION_REFERENCE) {
                            Wrap.createWrap(settings.cangjieCommonSettings.ASSIGNMENT_WRAP, true)
                        } else {
                            null
                        }
                    }
                }



                return ::noWrapping
            }
        }

        return ::noWrapping
    }

    private fun defaultTrailingCommaWrappingStrategy(
        leftAnchor: IElementType, rightAnchor: IElementType
    ): WrappingStrategy = fun(childElement: ASTNode): Wrap? =
        trailingCommaWrappingStrategyWithMultiLineCheck(leftAnchor, rightAnchor)(childElement)

    private val ASTNode.addTrailingComma: Boolean
        get() = (settings.cangjieCustomSettings.addTrailingCommaIsAllowedFor(this) || lastChildNode?.let {
            getSiblingWithoutWhitespaceAndComments(
                it
            )
        }?.elementType === COMMA) && psi?.let(PsiElement::isMultiline) == true


    private fun ASTNode.notDelimiterSiblingNodeInSequence(
        forward: Boolean,
        delimiterType: IElementType,
        typeOfLastElement: IElementType,
    ): ASTNode? {
        var sibling: ASTNode? = null
        for (element in siblings(forward).filter { it.elementType != WHITE_SPACE }
            .takeWhile { it.elementType != typeOfLastElement }) {
            val elementType = element.elementType
            if (!forward) {
                sibling = element
                if (elementType != delimiterType && elementType != EOL_COMMENT) break
            } else {
                if (elementType != EOL_COMMENT) break
                sibling = element
            }
        }

        return sibling
    }

    private fun thisOrPrevIsMultiLineElement(
        typeOfFirstElement: IElementType,
        typeOfLastElement: IElementType,
    ) = fun(childElement: ASTNode): Boolean {
        val delimiterType = COMMA
        when (childElement.elementType) {
            typeOfFirstElement,
            typeOfLastElement,
            delimiterType,
            EOL_COMMENT,
            in WHITESPACES,
                -> return false
        }

        val psi = childElement.psi ?: return false
        if (psi.isMultiline()) return true

        val startOffset =
            childElement.notDelimiterSiblingNodeInSequence(false, delimiterType, typeOfFirstElement)?.startOffset
                ?: psi.startOffset
        val endOffset =
            childElement.notDelimiterSiblingNodeInSequence(true, delimiterType, typeOfLastElement)?.psi?.endOffset
                ?: psi.endOffset

        return psi.parent.containsLineBreakInChild(startOffset, endOffset)
    }

    private fun trailingCommaWrappingStrategyWithMultiLineCheck(
        leftAnchor: IElementType,
        rightAnchor: IElementType,
    ) = trailingCommaWrappingStrategy(
        leftAnchor = leftAnchor,
        rightAnchor = rightAnchor,
        checkTrailingComma = true,
        additionalCheck = thisOrPrevIsMultiLineElement(leftAnchor, rightAnchor),
    )

    private fun trailingCommaWrappingStrategy(
        leftAnchor: IElementType? = null,
        rightAnchor: IElementType? = null,
        checkTrailingComma: Boolean = false,
        filter: (ASTNode) -> Boolean = { true },
        additionalCheck: (ASTNode) -> Boolean = { false },
    ): WrappingStrategy = fun(childElement: ASTNode): Wrap? {
        if (!filter(childElement)) return null
        val childElementType = childElement.elementType
        return createWrapAlwaysIf(
            (!checkTrailingComma || childElement.treeParent.addTrailingComma) && (rightAnchor != null && rightAnchor === childElementType || leftAnchor != null && leftAnchor === getPrevWithoutWhitespace(
                childElement
            )?.elementType || additionalCheck(childElement)),
        )
    }
}

private fun ASTNode.qualifierReceiver(): ASTNode? =
    unwrapQualifier()?.psi?.safeAs<CjQualifiedExpression>()?.receiverExpression?.node?.unwrapQualifier()

private tailrec fun ASTNode.unwrapQualifier(): ASTNode? {
    if (elementType in QUALIFIED_EXPRESSIONS) return this

    val psi = psi as? CjPostfixExpression ?: return null


    return psi.baseExpression?.node?.unwrapQualifier()
}

private fun ASTNode.receiverIsCall(): Boolean = qualifierReceiver()?.isCall == true

private val ASTNode.isCallChainWithoutWrap: Boolean
    get() {
        val callChainParent = parents().firstOrNull { !it.isQualifier } ?: return true
        return callChainParent.elementType in QUALIFIED_EXPRESSIONS_WITHOUT_WRAP
    }

private val ASTNode.isQualifier: Boolean
    get() {
        var currentNode: ASTNode? = this
        while (currentNode != null) {
            if (currentNode.elementType in QUALIFIED_EXPRESSIONS) return true

            currentNode = currentNode.treeParent
        }

        return false
    }

private val ASTNode.isCall: Boolean
    get() = unwrapQualifier()?.lastChildNode?.elementType == CALL_EXPRESSION

private fun anyCallInCallChainIsWrapped(node: ASTNode): Boolean {
    val sequentialNodes = generateSequence(node) {
        when (it.elementType) {
            POSTFIX_EXPRESSION, in QUALIFIED_EXPRESSIONS -> it.firstChildNode
            PARENTHESIZED -> getSiblingWithoutWhitespaceAndComments(it.firstChildNode, true)
            else -> null
        }
    }

    return sequentialNodes.any {
        val checkedElement = when (it.elementType) {
            in QUALIFIED_EXPRESSIONS -> it.findChildByType(QUALIFIED_OPERATION)
            PARENTHESIZED -> it.lastChildNode
            else -> null
        }

        checkedElement != null && hasLineBreakBefore(checkedElement)
    }
}

private fun ASTNode.isFirstParameter(): Boolean = treePrev?.elementType == LPAR

private fun wrapAfterAnnotation(wrapType: Int): WrappingStrategy =
    { childElement -> getWrapAfterAnnotation(childElement, wrapType) }

private fun getWrapAfterAnnotation(childElement: ASTNode, wrapType: Int): Wrap? {
    if (childElement.elementType in COMMENTS) return null
    var prevLeaf = childElement.treePrev
    while (prevLeaf?.elementType == TokenType.WHITE_SPACE) {
        prevLeaf = prevLeaf.treePrev
    }

    if (prevLeaf?.elementType == MODIFIER_LIST) {
        if (prevLeaf.lastChildNode?.elementType in ANNOTATIONS) {
            return Wrap.createWrap(wrapType, true)
        }
    }

    return null
}


private fun hasLineBreakBefore(node: ASTNode): Boolean {
    val prevSibling = node.leaves(false).dropWhile { it.psi is PsiComment }.firstOrNull()

    return prevSibling?.elementType == TokenType.WHITE_SPACE && prevSibling.textContains('\n') == true
}

private fun hasDoubleLineBreakBefore(node: ASTNode): Boolean {
    val prevSibling = node.leaves(false).firstOrNull() ?: return false

    return prevSibling.text.count { it == '\n' } >= 2
}

fun NodeIndentStrategy.PositionStrategy.continuationIf(
    option: (CangJieCodeStyleSettings) -> Boolean,
    indentFirst: Boolean = false,
): NodeIndentStrategy = set { settings ->
    if (option(settings.cangjieCustomSettings)) {
        if (indentFirst) Indent.getContinuationIndent()
        else Indent.getContinuationWithoutFirstIndent()
    } else Indent.getNormalIndent()
}

private fun getOperationType(node: ASTNode): IElementType? =
    node.findChildByType(OPERATION_REFERENCE)?.firstChildNode?.elementType

fun hasErrorElementBefore(node: ASTNode): Boolean {
    val prevSibling = getPrevWithoutWhitespace(node) ?: return false
    if (prevSibling.elementType == TokenType.ERROR_ELEMENT) return true
    val lastChild = TreeUtil.getLastChild(prevSibling)
    return lastChild?.elementType == TokenType.ERROR_ELEMENT
}


fun ASTNode.suppressBinaryExpressionIndent(): Boolean {
    var psi = psi.parent as? CjBinaryExpression ?: return false
    while (psi.parent is CjBinaryExpression) {
        psi = psi.parent as CjBinaryExpression
    }

    return psi.parent?.node?.elementType == CONDITION
}

private fun getAlignmentForChildInParenthesis(
    shouldAlignChild: Boolean,
    parameter: IElementType,
    shouldAlignParenthesis: Boolean,
): CommonAlignmentStrategy {
    val delimiter = COMMA
    val openBracket = LPAR
    val closeBracket = RPAR
    val parameterAlignment = if (shouldAlignChild) Alignment.createAlignment() else null
    val bracketsAlignment = if (shouldAlignParenthesis) Alignment.createAlignment() else null

    return object : CommonAlignmentStrategy() {
        override fun getAlignment(node: ASTNode): Alignment? {
            val childNodeType = node.elementType

            val prev = getPrevWithoutWhitespace(node)
            val hasTrailingComma = childNodeType === closeBracket && prev?.elementType == COMMA

            if (hasTrailingComma && hasDoubleLineBreakBefore(node)) {

                return parameterAlignment
            }

            if (childNodeType === openBracket || childNodeType === closeBracket) {
                return bracketsAlignment
            }

            if (childNodeType === parameter || childNodeType === delimiter || childNodeType === BLOCK_COMMENT || childNodeType === DOC_COMMENT) {
                return parameterAlignment
            }

            return null
        }
    }
}


private fun getSiblingWithoutWhitespaceAndComments(pNode: ASTNode, forward: Boolean = false): ASTNode? {
    return pNode.siblings(forward = forward).firstOrNull {
        it.elementType != TokenType.WHITE_SPACE && it.elementType !in COMMENTS
    }
}

private fun getWrappingStrategyForEnum(
    wrapType: Int,

    wrapFirstElement: Boolean = false,
    additionalWrap: WrappingStrategy? = null,
): WrappingStrategy {
    val itemWrap = Wrap.createWrap(wrapType, wrapFirstElement)
    return { childElement ->


        additionalWrap?.invoke(childElement)
            ?: if (childElement.elementType == ENUM_CONSTRUCTOR && (childElement.prev()?.elementType == OR || childElement.prev()
                    ?.prev()?.elementType == OR)
            ) {
//                Wrap.createWrap(CommonCodeStyleSettings.DO_NOT_WRAP, wrapFirstElement)
//                if (childElement.elementType == OR) {
//                    if (childElement.treeNext.elementType == WHITE_SPACE && childElement.treeNext.treeNext.elementType == ENUM_CONSTRUCTOR) {
//                        val psi = childElement.treePrev.psi
////                WriteCommandAction.runWriteCommandAction(psi.project, Runnable {
//                        // 在这里修改 PSI 元素的文本
//                        if (psi.text != " ") {
//                            psi.replace(
//                                CjPsiFactory.contextual(psi).createWhiteSpace()
//                            )
//                        }
//
//
//                    }
//                }
                null

            }/* if (childElement.elementType == OR && (childElement.treeNext?.elementType == ENUM_CONSTRUCTOR || childElement.treeNext
                     ?.treeNext?.elementType == ENUM_CONSTRUCTOR) ) {
                 Wrap.createWrap(CommonCodeStyleSettings.DO_NOT_WRAP, wrapFirstElement)

             }*/ else {

                if (childElement.elementType === OR) {
                    itemWrap
                } else {


//           如果第一个枚举没有 | 也换行
                    if (childElement.elementType == ENUM_CONSTRUCTOR && (childElement.prev()?.elementType == LBRACE || childElement.prev()
                            ?.prev()?.elementType == LBRACE)
                    ) {
                        itemWrap

                    } else

                        null
                }
            }
    }
}

private fun getWrappingStrategyForItemList(
    wrapType: Int,
    itemType: IElementType,
    wrapFirstElement: Boolean = false,
    additionalWrap: WrappingStrategy? = null,
): WrappingStrategy {
    val itemWrap = Wrap.createWrap(wrapType, wrapFirstElement)
    return { childElement ->
        additionalWrap?.invoke(childElement) ?: if (childElement.elementType === itemType) itemWrap else null
    }
}

private fun getWrappingStrategyForItemList(
    wrapType: Int, itemTypes: TokenSet, wrapFirstElement: Boolean = false
): WrappingStrategy {
    val itemWrap = Wrap.createWrap(wrapType, wrapFirstElement)
    return { childElement ->
        val thisType = childElement.elementType
        val prevType = getPrevWithoutWhitespace(childElement)?.elementType
        if (thisType in itemTypes || prevType in itemTypes && thisType != EOL_COMMENT && prevType != EOL_COMMENT) itemWrap
        else null
    }
}

fun ASTNode.prev(): ASTNode? {
    var prev = treePrev
    while (prev != null && prev.elementType == TokenType.WHITE_SPACE) {
        prev = prev.treePrev
    }

    if (prev != null) return prev
    return if (treeParent != null) treeParent.prev() else null
}

private fun List<ASTBlock>.indexOfBlockWithType(tokenSet: TokenSet): Int =
    indexOfFirst { block -> block.node?.elementType in tokenSet }

private fun extractIndent(node: ASTNode): String {
    val prevNode = node.treePrev
    return if (prevNode?.elementType != TokenType.WHITE_SPACE) "" else prevNode.text.substringAfterLast(
        "\n", prevNode.text
    )
}

private fun createWrapAlwaysIf(option: Boolean): Wrap? = if (option) Wrap.createWrap(WrapType.ALWAYS, true) else null


fun getPrevWithoutWhitespace(pNode: ASTNode): ASTNode? {
    return pNode.siblings(forward = false).firstOrNull { it.elementType != TokenType.WHITE_SPACE }
}

 