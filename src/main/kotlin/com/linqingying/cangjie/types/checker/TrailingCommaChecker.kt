package com.linqingying.cangjie.types.checker

import com.linqingying.cangjie.config.LanguageVersionSettings
import com.linqingying.cangjie.descriptors.BindingTrace
import com.intellij.psi.PsiElement


object TrailingCommaChecker {
    fun check(trailingComma: PsiElement?, trace: BindingTrace, languageVersionSettings: LanguageVersionSettings) {
//        if (!languageVersionSettings.supportsFeature(LanguageFeature.TrailingCommas) && trailingComma != null) {
//            trace.report(Errors.UNSUPPORTED_FEATURE.on(trailingComma, LanguageFeature.TrailingCommas to languageVersionSettings))
//        }
    }
}
