package com.linqingying.cangjie.descriptors.impl

import com.linqingying.cangjie.descriptors.*
import com.linqingying.cangjie.descriptors.annotations.Annotations
import com.linqingying.cangjie.name.Name
import com.linqingying.cangjie.name.SpecialNames
import com.linqingying.cangjie.resolve.descriptorUtil.builtIns
import com.linqingying.cangjie.types.CangJieType


class PropertySetterDescriptorImpl(
    correspondingProperty: PropertyDescriptor,
    annotations: Annotations,
    modality: Modality,
    visibility: DescriptorVisibility,
    isDefault: Boolean,

    kind: CallableMemberDescriptor.Kind,
    original: PropertySetterDescriptor?,
    source: SourceElement
) : PropertyAccessorDescriptorImpl(
    modality, visibility, correspondingProperty, annotations,
    Name.special("<set-${correspondingProperty.name}>"), isDefault,  kind, source
) , PropertySetterDescriptor{
    private var parameter: ValueParameterDescriptor? = null

    override val original: PropertySetterDescriptor = original ?: this

    fun initialize(parameter: ValueParameterDescriptor) {
        check(this.parameter == null) { "Parameter is already initialized" }
        this.parameter = parameter
    }

    fun initializeDefault() {
        initialize(createSetterParameter(this, correspondingProperty.type, Annotations.EMPTY))
    }

    companion object {

@JvmStatic
        fun createSetterParameter(
            setterDescriptor: PropertySetterDescriptor,
            type: CangJieType,
            annotations: Annotations
        ): ValueParameterDescriptorImpl {
            return ValueParameterDescriptorImpl(
                setterDescriptor, null, 0, annotations, SpecialNames.IMPLICIT_SET_PARAMETER,false, type,
                false, // declaresDefaultValue
//                false, // isCrossinline
//                false, // isNoinline
                /*null,*/ SourceElement.NO_SOURCE
            )
        }
    }

    @Suppress("UNCHECKED_CAST")

    override fun getOverriddenDescriptors(): Collection<PropertySetterDescriptor> {
        return super.getOverriddenDescriptors(false) as Collection<PropertySetterDescriptor>
    }


    override fun getValueParameters(): List<ValueParameterDescriptor> {
        return parameter?.let { listOf(it) } ?: throw IllegalStateException("Parameter is not initialized")
    }


    override fun getReturnType(): CangJieType {
        return builtIns.unitType
    }

    override fun <R, D> accept(visitor: DeclarationDescriptorVisitor<R, D>, data: D?): R? {
        return visitor.visitPropertySetterDescriptor(this, data)
    }


}
