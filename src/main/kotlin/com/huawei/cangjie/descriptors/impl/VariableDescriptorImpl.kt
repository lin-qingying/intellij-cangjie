package com.huawei.cangjie.descriptors.impl

import com.huawei.cangjie.descriptors.*
import com.huawei.cangjie.descriptors.annotations.Annotations
import com.huawei.cangjie.name.Name
import com.huawei.cangjie.types.CangJieType
import com.huawei.cangjie.types.TypeSubstitutor
import com.huawei.cangjie.utils.ReadOnly


open class VariableDescriptorImpl(
    containingDeclaration: DeclarationDescriptor,
    name: Name,
    outType: CangJieType?,
    isVar: Boolean,
    source: SourceElement,
    visibility:DescriptorVisibility
) : VariableDescriptorWithInitializerImpl(containingDeclaration, Annotations.EMPTY, name, outType, isVar, source) {
private var _visibility:DescriptorVisibility = visibility
    companion object {
        @JvmStatic
        fun create(
            containingDeclaration: DeclarationDescriptor,
            name: Name,
             visibility:DescriptorVisibility,
            isVar: Boolean,
            source: SourceElement
        ): VariableDescriptorImpl {
            return VariableDescriptorImpl(containingDeclaration, name, null, isVar, source,visibility)
        }
    }

    private var overriddenProperties: Collection<VariableDescriptorImpl>? = null

    override fun getOverriddenDescriptors(): Collection<VariableDescriptorImpl> {
        return overriddenProperties ?: emptyList()
    }


    override fun <R, D> accept(visitor: DeclarationDescriptorVisitor<R, D>, data: D?): R? {
        return visitor.visitVariableDescriptor(this, data)

    }

    override var visibility: DescriptorVisibility =  _visibility


    override fun substitute(substitutor: TypeSubstitutor): VariableDescriptor? {
        return null
    }
}
