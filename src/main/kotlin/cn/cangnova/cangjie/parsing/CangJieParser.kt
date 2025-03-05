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

import cn.cangnova.cangjie.lang.CangJieFileType
import cn.cangnova.cangjie.parsing.CangJieParsing.createForTopLevel
import cn.cangnova.cangjie.psi.CjFile
import com.intellij.lang.ASTNode
import com.intellij.lang.PsiBuilder
import com.intellij.lang.PsiParser
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.io.FileUtilRt
import com.intellij.psi.PsiFile
import com.intellij.psi.tree.IElementType
import org.jetbrains.annotations.NotNull

class CangJieParser(project: Project) : PsiParser {
    override fun parse(root: IElementType, builder: PsiBuilder): ASTNode {
        TODO("Not yet implemented")
    }

    companion object {
        @JvmStatic
        fun parseLambdaExpression(psiBuilder: PsiBuilder): ASTNode {
            val cjParsing: CangJieParsing =
                createForTopLevel(
                    SemanticWhitespaceAwarePsiBuilderImpl(psiBuilder),
                )
            cjParsing.parseLambdaExpression()
            return psiBuilder.treeBuilt
        }

        @JvmStatic
        fun parseBlockCodeFragment(psiBuilder: PsiBuilder): ASTNode {
            val cjParsing: CangJieParsing =
                createForTopLevel(
                    SemanticWhitespaceAwarePsiBuilderImpl(psiBuilder),
                )
            cjParsing.parseBlockCodeFragment()
            return psiBuilder.treeBuilt
        }

        @JvmStatic
        fun parseExpressionCodeFragment(psiBuilder: PsiBuilder): ASTNode {
            val cjParsing: CangJieParsing =
                createForTopLevel(
                    SemanticWhitespaceAwarePsiBuilderImpl(psiBuilder),
                )
            cjParsing.parseExpressionCodeFragment()
            return psiBuilder.treeBuilt
        }

        @JvmStatic
        fun parseTypeCodeFragment(psiBuilder: PsiBuilder): ASTNode {
            val cjParsing: CangJieParsing =
                createForTopLevel(
                    SemanticWhitespaceAwarePsiBuilderImpl(psiBuilder),
                )
            cjParsing.parseTypeCodeFragment()
            return psiBuilder.treeBuilt
        }

        @NotNull
        @JvmStatic
        fun parse(psiBuilder: PsiBuilder, psiFile: PsiFile): ASTNode {
            psiBuilder.setDebugMode(true)

            val cjParsing: CangJieParsing =
                createForTopLevel(
                    SemanticWhitespaceAwarePsiBuilderImpl(psiBuilder),
                )

            val extension = FileUtilRt.getExtension(psiFile.name)
            if (extension.isEmpty() || extension == CangJieFileType.EXTENSION || psiFile is CjFile && psiFile.isCompiled) {
                cjParsing.setDeclarationsFile(false)
                cjParsing.parseFile()
            } /* else if (psiFile is CjDeclarationsFile) {
                cjParsing.setDeclarationsFile(true)
                cjParsing.parseFile()

            }*/
            else {
                cjParsing.parseScript()
            }

            return psiBuilder.treeBuilt
        }

        @JvmStatic
        fun parseInitFunctionBlockExpression(psiBuilder: PsiBuilder): ASTNode {
            psiBuilder.setDebugMode(true)
            val cjParsing: CangJieParsing =
                createForTopLevel(
                    SemanticWhitespaceAwarePsiBuilderImpl(psiBuilder),
                )
            cjParsing.parseInitFunctionBody()

            return psiBuilder.treeBuilt
        }

        @JvmStatic
        fun parseBlockExpression(psiBuilder: PsiBuilder): ASTNode {
            psiBuilder.setDebugMode(true)
            val cjParsing: CangJieParsing =
                createForTopLevel(
                    SemanticWhitespaceAwarePsiBuilderImpl(psiBuilder),
                )
            cjParsing.parseBlockExpression()

            return psiBuilder.treeBuilt
        }
    }
}
