package com.linqingying.cangjie.ide.codeinsight.quickDoc.cdoc

import com.linqingying.cangjie.descriptors.CallableDescriptor
import com.linqingying.cangjie.descriptors.DeclarationDescriptor
import com.linqingying.cangjie.descriptors.DeclarationDescriptorWithSource
import com.linqingying.cangjie.doc.psi.impl.CDocSection
import com.linqingying.cangjie.doc.psi.impl.CDocTag
import com.linqingying.cangjie.psi.CjDeclaration
import com.linqingying.cangjie.psi.CjElement
import com.linqingying.cangjie.resolve.DescriptorToSourceUtils
import com.linqingying.cangjie.resolve.caches.resolveToDescriptorIfAny
import com.linqingying.cangjie.utils.liftToExpected
import com.intellij.psi.PsiElement

data class CDocContent(
    val contentTag: CDocTag,
    val sections: List<CDocSection>
)

fun DeclarationDescriptor.findCDoc(
    descriptorToPsi: (DeclarationDescriptorWithSource) -> PsiElement? = { DescriptorToSourceUtils.descriptorToDeclaration(it) }
): CDocContent? {
    if (this is DeclarationDescriptorWithSource) {
        val psiDeclaration = descriptorToPsi(this)?.navigationElement
        return (psiDeclaration as? CjElement)?.findCDoc(descriptorToPsi)
    }
    return null
}
private typealias DescriptorToPsi = (DeclarationDescriptorWithSource) -> PsiElement?

fun CjElement.findCDoc(descriptorToPsi: DescriptorToPsi): CDocContent? {
    return findCDocByPsi()
        ?: this.lookupInheritedCDoc(descriptorToPsi)
}
private fun CjElement.lookupInheritedCDoc(descriptorToPsi: DescriptorToPsi): CDocContent? {
    if (this is CjDeclaration) {
        val descriptor = resolveToDescriptorIfAny()

        if (descriptor is CallableDescriptor) {
            for (baseDescriptor in descriptor.overriddenDescriptors) {
                val baseCDoc = baseDescriptor.original.findCDoc(descriptorToPsi)
                if (baseCDoc != null) {
                    return baseCDoc
                }
            }
        }

        val expectedCDoc = descriptor?.liftToExpected().takeIf { it != descriptor }?.findCDoc(descriptorToPsi)

        if (expectedCDoc != null) {
            return expectedCDoc
        }
    }

    return null
}
