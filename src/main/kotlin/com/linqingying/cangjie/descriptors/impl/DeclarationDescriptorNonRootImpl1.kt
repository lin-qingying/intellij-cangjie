package com.linqingying.cangjie.descriptors.impl

import com.linqingying.cangjie.descriptors.DeclarationDescriptor
import com.linqingying.cangjie.descriptors.DeclarationDescriptorNonRoot
import com.linqingying.cangjie.descriptors.DeclarationDescriptorWithSource
import com.linqingying.cangjie.descriptors.SourceElement
import com.linqingying.cangjie.descriptors.annotations.Annotations
import com.linqingying.cangjie.name.Name

abstract class DeclarationDescriptorNonRootImpl protected constructor(
    override val containingDeclaration: DeclarationDescriptor,
    annotations: Annotations,
    name: Name,
    override val source: SourceElement
) : DeclarationDescriptorImpl(annotations, name), DeclarationDescriptorNonRoot {
    override val original: DeclarationDescriptorWithSource
        get() = super.original as DeclarationDescriptorWithSource

    override fun validate() {
        containingDeclaration.validate()
    }


}
