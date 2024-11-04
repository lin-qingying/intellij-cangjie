package com.linqingying.cangjie.ide.quickfix

import com.linqingying.cangjie.configurable.services.CangJieLanguageServerServices
import com.linqingying.cangjie.configurable.services.Feature
import com.linqingying.cangjie.diagnostics.Diagnostic
import com.linqingying.cangjie.psi.psiUtil.getNonStrictParentOfType
import com.intellij.psi.PsiElement


inline fun <reified T : PsiElement> Diagnostic.createIntentionForFirstParentOfType(
    factory: (T) -> CangJieQuickFixAction<T>?
) = psiElement.getNonStrictParentOfType<T>()?.let(factory)


internal fun checkQuickFixIsEnable(): Boolean {
    return CangJieLanguageServerServices.getInstance().astConfig.isFeatureEnabled(Feature.QUICK_FIX)
}
