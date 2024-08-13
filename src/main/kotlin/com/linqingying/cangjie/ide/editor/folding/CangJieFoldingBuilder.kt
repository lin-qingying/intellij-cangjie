package com.linqingying.cangjie.ide.editor.folding

import com.intellij.lang.ASTNode
import com.intellij.lang.folding.CustomFoldingBuilder
import com.intellij.lang.folding.FoldingDescriptor
import com.intellij.openapi.editor.Document
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement

class CangJieFoldingBuilder : CustomFoldingBuilder(), DumbAware {
    override fun buildLanguageFoldRegions(
        descriptors: MutableList<FoldingDescriptor>,
        root: PsiElement,
        document: Document,
        quick: Boolean
    ) {
//        TODO("Not yet implemented")
    }

    override fun getLanguagePlaceholderText(p0: ASTNode, p1: TextRange): String {


        return "{...}"
    }

    override fun isRegionCollapsedByDefault(p0: ASTNode): Boolean {
//        TODO("Not yet implemented")
        return true
    }
}
