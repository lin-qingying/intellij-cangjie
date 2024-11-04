package com.linqingying.cangjie.ide.searching.usages

import com.intellij.psi.PsiReference
import com.intellij.usageView.UsageInfo


class CangJieReferenceUsageInfo(reference: PsiReference) : UsageInfo(reference) {
    private val referenceType = reference::class.java

    override fun getReference(): PsiReference? {
        val element = element ?: return null
        return element.references.firstOrNull { it::class.java == referenceType }
    }
}

class CangJieReferencePreservingUsageInfo(private val reference: PsiReference) : UsageInfo(reference) {
    override fun getReference() = reference
}
