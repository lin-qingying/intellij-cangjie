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

package org.cangnova.cangjie.formatter

import org.cangnova.cangjie.lexer.CjTokens
import org.cangnova.cangjie.psi.CjNodeTypes.MATCH_ENTRY
import com.intellij.formatting.*
import com.intellij.formatting.alignment.AlignmentStrategy
import com.intellij.lang.ASTNode
import com.intellij.openapi.util.TextRange
import com.intellij.psi.codeStyle.CodeStyleSettings
import com.intellij.psi.formatter.FormatterUtil
import com.intellij.psi.formatter.common.AbstractBlock
import com.intellij.psi.tree.IElementType

class CangJieBlock(node: ASTNode,
                   private val myAlignmentStrategy: CommonAlignmentStrategy,
                   private val myIndent: Indent?,
                   wrap: Wrap?,
                   mySettings: CodeStyleSettings,
                   private val mySpacingBuilder: CangJieSpacingBuilder,
                   overrideChildren: Sequence<ASTNode>? = null)

: AbstractBlock(node, wrap, myAlignmentStrategy.getAlignment(node)) {
    private val cangjieDelegationBlock = object : CangJieCommonBlock(
        node, mySettings, mySpacingBuilder, myAlignmentStrategy, overrideChildren
    ) {
        override fun getNullAlignmentStrategy(): CommonAlignmentStrategy = NodeAlignmentStrategy.nullStrategy

        override fun createAlignmentStrategy(alignOption: Boolean, defaultAlignment: Alignment?): CommonAlignmentStrategy {
            return NodeAlignmentStrategy.fromTypes(AlignmentStrategy.wrap(createAlignment(alignOption, defaultAlignment)))
        }

        override fun getAlignmentForCaseBranch(shouldAlignInColumns: Boolean): CommonAlignmentStrategy {
            return if (shouldAlignInColumns) {
                NodeAlignmentStrategy.fromTypes(
                    AlignmentStrategy.createAlignmentPerTypeStrategy(listOf(CjTokens.DOUBLE_ARROW as IElementType), MATCH_ENTRY, true)
                )
            } else {
                NodeAlignmentStrategy.nullStrategy
            }
        }

        override fun getAlignment(): Alignment? = alignment

        override fun isIncompleteInSuper(): Boolean = super@CangJieBlock.isIncomplete()

        override fun getSuperChildAttributes(newChildIndex: Int): ChildAttributes = super@CangJieBlock.getChildAttributes(newChildIndex)

        override fun getSubBlocks(): List<Block> = subBlocks

        override fun createBlock(
            node: ASTNode,
            alignmentStrategy: CommonAlignmentStrategy,
            indent: Indent?,
            wrap: Wrap?,
            settings: CodeStyleSettings,
            spacingBuilder: CangJieSpacingBuilder,
            overrideChildren: Sequence<ASTNode>?
        ): ASTBlock {
            return CangJieBlock(
                node,
                alignmentStrategy,
                indent,
                wrap,
                mySettings,
                mySpacingBuilder,
                overrideChildren
            )
        }

        override fun createSyntheticSpacingNodeBlock(node: ASTNode): ASTBlock {
            return object : AbstractBlock(node, null, null) {
                override fun isLeaf(): Boolean = false
                override fun getSpacing(child1: Block?, child2: Block): Spacing? = null
                override fun buildChildren(): List<Block> = emptyList()
            }
        }
    }

    override fun getIndent(): Indent? = myIndent

    override fun buildChildren(): List<Block> = cangjieDelegationBlock.buildChildren()

    override fun getSpacing(child1: Block?, child2: Block): Spacing? = mySpacingBuilder.getSpacing(this, child1, child2)

    override fun getChildAttributes(newChildIndex: Int): ChildAttributes = cangjieDelegationBlock.getChildAttributes(newChildIndex)

    override fun isLeaf(): Boolean = cangjieDelegationBlock.isLeaf()

    override fun getTextRange() = cangjieDelegationBlock.getTextRange()

    override fun isIncomplete(): Boolean = cangjieDelegationBlock.isIncomplete()
}

object CangJieSpacingBuilderUtilImpl : CangJieSpacingBuilderUtil {
    override fun getPreviousNonWhitespaceLeaf(node: ASTNode?): ASTNode? {
        return FormatterUtil.getPreviousNonWhitespaceLeaf(node)
    }

    override fun isWhitespaceOrEmpty(node: ASTNode?): Boolean {
        return FormatterUtil.isWhitespaceOrEmpty(node)
    }

    override fun createLineFeedDependentSpacing(
        minSpaces: Int,
        maxSpaces: Int,
        minimumLineFeeds: Int,
        keepLineBreaks: Boolean,
        keepBlankLines: Int,
        dependency: TextRange,
        rule: DependentSpacingRule
    ): Spacing {
        return Spacing.createSpacing(
            minSpaces,
            maxSpaces,
            minimumLineFeeds,
            keepLineBreaks,
            keepBlankLines,
//            dependency
        )
//        TODO 兼容性问题
//        return object : DependantSpacingImpl(minSpaces, maxSpaces, dependency, keepLineBreaks, keepBlankLines, rule) {
//            override fun getMinLineFeeds(): Int {
//                val superMin = super.getMinLineFeeds()
//                return if (superMin == 0) minimumLineFeeds else superMin
//            }
//        }
    }
}

private fun createAlignment(alignOption: Boolean, defaultAlignment: Alignment?): Alignment? {
    return if (alignOption) createAlignmentOrDefault(null, defaultAlignment) else defaultAlignment
}

private fun createAlignmentOrDefault(base: Alignment?, defaultAlignment: Alignment?): Alignment? {
    return defaultAlignment ?: if (base == null) Alignment.createAlignment() else Alignment.createChildAlignment(base)
}

abstract class CommonAlignmentStrategy {
    abstract fun getAlignment(node: ASTNode): Alignment?
}
