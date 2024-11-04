package com.linqingying.cangjie.parsing

import com.linqingying.cangjie.lang.CangJieFileType
import com.linqingying.cangjie.lang.declarations.CjDeclarationsFile
import com.linqingying.cangjie.parsing.CangJieParsing.createForTopLevel
import com.linqingying.cangjie.psi.CjFile
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
                    SemanticWhitespaceAwarePsiBuilderImpl(psiBuilder)
                )
            cjParsing.parseLambdaExpression()
            return psiBuilder.treeBuilt
        }

        @JvmStatic

        fun parseBlockCodeFragment(psiBuilder: PsiBuilder): ASTNode {
            val cjParsing: CangJieParsing =
                createForTopLevel(
                    SemanticWhitespaceAwarePsiBuilderImpl(psiBuilder)
                )
            cjParsing.parseBlockCodeFragment()
            return psiBuilder.treeBuilt
        }

        @JvmStatic
        fun parseExpressionCodeFragment(psiBuilder: PsiBuilder): ASTNode {
            val cjParsing: CangJieParsing =
                createForTopLevel(
                    SemanticWhitespaceAwarePsiBuilderImpl(psiBuilder)
                )
            cjParsing.parseExpressionCodeFragment()
            return psiBuilder.treeBuilt
        }

        @JvmStatic
        fun parseTypeCodeFragment(psiBuilder: PsiBuilder): ASTNode {
            val cjParsing: CangJieParsing =
                createForTopLevel(
                    SemanticWhitespaceAwarePsiBuilderImpl(psiBuilder)
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
                    SemanticWhitespaceAwarePsiBuilderImpl(psiBuilder)
                )

            val extension = FileUtilRt.getExtension(psiFile.name)
            if (extension.isEmpty() || extension == CangJieFileType.EXTENSION || psiFile is CjFile && psiFile.isCompiled) {

                cjParsing.setDeclarationsFile(false)
                cjParsing.parseFile()

//                TODO LSP 使用 parseLspFile
//                cjParsing.parseLspFile()
            } else if (psiFile is CjDeclarationsFile) {
                cjParsing.setDeclarationsFile(true)
                cjParsing.parseFile()

            }
            /*if (psiFile.viewProvider is CangJieFileViewProvider) {
                cjParsing.parseDeclarationsFile()

            }*/ else {
                cjParsing.parseScript()
            }


            return psiBuilder.treeBuilt
        }

        @JvmStatic
        fun parseInitFunctionBlockExpression(psiBuilder: PsiBuilder): ASTNode {

            psiBuilder.setDebugMode(true)
            val cjParsing: CangJieParsing =
                createForTopLevel(
                    SemanticWhitespaceAwarePsiBuilderImpl(psiBuilder)
                )
            cjParsing.parseInitFunctionBody()

            return psiBuilder.treeBuilt

        }

        @JvmStatic
        fun parseBlockExpression(psiBuilder: PsiBuilder): ASTNode {

            psiBuilder.setDebugMode(true)
            val cjParsing: CangJieParsing =
                createForTopLevel(
                    SemanticWhitespaceAwarePsiBuilderImpl(psiBuilder)
                )
            cjParsing.parseBlockExpression()

            return psiBuilder.treeBuilt

        }
    }

}
