package com.huawei.cangjie.parsing

import com.huawei.cangjie.lang.CangJieFileType
import com.huawei.cangjie.parsing.CangJieParsing.createForTopLevel
import com.huawei.cangjie.psi.CjFile
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


        @NotNull
        @JvmStatic
        fun parse(psiBuilder: PsiBuilder, psiFile: PsiFile): ASTNode {
            val cjParsing: CangJieParsing =
                createForTopLevel(
                    SemanticWhitespaceAwarePsiBuilderImpl(psiBuilder)
                )

            val extension = FileUtilRt.getExtension(psiFile.name)
            if (extension.isEmpty() || extension == CangJieFileType.EXTENSION || psiFile is CjFile && (psiFile as CjFile).isCompiled) {
                cjParsing.parseFile()
            }else{
                cjParsing.parseScript()
            }


            return psiBuilder.treeBuilt
        }

        @JvmStatic
        fun parseBlockExpression(psiBuilder: PsiBuilder): ASTNode {
            val cjParsing: CangJieParsing =
                createForTopLevel(
                    SemanticWhitespaceAwarePsiBuilderImpl(psiBuilder)
                )
            cjParsing.parseBlockExpression()
            return psiBuilder.treeBuilt
        }
    }

}
