package com.linqingying.cangjie.psi

import com.intellij.psi.PsiElement


interface CjLightElement<out T : CjElement, out D : PsiElement> : PsiElement {
    val cangjieOrigin: T?

    /**
     * CjLightModifierList by default retrieves annotation from the relevant CjElement or from clsDelegate
     * But we have none of them for CjUltraLightAnnotationForDescriptor built upon descriptor
     * For that case, CjLightModifierList in the beginning checks `givenAnnotations` and uses them if it's not null
     * Probably, it's a bit dirty solution. But, for now it's not clear how to make it better
     */
//    val givenAnnotations: List<CjLightAbstractAnnotation>? get() = null
}
