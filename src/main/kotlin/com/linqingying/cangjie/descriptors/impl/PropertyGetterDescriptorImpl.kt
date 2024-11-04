package com.linqingying.cangjie.descriptors.impl

import com.linqingying.cangjie.descriptors.*
import com.linqingying.cangjie.descriptors.annotations.Annotations
import com.linqingying.cangjie.name.Name
import com.linqingying.cangjie.types.CangJieType


class PropertyGetterDescriptorImpl(
    correspondingProperty: PropertyDescriptor,
    annotations: Annotations,
    modality: Modality,
    visibility: DescriptorVisibility,
    isDefault: Boolean,

    kind: CallableMemberDescriptor.Kind,
    original: PropertyGetterDescriptor?,
    source: SourceElement
) : PropertyAccessorDescriptorImpl(
    modality, visibility, correspondingProperty, annotations,
    Name.special("<get-${correspondingProperty.name}>"), isDefault, kind, source
), PropertyGetterDescriptor {
    private var returnType: CangJieType? = null

    override val original: PropertyGetterDescriptor = original ?: this

    fun initialize(returnType: CangJieType?) {
        this.returnType = returnType ?: correspondingProperty.type
    }

    @Suppress("UNCHECKED_CAST")
    override fun getOverriddenDescriptors(): Collection<PropertyGetterDescriptor> {
        return super.getOverriddenDescriptors(true) as Collection<PropertyGetterDescriptor>
    }


    override fun getValueParameters(): List<ValueParameterDescriptor> {
        return emptyList()
    }

    override fun getReturnType(): CangJieType? {
        return returnType
    }

    override fun <R, D> accept(visitor: DeclarationDescriptorVisitor<R, D>, data: D?): R? {
        return visitor.visitPropertyGetterDescriptor(this, data)
    }


}
