package com.huawei.cangjie.descriptors

data class DescriptorWithDeprecation<out T : DeclarationDescriptor>(val descriptor: T, val isDeprecated: Boolean) {
    companion object {
        fun <T : DeclarationDescriptor> createNonDeprecated(descriptor: T): DescriptorWithDeprecation<T> =
            DescriptorWithDeprecation(descriptor, false)

        fun <T : DeclarationDescriptor> createDeprecated(descriptor: T): DescriptorWithDeprecation<T> =
            DescriptorWithDeprecation(descriptor, true)
    }
}

