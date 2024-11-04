package com.linqingying.cangjie.descriptors.impl

import com.linqingying.cangjie.descriptors.*
import com.linqingying.cangjie.descriptors.annotations.Annotations
import com.linqingying.cangjie.name.Name
import com.linqingying.cangjie.psi.CjVariableDeclaration
import com.linqingying.cangjie.resolve.source.getPsi
import com.linqingying.cangjie.types.CangJieType
import com.linqingying.cangjie.types.util.shouldBeUpdated
import com.linqingying.cangjie.utils.ReadOnly


abstract class AbstractVariableDescriptor(
    containingDeclaration: DeclarationDescriptor,
    annotations: Annotations,
    name: Name,
    private var _outType: CangJieType?,
    source: SourceElement

) : DeclarationDescriptorNonRootImpl(containingDeclaration, annotations, name, source),
    VariableDescriptor {
    private var _typeParameters: List<TypeParameterDescriptor> = emptyList()
    protected var _contextReceiverParameters: List<ReceiverParameterDescriptor> = emptyList()
    private var _dispatchReceiverParameter: ReceiverParameterDescriptor? = null
    private var _extensionReceiverParameter: ReceiverParameterDescriptor? = null
    override val original: VariableDescriptor = super.original as VariableDescriptor
    override fun getModality(): Modality {
        return Modality.FINAL
    }
    override val isStatic: Boolean
        get() {

            val element = source.getPsi()
            if(element is CjVariableDeclaration){
                return element.isStatic
            }

            return false
        }
    open fun setOutType(outType: CangJieType) {
        assert(this._outType == null || this._outType.shouldBeUpdated())
        this._outType = outType
    }
    override fun hasSynthesizedParameterNames(): Boolean  = false

    open fun setType(
        outType: CangJieType,
        @ReadOnly typeParameters: List<TypeParameterDescriptor>,
        dispatchReceiverParameter: ReceiverParameterDescriptor?,
        extensionReceiverParameter: ReceiverParameterDescriptor?,
        contextReceiverParameters: List<ReceiverParameterDescriptor>
    ) {
        setOutType(outType)

        this._typeParameters = typeParameters

        this._extensionReceiverParameter = extensionReceiverParameter
        this._dispatchReceiverParameter = dispatchReceiverParameter
        this._contextReceiverParameters = contextReceiverParameters
    }

    override fun getContextReceiverParameters(): List<ReceiverParameterDescriptor> {
        return _contextReceiverParameters
    }


    override var visibility: DescriptorVisibility = DescriptorVisibilities.LOCAL

    override fun getReturnType(): CangJieType {
        return type
    }

    override fun getExtensionReceiverParameter(): ReceiverParameterDescriptor? {
        return _extensionReceiverParameter
    }

    override fun getDispatchReceiverParameter(): ReceiverParameterDescriptor? {
        return _dispatchReceiverParameter
    }

    override val isConst: Boolean
        get() {
            return false
        }


    override fun hasStableParameterNames(): Boolean {
        return false
    }



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
//        ): AbstractVariableDescriptor {
//            return AbstractVariableDescriptor(
//                containingDeclaration, null, annotations,
//                modality, visibility, isVar, name, kind, source, lateInit, isConst,
//                isExpect, isActual, isExternal, isDelegated
//            )
//        }

    }

    override fun getType(): CangJieType {
        return _outType!!
    }


    override fun getTypeParameters(): List<TypeParameterDescriptor> {
        return _typeParameters
    }

    //    override fun getOverriddenDescriptors(): Collection<CallableDescriptor> {
//        return emptySet ()
//    }
    override fun getValueParameters(): List<ValueParameterDescriptor> {
        return emptyList()
    }

}
