package com.huawei.cangjie.ide.highlighter

import com.intellij.psi.PsiElement


// Returns original declaration if given PsiElement is a Kotlin light element, and element itself otherwise
val PsiElement.unwrapped: PsiElement?
    get() = when (this) {
//        is PsiElementWithOrigin<*> -> origin
//        is KtLightElement<*, *> -> cangjieOrigin
//        is KtLightElementBase -> cangjieOrigin
        else -> this
    }
