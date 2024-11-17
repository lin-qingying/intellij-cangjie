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

package com.linqingying.cangjie.ide.formatter.lineIndent

import com.linqingying.cangjie.lang.CangJieLanguage
import com.linqingying.cangjie.lexer.CjTokens
import com.intellij.formatting.Indent
import com.intellij.lang.Language
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.codeStyle.lineIndent.LineIndentProvider
import com.intellij.psi.impl.source.codeStyle.SemanticEditorPosition
import com.intellij.psi.impl.source.codeStyle.lineIndent.IndentCalculator
import com.intellij.psi.impl.source.codeStyle.lineIndent.JavaLikeLangLineIndentProvider
import com.intellij.psi.tree.IElementType
import com.intellij.util.text.CharArrayUtil
import com.intellij.psi.impl.source.codeStyle.lineIndent.JavaLikeLangLineIndentProvider.JavaLikeElement.*
import  com.linqingying.cangjie.ide.formatter.lineIndent.CangJieLangLineIndentProvider.CangJieElement.*

abstract class CangJieLangLineIndentProvider : JavaLikeLangLineIndentProvider() {
    abstract fun indentionSettings(editor: Editor): CangJieIndentationAdjuster

    override fun mapType(tokenType: IElementType): SemanticEditorPosition.SyntaxElement? = SYNTAX_MAP[tokenType]

    override fun isSuitableForLanguage(language: Language): Boolean = language.isKindOf(CangJieLanguage)

    override fun getLineIndent(project: Project, editor: Editor, language: Language?, offset: Int): String? {
        return if (offset > 0 && getPosition(editor, offset - 1).isAt(RegularStringPart))
            LineIndentProvider.DO_NOT_ADJUST
        else
            super.getLineIndent(project, editor, language, offset)
    }

    override fun getIndent(project: Project, editor: Editor, language: Language?, offset: Int): IndentCalculator? {
        val factory = IndentCalculatorFactory(project, editor)
        val settings = indentionSettings(editor)
        val currentPosition = getPosition(editor, offset)
        if (!currentPosition.matchesRule { it.isAt(Whitespace) && it.isAtMultiline }) return null

        val before = currentPosition.beforeOptionalMix(*WHITE_SPACE_OR_COMMENT_BIT_SET)
        val after = currentPosition.afterOptionalMix(*WHITE_SPACE_OR_COMMENT_BIT_SET)

        when {
            after.isAt(BlockClosingBrace) &&
                    !currentPosition.hasLineBreaksAfter(offset) &&
                    !(currentPosition.after().let { it.isAt(BlockComment) && it.isAtMultiline }) ->
                return factory.createIndentCalculatorForBrace(before, after, BlockOpeningBrace, BlockClosingBrace, Indent.getNoneIndent())

            before.isAt(BlockOpeningBrace) ->
                return factory.createIndentCalculatorForBrace(before, after, BlockOpeningBrace, BlockClosingBrace, Indent.getNormalIndent())

            before.isAt(Arrow) -> {
                return factory.createIndentCalculatorForArrow(before, after)
            }

            after.isAt(ArrayClosingBracket) && !currentPosition.hasLineBreaksAfter(offset) ->
                return factory.createIndentCalculatorForBrace(
                    before,
                    after,
                    ArrayOpeningBracket,
                    ArrayClosingBracket,
                    Indent.getNoneIndent()
                )

            before.isAt(ArrayOpeningBracket) && after.isAt( ArrayClosingBracket) -> {
                val indent = if (isSimilarToFunctionInvocation(before))
                    Indent.getContinuationIndent()
                else
                    Indent.getNormalIndent()

                return factory.createIndentCalculator(indent, before.startOffset)
            }


            before.isAt(Colon) && before.before().isAt(Quest) ->
                return factory.createIndentCalculator(Indent.getNoneIndent(), before.startOffset)

            before.isAt(TemplateEntryOpen) -> {
                val indent = if (!currentPosition.hasLineBreaksAfter(offset) && after.isAt(TemplateEntryClose))
                    Indent.getNoneIndent()
                else
                    Indent.getNormalIndent()

                return factory.createIndentCalculator(indent, before.startOffset)
            }

            before.isAtAnyOf(TryKeyword) || before.isFinallyKeyword() ->
                return factory.createIndentCalculator(Indent.getNoneIndent(), IndentCalculator.LINE_BEFORE)

            after.isAt(TemplateEntryClose) -> {
                val indent = if (currentPosition.hasEmptyLineAfter(offset)) Indent.getNormalIndent() else Indent.getNoneIndent()
                after.moveBeforeParentheses(TemplateEntryOpen, TemplateEntryClose)
                return factory.createIndentCalculator(indent, after.startOffset)
            }

            before.isAt(Eq) -> {
                val declaration = findFunctionOrPropertyOrMultiDeclarationBefore(before.beforeIgnoringWhiteSpaceOrComment())
                if (declaration != null) {
                    val indent = if (settings.continuationIndentForExpressionBodies)
                        Indent.getContinuationIndent()
                    else
                        Indent.getNormalIndent()

                    return factory.createIndentCalculator(indent, declaration.startOffset)
                }
            }

            after.isAt(RightParenthesis) ->
                factory.createIndentCalculatorForParenthesis(before, currentPosition, after, offset, settings)?.let { return it }

            after.isAt(Dot) || after.isAt(Quest) && after.after().isAt(Dot) ->
                factory.createIndentCalculatorForDot(before, settings)?.let { return it }
        }

        findFunctionOrPropertyOrMultiDeclarationBefore(before)?.let {
            val indent = if (after.similarToPropertyAccessorKeyword()) Indent.getNormalIndent() else Indent.getNoneIndent()
            return factory.createIndentCalculator(indent, it.startOffset)
        }

        findContextReceiverListBefore(before)?.let {
            return factory.createIndentCalculator(Indent.getNoneIndent(), it.startOffset)
        }

        return before.controlFlowStatementBefore()?.let { controlFlowKeywordPosition ->
            val indent = when {
                controlFlowKeywordPosition.similarToCatchKeyword() -> if (before.isAt(RightParenthesis)) Indent.getNoneIndent() else Indent.getNormalIndent()
                after.isAt(LeftParenthesis) -> if (before.isAt(BlockOpeningBrace)) Indent.getNormalIndent() else Indent.getContinuationIndent()
                after.isAtAnyOf(BlockOpeningBrace, Arrow) || controlFlowKeywordPosition.isWhileInsideDoWhile() -> Indent.getNoneIndent()
                else -> Indent.getNormalIndent()
            }

            factory.createIndentCalculator(indent, controlFlowKeywordPosition.startOffset)
        }
    }

    private enum class CangJieElement : SemanticEditorPosition.SyntaxElement {
        TemplateEntryOpen,
        TemplateEntryClose,

        Arrow,
        MatchKeyword,
        WhileKeyword,
        RegularStringPart,
        CDoc,
        Identifier,

        OpenTypeBrace,
        CloseTypeBrace,

        FunctionKeyword,
        Dot,
        Quest,

        Eq,

        Let, Var,

        OpenQuote, ClosingQuote,

        Literal,



    }

    companion object {
        private val SYNTAX_MAP: Map<IElementType, SemanticEditorPosition.SyntaxElement> = hashMapOf(
            CjTokens.WHITE_SPACE to Whitespace,
            CjTokens.EOL_COMMENT to LineComment,
            CjTokens.BLOCK_COMMENT to BlockComment,
            CjTokens.DOC_COMMENT to CDoc,
            CjTokens.ARROW to Arrow,

            CjTokens.LONG_TEMPLATE_ENTRY_START to TemplateEntryOpen,
            CjTokens.LONG_TEMPLATE_ENTRY_END to TemplateEntryClose,

            CjTokens.OPEN_QUOTE to OpenQuote,
            CjTokens.CLOSING_QUOTE to ClosingQuote,

            CjTokens.LBRACE to BlockOpeningBrace,
            CjTokens.RBRACE to BlockClosingBrace,

            CjTokens.LPAR to LeftParenthesis,
            CjTokens.RPAR to RightParenthesis,

            CjTokens.LBRACKET to ArrayOpeningBracket,
            CjTokens.RBRACKET to ArrayClosingBracket,

            CjTokens.LT to OpenTypeBrace,
            CjTokens.GT to CloseTypeBrace,

            CjTokens.IF_KEYWORD to IfKeyword,
            CjTokens.ELSE_KEYWORD to ElseKeyword,
            CjTokens.MATCH_KEYWORD to MatchKeyword,
            CjTokens.TRY_KEYWORD to TryKeyword,
            CjTokens.WHILE_KEYWORD to WhileKeyword,
            CjTokens.DO_KEYWORD to DoKeyword,
            CjTokens.FOR_KEYWORD to ForKeyword,

            CjTokens.REGULAR_STRING_PART to RegularStringPart,

            CjTokens.IDENTIFIER to Identifier,
            CjTokens.GET_KEYWORD to Identifier,
            CjTokens.SET_KEYWORD to Identifier,

            CjTokens.CATCH_KEYWORD to Identifier,


            CjTokens.FUNC_KEYWORD to FunctionKeyword,
            CjTokens.DOT to Dot,
            CjTokens.QUEST to Quest,
            CjTokens.COMMA to Comma,
            CjTokens.COLON to Colon,

            CjTokens.EQ to Eq,

            CjTokens.LET_KEYWORD  to Let,
            CjTokens.VAR_KEYWORD to Var,

            CjTokens.INTEGER_LITERAL to Literal,
            CjTokens.FLOAT_LITERAL to Literal,
            CjTokens.RUNE_LITERAL to Literal,
        )

        private val CONTROL_FLOW_KEYWORDS: HashSet<SemanticEditorPosition.SyntaxElement> = hashSetOf(
            MatchKeyword,
            IfKeyword,
            ElseKeyword,
            DoKeyword,
            WhileKeyword,
            ForKeyword,
            TryKeyword,
        )

        private val WHITE_SPACE_OR_COMMENT_BIT_SET: Array<SemanticEditorPosition.SyntaxElement> = arrayOf(
            Whitespace,
            LineComment,
            BlockComment,
        )

        private val PARENTHESES: List<Pair<SemanticEditorPosition.SyntaxElement, SemanticEditorPosition.SyntaxElement>> = listOf(
            LeftParenthesis to RightParenthesis,
            BlockOpeningBrace to BlockClosingBrace,
            ArrayOpeningBracket to ArrayClosingBracket,
        )

        private fun IndentCalculatorFactory.createIndentCalculatorForArrow(
            arrowPosition: SemanticEditorPosition,
            after: SemanticEditorPosition,
        ): IndentCalculator? {
            val leftBrace = arrowPosition.copyAnd {
                it.moveToLeftParenthesisBackwardsSkippingNested(BlockOpeningBrace, BlockClosingBrace)
            }

            if (leftBrace.isAtEnd) {
                return null
            }

            val normalIndent = Indent.getNormalIndent()
            return if (leftBrace.controlFlowStatementBefore() != null) {
                createIndentCalculator(normalIndent, arrowPosition.startOffset)
            } else {
                createIndentCalculatorForBrace(leftBrace, after, BlockOpeningBrace, BlockClosingBrace, normalIndent)
            }
        }

        private fun IndentCalculatorFactory.createIndentCalculatorForBrace(
            before: SemanticEditorPosition,
            after: SemanticEditorPosition,
            leftBraceType: SemanticEditorPosition.SyntaxElement,
            rightBraceType: SemanticEditorPosition.SyntaxElement,
            defaultIndent: Indent
        ): IndentCalculator {
            val leftBrace = before.copyAnd {
                it.moveToLeftParenthesisBackwardsSkippingNested(leftBraceType, rightBraceType)
            }

            if (leftBrace.isAtEnd) {
                return createIndentCalculator(defaultIndent, 0)
            }

            if (after.afterIgnoringWhiteSpaceOrComment().isAt(Comma)) {
                return createIndentCalculator(createAlignMultilineIndent(leftBrace), leftBrace.startOffset)
            }

            val beforeLeftBrace = leftBrace.copyAnd { it.moveBeforeIgnoringWhiteSpaceOrComment() }
            val leftAnchor = if (beforeLeftBrace.isAt(RightParenthesis)) {
                beforeLeftBrace.moveBeforeParentheses(LeftParenthesis, RightParenthesis)
                beforeLeftBrace
            } else {
                findFunctionDeclarationBeforeBody(beforeLeftBrace)
            }

            val resultPosition = leftAnchor?.takeIf { !it.isAtEnd } ?: leftBrace
            return createIndentCalculator(defaultIndent, resultPosition.startOffset)
        }

        private tailrec fun IndentCalculatorFactory.createIndentCalculatorForDot(
            beforeDotPosition: SemanticEditorPosition,
            settings: CangJieIndentationAdjuster,
        ): IndentCalculator? {

            val calleeOrReference = when {
                beforeDotPosition.isAt(Literal) -> beforeDotPosition
                beforeDotPosition.isAt(ClosingQuote) ->
                    beforeDotPosition.before().findLeftParenthesisBackwardsSkippingNested(
                        OpenQuote,
                        ClosingQuote,
                    )

                else -> findCalleeOrReference(beforeDotPosition)
            }

            if (calleeOrReference?.isAtEnd != false) {
                return null
            }

            val copyOfCalleeOrReference = calleeOrReference.copy()

            calleeOrReference.moveBefore()
            if (!calleeOrReference.moveBeforeWhileThisIsWhiteSpaceOnSameLineOrBlockComment() || calleeOrReference.isAtEnd) {

                return createIndentCalculator(settings.indentForChainedCalls, copyOfCalleeOrReference.startOffset)
            }


            if (calleeOrReference.isAt(Dot)) {
                val before = calleeOrReference.copyAnd(SemanticEditorPosition::moveBefore)
                if (before.isAt(Quest)) {
                    before.moveBefore()
                }

                if (!before.moveBeforeWhileThisIsWhiteSpaceOnSameLineOrBlockComment()) {

                    return createIndentCalculator(
                        Indent.getNoneIndent(),
                        calleeOrReference.startOffset,
                    )
                }


                return createIndentCalculatorForDot(before, settings)
            }


            return when {
                settings.alignMultilineParametersInCalls && calleeOrReference.isAt(LeftParenthesis) ->
                    createIndentCalculatorWithCustomBaseIndent(
                        settings.indentForChainedCalls,
                        " ".repeat(copyOfCalleeOrReference.startOffset - calculateLineOffset(copyOfCalleeOrReference)),
                    )

                else -> createIndentCalculator(settings.indentForChainedCalls, calleeOrReference.startOffset)
            }
        }

        private fun IndentCalculatorFactory.createIndentCalculatorForParenthesis(
            before: SemanticEditorPosition,
            currentPosition: SemanticEditorPosition,
            rightParenthesis: SemanticEditorPosition,
            offset: Int,
            settings: CangJieIndentationAdjuster,
        ): IndentCalculator? {
            assert(rightParenthesis.isAt(RightParenthesis))

            val leftParenthesis = currentPosition.findLeftParenthesisBackwardsSkippingNested(LeftParenthesis, RightParenthesis)
            if (!leftParenthesis.isAt(LeftParenthesis)) return null

            val hasLineBreaksAfter = currentPosition.hasLineBreaksAfter(offset)
            fun createIndent(
                isParameterList: Boolean,
                baseLineOffset: Int,
            ): IndentCalculator {
                val indent = if (before.isAt(Comma) || hasLineBreaksAfter) {
                    val firstElement = leftParenthesis.afterIgnoringWhiteSpaceOrComment()
                    when {
                        !firstElement.isAt(RightParenthesis) && (
                                isParameterList && settings.alignMultilineParameters ||
                                        !isParameterList && settings.alignMultilineParametersInCalls
                                ) -> {
                            return createIndentCalculator(createAlignMultilineIndent(firstElement), firstElement.startOffset)
                        }

                        isParameterList && settings.continuationIndentInParameterLists ||
                                !isParameterList && settings.continuationIndentInArgumentLists -> Indent.getContinuationIndent()

                        else -> Indent.getNormalIndent()
                    }
                } else {
                    if (settings.alignWhenMultilineFunctionParentheses)
                        createAlignMultilineIndent(leftParenthesis)
                    else
                        Indent.getNoneIndent()
                }

                return createIndentCalculator(indent, baseLineOffset)
            }

            findFunctionKeywordBeforeIdentifier(leftParenthesis.beforeIgnoringWhiteSpaceOrComment())?.let {
                return createIndent(isParameterList = true, it.startOffset)
            }


            if (isSimilarToFunctionInvocation(leftParenthesis)) {
                return createIndent(isParameterList = false, leftParenthesis.startOffset)
            }

            if (isDestructuringDeclaration(leftParenthesis, rightParenthesis)) {
                return createIndentCalculator(
                    if (before.isAt(Comma) || hasLineBreaksAfter) Indent.getNormalIndent() else Indent.getNoneIndent(),
                    leftParenthesis.startOffset
                )
            }

            leftParenthesis.beforeIgnoringWhiteSpaceOrComment().let { keyword ->
                val indent = when {
                    keyword.isAt(IfKeyword) && !before.isAt(LeftParenthesis) ->
                        if (settings.continuationIndentInIfCondition) Indent.getContinuationIndent() else Indent.getNormalIndent()

                    keyword.isControlFlowKeyword() -> if (hasLineBreaksAfter) Indent.getNormalIndent() else Indent.getNoneIndent()
                    else -> null
                }

                indent?.let { return createIndentCalculator(it, keyword.startOffset) }
            }

            return if (settings.alignWhenMultilineBinaryExpression && !hasLineBreaksAfter) {
                val anchor = if (before.isAt(LeftParenthesis)) leftParenthesis else leftParenthesis.afterIgnoringWhiteSpaceOrComment()
                createIndentCalculator(createAlignMultilineIndent(anchor), anchor.startOffset)
            } else {
                createIndentCalculator(Indent.getContinuationIndent(), leftParenthesis.startOffset)
            }
        }


        private fun findContextReceiverListBefore(before: SemanticEditorPosition): SemanticEditorPosition? {
            if (!before.isAt(RightParenthesis)) return null

            val probableContextReceiverKeyword = before.copy().apply {
                moveBeforeParentheses(LeftParenthesis, RightParenthesis)
            }

            if (probableContextReceiverKeyword.isAt(Identifier)
                && probableContextReceiverKeyword.after().isAt(LeftParenthesis)

            ) {
                return probableContextReceiverKeyword
            }
            return null
        }



        private fun findFunctionOrPropertyOrMultiDeclarationBefore(endOfDeclaration: SemanticEditorPosition): SemanticEditorPosition? =
            findFunctionDeclarationBeforeBody(endOfDeclaration)
                ?: findPropertyDeclarationBeforeAssignment(endOfDeclaration)
                ?: findMultiDeclarationBeforeAssignment(endOfDeclaration)

        private fun findMultiDeclarationBeforeAssignment(rightParenthesis: SemanticEditorPosition): SemanticEditorPosition? {
            if (!rightParenthesis.isAt(RightParenthesis)) return null
            return with(rightParenthesis.copy()) {
                if (!moveBeforeParenthesesIfPossible()) return null
                takeIf { isVarOrVal() }
            }
        }


        private fun findPropertyDeclarationBeforeAssignment(endOfDeclaration: SemanticEditorPosition): SemanticEditorPosition? {


            if (endOfDeclaration.isAt(Identifier)) {
                findPropertyKeywordBeforeIdentifier(endOfDeclaration)?.let { return it }
            }

            return with(endOfDeclaration.copy()) {

                if (moveBeforeTypeQualifierIfPossible(true)) {
                    if (!isAt(Colon)) return null
                    moveBeforeIgnoringWhiteSpaceOrComment()
                }

                findPropertyKeywordBeforeIdentifier(this)
            }
        }


        private fun findFunctionDeclarationBeforeBody(endOfDeclaration: SemanticEditorPosition): SemanticEditorPosition? =
            with(endOfDeclaration.copy()) {

                if (moveBeforeTypeQualifierIfPossible(true)) {
                    if (!isAt(Colon)) return null
                    moveBeforeIgnoringWhiteSpaceOrComment()
                }

                if (!moveBeforeParenthesesIfPossible()) return null

                findFunctionKeywordBeforeIdentifier(this)
            }

        private fun findPropertyKeywordBeforeIdentifier(identifierPosition: SemanticEditorPosition): SemanticEditorPosition? {
            if (!identifierPosition.isAt(Identifier)) return null
            return with(identifierPosition.copy()) {
                if (!moveBeforeTypeQualifierIfPossible(false)) return null

                moveBeforeTypeParametersIfPossible()
                takeIf { it.isVarOrVal() }
            }
        }


        private fun findFunctionKeywordBeforeIdentifier(identifierPosition: SemanticEditorPosition): SemanticEditorPosition? {

            if (identifierPosition.isAt(FunctionKeyword)) return identifierPosition

            return with(identifierPosition.copy()) {
                moveBeforeWhileThisIsWhiteSpaceOrComment()


                if (isAt(Dot)) {
                    moveBeforeIgnoringWhiteSpaceOrComment()
                    if (!moveBeforeTypeQualifierIfPossible(true)) return null
                    return if (isAt(FunctionKeyword)) this else null
                }


                if (!isAt(Identifier)) return null
                if (!moveBeforeTypeQualifierIfPossible(false)) return null

                moveBeforeTypeParametersIfPossible()

                takeIf { it.isAt(FunctionKeyword) }
            }
        }


        private fun isSimilarToFunctionInvocation(leftParenthesis: SemanticEditorPosition): Boolean =
            findCalleeOrReferenceBeforeLeftParenthesis(leftParenthesis) != null

        /**
         * @param position reference or left/right parentheses
         */
        private fun findCalleeOrReference(position: SemanticEditorPosition): SemanticEditorPosition? {
            if (position.isAt(Identifier)) return position.copy()

            for ((left, right) in PARENTHESES) {
                if (position.isAt(left)) {
                    return findCalleeOrReferenceBeforeLeftParenthesis(position)
                }

                if (!position.isAt(right)) continue

                val leftParenthesis = position.copyAnd {
                    it.moveBeforeParentheses(left, right)
                    it.moveAfter()
                }

                return moveToCalleeOrReferenceBeforeLeftParenthesisIfPossible(leftParenthesis)
            }

            return null
        }

        private fun findCalleeOrReferenceBeforeLeftParenthesis(leftParenthesis: SemanticEditorPosition): SemanticEditorPosition? =
            leftParenthesis.copy().let(::moveToCalleeOrReferenceBeforeLeftParenthesisIfPossible)

        private fun moveToCalleeOrReferenceBeforeLeftParenthesisIfPossible(leftParenthesis: SemanticEditorPosition): SemanticEditorPosition? =
            with(leftParenthesis) {
                val canBeWithTypeBraces = leftParenthesis.isAtAnyOf(LeftParenthesis, BlockOpeningBrace)
                moveBefore()

                if (!moveBeforeWhileThisIsWhiteSpaceOnSameLineOrBlockComment() || isAtEnd) return null


                if (isAt(CloseTypeBrace) && canBeWithTypeBraces) {
                    moveBeforeParentheses(OpenTypeBrace, CloseTypeBrace)
                    return this.takeIf { moveBeforeWhileThisIsWhiteSpaceOnSameLineOrBlockComment() && isIdentifier() }
                }

                return this.takeIf { isIdentifier() || isAtAnyOf(RightParenthesis, ArrayClosingBracket) }
            }

        private fun isDestructuringDeclaration(
            leftParenthesis: SemanticEditorPosition,
            rightParenthesis: SemanticEditorPosition
        ): Boolean = with(leftParenthesis.copy()) {
            moveBeforeIgnoringWhiteSpaceOrComment()


            if (isVarOrVal()) return true


            if (!rightParenthesis.moveBeforeParametersIfPossible()) return false

            return isAt(BlockOpeningBrace)
        }


        private fun SemanticEditorPosition.moveBeforeParametersIfPossible(): Boolean {
            while (!isAtEnd) {
                if (!moveBeforeParameterIfPossible()) return false
                if (!isAt(Comma)) return true
                moveBeforeIgnoringWhiteSpaceOrComment()
            }

            return false
        }


        private fun SemanticEditorPosition.moveBeforeParameterIfPossible(): Boolean {

            if (isAt(RightParenthesis)) return moveBeforeParenthesesIfPossible()

            if (!moveBeforeTypeQualifierIfPossible(true)) return false


            if (isAt(Colon)) {
                moveBeforeIgnoringWhiteSpaceOrComment()

                if (isAt(RightParenthesis)) return moveBeforeParenthesesIfPossible()


                if (!isAt(Identifier)) return false
                moveBeforeIgnoringWhiteSpaceOrComment()
            }

            return true
        }


        private fun SemanticEditorPosition.moveBeforeTypeQualifierIfPossible(canStartWithTypeParameter: Boolean): Boolean {
            if (!canStartWithTypeParameter && !isAt(Identifier)) return false

            while (!isAtEnd) {
                moveBeforeOptionalMix(Quest, *WHITE_SPACE_OR_COMMENT_BIT_SET)
                moveBeforeTypeParametersIfPossible()
                if (!isAt(Identifier)) return false
                moveBeforeIgnoringWhiteSpaceOrComment()
                if (!isAt(Dot)) return true
                moveBeforeIgnoringWhiteSpaceOrComment()
            }

            return false
        }

        private fun createAlignMultilineIndent(position: SemanticEditorPosition): Indent {
            val beforeLineStart = calculateLineOffset(position)
            val beforeLineWithoutIndentStart = CharArrayUtil.shiftForward(position.chars, beforeLineStart, " \t")
            return Indent.getSpaceIndent(position.startOffset - beforeLineWithoutIndentStart)
        }

        private fun calculateLineOffset(position: SemanticEditorPosition): Int {
            return CharArrayUtil.shiftBackwardUntil(position.chars, position.startOffset, "\n") + 1
        }

        private fun SemanticEditorPosition.isWhileInsideDoWhile(): Boolean {
            if (!isAt(WhileKeyword)) return false
            with(copy()) {
                moveBefore()
                var whileKeywordLevel = 1
                while (!isAtEnd) when {
                    isAt(BlockOpeningBrace) -> return false
                    isAt(DoKeyword) -> {
                        if (--whileKeywordLevel == 0) return true
                        moveBefore()
                    }

                    isAt(WhileKeyword) -> {
                        ++whileKeywordLevel
                        moveBefore()
                    }

                    isAt(BlockClosingBrace) -> moveBeforeParentheses(BlockOpeningBrace, BlockClosingBrace)
                    else -> moveBefore()
                }
            }

            return false
        }

        private fun SemanticEditorPosition.controlFlowStatementBefore(): SemanticEditorPosition? = with(copy()) {
            if (isAt(BlockOpeningBrace)) moveBeforeIgnoringWhiteSpaceOrComment()

            if (isControlFlowKeyword()) return this
            if (!moveBeforeParenthesesIfPossible()) return null

            return takeIf { isControlFlowKeyword() }
        }

        private fun SemanticEditorPosition.isControlFlowKeyword(): Boolean =
            currElement in CONTROL_FLOW_KEYWORDS || isCatchKeyword() || isFinallyKeyword()

        private fun SemanticEditorPosition.similarToCatchKeyword(): Boolean = textOfCurrentPosition() == CjTokens.CATCH_KEYWORD.value

        private fun SemanticEditorPosition.similarToPropertyAccessorKeyword(): Boolean =
            isAt(Identifier) && textOfCurrentPosition().let { text ->
                text == CjTokens.GET_KEYWORD.value || text == CjTokens.SET_KEYWORD.value
            }

        private fun SemanticEditorPosition.isCatchKeyword(): Boolean = with(copy()) {

            do {
                if (!isAt(Identifier)) return false
                if (!similarToCatchKeyword()) return false

                moveBeforeIgnoringWhiteSpaceOrComment()
                if (!moveBeforeBlockIfPossible()) return false

                if (isAt(TryKeyword)) return true

                if (!moveBeforeParenthesesIfPossible()) return false
            } while (!isAtEnd)

            return false
        }

        private fun SemanticEditorPosition.isFinallyKeyword(): Boolean {
            if (!isAt(Identifier)) return false
            if (textOfCurrentPosition() != CjTokens.FINALLY_KEYWORD.value) return false
            with(copy()) {
                moveBeforeIgnoringWhiteSpaceOrComment()
                if (!moveBeforeBlockIfPossible()) return false


                if (isAt(TryKeyword)) return true

                if (!moveBeforeParenthesesIfPossible()) return false


                return isCatchKeyword()
            }
        }

        private fun SemanticEditorPosition.moveBeforeWhileThisIsWhiteSpaceOrComment() =
            moveBeforeOptionalMix(*WHITE_SPACE_OR_COMMENT_BIT_SET)

        private fun SemanticEditorPosition.moveAfterWhileThisIsWhiteSpaceOrComment() =
            moveAfterOptionalMix(*WHITE_SPACE_OR_COMMENT_BIT_SET)

        private fun SemanticEditorPosition.moveBeforeIgnoringWhiteSpaceOrComment() {
            moveBefore()
            moveBeforeWhileThisIsWhiteSpaceOrComment()
        }

        private fun SemanticEditorPosition.moveAfterIgnoringWhiteSpaceOrComment() {
            moveAfter()
            moveAfterWhileThisIsWhiteSpaceOrComment()
        }

        private fun SemanticEditorPosition.beforeIgnoringWhiteSpaceOrComment() = copyAnd { it.moveBeforeIgnoringWhiteSpaceOrComment() }
        private fun SemanticEditorPosition.afterIgnoringWhiteSpaceOrComment() = copyAnd { it.moveAfterIgnoringWhiteSpaceOrComment() }
        private fun SemanticEditorPosition.moveBeforeWhileThisIsWhiteSpaceOnSameLineOrBlockComment(): Boolean {
            while (!isAtEnd) {
                if (isAt(Whitespace) && isAtMultiline) return false
                if (!isAtAnyOf(BlockComment, Whitespace)) break
                moveBefore()
            }

            return true
        }

        private fun SemanticEditorPosition.isIdentifier(): Boolean = isAt(Identifier) || isAt(CjTokens.THIS_KEYWORD)
        private fun SemanticEditorPosition.isVarOrVal(): Boolean = isAtAnyOf(Var, Let)
        private fun SemanticEditorPosition.moveBeforeBlockIfPossible(): Boolean = moveBeforeParenthesesIfPossible(
            leftParenthesis = BlockOpeningBrace,
            rightParenthesis = BlockClosingBrace,
        )

        private fun SemanticEditorPosition.moveBeforeTypeParametersIfPossible(): Boolean = moveBeforeParenthesesIfPossible(
            leftParenthesis = OpenTypeBrace,
            rightParenthesis = CloseTypeBrace,
        )

        private fun SemanticEditorPosition.moveBeforeParenthesesIfPossible(): Boolean = moveBeforeParenthesesIfPossible(
            leftParenthesis = LeftParenthesis,
            rightParenthesis = RightParenthesis,
        )

        private fun SemanticEditorPosition.moveBeforeParenthesesIfPossible(
            leftParenthesis: SemanticEditorPosition.SyntaxElement,
            rightParenthesis: SemanticEditorPosition.SyntaxElement,
        ): Boolean {
            if (!isAt(rightParenthesis)) return false

            moveBeforeParentheses(leftParenthesis, rightParenthesis)
            moveBeforeWhileThisIsWhiteSpaceOrComment()
            return true
        }
    }
}
private fun SemanticEditorPosition.textOfCurrentPosition(): String =
    if (isAtEnd) "" else chars.subSequence(startOffset, after().startOffset).toString()
private fun JavaLikeLangLineIndentProvider.IndentCalculatorFactory.createIndentCalculator(
    indent: Indent,
    baseLineOffset: Int,
): IndentCalculator = createIndentCalculator(indent) { baseLineOffset } ?: error("Contract (null, _ -> null) is broken")

private val CangJieIndentationAdjuster.indentForChainedCalls: Indent
    get() = if (continuationIndentForChainedCalls) Indent.getContinuationIndent() else Indent.getNormalIndent()


interface CangJieIndentationAdjuster {

    val alignWhenMultilineFunctionParentheses: Boolean get() = false


    val alignWhenMultilineBinaryExpression: Boolean get() = false


    val continuationIndentInElvis: Boolean get() = false


    val continuationIndentForExpressionBodies: Boolean get() = false


    val alignMultilineParameters: Boolean get() = true


    val alignMultilineParametersInCalls: Boolean get() = false


    val continuationIndentInParameterLists: Boolean get() = false


    val continuationIndentInArgumentLists: Boolean get() = false


    val continuationIndentInIfCondition: Boolean get() = false


    val continuationIndentForChainedCalls: Boolean get() = false
}
