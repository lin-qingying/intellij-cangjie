package com.huawei.cangjie1.parsing

import com.intellij.lang.PsiBuilder
import com.intellij.psi.tree.IElementType



interface SemanticWhitespaceAwarePsiBuilder : PsiBuilder {

    fun newlineBeforeCurrentToken(): Boolean
    fun disableNewlines()
    fun enableNewlines()
    fun restoreNewlinesState()
    fun restoreJoiningComplexTokensState()
    fun enableJoiningComplexTokens()
    fun disableJoiningComplexTokens()
    override fun isWhitespaceOrComment(elementType: IElementType): Boolean
}
