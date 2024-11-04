package com.linqingying.cangjie.psi

import com.intellij.psi.PsiElement


interface  CjVariableDeclaration : CjCallableDeclaration, CjDeclarationWithInitializer, CjLetVarKeywordOwner {
    val isVar: Boolean
    val isStatic :Boolean get() = false
}


interface CjLetVarKeywordOwner : PsiElement {
    val letOrVarKeyword: PsiElement?
}


interface CjDeclarationWithInitializer : CjDeclaration {
    val initializer: CjExpression?



    fun hasInitializer(): Boolean
}

