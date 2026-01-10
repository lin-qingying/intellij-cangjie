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
package org.cangnova.cangjie.psi.stubs.elements

import org.cangnova.cangjie.psi.ElementTypeUtils
import org.cangnova.cangjie.lang.CangJieLanguage
import org.cangnova.cangjie.lexer.CangJieLexer
import org.cangnova.cangjie.lexer.CjTokens
import org.cangnova.cangjie.parsing.CangJieParser
import org.cangnova.cangjie.psi.CjFunctionLiteral
import org.cangnova.cangjie.psi.CjLambdaExpression
import org.cangnova.cangjie.psi.CjParameterList
import com.intellij.lang.ASTNode
import com.intellij.lang.Language
import com.intellij.lang.PsiBuilder
import com.intellij.lang.PsiBuilderFactory
import com.intellij.lexer.Lexer
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.tree.IElementType
import com.intellij.psi.tree.IErrorCounterReparseableElementType
import com.intellij.psi.util.PsiTreeUtil

  class LambdaExpressionElementType :
    IErrorCounterReparseableElementType("LAMBDA_EXPRESSION", CangJieLanguage) {
    override fun parseContents(chameleon: ASTNode): ASTNode {
        val project = chameleon.psi.project
        val builder: PsiBuilder = PsiBuilderFactory.getInstance().createBuilder(
            project,
            chameleon,
            null,
            CangJieLanguage,
            chameleon.chars,
        )
        return CangJieParser.parseLambdaExpression(builder).firstChildNode
    }

    override fun createNode(text: CharSequence?): ASTNode {
        return CjLambdaExpression(text)
    }

    override fun isParsable(parent: ASTNode?, buffer: CharSequence, fileLanguage: Language, project: Project): Boolean {
        return super.isParsable(parent, buffer, fileLanguage, project) && !wasArrowMovedOrDeleted(
            parent,
            buffer,
        ) && !wasParameterCommaMovedOrDeleted(parent, buffer)
    }

    override fun getErrorsCount(seq: CharSequence, fileLanguage: Language?, project: Project?): Int {
        return ElementTypeUtils.getCangJieBlockImbalanceCount(seq)
    }

    companion object {
        private fun wasArrowMovedOrDeleted(parent: ASTNode?, buffer: CharSequence): Boolean {
            val lambdaExpression: CjLambdaExpression? = findLambdaExpression(parent)
            if (lambdaExpression == null) {
                return false
            }

            val literal: CjFunctionLiteral = lambdaExpression.functionLiteral
            val arrow: PsiElement? = literal.arrow

            // No arrow in original node
            if (arrow == null) return false

            val arrowOffset: Int = arrow.startOffsetInParent + literal.startOffsetInParent

            return hasTokenMoved(lambdaExpression.text, buffer, arrowOffset, CjTokens.ARROW)
        }

        private fun wasParameterCommaMovedOrDeleted(parent: ASTNode?, buffer: CharSequence): Boolean {
            val lambdaExpression: CjLambdaExpression? = findLambdaExpression(parent)
            if (lambdaExpression == null) {
                return false
            }

            val literal: CjFunctionLiteral = lambdaExpression.functionLiteral
            val valueParameterList: CjParameterList? = literal.valueParameterList
            if (valueParameterList == null || valueParameterList.parameters.size <= 1) {
                return false
            }

            val comma: PsiElement? = valueParameterList.firstComma
            if (comma == null) {
                return false
            }

            val commaOffset: Int = comma.textOffset - lambdaExpression.textOffset
            return hasTokenMoved(lambdaExpression.text, buffer, commaOffset, CjTokens.COMMA)
        }

        private fun findLambdaExpression(parent: ASTNode?): CjLambdaExpression? {
            if (parent == null) return null

            val parentPsi: PsiElement? = parent.psi
            val lambdaExpressions: Array<CjLambdaExpression?>? =
                PsiTreeUtil.getChildrenOfType(parentPsi, CjLambdaExpression::class.java)
            if (lambdaExpressions == null || lambdaExpressions.size != 1) return null

            // Now works only when actual node can be spotted ambiguously. Need change in API.
            return lambdaExpressions[0]
        }

        private fun hasTokenMoved(
            oldText: String,
            buffer: CharSequence,
            oldOffset: Int,
            tokenType: IElementType?,
        ): Boolean {
            val oldLexer: Lexer = CangJieLexer()
            oldLexer.start(oldText)

            val newLexer: Lexer = CangJieLexer()
            newLexer.start(buffer)

            while (true) {
                val oldType = oldLexer.tokenType
                if (oldType == null) break // Didn't find an expected token. Consider it as no token was present.

                val newType = newLexer.tokenType
                if (newType == null) return true // New text was finished before reaching expected token in old text

                if (newType !== oldType) {
                    if (newType === CjTokens.WHITE_SPACE) {
                        newLexer.advance()
                        continue
                    } else if (oldType === CjTokens.WHITE_SPACE) {
                        oldLexer.advance()
                        continue
                    }

                    return true // Expected token was moved or deleted
                }

                if (oldType === tokenType && oldLexer.currentPosition.offset == oldOffset) {
                    break
                }

                oldLexer.advance()
                newLexer.advance()
            }

            return false
        }
    }
}
