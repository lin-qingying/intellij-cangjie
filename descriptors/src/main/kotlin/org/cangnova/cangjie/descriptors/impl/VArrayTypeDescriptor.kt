/*
 * Copyright 2025 LinQingYing. and contributors.
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

package org.cangnova.cangjie.descriptors.impl

import org.cangnova.cangjie.builtins.CangJieBuiltIns
import org.cangnova.cangjie.descriptors.ClassConstructorDescriptor
import org.cangnova.cangjie.descriptors.ClassKind
import org.cangnova.cangjie.descriptors.DeclarationDescriptor
import org.cangnova.cangjie.descriptors.FunctionDescriptor
import org.cangnova.cangjie.descriptors.Modality
import org.cangnova.cangjie.descriptors.SourceElement
import org.cangnova.cangjie.descriptors.TypeParameterDescriptor
import org.cangnova.cangjie.descriptors.annotations.Annotations
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.name.OperatorNameConventions
import org.cangnova.cangjie.resolve.module
import org.cangnova.cangjie.resolve.scopes.GivenFunctionsMemberScope
import org.cangnova.cangjie.resolve.scopes.MemberScope
import org.cangnova.cangjie.storage.NotNullLazyValue
import org.cangnova.cangjie.storage.StorageManager
import org.cangnova.cangjie.types.CangJieType
import org.cangnova.cangjie.types.SimpleType
import org.cangnova.cangjie.types.VArrayType
import org.cangnova.cangjie.types.Variance
import org.cangnova.cangjie.types.asTypeProjection
import org.cangnova.cangjie.types.checker.CangJieTypeRefiner
import org.cangnova.cangjie.types.createFunctionType

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
                    this@VArrayTypeDescriptor.defaultType

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
                    this@VArrayTypeDescriptor.defaultType
                )
            }
        )

    }

    lateinit var typeParameters: List<TypeParameterDescriptor>

    override fun getUnsubstitutedMemberScope(cangjieTypeRefiner: CangJieTypeRefiner) = memberScope
    override var unsubstitutedMemberScope: MemberScope
        get() = super.unsubstitutedMemberScope
        set(value) {}


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

    override var constructors: List<ClassConstructorDescriptor>
        get() = _constructors
        set(value) {}


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

