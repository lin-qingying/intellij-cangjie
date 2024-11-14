package com.linqingying.cangjie.descriptors.impl

import com.linqingying.cangjie.descriptors.CallableMemberDescriptor
import com.linqingying.cangjie.descriptors.DeclarationDescriptor
import com.linqingying.cangjie.descriptors.SourceElement
import com.linqingying.cangjie.descriptors.TypeParameterDescriptor
import com.linqingying.cangjie.descriptors.annotations.Annotations
import com.linqingying.cangjie.name.Name

class SimpleFunctionDescriptorForExtendImpl(


    containingDeclaration: DeclarationDescriptor,
    original: SimpleFunctionDescriptorForExtendImpl?,
    annotations: Annotations,
    name: Name,
    kind: CallableMemberDescriptor.Kind,
    source: SourceElement
) : SimpleFunctionDescriptorImpl(
    containingDeclaration, original, annotations, name, kind, source
){
    var typeParametersForExtend :List<TypeParameterDescriptor>  = emptyList()


    companion object{

        fun create(
            containingDeclaration: DeclarationDescriptor,
            annotations: Annotations,
            name: Name,
            kind: CallableMemberDescriptor.Kind,
            source: SourceElement
        ): SimpleFunctionDescriptorForExtendImpl {
            return SimpleFunctionDescriptorForExtendImpl(containingDeclaration, null, annotations, name, kind, source)
        }
    }
}
