package com.huawei.cangjie.ide.completion

import com.huawei.cangjie.descriptors.DeclarationDescriptor
import com.intellij.openapi.extensions.ExtensionPointName

interface CompletionInformationProvider {
    companion object {
        val EP_NAME: ExtensionPointName<CompletionInformationProvider> =
            ExtensionPointName.create("com.huawei.cangjie.completionInformationProvider")
    }

    fun getContainerAndReceiverInformation(descriptor: DeclarationDescriptor): String?
}
