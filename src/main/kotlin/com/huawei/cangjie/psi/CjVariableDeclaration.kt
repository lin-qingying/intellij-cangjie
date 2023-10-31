package com.huawei.cangjie.psi

import com.intellij.psi.PsiElement


interface CjVariableDeclaration : CjCallableDeclaration, CjDeclarationWithInitializer, CjLetVarKeywordOwner {
    val isVar: Boolean
}


interface CjLetVarKeywordOwner : PsiElement {
    val letOrVarKeyword: PsiElement?
}


interface CjDeclarationWithInitializer : CjDeclaration {
    val initializer: CjExpression?

    fun hasInitializer(): Boolean
}

