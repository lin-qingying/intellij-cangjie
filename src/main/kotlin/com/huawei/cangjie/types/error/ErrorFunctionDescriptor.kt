package com.huawei.cangjie.types.error

import com.huawei.cangjie.descriptors.*
import com.huawei.cangjie.descriptors.annotations.Annotations
import com.huawei.cangjie.descriptors.annotations.Annotations.Companion.EMPTY
import com.huawei.cangjie.descriptors.impl.FunctionDescriptorImpl
import com.huawei.cangjie.descriptors.impl.SimpleFunctionDescriptorImpl
import com.huawei.cangjie.name.Name
import com.huawei.cangjie.types.CangJieType
import com.huawei.cangjie.types.ErrorUtils
import com.huawei.cangjie.types.TypeSubstitution


class ErrorFunctionDescriptor(containingDeclaration: ClassDescriptor) : SimpleFunctionDescriptorImpl(
    containingDeclaration,
    null,
    EMPTY,
    Name.special(ErrorEntity.ERROR_FUNCTION.debugText),
    CallableMemberDescriptor.Kind.DECLARATION,
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
    ): FunctionDescriptorImpl = this

    override fun copy(
        newOwner: DeclarationDescriptor,
        modality: Modality,
        visibility: DescriptorVisibility,
        kind: CallableMemberDescriptor.Kind,
        copyOverrides: Boolean
    ): SimpleFunctionDescriptor = this


    override fun newCopyBuilder(): FunctionDescriptor.CopyBuilder<SimpleFunctionDescriptor?> =
        object : FunctionDescriptor.CopyBuilder<SimpleFunctionDescriptor?> {
            override fun setOwner(owner: DeclarationDescriptor): FunctionDescriptor.CopyBuilder<SimpleFunctionDescriptor?> =
                this

            override fun setModality(modality: Modality): FunctionDescriptor.CopyBuilder<SimpleFunctionDescriptor?> =
                this

            override fun setVisibility(visibility: DescriptorVisibility): FunctionDescriptor.CopyBuilder<SimpleFunctionDescriptor?> =
                this

            override fun setKind(kind: CallableMemberDescriptor.Kind): FunctionDescriptor.CopyBuilder<SimpleFunctionDescriptor?> =
                this

            override fun setCopyOverrides(copyOverrides: Boolean): FunctionDescriptor.CopyBuilder<SimpleFunctionDescriptor?> =
                this

            override fun setName(name: Name): FunctionDescriptor.CopyBuilder<SimpleFunctionDescriptor?> = this
            override fun setSubstitution(substitution: TypeSubstitution): FunctionDescriptor.CopyBuilder<SimpleFunctionDescriptor?> =
                this

            override fun setValueParameters(
                parameters: List<ValueParameterDescriptor>
            ): FunctionDescriptor.CopyBuilder<SimpleFunctionDescriptor?> = this

            override fun <V> putUserData(
                userDataKey: CallableDescriptor.UserDataKey<V>, value: V
            ): FunctionDescriptor.CopyBuilder<SimpleFunctionDescriptor?> = this

            override fun setTypeParameters(
                parameters: List<TypeParameterDescriptor>
            ): FunctionDescriptor.CopyBuilder<SimpleFunctionDescriptor?> = this

            override fun setReturnType(type: CangJieType): FunctionDescriptor.CopyBuilder<SimpleFunctionDescriptor?> =
                this

            override fun setContextReceiverParameters(
                contextReceiverParameters: List<ReceiverParameterDescriptor>
            ): FunctionDescriptor.CopyBuilder<SimpleFunctionDescriptor?> = this

            override fun setExtensionReceiverParameter(
                extensionReceiverParameter: ReceiverParameterDescriptor?
            ): FunctionDescriptor.CopyBuilder<SimpleFunctionDescriptor?> = this

            override fun setDispatchReceiverParameter(
                dispatchReceiverParameter: ReceiverParameterDescriptor?
            ): FunctionDescriptor.CopyBuilder<SimpleFunctionDescriptor?> = this

            override fun setOriginal(original: CallableMemberDescriptor?): FunctionDescriptor.CopyBuilder<SimpleFunctionDescriptor?> =
                this

            override fun setSignatureChange(): FunctionDescriptor.CopyBuilder<SimpleFunctionDescriptor?> = this
            override fun setPreserveSourceElement(): FunctionDescriptor.CopyBuilder<SimpleFunctionDescriptor?> = this
            override fun setDropOriginalInContainingParts(): FunctionDescriptor.CopyBuilder<SimpleFunctionDescriptor?> =
                this

            override fun setHiddenToOvercomeSignatureClash(): FunctionDescriptor.CopyBuilder<SimpleFunctionDescriptor?> =
                this

            override fun setHiddenForResolutionEverywhereBesideSupercalls(): FunctionDescriptor.CopyBuilder<SimpleFunctionDescriptor?> =
                this

            override fun setAdditionalAnnotations(
                additionalAnnotations: Annotations
            ): FunctionDescriptor.CopyBuilder<SimpleFunctionDescriptor?> = this

            override fun build(): SimpleFunctionDescriptor = this@ErrorFunctionDescriptor
        }


    //    override fun <V> getUserData(key: CallableDescriptor.UserDataKey<V>): V? = null
    override fun setOverriddenDescriptors(overriddenDescriptors: Collection<CallableMemberDescriptor?>) {}
    override fun <V : Any?> getUserData(key: CallableDescriptor.UserDataKey<V>?): V? = null
}
