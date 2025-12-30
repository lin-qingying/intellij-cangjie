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
import org.cangnova.cangjie.psi.CjNodeTypes
import org.cangnova.cangjie.psi.psiUtil.children
import org.cangnova.cangjie.psi.stubs.elements.CjModifierListElementType
import com.intellij.formatting.*
import com.intellij.formatting.DependentSpacingRule.Anchor
import com.intellij.formatting.DependentSpacingRule.Trigger
import com.intellij.lang.ASTNode
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiComment
import com.intellij.psi.codeStyle.CommonCodeStyleSettings
import com.intellij.psi.tree.IElementType
import com.intellij.psi.tree.TokenSet

import kotlin.math.max

fun CommonCodeStyleSettings.createSpaceBeforeRBrace(numSpacesOtherwise: Int, textRange: TextRange): Spacing? {
    return Spacing.createDependentLFSpacing(
        numSpacesOtherwise, numSpacesOtherwise, textRange,
        KEEP_LINE_BREAKS,
        KEEP_BLANK_LINES_BEFORE_RBRACE
    )
}

class CangJieSpacingBuilder(val commonCodeStyleSettings: CommonCodeStyleSettings, val spacingBuilderUtil: CangJieSpacingBuilderUtil) {
    private val builders = ArrayList<Builder>()

    private interface Builder {
        fun getSpacing(parent: ASTBlock, left: ASTBlock, right: ASTBlock): Spacing?
    }

    inner class BasicSpacingBuilder : SpacingBuilder(commonCodeStyleSettings), Builder {
        override fun getSpacing(parent: ASTBlock, left: ASTBlock, right: ASTBlock): Spacing? {
            return super.getSpacing(parent, left, right)
        }
    }

    private data class Condition(
        val parent: IElementType? = null,
        val left: IElementType? = null,
        val right: IElementType? = null,
        val parentSet: TokenSet? = null,
        val leftSet: TokenSet? = null,
        val rightSet: TokenSet? = null
    ) : (ASTBlock, ASTBlock, ASTBlock) -> Boolean {
        override fun invoke(p: ASTBlock, l: ASTBlock, r: ASTBlock): Boolean =
            (parent == null || p.requireNode().elementType == parent) &&
                    (left == null || l.requireNode().elementType == left) &&
                    (right == null || r.requireNode().elementType == right) &&
                    (parentSet == null || parentSet.contains(p.requireNode().elementType)) &&
                    (leftSet == null || leftSet.contains(l.requireNode().elementType)) &&
                    (rightSet == null || rightSet.contains(r.requireNode().elementType))
    }

    private data class Rule(
        val conditions: List<Condition>,
        val action: (ASTBlock, ASTBlock, ASTBlock) -> Spacing?
    ) : (ASTBlock, ASTBlock, ASTBlock) -> Spacing? {
        override fun invoke(p: ASTBlock, l: ASTBlock, r: ASTBlock): Spacing? =
            if (conditions.all { it(p, l, r) }) action(p, l, r) else null
    }

    inner class CustomSpacingBuilder : Builder {
        private val rules = ArrayList<Rule>()
        private var conditions = ArrayList<Condition>()

        override fun getSpacing(parent: ASTBlock, left: ASTBlock, right: ASTBlock): Spacing? {
            for (rule in rules) {
                val spacing = rule(parent, left, right)
                if (spacing != null) {
                    return spacing
                }
            }
            return null
        }

        fun inPosition(
            parent: IElementType? = null, left: IElementType? = null, right: IElementType? = null,
            parentSet: TokenSet? = null, leftSet: TokenSet? = null, rightSet: TokenSet? = null
        ): CustomSpacingBuilder {
            conditions.add(Condition(parent, left, right, parentSet, leftSet, rightSet))
            return this
        }

        fun lineBreakIfLineBreakInParent(numSpacesOtherwise: Int, allowBlankLines: Boolean = true) {
            newRule { p, _, _ ->
                Spacing.createDependentLFSpacing(
                    numSpacesOtherwise, numSpacesOtherwise, p.textRange,
                    commonCodeStyleSettings.KEEP_LINE_BREAKS,
                    if (allowBlankLines) commonCodeStyleSettings.KEEP_BLANK_LINES_IN_CODE else 0
                )
            }
        }

        fun emptyLinesIfLineBreakInLeft(emptyLines: Int, numberOfLineFeedsOtherwise: Int = 1, numSpacesOtherwise: Int = 0) {
            newRule { _: ASTBlock, left: ASTBlock, _: ASTBlock ->
                val lastChild = left.node?.psi?.lastChild
                val leftEndsWithComment = lastChild is PsiComment && lastChild.tokenType == CjTokens.EOL_COMMENT
                val dependentSpacingRule = DependentSpacingRule(Trigger.HAS_LINE_FEEDS).registerData(Anchor.MIN_LINE_FEEDS, emptyLines + 1)
                val textRange = left.node
                    ?.startOfDeclaration()
                    ?.startOffset
                    ?.let { TextRange.create(it, left.textRange.endOffset) }
                    ?: left.textRange

                spacingBuilderUtil.createLineFeedDependentSpacing(
                    numSpacesOtherwise,
                    numSpacesOtherwise,
                    if (leftEndsWithComment) max(1, numberOfLineFeedsOtherwise) else numberOfLineFeedsOtherwise,
                    commonCodeStyleSettings.KEEP_LINE_BREAKS,
                    commonCodeStyleSettings.KEEP_BLANK_LINES_IN_DECLARATIONS,
                    textRange,
                    dependentSpacingRule
                )
            }
        }

        fun spacing(spacing: Spacing) {
            newRule { _, _, _ -> spacing }
        }

        fun customRule(block: (parent: ASTBlock, left: ASTBlock, right: ASTBlock) -> Spacing?) {
            newRule(block)
        }

        private fun newRule(rule: (ASTBlock, ASTBlock, ASTBlock) -> Spacing?) {
            val savedConditions = ArrayList(conditions)
            rules.add(Rule(savedConditions, rule))
            conditions.clear()
        }
    }

    fun getSpacing(parent: Block, child1: Block?, child2: Block): Spacing? {
        if (parent !is ASTBlock || child1 !is ASTBlock || child2 !is ASTBlock) {
            return null
        }

        for (builder in builders) {
            val spacing = builder.getSpacing(parent, child1, child2)

            if (spacing != null) {

                if (child1.requireNode().elementType == CjTokens.EOL_COMMENT && spacing.toString().contains("minLineFeeds=0")) {
                    val isBeforeBlock =
                        child2.requireNode().elementType == CjNodeTypes.BLOCK || child2.requireNode().firstChildNode
                            ?.elementType == CjNodeTypes.BLOCK
                    val keepBlankLines = if (isBeforeBlock) 0 else commonCodeStyleSettings.KEEP_BLANK_LINES_IN_CODE
                    return createSpacing(0, minLineFeeds = 1, keepLineBreaks = true, keepBlankLines = keepBlankLines)
                }
                return spacing
            }
        }
        return null
    }

    fun simple(init: BasicSpacingBuilder.() -> Unit) {
        val builder = BasicSpacingBuilder()
        builder.init()
        builders.add(builder)
    }

    fun custom(init: CustomSpacingBuilder.() -> Unit) {
        val builder = CustomSpacingBuilder()
        builder.init()
        builders.add(builder)
    }

    fun createSpacing(
        minSpaces: Int,
        maxSpaces: Int = minSpaces,
        minLineFeeds: Int = 0,
        keepLineBreaks: Boolean = commonCodeStyleSettings.KEEP_LINE_BREAKS,
        keepBlankLines: Int = commonCodeStyleSettings.KEEP_BLANK_LINES_IN_CODE
    ): Spacing {
        return Spacing.createSpacing(minSpaces, maxSpaces, minLineFeeds, keepLineBreaks, keepBlankLines)
    }
}

interface CangJieSpacingBuilderUtil {
    fun createLineFeedDependentSpacing(
        minSpaces: Int,
        maxSpaces: Int,
        minimumLineFeeds: Int,
        keepLineBreaks: Boolean,
        keepBlankLines: Int,
        dependency: TextRange,
        rule: DependentSpacingRule
    ): Spacing

    fun getPreviousNonWhitespaceLeaf(node: ASTNode?): ASTNode?

    fun isWhitespaceOrEmpty(node: ASTNode?): Boolean
}

fun rules(
    commonCodeStyleSettings: CommonCodeStyleSettings,
    builderUtil: CangJieSpacingBuilderUtil,
    init: CangJieSpacingBuilder.() -> Unit
): CangJieSpacingBuilder {
    val builder = CangJieSpacingBuilder(commonCodeStyleSettings, builderUtil)
    builder.init()
    return builder
}

internal fun ASTNode.startOfDeclaration(): ASTNode? = children().firstOrNull {
    val elementType = it.elementType
    elementType !is CjModifierListElementType<*> && elementType !in CjTokens.WHITE_SPACE_OR_COMMENT_BIT_SET
}
