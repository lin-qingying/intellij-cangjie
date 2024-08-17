package com.huawei.cangjie.types.checker

import com.huawei.cangjie.config.LanguageFeature
import com.huawei.cangjie.config.LanguageVersionSettings
import com.huawei.cangjie.descriptors.BindingTrace
import com.huawei.cangjie.descriptors.Errors
import com.intellij.psi.PsiElement


object TrailingCommaChecker {
    fun check(trailingComma: PsiElement?, trace: BindingTrace, languageVersionSettings: LanguageVersionSettings) {
//        if (!languageVersionSettings.supportsFeature(LanguageFeature.TrailingCommas) && trailingComma != null) {
//            trace.report(Errors.UNSUPPORTED_FEATURE.on(trailingComma, LanguageFeature.TrailingCommas to languageVersionSettings))
//        }
    }
}
