package com.huawei.cangjie.resolve.lazy

import com.huawei.cangjie.container.PlatformSpecificExtension
import com.huawei.cangjie.descriptors.DeclarationDescriptor
import com.huawei.cangjie.psi.CjDeclaration
import com.huawei.cangjie.utils.CangJieExceptionWithAttachments

interface AbsentDescriptorHandler : PlatformSpecificExtension<AbsentDescriptorHandler> {
    fun diagnoseDescriptorNotFound(declaration: CjDeclaration): DeclarationDescriptor
}

class BasicAbsentDescriptorHandler : AbsentDescriptorHandler {
    override fun diagnoseDescriptorNotFound(declaration: CjDeclaration) = throw NoDescriptorForDeclarationException(declaration)
}

class NoDescriptorForDeclarationException @JvmOverloads constructor(declaration: CjDeclaration, additionalDetails: String? = null) :
    CangJieExceptionWithAttachments(
        "Descriptor wasn't found for declaration $declaration"
                + (additionalDetails?.let { "\n---------------------------------------------------\n$it" } ?: "")
    ) {
    init {
        withPsiAttachment("declaration.kt", declaration)
    }
}