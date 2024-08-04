package com.linqingying.cangjie.ide.formatter

import com.linqingying.cangjie.ide.run.cjpm.toolchain
import com.intellij.formatting.*
import com.intellij.lang.ASTNode
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiFile

class CangJieFormattingModelBuilder  : FormattingModelBuilder {

    override fun createModel(formattingContext: FormattingContext): FormattingModel {

        formattingContext.project.toolchain
        val settings = formattingContext.codeStyleSettings
        val containingFile = formattingContext.containingFile
        val block = CangJieBlock(
            containingFile.node,
            NodeAlignmentStrategy.getNullStrategy(),
            Indent.getNoneIndent(),
            wrap = null,
            settings,
            createSpacingBuilder(settings, CangJieSpacingBuilderUtilImpl)
        )

        return FormattingModelProvider.createFormattingModelForPsiFile(containingFile, block, settings)

    }

    override fun getRangeAffectingIndent(psiFile: PsiFile, i: Int, astNode: ASTNode): TextRange? {
        return null
    }
}
