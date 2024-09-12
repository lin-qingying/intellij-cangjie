package com.huawei.cangjie.diagnostics

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile

interface Diagnostic : UnboundDiagnostic, DiagnosticMarker {
    override val psiElement: PsiElement
    val psiFile: PsiFile

    override val factoryName: String
        get() = factory.name
}
