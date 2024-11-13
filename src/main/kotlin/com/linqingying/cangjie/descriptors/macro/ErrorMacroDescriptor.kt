package com.linqingying.cangjie.descriptors.macro

import com.linqingying.cangjie.descriptors.*
import com.linqingying.cangjie.descriptors.annotations.Annotations
import com.linqingying.cangjie.descriptors.annotations.Annotations.Companion.EMPTY
import com.linqingying.cangjie.descriptors.impl.FunctionDescriptorImpl
import com.linqingying.cangjie.descriptors.impl.SimpleFunctionDescriptorImpl
import com.linqingying.cangjie.name.Name
import com.linqingying.cangjie.types.CangJieType
import com.linqingying.cangjie.types.ErrorUtils
import com.linqingying.cangjie.types.TypeSubstitution
import com.linqingying.cangjie.types.error.ErrorEntity
import com.linqingying.cangjie.types.error.ErrorTypeKind


class ErrorMacroDescriptor(containingDeclaration: ClassDescriptor) : MacroDescriptorImpl(
    containingDeclaration,

    Name.special(ErrorEntity.ERROR_FUNCTION.debugText),

    SourceElement.NO_SOURCE
) {
    init {
        initialize(
            null,
            null,
            emptyList(),
            emptyList(),
            emptyList(),
            ErrorUtils.createErrorType(ErrorTypeKind.RETURN_TYPE_FOR_FUNCTION),
            Modality.OPEN,
            DescriptorVisibilities.PUBLIC
        )
    }

    override fun createSubstitutedCopy(
        newOwner: DeclarationDescriptor,
        original: FunctionDescriptor?,
        kind: CallableMemberDescriptor.Kind,
        newName: Name?,
        annotations: Annotations,
        source: SourceElement
    ): MacroDescriptorImpl = this

    override fun copy(
        newOwner: DeclarationDescriptor,
        modality: Modality,
        visibility: DescriptorVisibility,
        kind: CallableMemberDescriptor.Kind,
        copyOverrides: Boolean
    ): MacroDescriptorImpl = this


    override fun newCopyBuilder(): FunctionDescriptor.CopyBuilder<MacroDescriptor> =
        object : FunctionDescriptor.CopyBuilder<MacroDescriptor > {
            override fun setOwner(owner: DeclarationDescriptor): FunctionDescriptor.CopyBuilder<MacroDescriptor> =
                this

            override fun setModality(modality: Modality): FunctionDescriptor.CopyBuilder<MacroDescriptor> =
                this

            override fun setVisibility(visibility: DescriptorVisibility): FunctionDescriptor.CopyBuilder<MacroDescriptor> =
                this

            override fun setKind(kind: CallableMemberDescriptor.Kind): FunctionDescriptor.CopyBuilder<MacroDescriptor> =
                this

            override fun setCopyOverrides(copyOverrides: Boolean): FunctionDescriptor.CopyBuilder<MacroDescriptor> =
                this

            override fun setName(name: Name): FunctionDescriptor.CopyBuilder<MacroDescriptor> = this
            override fun setSubstitution(substitution: TypeSubstitution): FunctionDescriptor.CopyBuilder<MacroDescriptor> =
                this

            override fun setValueParameters(
                parameters: List<ValueParameterDescriptor>
            ): FunctionDescriptor.CopyBuilder<MacroDescriptor> = this

            override fun <V> putUserData(
                userDataKey: CallableDescriptor.UserDataKey<V>, value: V
            ): FunctionDescriptor.CopyBuilder<MacroDescriptor> = this

            override fun setTypeParameters(
                parameters: List<TypeParameterDescriptor>
            ): FunctionDescriptor.CopyBuilder<MacroDescriptor> = this

            override fun setReturnType(type: CangJieType): FunctionDescriptor.CopyBuilder<MacroDescriptor> =
                this

            override fun setContextReceiverParameters(
                contextReceiverParameters: List<ReceiverParameterDescriptor>
            ): FunctionDescriptor.CopyBuilder<MacroDescriptor> = this

            override fun setExtensionReceiverParameter(
                extensionReceiverParameter: ReceiverParameterDescriptor?
            ): FunctionDescriptor.CopyBuilder<MacroDescriptor> = this

            override fun setDispatchReceiverParameter(
                dispatchReceiverParameter: ReceiverParameterDescriptor?
            ): FunctionDescriptor.CopyBuilder<MacroDescriptor> = this

            override fun setOriginal(original: CallableMemberDescriptor?): FunctionDescriptor.CopyBuilder<MacroDescriptor> =
                this

            override fun setSignatureChange(): FunctionDescriptor.CopyBuilder<MacroDescriptor> = this
            override fun setPreserveSourceElement(): FunctionDescriptor.CopyBuilder<MacroDescriptor> = this
            override fun setDropOriginalInContainingParts(): FunctionDescriptor.CopyBuilder<MacroDescriptor> =
                this

            override fun setHiddenToOvercomeSignatureClash(): FunctionDescriptor.CopyBuilder<MacroDescriptor> =
                this

            override fun setHiddenForResolutionEverywhereBesideSupercalls(): FunctionDescriptor.CopyBuilder<MacroDescriptor> =
                this

            override fun setAdditionalAnnotations(
                additionalAnnotations: Annotations
            ): FunctionDescriptor.CopyBuilder<MacroDescriptor> = this

            override fun build(): MacroDescriptor = this@ErrorMacroDescriptor
        }



    //    override fun <V> getUserData(key: CallableDescriptor.UserDataKey<V>): V? = null
    override fun setOverriddenDescriptors(overriddenDescriptors: Collection<CallableMemberDescriptor?>) {}
    override fun <V : Any?> getUserData(key: CallableDescriptor.UserDataKey<V>?): V? = null
}
