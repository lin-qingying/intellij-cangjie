package com.huawei.cangjie1.psi

import com.intellij.psi.PsiElement


interface CjVariableDeclaration : CjCallableDeclaration, CjDeclarationWithInitializer, CjValVarKeywordOwner {
    val isVar: Boolean
}


interface CjValVarKeywordOwner : PsiElement {
    val valOrVarKeyword: PsiElement?
}


interface CjDeclarationWithInitializer : CjDeclaration {
    val initializer: CjExpression?

    fun hasInitializer(): Boolean
}

