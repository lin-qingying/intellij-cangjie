package com.linqingying.cangjie.descriptors

interface DeclarationDescriptorWithNavigationSubstitute : DeclarationDescriptor {
    val substitute: DeclarationDescriptor
}
