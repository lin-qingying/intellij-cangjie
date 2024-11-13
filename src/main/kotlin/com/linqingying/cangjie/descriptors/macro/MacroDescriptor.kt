package com.linqingying.cangjie.descriptors.macro

import com.linqingying.cangjie.descriptors.*
import com.linqingying.cangjie.descriptors.annotations.Annotations
import com.linqingying.cangjie.descriptors.impl.FunctionDescriptorImpl
import com.linqingying.cangjie.name.Name
import com.linqingying.cangjie.types.CangJieType


interface MacroDescriptor : FunctionDescriptor {

    override fun newCopyBuilder(): FunctionDescriptor.CopyBuilder<MacroDescriptor>
    fun initialize(
        dispatchReceiverParameterIfNeeded: ReceiverParameterDescriptor?,
        contextReceiverDescriptors: List<ReceiverParameterDescriptor>,
        valueParameterDescriptors: List<ValueParameterDescriptor>,
        returnType: CangJieType,
        modality: Modality,
        visibility: DescriptorVisibility
    )
}

open class MacroDescriptorImpl(
    containingDeclaration: DeclarationDescriptor,
    name: Name,
    source: SourceElement
) : FunctionDescriptorImpl(
    containingDeclaration, null, Annotations.EMPTY, name, CallableMemberDescriptor.Kind.DECLARATION, source
), MacroDescriptor {
    override fun createSubstitutedCopy(
        newOwner: DeclarationDescriptor,
        original: FunctionDescriptor?,
        kind: CallableMemberDescriptor.Kind,
        newName: Name?,
        annotations: Annotations,
        source: SourceElement
    ): MacroDescriptorImpl {


        return this
    }

    override fun newCopyBuilder(): FunctionDescriptor.CopyBuilder<MacroDescriptor> {
        return super.newCopyBuilder() as FunctionDescriptor.CopyBuilder<MacroDescriptor>
    }

    override fun initialize(
        dispatchReceiverParameterIfNeeded: ReceiverParameterDescriptor?,
        contextReceiverDescriptors: List<ReceiverParameterDescriptor>,
        valueParameterDescriptors: List<ValueParameterDescriptor>,
        returnType: CangJieType,
        modality: Modality,
        visibility: DescriptorVisibility
    ) {
        super.initialize(
            null,
            dispatchReceiverParameterIfNeeded,
            contextReceiverDescriptors,
            emptyList(),
            valueParameterDescriptors,
            returnType,
            modality,
            visibility
        )
    }

    companion object {

        fun create(
            containingDeclaration: DeclarationDescriptor,

            name: Name,

            source: SourceElement
        ): MacroDescriptorImpl {
            return MacroDescriptorImpl(containingDeclaration, name, source)
        }
    }
}
