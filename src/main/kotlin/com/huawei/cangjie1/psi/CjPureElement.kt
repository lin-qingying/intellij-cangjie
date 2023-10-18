package com.huawei.cangjie1.psi

import com.intellij.psi.PsiElement

interface CjPureElement {

    fun getPsiOrParent(): CjElement


    fun getParent(): PsiElement?


    fun getContainingCjFile(): CjFile


}
