package com.huawei.cangjie.resolve

import com.huawei.cangjie.descriptors.*
import com.huawei.cangjie.descriptors.synthetic.SyntheticMemberDescriptor
import com.huawei.cangjie.psi.CjCallableDeclaration
import com.huawei.cangjie.resolve.scopes.receivers.ExtensionReceiver
import com.huawei.cangjie.resolve.source.getPsi
import com.intellij.psi.PsiElement
import java.util.ArrayList
import com.huawei.cangjie.descriptors.CallableMemberDescriptor.Kind.DECLARATION
import com.huawei.cangjie.descriptors.CallableMemberDescriptor.Kind.SYNTHESIZED
object DescriptorToSourceUtils {
    // TODO Fix in descriptor
    @JvmStatic
    private fun getSourceForExtensionReceiverParameterDescriptor(descriptor: ReceiverParameterDescriptor): PsiElement? {
        // Only for extension receivers
        if (descriptor.source != SourceElement.NO_SOURCE || descriptor.value !is ExtensionReceiver) return null
        val containingDeclaration = descriptor.containingDeclaration as? CallableDescriptor ?: return null
        val psi = containingDeclaration.source.getPsi() as? CjCallableDeclaration ?: return null
        return psi.receiverTypeReference
    }

    @JvmStatic
    fun getSourceFromDescriptor(descriptor: DeclarationDescriptor): PsiElement? {
        if (descriptor is ReceiverParameterDescriptor) {
            getSourceForExtensionReceiverParameterDescriptor(descriptor)?.let { return it }
        }

        return (descriptor as? DeclarationDescriptorWithSource)?.source?.getPsi()
    }

    // NOTE this is also used by KDoc
    // Returns PSI element for descriptor. If there are many relevant elements (e.g. it is fake override
    // with multiple declarations), returns null. It can't find declarations in builtins or decompiled code.
    // In IDE, use DescriptorToSourceUtilsIde instead.
    @JvmStatic
    fun descriptorToDeclaration(descriptor: DeclarationDescriptor): PsiElement? {
        val effectiveReferencedDescriptors = getEffectiveReferencedDescriptors(descriptor)
        return if (effectiveReferencedDescriptors.size == 1) getSourceFromDescriptor(effectiveReferencedDescriptors.firstOrNull()!!) else null
    }
    private fun collectEffectiveReferencedDescriptors(result: MutableList<DeclarationDescriptor>, descriptor: DeclarationDescriptor) {
//        if (descriptor is DeclarationDescriptorWithNavigationSubstitute) {
//            collectEffectiveReferencedDescriptors(result, descriptor.substitute)
//            return
//        }

        if (descriptor is CallableMemberDescriptor) {
            val kind = descriptor.kind
            if (kind != DECLARATION && kind != SYNTHESIZED) {
                for (overridden in descriptor.overriddenDescriptors) {
                    collectEffectiveReferencedDescriptors(result, overridden.original)
                }
                return
            }
            if (descriptor is SyntheticMemberDescriptor<*>) {
                collectEffectiveReferencedDescriptors(result, descriptor.baseDescriptorForSynthetic)
                return
            }
        }
        result.add(descriptor)
    }

    @JvmStatic
    fun getEffectiveReferencedDescriptors(descriptor: DeclarationDescriptor): Collection<DeclarationDescriptor> {
        val result = ArrayList<DeclarationDescriptor>()
        collectEffectiveReferencedDescriptors(result, descriptor.original)
        return result
    }
}
