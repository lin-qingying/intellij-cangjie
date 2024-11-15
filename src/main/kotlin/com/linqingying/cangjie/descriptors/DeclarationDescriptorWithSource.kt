package com.linqingying.cangjie.descriptors

interface DeclarationDescriptorWithSource : DeclarationDescriptor {

    val source: SourceElement

    override val original: DeclarationDescriptorWithSource
}
