package com.linqingying.cangjie.descriptors.impl

import com.linqingying.cangjie.descriptors.DeclarationDescriptor
import com.linqingying.cangjie.descriptors.DeclarationDescriptorVisitor
import com.linqingying.cangjie.descriptors.DescriptorVisibilities
import com.linqingying.cangjie.descriptors.SourceElement
import com.linqingying.cangjie.descriptors.annotations.Annotations
import com.linqingying.cangjie.name.Name
import com.linqingying.cangjie.types.CangJieType
import com.linqingying.cangjie.types.TypeSubstitutor

open class LocalVariableDescriptor
    (
    containingDeclaration: DeclarationDescriptor,
    annotations: Annotations,
    name: Name,
    type: CangJieType?,
    mutable: Boolean,

    source: SourceElement
) : VariableDescriptorImpl(containingDeclaration, name, type, mutable, source, DescriptorVisibilities.LOCAL) {
    override fun substitute(substitutor: TypeSubstitutor): LocalVariableDescriptor {
        if (substitutor.isEmpty) return this
        throw UnsupportedOperationException() // TODO
    }


}
