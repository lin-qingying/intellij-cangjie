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

package cn.cangnova.cangjie.parsing

import cn.cangnova.cangjie.psi.CjNodeType
import cn.cangnova.cangjie.psi.CjNodeTypes
import cn.cangnova.cangjie.psi.cdoc.lexer.CDocTokens
import cn.cangnova.cangjie.psi.cdoc.parser.CDocElementType
import cn.cangnova.cangjie.psi.cdoc.psi.impl.CDocLink
import cn.cangnova.cangjie.lang.CangJieLanguage
import cn.cangnova.cangjie.lang.declarations.CangJieDeclarationsFileType
import cn.cangnova.cangjie.lang.declarations.CjDeclarationsFile
import cn.cangnova.cangjie.lexer.CangJieLexer
import cn.cangnova.cangjie.lexer.CjToken
import cn.cangnova.cangjie.lexer.CjTokens
import cn.cangnova.cangjie.psi.CjFile
import cn.cangnova.cangjie.psi.stubs.elements.CjFileElementType
import cn.cangnova.cangjie.psi.stubs.elements.CjStubElementType
import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.lang.ASTNode
import com.intellij.lang.LanguageParserDefinitions
import com.intellij.lang.ParserDefinition
import com.intellij.lang.PsiParser
import com.intellij.lexer.Lexer
import com.intellij.openapi.project.Project
import com.intellij.psi.FileViewProvider
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.tree.IElementType
import com.intellij.psi.tree.IFileElementType
import com.intellij.psi.tree.TokenSet

class CangJieParserDefinition : ParserDefinition {
    override fun createLexer(project: Project?): Lexer = CangJieLexer()

    override fun createParser(project: Project?): PsiParser = CangJieParser(project!!)


    override fun getFileNodeType(): IFileElementType = CjFileElementType.INSTANCE

    val EOL_COMMENT = CjToken("EOL_COMMENT", 4)
    val BLOCK_COMMENT = CjToken("BLOCK_COMMENT", 3)
    val DOC_COMMENT: IElementType = CDocTokens.CDOC

    val SHEBANG_COMMENT = CjToken("SHEBANG_COMMENT", 5)
    val COMMENTS = TokenSet.create(
        EOL_COMMENT,
        BLOCK_COMMENT,
        DOC_COMMENT,
        SHEBANG_COMMENT,
    )

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


        if (viewProvider.fileType is CangJieDeclarationsFileType) {
            return CjDeclarationsFile(viewProvider)
        }



        return CjFile(viewProvider, false)
    }

    object Util {
        const val STD_SCRIPT_SUFFIX = "cj"

        @JvmField
        val STD_SCRIPT_EXT = ".$STD_SCRIPT_SUFFIX"
        val instance: CangJieParserDefinition
            get() = LanguageParserDefinitions.INSTANCE.forLanguage(CangJieLanguage) as CangJieParserDefinition
    }
}
