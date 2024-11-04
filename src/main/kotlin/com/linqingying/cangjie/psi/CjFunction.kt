package com.linqingying.cangjie.psi

import com.intellij.psi.PsiElement


interface CjFunction : CjDeclarationWithBody, CjCallableDeclaration {
    val isLocal: Boolean
    val isStatic: Boolean
        get() = false
    val isUnsafe: Boolean
        get() = false
    val isOperator: Boolean
        get() = false
    val isMut get() = false

    val isConst get() =  false

    val keyword :PsiElement? get() = null
}

