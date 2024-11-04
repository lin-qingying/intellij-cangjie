package com.linqingying.cangjie.ide.completion

import com.linqingying.cangjie.descriptors.DeclarationDescriptor
import com.intellij.openapi.extensions.ExtensionPointName

interface CompletionInformationProvider {
    companion object {
        val EP_NAME: ExtensionPointName<CompletionInformationProvider> =
            ExtensionPointName.create("com.linqingying.cangjie.completionInformationProvider")
    }

    fun getContainerAndReceiverInformation(descriptor: DeclarationDescriptor): String?
}
