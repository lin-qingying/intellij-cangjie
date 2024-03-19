package com.huawei.cangjie.descriptors

import com.intellij.psi.PsiElement

interface ParametrizedDiagnostic<E : PsiElement> : Diagnostic {
    override val psiElement: E

    override val factoryName: String
        get() = factory.name
}
