package com.huawei.cangjie.descriptors.impl

import com.huawei.cangjie.descriptors.*
import com.huawei.cangjie.descriptors.annotations.Annotations
import com.huawei.cangjie.name.Name
import com.huawei.cangjie.types.CangJieType
import com.huawei.cangjie.types.shouldBeUpdated

abstract class VariableDescriptorImpl(
    containingDeclaration: DeclarationDescriptor,
    annotations: Annotations,
    name: Name,
    private var _outType: CangJieType?,
    source: SourceElement

) : DeclarationDescriptorNonRootImpl(containingDeclaration, annotations, name, source),
    VariableDescriptor {

    override val original: VariableDescriptor = super.original as VariableDescriptor
    override fun getContextReceiverParameters(): List<ReceiverParameterDescriptor> {
        return emptyList()
    }
    open fun setOutType(outType:CangJieType) {
        assert(this._outType == null || this._outType.shouldBeUpdated())
        this._outType = outType
    }
    override fun getVisibility() = DescriptorVisibilities.LOCAL

    override fun getReturnType(): CangJieType {
        return type
    }

    override fun getDispatchReceiverParameter(): ReceiverParameterDescriptor? {
        return null
    }


//    fun setType(
//        _outType: CangJieType,
//        @ReadOnly typeParameters: List<TypeParameterDescriptor?>,
//        dispatchReceiverParameter: ReceiverParameterDescriptor?,
//        extensionReceiverParameter: ReceiverParameterDescriptor?,
//        contextReceiverParameters: List<ReceiverParameterDescriptor?>
//    ) {
//        setOutType(_outType)
//
//        this.typeParameters = ArrayList<TypeParameterDescriptor>(typeParameters)
//
//        this.extensionReceiverParameter = extensionReceiverParameter
//        this.dispatchReceiverParameter = dispatchReceiverParameter
//        this.contextReceiverParameters = contextReceiverParameters
//    }

    companion object {
//        fun create(
//            containingDeclaration: DeclarationDescriptor,
//            annotations: Annotations,
//            modality: Modality,
//            visibility: DescriptorVisibility,
//            isVar: Boolean,
//            name: Name,
//            kind: CallableMemberDescriptor.Kind,
//            source: SourceElement,
//            lateInit: Boolean,
//            isConst: Boolean,
//            isExpect: Boolean,
//            isActual: Boolean,
//            isExternal: Boolean,
//            isDelegated: Boolean
//        ): VariableDescriptorImpl {
//            return VariableDescriptorImpl(
//                containingDeclaration, null, annotations,
//                modality, visibility, isVar, name, kind, source, lateInit, isConst,
//                isExpect, isActual, isExternal, isDelegated
//            )
//        }

    }

    override fun getType(): CangJieType {
        return _outType!!
    }

    override fun getExtensionReceiverParameter(): ReceiverParameterDescriptor? {
        return null
    }

    override fun getTypeParameters(): List<TypeParameterDescriptor> {
        return emptyList<TypeParameterDescriptor>()
    }

    override fun getValueParameters(): List<ValueParameterDescriptor> {
        return emptyList()
    }

}
