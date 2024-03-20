package com.huawei.cangjie.descriptors

import com.intellij.psi.PsiElement

interface DiagnosticMarker {
    val psiElement: PsiElement
    val factoryName: String
}

interface DiagnosticWithParameters1Marker<A> : DiagnosticMarker {
    val a: A
}