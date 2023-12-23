package com.huawei.cangjie.idea.editor

import com.huawei.cangjie.lang.CangJieLanguage
import com.huawei.cangjie.lexer.CjTokens
import com.intellij.lang.BracePair
import com.intellij.lang.PairedBraceMatcher
import com.intellij.psi.PsiFile
import com.intellij.psi.tree.IElementType
import com.intellij.util.containers.toArray

class CangJiePairedBraceMatcher: PairedBraceMatcher {
    override fun getPairs(): Array<BracePair> {
        return CjTokens.BRACE_PAIRS
    }

    override fun isPairedBracesAllowedBeforeType(lbraceType: IElementType, contextType: IElementType?): Boolean {
        return true
    }

    override fun getCodeConstructStart(file: PsiFile?, openingBraceOffset: Int): Int {
        return openingBraceOffset
    }

}