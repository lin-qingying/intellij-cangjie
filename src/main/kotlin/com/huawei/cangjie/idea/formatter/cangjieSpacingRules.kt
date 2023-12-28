package com.huawei.cangjie.idea.formatter

import com.intellij.formatting.ASTBlock
import com.intellij.formatting.DependentSpacingRule
import com.intellij.formatting.Spacing
import com.intellij.lang.ASTNode
import com.intellij.openapi.util.TextRange
import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.PsiWhiteSpace
import com.intellij.psi.codeStyle.CodeStyleSettings
import com.intellij.psi.tree.IElementType
import com.intellij.psi.tree.TokenSet
import com.huawei.cangjie.lexer.CjTokens.*
import  com.huawei.cangjie.CjNodeTypes.*
import com.huawei.cangjie.psi.*
import com.huawei.cangjie.psi.psiUtil.children
import com.huawei.cangjie.psi.psiUtil.textRangeWithoutComments
import com.huawei.cangjie.psi.stubs.elements.CjStubElementTypes.CLASS_INITIALIZER
import com.intellij.formatting.SpacingBuilder
import com.intellij.util.text.TextRangeUtil

import com.intellij.formatting.SpacingBuilder.RuleBuilder


val MODIFIERS_LIST_ENTRIES = TokenSet.orSet(MODIFIER_KEYWORDS)

val EXTEND_COLON_ELEMENTS =
    TokenSet.create(TYPE_CONSTRAINT, CLASS, TYPE_PARAMETER, ENUM_ENTRY, SECONDARY_CONSTRUCTOR)

 val TYPE_COLON_ELEMENTS = TokenSet.create(PROPERTY, FUNC, VALUE_PARAMETER, DESTRUCTURING_DECLARATION_ENTRY, FUNCTION_LITERAL)


val DECLARATIONS = TokenSet.create(PROPERTY, FUNC, CLASS, ENUM_ENTRY, SECONDARY_CONSTRUCTOR, CLASS_INITIALIZER)
fun createSpacingBuilder(settings: CodeStyleSettings, builderUtil: CangJieSpacingBuilderUtil): CangJieSpacingBuilder {
    val cangjieCommonSettings = settings.cangjieCommonSettings
    val cangjieCustomSettings = settings.cangjieCustomSettings
    return rules(cangjieCommonSettings, builderUtil) {
        simple {

            between(IMPORT_DIRECTIVE, IMPORT_DIRECTIVE).lineBreakInCode()
            after(IMPORT_LIST).blankLines(1)
        }

        custom {
            fun commentSpacing(minSpaces: Int): Spacing {
                if (cangjieCommonSettings.KEEP_FIRST_COLUMN_COMMENT) {
                    return Spacing.createKeepingFirstColumnSpacing(
                        minSpaces,
                        Int.MAX_VALUE,
                        cangjieCommonSettings.KEEP_LINE_BREAKS,
                        cangjieCommonSettings.KEEP_BLANK_LINES_IN_CODE
                    )
                }
                return Spacing.createSpacing(
                    minSpaces,
                    Int.MAX_VALUE,
                    0,
                    cangjieCommonSettings.KEEP_LINE_BREAKS,
                    cangjieCommonSettings.KEEP_BLANK_LINES_IN_CODE
                )
            }


            inPosition(parent = null, left = EOL_COMMENT, right = EOL_COMMENT).customRule { _, _, right ->
                val nodeBeforeRight = right.requireNode().treePrev
                if (nodeBeforeRight is PsiWhiteSpace && !nodeBeforeRight.textContains('\n')) {
                    createSpacing(0, minLineFeeds = 1)
                } else {
                    null
                }
            }

            inPosition(right = BLOCK_COMMENT).spacing(
                Spacing.createSpacing(
                    0,
                    Int.MAX_VALUE,
                    0,
                    cangjieCommonSettings.KEEP_LINE_BREAKS,
                    cangjieCommonSettings.KEEP_BLANK_LINES_IN_CODE,
                )
            )

            inPosition(right = EOL_COMMENT).spacing(commentSpacing(1))
            inPosition(parent = FUNCTION_LITERAL, right = BLOCK).customRule { _, _, right ->
                when (right.node?.children()?.firstOrNull()?.elementType) {
                    BLOCK_COMMENT -> commentSpacing(0)
                    EOL_COMMENT -> commentSpacing(1)
                    else -> null
                }
            }
        }

        simple {

            after(PACKAGE_DIRECTIVE).blankLines(1)
        }

        custom {
            inPosition(leftSet = DECLARATIONS, rightSet = DECLARATIONS).customRule(fun(
                _: ASTBlock,
                _: ASTBlock,
                right: ASTBlock
            ): Spacing? {
                val node = right.node ?: return null
                val elementStart = node.startOfDeclaration() ?: return null
                return if (StringUtil.containsLineBreak(
                        node.text.subSequence(0, elementStart.startOffset - node.startOffset).trimStart()
                    )
                )
                    createSpacing(0, minLineFeeds = 1 + cangjieCustomSettings.BLANK_LINES_BEFORE_DECLARATION_WITH_COMMENT_OR_ANNOTATION_ON_SEPARATE_LINE)
                else
                    null
            })

            inPosition(left = CLASS, right = CLASS).emptyLinesIfLineBreakInLeft(1)

            inPosition(left = FUNC, right = FUNC).emptyLinesIfLineBreakInLeft(1)
            inPosition(left = PROPERTY, right = FUNC).emptyLinesIfLineBreakInLeft(1)
            inPosition(left = FUNC, right = PROPERTY).emptyLinesIfLineBreakInLeft(1)
            inPosition(left = SECONDARY_CONSTRUCTOR, right = SECONDARY_CONSTRUCTOR).emptyLinesIfLineBreakInLeft(1)



            inPosition(left = FUNC, right = CLASS).emptyLinesIfLineBreakInLeft(1)
            inPosition(left = ENUM_ENTRY, right = ENUM_ENTRY).emptyLinesIfLineBreakInLeft(
                emptyLines = 0, numberOfLineFeedsOtherwise = 0, numSpacesOtherwise = 1
            )

            inPosition(parent = CLASS_BODY, left = SEMICOLON).customRule { parent, _, right ->
                val cjlass = parent.requireNode().treeParent.psi as? CjClass ?: return@customRule null
//                if (cjlass.isEnum() && right.requireNode().elementType in DECLARATIONS) {
//                    createSpacing(0, minLineFeeds = 2, keepBlankLines = cangjieCommonSettings.KEEP_BLANK_LINES_IN_DECLARATIONS)
//                } else
                    null
            }

            inPosition(parent = CLASS_BODY, left = LBRACE).customRule { parent, left, right ->
                if (right.requireNode().elementType == RBRACE) {
                    return@customRule createSpacing(0)
                }
                val classBody = parent.requireNode().psi as CjClassBody
                val parentPsi = classBody.parent as? CjClassOrStruct ?: return@customRule null
                if (cangjieCommonSettings.BLANK_LINES_AFTER_CLASS_HEADER == 0  ) {
                    null
                } else {
                    val minLineFeeds = if (right.requireNode().elementType == FUNC || right.requireNode().elementType == PROPERTY)
                        cangjieCommonSettings.BLANK_LINES_AFTER_CLASS_HEADER + 1
                    else
                        0

                    builderUtil.createLineFeedDependentSpacing(
                        1,
                        1,
                        minLineFeeds,
                        commonCodeStyleSettings.KEEP_LINE_BREAKS,
                        commonCodeStyleSettings.KEEP_BLANK_LINES_IN_DECLARATIONS,
                        TextRange(parentPsi.textRange.startOffset, left.requireNode().psi.textRange.startOffset),
                        DependentSpacingRule(DependentSpacingRule.Trigger.HAS_LINE_FEEDS)
                            .registerData(
                                DependentSpacingRule.Anchor.MIN_LINE_FEEDS,
                                cangjieCommonSettings.BLANK_LINES_AFTER_CLASS_HEADER + 1
                            )
                    )
                }
            }

            val parameterWithDocCommentRule = { _: ASTBlock, _: ASTBlock, right: ASTBlock ->
                if (right.requireNode().firstChildNode.elementType == DOC_COMMENT) {
                    createSpacing(0, minLineFeeds = 1, keepLineBreaks = true, keepBlankLines = cangjieCommonSettings.KEEP_BLANK_LINES_IN_DECLARATIONS)
                } else {
                    null
                }
            }
            inPosition(parent = VALUE_PARAMETER_LIST, right = VALUE_PARAMETER).customRule(parameterWithDocCommentRule)

            inPosition(parent = PROPERTY, right = PROPERTY_ACCESSOR).customRule { parent, _, _ ->
                val startNode = parent.requireNode().let { it.startOfDeclaration() ?: it }
                Spacing.createDependentLFSpacing(
                    1, 1,
                    TextRange(startNode.startOffset, parent.textRange.endOffset),
                    false, 0
                )
            }

            inPosition(parent = VALUE_ARGUMENT_LIST, left = LPAR).customRule { parent, _, _ ->
                when {
                    cangjieCommonSettings.CALL_PARAMETERS_LPAREN_ON_NEXT_LINE   -> {
                        Spacing.createDependentLFSpacing(
                            0, 0,
                            excludeLambdas(parent),
                            commonCodeStyleSettings.KEEP_LINE_BREAKS,
                            commonCodeStyleSettings.KEEP_BLANK_LINES_IN_CODE
                        )
                    }

                    cangjieCustomSettings.ALLOW_TRAILING_COMMA -> null
                    else -> createSpacing(0)
                }
            }

            inPosition(parent = VALUE_ARGUMENT_LIST, right = RPAR).customRule { parent, left, _ ->
                when {
                    cangjieCommonSettings.CALL_PARAMETERS_RPAREN_ON_NEXT_LINE -> {
                        Spacing.createDependentLFSpacing(
                            0, 0,
                            excludeLambdas(parent),
                            commonCodeStyleSettings.KEEP_LINE_BREAKS,
                            commonCodeStyleSettings.KEEP_BLANK_LINES_IN_CODE
                        )
                    }

                    cangjieCustomSettings.ALLOW_TRAILING_COMMA -> null
                    left.requireNode().elementType == COMMA ->  createSpacing(1)
                    else -> createSpacing(0)
                }
            }

            inPosition(left = CONDITION, right = RPAR).customRule { _, left, _ ->
                if (cangjieCustomSettings.IF_RPAREN_ON_NEW_LINE) {
                    Spacing.createDependentLFSpacing(
                        0, 0,
                        excludeLambdas(left),
                        commonCodeStyleSettings.KEEP_LINE_BREAKS,
                        commonCodeStyleSettings.KEEP_BLANK_LINES_IN_CODE
                    )
                } else {
                    createSpacing(0)
                }
            }

            inPosition(left = VALUE_PARAMETER, right = COMMA).customRule { _, left, _ ->
                if (left.node?.lastChildNode?.elementType === EOL_COMMENT)
                    createSpacing(0, minLineFeeds = 1)
                else
                    null
            }

            inPosition(parent = LONG_STRING_TEMPLATE_ENTRY, right = LONG_TEMPLATE_ENTRY_END).lineBreakIfLineBreakInParent(0)
            inPosition(parent = LONG_STRING_TEMPLATE_ENTRY, left = LONG_TEMPLATE_ENTRY_START).lineBreakIfLineBreakInParent(0)
        }

        simple {

            before(DOC_COMMENT).lineBreakInCode()
            between(PROPERTY, PROPERTY).lineBreakInCode()

            between(CLASS, DECLARATIONS).blankLines(1)


            between(FUNC, DECLARATIONS).blankLines(1)


            between(PROPERTY, DECLARATIONS).blankLines(1)



            between(SECONDARY_CONSTRUCTOR, DECLARATIONS).blankLines(1)
            between(CLASS_INITIALIZER, DECLARATIONS).blankLines(1)

            // TYPEALIAS - TYPEALIAS is an exception
//            between(TYPEALIAS, DECLARATIONS).blankLines(1)
//            before(TYPEALIAS).lineBreakInCode()


            between(ENUM_ENTRY, DECLARATIONS).blankLines(1)

            between(ENUM_ENTRY, SEMICOLON).spaces(0)

            between(COMMA, SEMICOLON).lineBreakInCodeIf(cangjieCustomSettings.ALLOW_TRAILING_COMMA)

            beforeInside(FUNC, TokenSet.create(BODY, CLASS_BODY)).lineBreakInCode()
            beforeInside(SECONDARY_CONSTRUCTOR, TokenSet.create(BODY, CLASS_BODY)).lineBreakInCode()
            beforeInside(CLASS, TokenSet.create(BODY, CLASS_BODY)).lineBreakInCode()

            beforeInside(PROPERTY, MATCH).spaces(0)

            before(PROPERTY).lineBreakInCode()

            after(DOC_COMMENT).lineBreakInCode()




            between(EOL_COMMENT, COMMA).lineBreakInCode()
            before(COMMA).spacesNoLineBreak(if (cangjieCommonSettings.SPACE_BEFORE_COMMA) 1 else 0)
            after(COMMA).spaceIf(cangjieCommonSettings.SPACE_AFTER_COMMA)

            val spacesAroundAssignment = if (cangjieCommonSettings.SPACE_AROUND_ASSIGNMENT_OPERATORS) 1 else 0
            beforeInside(EQ, PROPERTY).spacesNoLineBreak(spacesAroundAssignment)
            beforeInside(EQ, FUNC).spacing(spacesAroundAssignment, spacesAroundAssignment, 0, false, 0)

            around(
                TokenSet.create(EQ, MULTEQ, DIVEQ, PLUSEQ, MINUSEQ, PERCEQ)
            ).spaceIf(cangjieCommonSettings.SPACE_AROUND_ASSIGNMENT_OPERATORS)
            around(TokenSet.create(ANDAND, OROR)).spaceIf(cangjieCommonSettings.SPACE_AROUND_LOGICAL_OPERATORS)
            around(TokenSet.create(EQEQ, EXCLEQ)).spaceIf(cangjieCommonSettings.SPACE_AROUND_EQUALITY_OPERATORS)
            aroundInside(
                TokenSet.create(LT, GT, LTEQ, GTEQ), BINARY_EXPRESSION
            ).spaceIf(cangjieCommonSettings.SPACE_AROUND_RELATIONAL_OPERATORS)
            aroundInside(TokenSet.create(PLUS, MINUS), BINARY_EXPRESSION).spaceIf(cangjieCommonSettings.SPACE_AROUND_ADDITIVE_OPERATORS)
            aroundInside(
                TokenSet.create(MUL, DIV, PERC), BINARY_EXPRESSION
            ).spaceIf(cangjieCommonSettings.SPACE_AROUND_MULTIPLICATIVE_OPERATORS)
            around(
                TokenSet.create(PLUSPLUS, MINUSMINUS, MINUS, PLUS, EXCL)
            ).spaceIf(cangjieCommonSettings.SPACE_AROUND_UNARY_OPERATOR)


            around(RANGE).spaceIf(cangjieCustomSettings.SPACE_AROUND_RANGE)

            after(MODIFIER_LIST).spaces(1)

            beforeInside(IDENTIFIER, CLASS).spaces(1)


//            after(TYPE_ALIAS_KEYWORD).spaces(1)
            after(CONST_KEYWORD).spaces(1)
            after(LET_KEYWORD).spaces(1)
            after(VAR_KEYWORD).spaces(1)
            betweenInside(TYPE_PARAMETER_LIST, IDENTIFIER, PROPERTY).spaces(1)
            betweenInside(TYPE_REFERENCE, DOT, PROPERTY).spacing(0, 0, 0, false, 0)
            betweenInside(DOT, IDENTIFIER, PROPERTY).spacing(0, 0, 0, false, 0)

            betweenInside(RETURN_KEYWORD, LABEL_QUALIFIER, RETURN).spaces(0)
            afterInside(RETURN_KEYWORD, RETURN).spaces(1)
            afterInside(LABEL_QUALIFIER, RETURN).spaces(1)



            betweenInside(FUNC_KEYWORD, VALUE_PARAMETER_LIST, FUNC).spacing(0, 0, 0, false, 0)
            after(FUNC_KEYWORD).spaces(1)
            betweenInside(TYPE_PARAMETER_LIST, TYPE_REFERENCE, FUNC).spaces(1)
            betweenInside(TYPE_PARAMETER_LIST, IDENTIFIER, FUNC).spaces(1)
            betweenInside(TYPE_REFERENCE, DOT, FUNC).spacing(0, 0, 0, false, 0)
            betweenInside(DOT, IDENTIFIER, FUNC).spacing(0, 0, 0, false, 0)
            afterInside(IDENTIFIER, FUNC).spacing(0, 0, 0, false, 0)
            aroundInside(DOT, USER_TYPE).spaces(0)

            around(AS_KEYWORD).spaces(1)

            around(IS_KEYWORD).spaces(1)

            around(IN_KEYWORD).spaces(1)

            aroundInside(IDENTIFIER, BINARY_EXPRESSION).spaces(1)

            after(THROW_KEYWORD).spacesNoLineBreak(1)



            custom {
                inPosition(right = PRIMARY_CONSTRUCTOR).customRule { _, _, r ->
                    val spacesCount = if (r.requireNode().findLeafElementAt(0)?.elementType != LPAR) 1 else 0
                    createSpacing(spacesCount, minLineFeeds = 0, keepLineBreaks = true, keepBlankLines = 0)
                }
            }


            betweenInside(IDENTIFIER, TYPE_PARAMETER_LIST, CLASS).spaces(0)

            beforeInside(DOT, DOT_QUALIFIED_EXPRESSION).spaces(0)
            afterInside(DOT, DOT_QUALIFIED_EXPRESSION).spacesNoLineBreak(0)


            between(MODIFIERS_LIST_ENTRIES, MODIFIERS_LIST_ENTRIES).spaces(1)

            after(LBRACKET).spaces(0)
            before(RBRACKET).spaces(0)

            afterInside(LPAR, VALUE_PARAMETER_LIST).spaces(0, cangjieCommonSettings.METHOD_PARAMETERS_LPAREN_ON_NEXT_LINE)
            beforeInside(RPAR, VALUE_PARAMETER_LIST).spaces(0, cangjieCommonSettings.METHOD_PARAMETERS_RPAREN_ON_NEXT_LINE)
            afterInside(LT, TYPE_PARAMETER_LIST).spaces(0)
            beforeInside(GT, TYPE_PARAMETER_LIST).spaces(0)
            afterInside(LT, TYPE_ARGUMENT_LIST).spaces(0)
            beforeInside(GT, TYPE_ARGUMENT_LIST).spaces(0)
            before(TYPE_ARGUMENT_LIST).spaces(0)

            after(LPAR).spaces(0)
            before(RPAR).spaces(0)

            betweenInside(FOR_KEYWORD, LPAR, FOR).spaceIf(cangjieCommonSettings.SPACE_BEFORE_FOR_PARENTHESES)
            betweenInside(IF_KEYWORD, LPAR, IF).spaceIf(cangjieCommonSettings.SPACE_BEFORE_IF_PARENTHESES)
            betweenInside(WHILE_KEYWORD, LPAR, WHILE).spaceIf(cangjieCommonSettings.SPACE_BEFORE_WHILE_PARENTHESES)
            betweenInside(WHILE_KEYWORD, LPAR, DO_WHILE).spaceIf(cangjieCommonSettings.SPACE_BEFORE_WHILE_PARENTHESES)
            betweenInside(MATCH_KEYWORD, LPAR, MATCH).spaceIf(cangjieCustomSettings.SPACE_BEFORE_MATCH_PARENTHESES)
            betweenInside(CATCH_KEYWORD, VALUE_PARAMETER_LIST, CATCH).spaceIf(cangjieCommonSettings.SPACE_BEFORE_CATCH_PARENTHESES)

            betweenInside(LPAR, VALUE_PARAMETER, FOR).spaces(0)
            betweenInside(LPAR, DESTRUCTURING_DECLARATION, FOR).spaces(0)
            betweenInside(LOOP_RANGE, RPAR, FOR).spaces(0)



            before(SEMICOLON).spaces(0)




            beforeInside(COLON, TYPE_COLON_ELEMENTS) { spaceIf(cangjieCustomSettings.SPACE_BEFORE_TYPE_COLON) }
            afterInside(COLON, TYPE_COLON_ELEMENTS) { spaceIf(cangjieCustomSettings.SPACE_AFTER_TYPE_COLON) }

            afterInside(COLON, EXTEND_COLON_ELEMENTS) { spaceIf(cangjieCustomSettings.SPACE_AFTER_EXTEND_COLON) }

            beforeInside(ARROW, FUNCTION_LITERAL).spaceIf(cangjieCustomSettings.SPACE_BEFORE_LAMBDA_ARROW)

            aroundInside(ARROW, FUNCTION_TYPE).spaceIf(cangjieCustomSettings.SPACE_AROUND_FUNCTION_TYPE_ARROW)

            before(VALUE_ARGUMENT_LIST).spaces(0)





            before(INDICES).spaces(0)
            before(WHERE_KEYWORD).spaces(1)

            afterInside(GET_KEYWORD, PROPERTY_ACCESSOR).spaces(0)
            afterInside(SET_KEYWORD, PROPERTY_ACCESSOR).spaces(0)
        }
        custom {

            fun CangJieSpacingBuilder.CustomSpacingBuilder.ruleForKeywordOnNewLine(
                shouldBeOnNewLine: Boolean,
                keyword: IElementType,
                parent: IElementType,
                afterBlockFilter: (wordParent: ASTNode, block: ASTNode) -> Boolean = { _, _ -> true }
            ) {
                if (shouldBeOnNewLine) {
                    inPosition(parent = parent, right = keyword)
                        .lineBreakIfLineBreakInParent(numSpacesOtherwise = 1, allowBlankLines = false)
                } else {
                    inPosition(parent = parent, right = keyword).customRule { _, _, right ->

                        val previousLeaf = builderUtil.getPreviousNonWhitespaceLeaf(right.requireNode())
                        val leftBlock = if (
                            previousLeaf != null &&
                            previousLeaf.elementType == RBRACE &&
                            previousLeaf.treeParent?.elementType == BLOCK
                        ) {
                            previousLeaf.treeParent!!
                        } else null

                        val removeLineBreaks = leftBlock != null && afterBlockFilter(right.node?.treeParent!!, leftBlock)
                        createSpacing(1, minLineFeeds = 0, keepLineBreaks = !removeLineBreaks, keepBlankLines = 0)
                    }
                }
            }

            ruleForKeywordOnNewLine(cangjieCommonSettings.ELSE_ON_NEW_LINE, keyword = ELSE_KEYWORD, parent = IF) { keywordParent, block ->
                block.treeParent?.elementType == THEN && block.treeParent?.treeParent == keywordParent
            }
            ruleForKeywordOnNewLine(
                cangjieCommonSettings.WHILE_ON_NEW_LINE,
                keyword = WHILE_KEYWORD,
                parent = DO_WHILE
            ) { keywordParent, block ->
                block.treeParent?.elementType == BODY && block.treeParent?.treeParent == keywordParent
            }
            ruleForKeywordOnNewLine(cangjieCommonSettings.CATCH_ON_NEW_LINE, keyword = CATCH, parent = TRY)
            ruleForKeywordOnNewLine(cangjieCommonSettings.FINALLY_ON_NEW_LINE, keyword = FINALLY, parent = TRY)


            fun spacingForLeftBrace(block: ASTNode?, blockType: IElementType = BLOCK): Spacing {
                if (block != null && block.elementType == blockType) {
                    val leftBrace = block.findChildByType(LBRACE)
                    if (leftBrace != null) {
                        val previousLeaf = builderUtil.getPreviousNonWhitespaceLeaf(leftBrace)
                        val isAfterEolComment = previousLeaf != null && (previousLeaf.elementType == EOL_COMMENT)
                        val keepLineBreaks = cangjieCustomSettings.LBRACE_ON_NEXT_LINE || isAfterEolComment
                        val minimumLF = if (cangjieCustomSettings.LBRACE_ON_NEXT_LINE) 1 else 0
                        return createSpacing(1, minLineFeeds = minimumLF, keepLineBreaks = keepLineBreaks, keepBlankLines = 0)
                    }
                }
                return createSpacing(1)
            }

            fun leftBraceRule(blockType: IElementType = BLOCK) = { _: ASTBlock, _: ASTBlock, right: ASTBlock ->
                spacingForLeftBrace(right.node, blockType)
            }

            val leftBraceRuleIfBlockIsWrapped = { _: ASTBlock, _: ASTBlock, right: ASTBlock ->
                spacingForLeftBrace(right.requireNode().firstChildNode)
            }


            inPosition(left = SEMICOLON).customRule { _, left, _ ->
                val nodeAfterLeft = left.requireNode().treeNext
                if (nodeAfterLeft is PsiWhiteSpace && !nodeAfterLeft.textContains('\n')) {
                    createSpacing(1)
                } else {
                    null
                }
            }

            inPosition(parent = IF, right = THEN).customRule(leftBraceRuleIfBlockIsWrapped)
            inPosition(parent = IF, right = ELSE).customRule(leftBraceRuleIfBlockIsWrapped)

            inPosition(parent = FOR, right = BODY).customRule(leftBraceRuleIfBlockIsWrapped)
            inPosition(parent = WHILE, right = BODY).customRule(leftBraceRuleIfBlockIsWrapped)
            inPosition(parent = DO_WHILE, right = BODY).customRule(leftBraceRuleIfBlockIsWrapped)

            inPosition(parent = TRY, right = BLOCK).customRule(leftBraceRule())
            inPosition(parent = CATCH, right = BLOCK).customRule(leftBraceRule())
            inPosition(parent = FINALLY, right = BLOCK).customRule(leftBraceRule())

            inPosition(parent = FUNC, right = BLOCK).customRule(leftBraceRule())
            inPosition(parent = SECONDARY_CONSTRUCTOR, right = BLOCK).customRule(leftBraceRule())
            inPosition(parent = CLASS_INITIALIZER, right = BLOCK).customRule(leftBraceRule())
            inPosition(parent = PROPERTY_ACCESSOR, right = BLOCK).customRule(leftBraceRule())

            inPosition(right = CLASS_BODY).customRule(leftBraceRule(blockType = CLASS_BODY))

            inPosition(left = MATCH_ENTRY, right = MATCH_ENTRY).customRule { _, left, right ->
                val blankLines = cangjieCustomSettings.BLANK_LINES_AROUND_BLOCK_MATCH_BRANCHES
                if (blankLines != 0) {
                    val leftEntry = left.requireNode().psi as CjMatchEntry
                    val rightEntry = right.requireNode().psi as CjMatchEntry
                    if (leftEntry.expression is CjBlockExpression || rightEntry.expression is CjBlockExpression) {
                        return@customRule createSpacing(0, minLineFeeds = blankLines + 1)
                    }
                }

                if (cangjieCustomSettings.LINE_BREAK_AFTER_MULTILINE_MATCH_ENTRY) {
                    builderUtil.createLineFeedDependentSpacing(
                        minSpaces = 0,
                        maxSpaces = 0,
                        minimumLineFeeds = 1,
                        keepLineBreaks = commonCodeStyleSettings.KEEP_LINE_BREAKS,
                        keepBlankLines = commonCodeStyleSettings.KEEP_BLANK_LINES_IN_CODE,
                        dependency = left.textRange,
                        rule = DependentSpacingRule(DependentSpacingRule.Trigger.HAS_LINE_FEEDS).registerData(
                            DependentSpacingRule.Anchor.MIN_LINE_FEEDS,
                            2,
                        ),
                    )
                } else {
                    createSpacing(0, minLineFeeds = 1)
                }
            }

            inPosition(parent = MATCH_ENTRY, right = BLOCK).customRule(leftBraceRule())
            inPosition(parent = MATCH, right = LBRACE).customRule { parent, _, _ ->
                spacingForLeftBrace(block = parent.requireNode(), blockType = MATCH)
            }

            inPosition(left = LBRACE, right = MATCH_ENTRY).customRule { _, _, _ ->
                createSpacing(0, minLineFeeds = 1)
            }

            val spacesInSimpleFunction = if (cangjieCustomSettings.INSERT_WHITESPACES_IN_SIMPLE_ONE_LINE_METHOD) 1 else 0
            inPosition(
                parent = FUNCTION_LITERAL,
                left = LBRACE,
                right = BLOCK
            ).lineBreakIfLineBreakInParent(numSpacesOtherwise = spacesInSimpleFunction)

            inPosition(
                parent = FUNCTION_LITERAL,
                left = ARROW,
                right = BLOCK
            ).lineBreakIfLineBreakInParent(numSpacesOtherwise = 1)

            inPosition(
                parent = FUNCTION_LITERAL,
                left = LBRACE,
                right = RBRACE
            ).spacing(createSpacing(minSpaces = 0, maxSpaces = 1))

            inPosition(
                parent = FUNCTION_LITERAL,
                right = RBRACE
            ).lineBreakIfLineBreakInParent(numSpacesOtherwise = spacesInSimpleFunction)

            inPosition(
                parent = FUNCTION_LITERAL,
                left = LBRACE
            ).customRule { _, _, right ->
                val rightNode = right.requireNode()
                val rightType = rightNode.elementType
                if (rightType == VALUE_PARAMETER_LIST) {
                    createSpacing(spacesInSimpleFunction, keepLineBreaks = false)
                } else {
                    createSpacing(spacesInSimpleFunction)
                }
            }

            inPosition(parent = CLASS_BODY, right = RBRACE).customRule { parent, _, _ ->
                cangjieCommonSettings.createSpaceBeforeRBrace(1, parent.textRange)
            }

            inPosition(parent = BLOCK, right = RBRACE).customRule(fun(block: ASTBlock, left: ASTBlock, _: ASTBlock): Spacing? {
                val psiElement = block.requireNode().treeParent.psi

                val empty = left.requireNode().elementType == LBRACE

                when (psiElement) {
                    is CjDeclarationWithBody -> if (psiElement.name != null && !empty) return null
                    is CjMatchEntry, is CjClassInitializer -> if (!empty) return null
                    else -> return null
                }

                val spaces = if (empty) 0 else spacesInSimpleFunction
                return cangjieCommonSettings.createSpaceBeforeRBrace(spaces, psiElement.textRangeWithoutComments)
            })

            inPosition(parent = BLOCK, left = LBRACE).customRule { parent, _, _ ->
                val psiElement = parent.requireNode().treeParent.psi
                val funNode = psiElement as? CjFunction ?: return@customRule null

                if (funNode.name != null) return@customRule null


                Spacing.createDependentLFSpacing(
                    spacesInSimpleFunction, spacesInSimpleFunction, funNode.textRangeWithoutComments,
                    cangjieCommonSettings.KEEP_LINE_BREAKS,
                    cangjieCommonSettings.KEEP_BLANK_LINES_IN_CODE
                )
            }

            inPosition(parentSet = EXTEND_COLON_ELEMENTS, left = PRIMARY_CONSTRUCTOR, right = COLON).customRule { _, left, _ ->
                val primaryConstructor = left.requireNode().psi as CjPrimaryConstructor
                val rightParenthesis = primaryConstructor.valueParameterList?.rightParenthesis
                val prevSibling = rightParenthesis?.prevSibling
                val spaces = if (cangjieCustomSettings.SPACE_BEFORE_EXTEND_COLON) 1 else 0

                if ((prevSibling as? PsiWhiteSpace)?.textContains('\n') == true || cangjieCommonSettings
                        .METHOD_PARAMETERS_RPAREN_ON_NEXT_LINE
                ) {
                    createSpacing(spaces, keepLineBreaks = false)
                } else {
                    createSpacing(spaces)
                }
            }

            inPosition(
                parent = CLASS_BODY,
                left = LBRACE,
                right = ENUM_ENTRY
            ).lineBreakIfLineBreakInParent(numSpacesOtherwise = 1)
        }

        simple {
            afterInside(LBRACE, BLOCK).lineBreakInCode()
            beforeInside(RBRACE, BLOCK).spacing(
                1, 0, 1,
                cangjieCommonSettings.KEEP_LINE_BREAKS,
                cangjieCommonSettings.KEEP_BLANK_LINES_BEFORE_RBRACE
            )
            between(LBRACE, ENUM_ENTRY).spacing(1, 0, 0, true, cangjieCommonSettings.KEEP_BLANK_LINES_IN_CODE)
            beforeInside(RBRACE, MATCH).lineBreakInCode()
            between(RPAR, BODY).spaces(1)

            aroundInside(ARROW, MATCH_ENTRY).spaceIf(cangjieCustomSettings.SPACE_AROUND_MATCH_ARROW)

            beforeInside(COLON, EXTEND_COLON_ELEMENTS) { spaceIf(cangjieCustomSettings.SPACE_BEFORE_EXTEND_COLON) }

            after(EOL_COMMENT).lineBreakInCode()
        }
    }
}
fun SpacingBuilder.RuleBuilder.spacesNoLineBreak(spaces: Int): SpacingBuilder? =
    spacing(spaces, spaces, 0, false, 0)

fun SpacingBuilder.afterInside(element: IElementType, tokenSet: TokenSet, spacingFun: RuleBuilder.() -> Unit) {
    tokenSet.types.forEach { inType -> afterInside(element, inType).spacingFun() }
}
fun SpacingBuilder.beforeInside(element: IElementType, tokenSet: TokenSet, spacingFun: RuleBuilder.() -> Unit) {
    tokenSet.types.forEach { inType -> beforeInside(element, inType).spacingFun() }
}

private fun excludeLambdas(parent: ASTBlock): List<TextRange> {
    val rangesToExclude = mutableListOf<TextRange>()
//    parent.requireNode().psi.accept(object : CjTreeVisitorVoid() {
//        override fun visitLambdaExpression(lambdaExpression: CjLambdaExpression) {
//            super.visitLambdaExpression(lambdaExpression)
//            rangesToExclude.add(lambdaExpression.textRange)
//        }
//
//        override fun visitObjectLiteralExpression(expression: CjObjectLiteralExpression) {
//            super.visitObjectLiteralExpression(expression)
//            rangesToExclude.add(expression.textRange)
//        }
//
//        override fun visitNamedFunction(function: CjNamedFunction) {
//            super.visitNamedFunction(function)
//            if (function.name == null) {
//                rangesToExclude.add(function.textRange)
//            }
//        }
//    })
    return TextRangeUtil.excludeRanges(parent.textRange, rangesToExclude).toList()
}
