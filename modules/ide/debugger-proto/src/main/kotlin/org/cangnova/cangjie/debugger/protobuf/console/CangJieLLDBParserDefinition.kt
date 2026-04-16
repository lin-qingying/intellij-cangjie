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

package org.cangnova.cangjie.debugger.protobuf.console

import com.intellij.lang.ASTNode
import com.intellij.lang.ParserDefinition
import com.intellij.lang.PsiBuilder
import com.intellij.lang.PsiParser
import com.intellij.lexer.EmptyLexer
import com.intellij.lexer.Lexer
import com.intellij.openapi.project.Project
import com.intellij.psi.FileViewProvider
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.tree.IElementType
import com.intellij.psi.tree.IFileElementType
import com.intellij.psi.tree.TokenSet

/**
 * LLDB 命令语言的 Parser 定义
 *
 * 提供简单的解析支持,主要用于控制台输入
 */
class CangJieLLDBParserDefinition : ParserDefinition {

    companion object {
        val FILE = IFileElementType(CangJieDebugProtoLLDBLanguage)
    }

    override fun createLexer(project: Project?): Lexer {
        // 使用简单的 PlainText lexer，因为我们不需要复杂的语法解析
        return EmptyLexer()
    }

    override fun createParser(project: Project?): PsiParser {
        // 返回一个简单的 parser
        return object : PsiParser {
            override fun parse(root: IElementType, builder: PsiBuilder): ASTNode {
                val marker = builder.mark()
                while (!builder.eof()) {
                    builder.advanceLexer()
                }
                marker.done(root)
                return builder.treeBuilt
            }


        }
    }

    override fun getFileNodeType(): IFileElementType = FILE

    override fun getCommentTokens(): TokenSet = TokenSet.EMPTY

    override fun getStringLiteralElements(): TokenSet = TokenSet.EMPTY

    override fun createElement(node: ASTNode): PsiElement {
        return com.intellij.psi.impl.source.tree.LeafPsiElement(node.elementType, node.text)
    }

    override fun createFile(viewProvider: FileViewProvider): PsiFile {
        return object : com.intellij.extapi.psi.PsiFileBase(viewProvider, CangJieDebugProtoLLDBLanguage) {
            override fun getFileType() = CangJieProtoLLDBFileType
            override fun toString() = "LLDB Command File"
        }
    }
}