package com.huawei.cangjie.descriptors

import com.intellij.psi.PsiElement

interface DiagnosticMarker {
    val psiElement: PsiElement
    val factoryName: String
}