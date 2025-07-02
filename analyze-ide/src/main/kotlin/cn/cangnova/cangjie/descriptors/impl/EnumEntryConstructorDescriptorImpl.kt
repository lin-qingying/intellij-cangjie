/*
 * Copyright 2024 LinQingYing. and contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * The use of this source code is governed by the Apache License 2.0,
 * which allows users to freely use, modify, and distribute the code,
 * provided they adhere to the terms of the license.
 *
 * The software is provided "as-is", and the authors are not responsible for
 * any damages or issues arising from its use.
 *
 */

package cn.cangnova.cangjie.descriptors.impl

import cn.cangnova.cangjie.descriptors.*
import cn.cangnova.cangjie.descriptors.annotations.Annotations
import cn.cangnova.cangjie.name.Name
import cn.cangnova.cangjie.resolve.lazy.descriptors.LazyClassDescriptor
import cn.cangnova.cangjie.types.CangJieType
import cn.cangnova.cangjie.types.checker.CangJieTypeChecker
import cn.cangnova.cangjie.types.expressions.match.ClassAndEnumConstructorDescriptor


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
    private lateinit var values: MutableList<ValueParameterDescriptor>

    init {
values = mutableListOf()
        val types = types()


        for (index in types.indices) {
            values.add(
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

    private fun fillValues() {
        values = mutableListOf()
        val types = types()


        for (index in types.indices) {
            values.add(
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

    override fun getValueParameters(): List<ValueParameterDescriptor> {
        if (!::values.isInitialized) {
            fillValues()
        }
        return values
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
        if (this.values.size != other.values.size) return false

        if (!this.values.any {
                other.values.any { value2 ->
                    CangJieTypeChecker.DEFAULT.equalTypes(it.type, value2.type)
                }
            }) {
            return false
        }



        return true
    }

    override fun hasSynthesizedParameterNames(): Boolean {
        return false
    }


    fun getConstructorTypes(): List<CangJieType> {
        return getValueParameters().map {
            it.type
        }
    }

    override fun hashCode(): Int {
        var result = types.hashCode()
        result = 31 * result + values.hashCode()
        return result
    }
}
