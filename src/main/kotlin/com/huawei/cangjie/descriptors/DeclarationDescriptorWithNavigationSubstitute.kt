package com.huawei.cangjie.descriptors

interface DeclarationDescriptorWithNavigationSubstitute : DeclarationDescriptor {
    val substitute: DeclarationDescriptor
}
