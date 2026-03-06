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
 */

package org.cangnova.cangjie.parsing

import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.lang.ASTNode
import com.intellij.lang.ParserDefinition
import com.intellij.lang.PsiBuilderFactory
import com.intellij.lang.PsiParser
import com.intellij.lexer.Lexer
import com.intellij.openapi.project.Project
import com.intellij.psi.FileViewProvider
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.tree.IFileElementType
import com.intellij.psi.tree.TokenSet
import org.cangnova.cangjie.lang.CangJieMacroCallLanguage
import org.cangnova.cangjie.lexer.CangJieLexer
import org.cangnova.cangjie.lexer.CjTokens
import org.cangnova.cangjie.lexer.cdoc.lexer.CDocTokens
import org.cangnova.cangjie.lexer.cdoc.parser.CDocElementType
import org.cangnova.cangjie.lexer.cdoc.psi.impl.CDocLink
import org.cangnova.cangjie.macro.file.CjMacroCallFile
import org.cangnova.cangjie.parsing.CangJieParser.Companion.parse
import org.cangnova.cangjie.psi.CjNodeType
import org.cangnova.cangjie.psi.CjNodeTypes
import org.cangnova.cangjie.psi.stubs.elements.CjStubElementType

/**
 * `.cj.macrocall` 文件的独立 ParserDefinition
 *
 * 与 [CangJieParserDefinition] 的关键区别：
 * - [getFileNodeType] 返回普通 [IFileElementType]（非 IStubFileElementType），
 *   不触发 Stub 索引构建，避免与源文件产生 REDECLARATION 冲突
 * - 复用相同的 Lexer 和 Parser，保证 PSI 树结构一致
 */
internal class CangJieMacroCallParserDefinition : ParserDefinition {

    companion object {
        private const val NAME = "cangjie.MACROCALL_FILE"

        val FILE_ELEMENT_TYPE: IFileElementType = object : IFileElementType(NAME, CangJieMacroCallLanguage) {
            override fun doParseContents(chameleon: ASTNode, psi: PsiElement): ASTNode {
                val project = psi.project
                val languageForParser = getLanguageForParser(psi)
                val builder = PsiBuilderFactory.getInstance()
                    .createBuilder(project, chameleon, null, languageForParser, chameleon.chars)
                return parse(builder, psi.containingFile).firstChildNode
            }
        }
    }

    override fun createLexer(project: Project?): Lexer = CangJieLexer()

    override fun createParser(project: Project?): PsiParser = CangJieParser(project!!)

    override fun getFileNodeType(): IFileElementType = FILE_ELEMENT_TYPE

    override fun getCommentTokens(): TokenSet = CjTokens.COMMENTS

    override fun getWhitespaceTokens(): TokenSet = CjTokens.WHITESPACES

    override fun getStringLiteralElements(): TokenSet = TokenSet.EMPTY

    override fun createElement(astNode: ASTNode): PsiElement {
        return when (val elementType = astNode.elementType) {
            is CjStubElementType<*, *> -> elementType.createPsiFromAst(astNode)
            CjNodeTypes.TYPE_CODE_FRAGMENT, CjNodeTypes.EXPRESSION_CODE_FRAGMENT, CjNodeTypes.BLOCK_CODE_FRAGMENT -> ASTWrapperPsiElement(
                astNode,
            )
            is CDocElementType -> elementType.createPsi(astNode)
            CDocTokens.MARKDOWN_LINK -> CDocLink(astNode)
            else -> (elementType as CjNodeType).createPsi(astNode)
        }
    }

    override fun createFile(viewProvider: FileViewProvider): PsiFile {
        return CjMacroCallFile(viewProvider)
    }
}
