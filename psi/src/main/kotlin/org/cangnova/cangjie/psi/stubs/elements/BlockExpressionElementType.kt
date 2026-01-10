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
import org.cangnova.cangjie.psi.CjBlockExpression
import org.cangnova.cangjie.psi.CjCaseBlockExpression
import org.cangnova.cangjie.psi.CjInitBlockExpression
import com.intellij.lang.ASTNode
import com.intellij.lang.Language
import com.intellij.lang.PsiBuilderFactory
import com.intellij.openapi.project.Project
import com.intellij.psi.tree.ICompositeElementType
import com.intellij.psi.tree.IErrorCounterReparseableElementType
class CaseBlockExpressionElementType : BlockExpressionElementType("CASE_BLOCK") {
    override fun parseContents(chameleon: ASTNode): ASTNode {
        val project = chameleon.psi.project
        val builder = PsiBuilderFactory.getInstance().createBuilder(
            project,
            chameleon,
            null,
            CangJieLanguage,
            chameleon.chars,
        )

        return CangJieParser.parseInitFunctionBlockExpression(builder).firstChildNode
    }

    override fun createCompositeNode() = CjCaseBlockExpression(null)

    override fun createNode(text: CharSequence?) = CjCaseBlockExpression(text)
}

class InitBlockExpressionElementType : BlockExpressionElementType("INIT_BLOCK") {
    override fun parseContents(chameleon: ASTNode): ASTNode {
        val project = chameleon.psi.project
        val builder = PsiBuilderFactory.getInstance().createBuilder(
            project,
            chameleon,
            null,
            CangJieLanguage,
            chameleon.chars,
        )

        return CangJieParser.parseInitFunctionBlockExpression(builder).firstChildNode
    }

    override fun createCompositeNode() = CjInitBlockExpression(null)

    override fun createNode(text: CharSequence?) = CjInitBlockExpression(text)
}

open class BlockExpressionElementType(debugName: String = "BLOCK") :
    IErrorCounterReparseableElementType(debugName, CangJieLanguage),
    ICompositeElementType {

    init {
//            DummyHolderFactory.setFactory(CangJieDummyHolderFactory())
    }

    override fun createCompositeNode() = CjBlockExpression(null)

    override fun createNode(text: CharSequence?) = CjBlockExpression(text)

    override fun isParsable(parent: ASTNode?, buffer: CharSequence, fileLanguage: Language, project: Project) =
        fileLanguage == CangJieLanguage &&
            isAllowedParentNode(parent) &&
            isReparseableBlock(buffer) &&
            super.isParsable(buffer, fileLanguage, project)

    override fun getErrorsCount(seq: CharSequence, fileLanguage: Language, project: Project) =
        ElementTypeUtils.getCangJieBlockImbalanceCount(seq)

    override fun parseContents(chameleon: ASTNode): ASTNode {

        val project = chameleon.psi.project
        val builder = PsiBuilderFactory.getInstance().createBuilder(
            project,
            chameleon,
            null,
            CangJieLanguage,
            chameleon.chars,
        )

        return CangJieParser.parseBlockExpression(builder).firstChildNode
    }

    companion object {

        private fun isAllowedParentNode(node: ASTNode?) =
            node != null

        fun isReparseableBlock(blockText: CharSequence): Boolean {
            fun advanceWhitespacesCheckIsEndOrArrow(lexer: CangJieLexer): Boolean {
                lexer.advance()
                while (lexer.tokenType != null && lexer.tokenType != CjTokens.EOF) {
                    if (lexer.tokenType == CjTokens.ARROW) return true
                    if (lexer.tokenType != CjTokens.WHITE_SPACE) return false
                    lexer.advance()
                }
                return true
            }

            val lexer = CangJieLexer()
            lexer.start(blockText)

            // 尝试解析后面跟一个箭头的简单名称列表
            //   {a -> ...}
            //   {a, b -> ...}
            //   {(a, b) -> ... }
            if (lexer.tokenType != CjTokens.LBRACE) return false

            if (advanceWhitespacesCheckIsEndOrArrow(lexer)) return false

            if (lexer.tokenType != CjTokens.COLON &&
                lexer.tokenType != CjTokens.IDENTIFIER &&
                lexer.tokenType != CjTokens.LPAR
            ) {
                return true
            }

            val searchForRPAR = lexer.tokenType == CjTokens.LPAR

            if (advanceWhitespacesCheckIsEndOrArrow(lexer)) return false

            val preferParamsToExpressions = lexer.tokenType == CjTokens.COMMA || lexer.tokenType == CjTokens.COLON

            while (true) {
                if (lexer.tokenType == CjTokens.LBRACE) return true
                if (lexer.tokenType == CjTokens.RBRACE) return !preferParamsToExpressions

                if (searchForRPAR && lexer.tokenType == CjTokens.RPAR) {
                    return !advanceWhitespacesCheckIsEndOrArrow(lexer)
                }

                if (advanceWhitespacesCheckIsEndOrArrow(lexer)) return false
            }
        }
    }
}
