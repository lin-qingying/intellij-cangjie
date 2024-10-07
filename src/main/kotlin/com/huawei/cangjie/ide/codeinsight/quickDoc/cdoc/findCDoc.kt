package com.huawei.cangjie.ide.codeinsight.quickDoc.cdoc

import com.huawei.cangjie.descriptors.CallableDescriptor
import com.huawei.cangjie.descriptors.DeclarationDescriptor
import com.huawei.cangjie.descriptors.DeclarationDescriptorWithSource
import com.huawei.cangjie.doc.psi.impl.CDocSection
import com.huawei.cangjie.doc.psi.impl.CDocTag
import com.huawei.cangjie.psi.CjDeclaration
import com.huawei.cangjie.psi.CjElement
import com.huawei.cangjie.resolve.DescriptorToSourceUtils
import com.huawei.cangjie.resolve.caches.resolveToDescriptorIfAny
import com.huawei.cangjie.utils.liftToExpected
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
