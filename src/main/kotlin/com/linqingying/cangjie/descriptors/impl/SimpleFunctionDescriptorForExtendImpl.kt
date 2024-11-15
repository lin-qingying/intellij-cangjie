package com.linqingying.cangjie.descriptors.impl

import com.linqingying.cangjie.descriptors.*
import com.linqingying.cangjie.descriptors.annotations.Annotations
import com.linqingying.cangjie.name.Name
import com.linqingying.cangjie.types.CangJieType

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
    override fun initialize(
        extensionReceiverParameter: ReceiverParameterDescriptor?,
        dispatchReceiverParameter: ReceiverParameterDescriptor?,
        contextReceiverParameters: MutableList<ReceiverParameterDescriptor>,
        typeParameters: MutableList<out TypeParameterDescriptor>,
        unsubstitutedValueParameters: MutableList<ValueParameterDescriptor>,
        unsubstitutedReturnType: CangJieType?,
        modality: Modality?,
        visibility: DescriptorVisibility,
        userData: MutableMap<out CallableDescriptor.UserDataKey<*>, *>?
    ): SimpleFunctionDescriptorImpl {
        return super.initialize(
            extensionReceiverParameter,
            dispatchReceiverParameter,
            contextReceiverParameters,
            typeParameters,
            unsubstitutedValueParameters,
            unsubstitutedReturnType,
            modality,
            visibility,
            userData
        )
    }
    override fun initialize(
        extensionReceiverParameter: ReceiverParameterDescriptor?,
        dispatchReceiverParameter: ReceiverParameterDescriptor?,
        contextReceiverParameters: MutableList<ReceiverParameterDescriptor>,
        typeParameters: MutableList<out TypeParameterDescriptor>,
        unsubstitutedValueParameters: MutableList<ValueParameterDescriptor>,
        unsubstitutedReturnType: CangJieType?,
        modality: Modality?,
        visibility: DescriptorVisibility
    ): SimpleFunctionDescriptorImpl {
        return super.initialize(
            extensionReceiverParameter,
            dispatchReceiverParameter,
            contextReceiverParameters,
            typeParameters,
            unsubstitutedValueParameters,
            unsubstitutedReturnType,
            modality,
            visibility
        )
    }
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
