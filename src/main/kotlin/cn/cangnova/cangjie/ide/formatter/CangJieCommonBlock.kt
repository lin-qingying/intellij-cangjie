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

package cn.cangnova.cangjie.ide.formatter

import cn.cangnova.cangjie.psi.CjNodeTypes.*
import cn.cangnova.cangjie.psi.cdoc.lexer.CDocTokens
import cn.cangnova.cangjie.psi.cdoc.parser.CDocElementTypes
import cn.cangnova.cangjie.ide.formatter.NodeIndentStrategy.Companion.strategy
import cn.cangnova.cangjie.ide.formatter.util.TrailingCommaHelper.trailingCommaExistsOrCanExist
import cn.cangnova.cangjie.ide.formatter.util.addTrailingCommaIsAllowedFor
import cn.cangnova.cangjie.lexer. CjTokens.*


import cn.cangnova.cangjie.psi.*
import cn.cangnova.cangjie.psi.psiUtil.*
import cn.cangnova.cangjie.utils.safeAs
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


private val QUALIFIED_OPERATION = TokenSet.create( DOT)
private val QUALIFIED_EXPRESSIONS = TokenSet.create( DOT_QUALIFIED_EXPRESSION)
private val ELVIS_SET = TokenSet.create()
private val QUALIFIED_EXPRESSIONS_WITHOUT_WRAP = TokenSet.create( IMPORT_DIRECTIVE,  PACKAGE_DIRECTIVE)

private const val CDOC_COMMENT_INDENT = 1

private val BINARY_EXPRESSIONS = TokenSet.create( BINARY_EXPRESSION,  BINARY_WITH_TYPE,  IS_EXPRESSION)
private val CDOC_CONTENT = TokenSet.create(CDocTokens.CDOC, CDocElementTypes.CDOC_SECTION, CDocElementTypes.CDOC_TAG)

private val CODE_BLOCKS = TokenSet.create( BLOCK,  CLASS_BODY,  FUNCTION_LITERAL,  PROPERTY_BODY)

private val ALIGN_FOR_BINARY_OPERATIONS = TokenSet.create( MUL,  DIV,  PERC,  PLUS,  MINUS,  LT,  GT,  LTEQ,  GTEQ,  ANDAND,  OROR)
private val ANNOTATIONS = TokenSet.create()

typealias WrappingStrategy = (childElement: ASTNode) -> Wrap?

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


        return node.elementType ==  MODIFIER_LIST && node.treeParent?.elementType ==   CLASS_BODY
    }

    fun buildChildren(): List<Block> {
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
        if (elementType !=  POSTFIX_EXPRESSION && elementType !in QUALIFIED_EXPRESSIONS) return this

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
                val skipOperationNodeParent = if (parent.elementType ===  OPERATION_REFERENCE) {
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
            if (prev != null &&  COMMENTS.contains(prev.elementType) && getSiblingWithoutWhitespaceAndComments(prev) == null) {
                return Indent.getNoneIndent()
            }
        }

        for (strategy in INDENT_RULES) {
            val indent = strategy.getIndent(child, settings)
            if (indent != null) {
                return indent
            }
        }


        if (childParent != null) {
            val parentType = childParent.elementType

            if (parentType === VALUE_PARAMETER_LIST || parentType === VALUE_ARGUMENT_LIST) {
                val prev = getPrevWithoutWhitespace(child)
                if (childType ===  RPAR && (prev == null || prev.elementType !==  COMMA || !hasDoubleLineBreakBefore(child))) {
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

        if (node.elementType !=   BLOCK) {
            return false
        }

        val parentType = parent.elementType
        return parentType == EXPRESSION_CODE_FRAGMENT ||
                parentType ==  TYPE_CODE_FRAGMENT
//       || parentType == BLOCK_CODE_FRAGMENT
    }

    fun getChildAttributes(newChildIndex: Int): ChildAttributes {
        val type = node.elementType

        if (isInCodeChunk(node)) {
            return ChildAttributes(Indent.getNoneIndent(), null)
        }

        if (type == IF) {
            val elseBlock = mySubBlocks?.getOrNull(newChildIndex)
            if (elseBlock != null && elseBlock.requireNode().elementType ==  ELSE_KEYWORD) {
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
            in CODE_BLOCKS,  MATCH, IF, FOR,  WHILE, DO_WHILE, MATCH_ENTRY -> ChildAttributes(
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
                            if (lastInParent is ASTBlock && lastInParent.node?.elementType in  ALL_ASSIGNMENTS) {
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
            parentType ===   VALUE_PARAMETER_LIST -> getAlignmentForChildInParenthesis(
                cangjieCommonSettings.ALIGN_MULTILINE_PARAMETERS,
                    VALUE_PARAMETER,
                cangjieCommonSettings.ALIGN_MULTILINE_METHOD_BRACKETS,
            )

            parentType ===  VALUE_ARGUMENT_LIST -> getAlignmentForChildInParenthesis(
                cangjieCommonSettings.ALIGN_MULTILINE_PARAMETERS_IN_CALLS,
                 VALUE_ARGUMENT,
                cangjieCommonSettings.ALIGN_MULTILINE_METHOD_BRACKETS,
            )

            parentType ===  MATCH -> getAlignmentForCaseBranch(cangjieCustomSettings.ALIGN_IN_COLUMNS_CASE_BRANCH)

            parentType ===   MATCH_ENTRY -> alignmentStrategy

            parentType in BINARY_EXPRESSIONS && getOperationType(node) in ALIGN_FOR_BINARY_OPERATIONS -> createAlignmentStrategy(
                cangjieCommonSettings.ALIGN_MULTILINE_BINARY_OPERATION,
                getAlignment()
            )

            parentType === SUPER_TYPE_LIST -> createAlignmentStrategy(
                cangjieCommonSettings.ALIGN_MULTILINE_EXTENDS_LIST,
                getAlignment()
            )

            parentType ===   PARENTHESIZED -> object : CommonAlignmentStrategy() {
                private var bracketsAlignment: Alignment? =
                    if (cangjieCommonSettings.ALIGN_MULTILINE_BINARY_OPERATION) Alignment.createAlignment() else null

                override fun getAlignment(node: ASTNode): Alignment? {
                    val childNodeType = node.elementType
                    val prev = getPrevWithoutWhitespace(node)

                    if (prev != null && prev.elementType === TokenType.ERROR_ELEMENT || childNodeType === TokenType.ERROR_ELEMENT) {
                        return bracketsAlignment
                    }

                    if (childNodeType ===  LPAR || childNodeType ===  RPAR) {
                        return bracketsAlignment
                    }

                    return null
                }
            }

            parentType ==  TYPE_CONSTRAINT_LIST -> createAlignmentStrategy(true, getAlignment())

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


//        if (child.elementType === OPERATION_REFERENCE) {
////            val operationNode = child.firstChildNode
//                        val operationNode = child
//            if (operationNode != null) {
//                return createBlock(
//                    operationNode,
//                    alignmentStrategy,
//                    createChildIndent(child),
//                    childWrap,
//                    settings,
//                    spacingBuilder,
//                    overrideChildren,
//                )
//            }
//        }

        return createBlock(
            child, alignmentStrategy, createChildIndent(child), childWrap, settings, spacingBuilder, overrideChildren
        )
    }

    private fun buildSubBlocks(): List<ASTBlock> {
        val childrenAlignmentStrategy = getChildrenAlignmentStrategy()
        val wrappingStrategy = getWrappingStrategy()

        val childNodes = when {
            overrideChildren != null -> overrideChildren
            node.elementType ==  BINARY_EXPRESSION -> {
                val binaryExpression = node.psi as? CjBinaryExpression
                if (binaryExpression != null &&  ALL_ASSIGNMENTS.contains(binaryExpression.operationToken)) {
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
        if (node.elementType ==  FUNC) {
            val filteredChildren = node.children().filter {
                it.textRange.length > 0 && it.elementType != TokenType.WHITE_SPACE
            }
            val significantChildren = filteredChildren.dropWhile { it.elementType ==  EOL_COMMENT }
            val funIndent = extractIndent(significantChildren.first())
            val eolComments = filteredChildren.takeWhile {
                it.elementType ==  EOL_COMMENT && extractIndent(it) != funIndent
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
            if (child.elementType ==   BINARY_EXPRESSION) {
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
            elementType ===   VALUE_ARGUMENT_LIST -> {
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

            elementType ===  VALUE_PARAMETER_LIST -> {
                when (parentElementType) {
                     FUNC,  PRIMARY_CONSTRUCTOR,   SECONDARY_CONSTRUCTOR -> return getWrappingStrategyForItemList(
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

            elementType === DESTRUCTURING_DECLARATION -> {
                nodePsi as CjDestructuringDeclaration
                if (nodePsi.letOrVarKeyword == null) return defaultTrailingCommaWrappingStrategy(LPAR, RPAR)
                if (trailingCommaExistsOrCanExist(nodePsi, settings)) {
                    val check = thisOrPrevIsMultiLineElement(LPAR, RPAR)
                    return trailingCommaWrappingStrategy(
                        leftAnchor = LPAR,
                        rightAnchor = RPAR,
                        filter = { it.elementType !== EQ }) {
                        getSiblingWithoutWhitespaceAndComments(it, true) != null && check(it)
                    }
                }
            }

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

//            elementType === CLASS_BODY -> return getWrappingStrategyForItemList(
//                commonSettings.ENUM_CONSTANTS_WRAP,
//                ENUM_ENTRY
//            )

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

                    is CjVariable -> return getWrappingStrategyForItemList(
                        if (parent.isLocal) commonSettings.VARIABLE_ANNOTATION_WRAP
                        else commonSettings.FIELD_ANNOTATION_WRAP,
                        ANNOTATIONS,
                    )
                }
            }

            elementType === VALUE_PARAMETER -> return wrapAfterAnnotation(commonSettings.PARAMETER_ANNOTATION_WRAP)

            nodePsi is CjEnumBody -> return getWrappingStrategyForEnum(commonSettings.ENUM_CONSTANTS_WRAP)
//                return getWrappingStrategyForItemList(commonSettings.ENUM_CONSTANTS_WRAP, ENUM_ENTRY)

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

            nodePsi is CjVariable -> return wrap@{ childElement ->
                val wrapSetting =
                    if (nodePsi.isLocal) commonSettings.VARIABLE_ANNOTATION_WRAP else commonSettings.FIELD_ANNOTATION_WRAP
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

//                if (  nodePsi.getStrictParentOfType<CjStringTemplateExpression>() == null) {
//                    return { childElement ->
//                        if (childElement.elementType == OPERATION_REFERENCE && (childElement.psi as? CjOperationReferenceExpression)?.operationSignTokenType == COALESCING) {
//                            Wrap.createWrap(settings.cangjieCustomSettings.WRAP_ELVIS_EXPRESSIONS, true)
//                        } else {
//                            null
//                        }
//                    }
//                }

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

//TODO 块相对于父级块的缩进
private val INDENT_RULES  = arrayOf(

    // 不对块中的右大括号和左大括号进行缩进
    strategy("No indent for braces in blocks").within(BLOCK, INIT_BLOCK, CLASS_BODY, FUNCTION_LITERAL, ENUM_BODY)
        .forType(RBRACE, LBRACE).set(Indent.getNoneIndent()),

    // 对块内容进行正常缩进
    strategy("Indent for block content").within(
        BLOCK,
        INIT_BLOCK,
        CLASS_BODY,
        FUNCTION_LITERAL,
        PROPERTY_BODY,
        ENUM_BODY
    )
        .notForType(RBRACE, LBRACE, BLOCK, ENUM_ENTRY).set(Indent.getNormalIndent()),

    // 对模板内容进行正常缩进
    strategy("Indent for template content").within(LONG_STRING_TEMPLATE_ENTRY)
        .notForType(LONG_TEMPLATE_ENTRY_START, LONG_TEMPLATE_ENTRY_END).set(Indent.getNormalIndent()),

    // 对模板中的大括号不进行缩进
    strategy("No indent for braces in template").within(LONG_STRING_TEMPLATE_ENTRY)
        .forType(LONG_TEMPLATE_ENTRY_START, LONG_TEMPLATE_ENTRY_END).set(Indent.getNoneIndent()),

    // 对属性访问器进行正常缩进
    strategy("Indent for property accessors").within(PROPERTY).forType(PROPERTY_BODY).set(Indent.getNormalIndent()),

    // 对 'for' 循环中的单一语句进行正常缩进
    strategy("For a single statement in 'for'").within(BODY).notForType(BLOCK).set(Indent.getNormalIndent()),

    // 对 'THEN' 和 'ELSE' 中的单一语句进行正常缩进
    strategy("For single statement in THEN and ELSE").within(THEN, ELSE).notForType(BLOCK)
        .set(Indent.getNormalIndent()),

    // 对表达式体应用缩进
    strategy("Expression body").within(FUNC).notForType(OPERATION_REFERENCE).forElement {
        (it.psi is CjExpression && it.psi !is CjBlockExpression)
    }.continuationIf(CangJieCodeStyleSettings::CONTINUATION_INDENT_FOR_EXPRESSION_BODIES, indentFirst = true),

    // 对表达式体位置的行注释应用缩进
    strategy("Line comment at expression body position").forElement { node ->
        val psi = node.psi
        val parent = psi.parent
        if (psi is PsiComment && parent is CjDeclarationWithInitializer) {
            psi.getNextSiblingIgnoringWhitespace() == parent.initializer
        } else {
            false
        }
    }.continuationIf(CangJieCodeStyleSettings::CONTINUATION_INDENT_FOR_EXPRESSION_BODIES, indentFirst = true),

    // 对 if 条件中的缩进类型进行动态设置
    strategy("If condition").within(CONDITION).set { settings ->
        val indentType =
            if (settings.cangjieCustomSettings.CONTINUATION_INDENT_IN_IF_CONDITIONS) Indent.Type.CONTINUATION
            else Indent.Type.NORMAL

        Indent.getIndent(indentType, false, true)
    },

    // 对属性访问器中的表达式体进行正常缩进
    strategy("Property accessor expression body").within(PROPERTY_ACCESSOR).forElement {
        it.psi is CjExpression && it.psi !is CjBlockExpression
    }.set(Indent.getNormalIndent()),

    // 对属性初始化器中的表达式进行连续缩进
    strategy("Property initializer").within(PROPERTY).forElement {
        it.psi is CjExpression
    }.continuationIf(CangJieCodeStyleSettings::CONTINUATION_INDENT_FOR_EXPRESSION_BODIES),

    // 对解构声明中的表达式应用缩进
    strategy("Destructuring declaration").within(DESTRUCTURING_DECLARATION).forElement {
        it.psi is CjExpression
    }.continuationIf(CangJieCodeStyleSettings::CONTINUATION_INDENT_FOR_EXPRESSION_BODIES),

    // 对赋值表达式的右侧部分应用缩进
    strategy("Assignment expressions").within(BINARY_EXPRESSION).within {
        val binaryExpression = it.psi as? CjBinaryExpression ?: return@within false
        ALL_ASSIGNMENTS.contains(binaryExpression.operationToken)
    }.forElement {
        val psi = it.psi
        val binaryExpression = psi?.parent as? CjBinaryExpression
        binaryExpression?.right == psi
    }.continuationIf(CangJieCodeStyleSettings::CONTINUATION_INDENT_FOR_EXPRESSION_BODIES),

    // 对块和函数等部分进行缩进，排除某些特定类型
    strategy("Indent for parts").within(
        PROPERTY,
        FUNC,
        DESTRUCTURING_DECLARATION,
        PRIMARY_CONSTRUCTOR,
        SECONDARY_CONSTRUCTOR,
        END_SECONDARY_CONSTRUCTOR
    ).notForType(
        BLOCK,
        INIT_BLOCK,
        FUNC_KEYWORD,
        CONST_KEYWORD,
        LET_KEYWORD,
        VAR_KEYWORD,
        INIT_KEYWORD,
        RPAR,
        EOL_COMMENT,
        CONTEXT_RECEIVER_LIST,
        MODIFIER_LIST
    ).set(Indent.getContinuationWithoutFirstIndent()),

    // 对链式调用进行缩进
    strategy("Chained calls").within(QUALIFIED_EXPRESSIONS)
        .forType(EOL_COMMENT, BLOCK_COMMENT, DOC_COMMENT, SHEBANG_COMMENT)
        .continuationIf(CangJieCodeStyleSettings::CONTINUATION_INDENT_FOR_CHAINED_CALLS),

    // 对继承列表进行缩进
    strategy("Delegation list").within(SUPER_TYPE_LIST)
        .continuationIf(CangJieCodeStyleSettings::CONTINUATION_INDENT_IN_SUPERTYPE_LISTS, indentFirst = true),

    // 对索引进行缩进，排除右中括号
    strategy("Indices").within(INDICES).notForType(RBRACKET)
        .continuationIf(CangJieCodeStyleSettings::CONTINUATION_INDENT_IN_ARGUMENT_LISTS),

    // 对二元表达式进行缩进
    strategy("Binary expressions").within(BINARY_EXPRESSIONS)
        .forElement { node -> !node.suppressBinaryExpressionIndent() }
        .set(Indent.getContinuationWithoutFirstIndent(false)),

    // 对括号中的表达式进行缩进
    strategy("Parenthesized expression").within(PARENTHESIZED).set(Indent.getContinuationWithoutFirstIndent(false)),

    // 对条件中的左括号应用缩进
    strategy("Opening parenthesis for conditions").forType(LPAR).within(IF, MATCH_ENTRY, WHILE, DO_WHILE, FOR, MATCH)
        .set(Indent.getContinuationWithoutFirstIndent(true)),

    // 对条件中的右括号应用不缩进
    strategy("Closing parenthesis for conditions").forType(RPAR)
        .forElement { node -> !hasErrorElementBefore(node) || node.prev()?.textLength == 0 }
        .within(IF, MATCH_ENTRY, WHILE, DO_WHILE, FOR, MATCH).set(Indent.getNoneIndent()),

    // 对不完整条件中的右括号应用连续缩进
    strategy("Closing parenthesis for incomplete conditions").forType(RPAR)
        .forElement { node -> hasErrorElementBefore(node) }.within(IF, MATCH_ENTRY, WHILE, DO_WHILE, FOR, MATCH)
        .set(Indent.getContinuationWithoutFirstIndent()),

    // 对 MATCH 内容进行正常缩进
    strategy("For MATCH content").within(MATCH).notForType(RBRACE, LBRACE, MATCH_KEYWORD).set(Indent.getNormalIndent()),

    // 对 CDoc 注释中的星号和结束标记进行缩进
    strategy("CDoc comment indent").within(CDOC_CONTENT).forType(CDocTokens.LEADING_ASTERISK, CDocTokens.END)
        .set(Indent.getSpaceIndent(CDOC_COMMENT_INDENT)),

    // 对参数列表中的元素应用缩进
    strategy("Parameter list").within(VALUE_PARAMETER_LIST)
        .forElement { it.elementType == VALUE_PARAMETER && it.psi.prevSibling != null }
        .continuationIf(CangJieCodeStyleSettings::CONTINUATION_INDENT_IN_PARAMETER_LISTS, indentFirst = true),

    // 对 WHERE 子句应用缩进
    strategy("Where clause").within(CLASS, EXTEND, STRUCT, ENUM, INTERFACE, FUNC, PROPERTY).forType(WHERE_KEYWORD)
        .set(Indent.getContinuationIndent()),

    // 对数组字面量中的元素进行缩进
    strategy("Array literals").within(COLLECTION_LITERAL_EXPRESSION).notForType(LBRACKET, RBRACKET)
        .set(Indent.getNormalIndent()),

    // 对默认参数值应用缩进
    strategy("Default parameter values").within(VALUE_PARAMETER)
        .forElement { node -> node.psi != null && node.psi == (node.psi.parent as? CjParameter)?.defaultValue }
        .continuationIf(CangJieCodeStyleSettings::CONTINUATION_INDENT_FOR_EXPRESSION_BODIES, indentFirst = true),
)


private fun getOperationType(node: ASTNode): IElementType? =
    node.findChildByType(OPERATION_REFERENCE)?.firstChildNode?.elementType

fun hasErrorElementBefore(node: ASTNode): Boolean {
    val prevSibling = getPrevWithoutWhitespace(node) ?: return false
    if (prevSibling.elementType == TokenType.ERROR_ELEMENT) return true
    val lastChild = TreeUtil.getLastChild(prevSibling)
    return lastChild?.elementType == TokenType.ERROR_ELEMENT
}


private fun ASTNode.suppressBinaryExpressionIndent(): Boolean {
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

private fun getPrevWithoutWhitespace(pNode: ASTNode): ASTNode? {
    return pNode.siblings(forward = false).firstOrNull { it.elementType != TokenType.WHITE_SPACE }
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
            ?: if (childElement.elementType == ENUM_ENTRY && (childElement.prev()?.elementType == OR || childElement.prev()
                    ?.prev()?.elementType == OR)
            ) {
//                Wrap.createWrap(CommonCodeStyleSettings.DO_NOT_WRAP, wrapFirstElement)
//                if (childElement.elementType == OR) {
//                    if (childElement.treeNext.elementType == WHITE_SPACE && childElement.treeNext.treeNext.elementType == ENUM_ENTRY) {
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

            }/* if (childElement.elementType == OR && (childElement.treeNext?.elementType == ENUM_ENTRY || childElement.treeNext
                     ?.treeNext?.elementType == ENUM_ENTRY) ) {
                 Wrap.createWrap(CommonCodeStyleSettings.DO_NOT_WRAP, wrapFirstElement)

             }*/ else {

                if (childElement.elementType === OR) {
                    itemWrap
                } else {


//           如果第一个枚举没有 | 也换行
                    if (childElement.elementType == ENUM_ENTRY && (childElement.prev()?.elementType == LBRACE || childElement.prev()
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

private fun ASTNode.prev(): ASTNode? {
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
