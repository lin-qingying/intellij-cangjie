package com.huawei.cangjie.diagnostics

import com.huawei.cangjie.descriptors.Diagnostic
import com.intellij.psi.PsiElement

interface ParametrizedDiagnostic<E : PsiElement> : Diagnostic {
    override val psiElement: E

    override val factoryName: String
        get() = factory.name
}
