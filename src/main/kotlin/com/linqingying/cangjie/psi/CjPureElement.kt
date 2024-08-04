package com.linqingying.cangjie.psi

import com.intellij.psi.PsiElement

interface CjPureElement {

    fun getPsiOrParent(): CjElement


    fun getParent(): PsiElement?


    fun getContainingCjFile(): CjFile


}
