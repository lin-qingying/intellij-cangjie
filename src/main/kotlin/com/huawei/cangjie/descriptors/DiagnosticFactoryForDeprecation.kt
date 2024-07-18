package com.huawei.cangjie.descriptors

import com.huawei.cangjie.config.LanguageFeature
import com.huawei.cangjie.config.LanguageVersionSettings
import com.huawei.cangjie.diagnostics.DiagnosticFactoryWithPsiElement
import com.intellij.psi.PsiElement

sealed class DiagnosticFactoryForDeprecation<E : PsiElement, D : Diagnostic, F : DiagnosticFactoryWithPsiElement<E, D>>(
    val deprecatingFeature: LanguageFeature,
    val warningFactory: F,
    val errorFactory: F
)
{
    fun LanguageVersionSettings.chooseFactory(): F {
        return if (supportsFeature(deprecatingFeature)) errorFactory else warningFactory
    }
}
