package com.huawei.cangjie.psi

import com.intellij.psi.PsiElement


interface CjLightElement<out T : CjElement, out D : PsiElement> : PsiElement {
    val cangjieOrigin: T?

    /**
     * KtLightModifierList by default retrieves annotation from the relevant KtElement or from clsDelegate
     * But we have none of them for KtUltraLightAnnotationForDescriptor built upon descriptor
     * For that case, KtLightModifierList in the beginning checks `givenAnnotations` and uses them if it's not null
     * Probably, it's a bit dirty solution. But, for now it's not clear how to make it better
     */
//    val givenAnnotations: List<CjLightAbstractAnnotation>? get() = null
}
