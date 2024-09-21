package com.huawei.cangjie.descriptors.impl

import com.huawei.cangjie.descriptors.*
import com.huawei.cangjie.descriptors.annotations.Annotations
import com.huawei.cangjie.name.Name
import com.huawei.cangjie.types.CangJieType


val String.name get() = Name.identifier(this)

class EnumEntryConstructorDescriptor(
//    val types: List<CangJieType>,
    containingDeclaration: ClassDescriptor,
    original: ConstructorDescriptor?,
    source: SourceElement,
    val types: () -> List<CangJieType>
) : ClassConstructorDescriptorImpl(
    containingDeclaration,
    original,
    Annotations.EMPTY,
    true,
    CallableMemberDescriptor.Kind.DECLARATION, //声明
    source

) {
    private var values: MutableList<ValueParameterDescriptor>? = null

    private fun fillValues() {
        values = mutableListOf()
        val types = types()
        for (index in types.indices) {
            values!!.add(
                ValueParameterDescriptorImpl.createWithDestructuringDeclarations(
                    this,
                    null,
                    index,
                    Annotations.EMPTY,
                    "e$index".name,
                    false,
                    types[index],
                    false,
                    SourceElement.NO_SOURCE,
                    null
                )
            )
        }
    }

    override fun getTypeParameters(): List<TypeParameterDescriptor> {
        return emptyList()
    }

    fun getEnumType(): CangJieType {

        var _containingDeclaration: ClassDescriptor? = containingDeclaration

        while (_containingDeclaration != null) {
            if (_containingDeclaration.kind != ClassKind.ENUM) {
                _containingDeclaration = _containingDeclaration.containingDeclaration as? ClassDescriptor
            }else{
                break
            }
        }
        return _containingDeclaration?.defaultType ?: this.containingDeclaration.defaultType

    }

    override fun getReturnType(): CangJieType {
        return getEnumType()
    }

    override fun hasSynthesizedParameterNames(): Boolean {
        return false
    }

    override fun getValueParameters(): List<ValueParameterDescriptor> {
        if (values == null) {
            fillValues()
        }
        return values!!
    }
}
