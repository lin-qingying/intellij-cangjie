package com.huawei.cangjie.descriptors.impl

import com.huawei.cangjie.descriptors.*
import com.huawei.cangjie.descriptors.annotations.Annotations
import com.huawei.cangjie.name.Name
import com.huawei.cangjie.resolve.lazy.descriptors.LazyClassDescriptor
import com.huawei.cangjie.types.CangJieType
import com.huawei.cangjie.types.checker.CangJieTypeChecker
import com.huawei.cangjie.types.expressions.match.ClassAndEnumConstructorDescriptor


val String.name get() = Name.identifier(this)
fun EnumEntryConstructorDescriptor.getEnumTypeParameters(): List<TypeParameterDescriptor> {
    return (containingDeclaration.containingDeclaration as? LazyClassDescriptor)?.declaredTypeParameters?.toList()
        ?: emptyList()
}

class EnumEntryConstructorDescriptor(

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

), ClassAndEnumConstructorDescriptor {
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


    override val visibility: DescriptorVisibility
        get() = containingDeclaration.visibility

    override fun getTypeParameters(): List<TypeParameterDescriptor> {
//        if(values ?.isEmpty() == true) return emptyList()
        val typeParameters = getEnumTypeParameters()
//        val tempTypeParameters = mutableListOf<TypeParameterDescriptor>()
//        for (typeParameter in typeParameters) {
//            if (values!!.map {it.type.constructor.declarationDescriptor?.name }.contains(typeParameter.name))
//                tempTypeParameters.add(typeParameter)
//        }

        return typeParameters
    }

    fun getEnumType(): CangJieType {

        var _containingDeclaration: ClassDescriptor? = containingDeclaration

        while (_containingDeclaration != null) {
            if (_containingDeclaration.kind != ClassKind.ENUM) {
                _containingDeclaration = _containingDeclaration.containingDeclaration as? ClassDescriptor
            } else {
                break
            }
        }
        return _containingDeclaration?.defaultType ?: this.containingDeclaration.defaultType

    }

    override fun getReturnType(): CangJieType {
        return getEnumType()
    }

    override fun equals(other: Any?): Boolean {


        if (this === other) return true
        if (other !is EnumEntryConstructorDescriptor) return false
        if (this.containingDeclaration != other.containingDeclaration) return false
        if (this.values?.size != other.values?.size) return false

        if (this.values?.any {
                other.values?.any { value2 ->
                    CangJieTypeChecker.DEFAULT.equalTypes(it.type, value2.type)
                } == true
            } == false) {
            return false
        }



        return true
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

    fun getConstructorTypes(): List<CangJieType> {
        return getValueParameters().map {
            it.type
        }
    }

    override fun hashCode(): Int {
        var result = types.hashCode()
        result = 31 * result + (values?.hashCode() ?: 0)
        return result
    }
}
