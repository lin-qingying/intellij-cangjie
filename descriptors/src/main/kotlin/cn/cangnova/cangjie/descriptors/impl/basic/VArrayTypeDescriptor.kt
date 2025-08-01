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

package cn.cangnova.cangjie.descriptors.impl.basic

import cn.cangnova.cangjie.builtins.CangJieBuiltIns
import cn.cangnova.cangjie.builtins.createFunctionType
import cn.cangnova.cangjie.descriptors.*
import cn.cangnova.cangjie.descriptors.annotations.Annotations
import cn.cangnova.cangjie.descriptors.impl.ClassConstructorDescriptorImpl
import cn.cangnova.cangjie.descriptors.impl.ClassDescriptorImpl
import cn.cangnova.cangjie.descriptors.impl.TypeParameterDescriptorImpl
import cn.cangnova.cangjie.descriptors.impl.ValueParameterDescriptorImpl
import cn.cangnova.cangjie.name.OperatorNameConventions
import cn.cangnova.cangjie.resolve.descriptorUtil.module
import cn.cangnova.cangjie.resolve.scopes.GivenFunctionsMemberScope
import cn.cangnova.cangjie.storage.NotNullLazyValue
import cn.cangnova.cangjie.types.CangJieType
import cn.cangnova.cangjie.types.VArrayType
import cn.cangnova.cangjie.types.Variance
import cn.cangnova.cangjie.types.checker.CangJieTypeRefiner
import cn.cangnova.cangjie.types.util.asTypeProjection

class VArrayTypeDescriptor(

    containingDeclaration: DeclarationDescriptor,
    val builtIns: CangJieBuiltIns,
    val storageManager: StorageManager
) : ClassDescriptorImpl(
    containingDeclaration,
    Name.identifier("VArray"),
    Modality.FINAL, ClassKind.CLASS, listOf(builtIns.anyType), SourceElement.NO_SOURCE, false, storageManager
) {
    private val memberScope = VArrayClassScope(storageManager, this)

    lateinit var typeParameter: TypeParameterDescriptorImpl

    var size: Int = -1

    private var isInitialized: Boolean = false

    lateinit var argumentType: CangJieType

    private fun checkInitialized() {
        require(isInitialized) { "VArrayTypeDescriptor is not initialized" }
    }

    var _constructors: MutableList<ClassConstructorDescriptor> = mutableListOf()
    fun init(
        argumentType: CangJieType,
        size: Int,
    ) {
        isInitialized = true
        this.argumentType = argumentType
        this.size = size
        typeParameter = TypeParameterDescriptorImpl.createWithDefaultBound(
            this, Annotations.EMPTY, Variance.INVARIANT,
            argumentType.constructor.declarationDescriptor?.name ?: Name.identifier("T"), 0, storageManager
        ) as TypeParameterDescriptorImpl
        argumentType.arguments.forEach {
            typeParameter.addUpperBound(it.type)

        }
        typeParameters = listOf(
            typeParameter
        )

        _constructors.add(
            ClassConstructorDescriptorImpl.create(
                this, Annotations.EMPTY, false, SourceElement.NO_SOURCE, false
            ).apply {
                initialize(
                    listOf(
                        ValueParameterDescriptorImpl.createWithDestructuringDeclarations(
                            this, null, 0, Annotations.EMPTY, Name.identifier("initElement"),
                            false, createFunctionType(
                                builtIns, Annotations.EMPTY, null, emptyList(), listOf(argumentType), null, argumentType
                            ), false, SourceElement.NO_SOURCE
                        )
                    ),
                    this@VArrayTypeDescriptor.getDefaultType()

                )

            }

        )
        _constructors.add(
            ClassConstructorDescriptorImpl.create(
                this, Annotations.EMPTY, false, SourceElement.NO_SOURCE, false
            ).apply {
                initialize(
                    listOf(
                        ValueParameterDescriptorImpl.createWithDestructuringDeclarations(
                            this, null, 0, Annotations.EMPTY, Name.identifier("repeat"),
                            true, argumentType, false, SourceElement.NO_SOURCE
                        )
                    ),
                    this@VArrayTypeDescriptor.getDefaultType()
                )
            }
        )

    }

    lateinit var typeParameters: List<TypeParameterDescriptor>

    override fun getUnsubstitutedMemberScope(cangjieTypeRefiner: CangJieTypeRefiner) = memberScope

    override val declaredTypeParameters: List<TypeParameterDescriptor>
        get() {
            checkInitialized()
            return typeParameters
        }

    private val defaultTypeVarrayType: NotNullLazyValue<VArrayType> = storageManager.createLazyValue {
        checkInitialized()
        VArrayType(
            size, argumentType.asTypeProjection(), typeConstructor, false, memberScope
        ) {
            null
        }
    }



    override val defaultType: VArrayType
        get() {

            checkInitialized()
            return this.defaultTypeVarrayType.invoke()
        }

    override val constructors: Collection<ClassConstructorDescriptor>
        get() = _constructors

}

class VArrayClassScope(
    storageManager: StorageManager,
    containingClass: VArrayTypeDescriptor
) : GivenFunctionsMemberScope(storageManager, containingClass) {

    val func = storageManager.createLazyValue {

        val result = mutableListOf<OperatorFunctionDescriptor>()

        result.add(
            OperatorFunctionDescriptor(
                containingClass, OperatorNameConventions.GET,
                listOf(
                    ValueNameAndType(
                        "index",
                        containingClass.module.builtIns.int64Type
                    )
                ), containingClass.defaultType
            )
        )
        result.add(
            OperatorFunctionDescriptor(
                containingClass, OperatorNameConventions.SET,
                listOf(
                    ValueNameAndType(
                        "index",
                        containingClass.module.builtIns.int64Type
                    ),
                    ValueNameAndType(
                        "value",
                        containingClass.argumentType,
                        isNamed = true
                    )
                ), containingClass.defaultType
            )
        )
        result
    }

    override fun computeDeclaredFunctions(): List<FunctionDescriptor> {

        return func()
    }
}

