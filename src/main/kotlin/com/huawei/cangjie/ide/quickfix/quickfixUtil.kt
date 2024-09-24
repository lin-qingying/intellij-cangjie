package com.huawei.cangjie.ide.quickfix

import com.huawei.cangjie.configurable.services.CangJieLanguageServerServices
import com.huawei.cangjie.configurable.services.Feature
import com.huawei.cangjie.diagnostics.Diagnostic
import com.huawei.cangjie.psi.psiUtil.getNonStrictParentOfType
import com.intellij.psi.PsiElement


inline fun <reified T : PsiElement> Diagnostic.createIntentionForFirstParentOfType(
    factory: (T) -> CangJieQuickFixAction<T>?
) = psiElement.getNonStrictParentOfType<T>()?.let(factory)


internal fun checkQuickFixIsEnable(): Boolean {
    return CangJieLanguageServerServices.getInstance().astConfig.isFeatureEnabled(Feature.QUICK_FIX)
}
