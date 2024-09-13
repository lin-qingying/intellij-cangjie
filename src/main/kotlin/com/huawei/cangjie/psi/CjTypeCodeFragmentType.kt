package com.huawei.cangjie.psi

import com.huawei.cangjie.lang.CangJieLanguage
import com.huawei.cangjie.parsing.CangJieParser
import com.intellij.lang.ASTNode
import com.intellij.lang.PsiBuilderFactory
import com.intellij.psi.PsiElement
import com.intellij.psi.impl.source.tree.ICodeFragmentElementType



class CjTypeCodeFragmentType :
    ICodeFragmentElementType(NAME, CangJieLanguage) {
    override fun doParseContents(chameleon: ASTNode, psi: PsiElement): ASTNode {
        val project = psi.project
        val languageForParser = getLanguageForParser(psi)
        val builder =
            PsiBuilderFactory.getInstance().createBuilder(project, chameleon, null, languageForParser, chameleon.chars)
        return CangJieParser.parseTypeCodeFragment(builder).firstChildNode
    }

    companion object {
        private const val NAME = "cangjie.TYPE_CODE_FRAGMENT"
    }
}

