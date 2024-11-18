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

package com.linqingying.cangjie.descriptors.impl.basic

import com.linqingying.cangjie.builtins.CangJieBuiltIns
import com.linqingying.cangjie.descriptors.*
import com.linqingying.cangjie.descriptors.annotations.Annotations
import com.linqingying.cangjie.descriptors.impl.ClassDescriptorImpl
import com.linqingying.cangjie.descriptors.impl.TypeParameterDescriptorImpl
import com.linqingying.cangjie.name.Name
import com.linqingying.cangjie.resolve.scopes.GivenFunctionsMemberScope
import com.linqingying.cangjie.storage.NotNullLazyValue
import com.linqingying.cangjie.storage.StorageManager
import com.linqingying.cangjie.types.CangJieType
import com.linqingying.cangjie.types.VArrayType
import com.linqingying.cangjie.types.Variance
import com.linqingying.cangjie.types.checker.CangJieTypeRefiner
import com.linqingying.cangjie.types.util.asTypeProjection

class VArrayTypeDescriptor(
    val argumentType: CangJieType,
    val size: Int,
    containingDeclaration: DeclarationDescriptor,
    val builtIns: CangJieBuiltIns,
    storageManager: StorageManager
) : ClassDescriptorImpl(
    containingDeclaration,
    Name.identifier("VArray"),
    Modality.FINAL, ClassKind.CLASS, listOf(builtIns.anyType), SourceElement.NO_SOURCE, false, storageManager
) {
    private val memberScope = VArrayClassScope(storageManager, this)

    val typeParameter: TypeParameterDescriptorImpl

    init {
        typeParameter = TypeParameterDescriptorImpl.createWithDefaultBound(
            this, Annotations.EMPTY, Variance.INVARIANT,
            argumentType.constructor.declarationDescriptor?.name ?: Name.identifier("T"), 0, storageManager
        ) as TypeParameterDescriptorImpl
        argumentType.arguments.forEach {
            typeParameter.addUpperBound(it.type)

        }

    }

    private val typeParameters = listOf(
        typeParameter
    )

    override fun getUnsubstitutedMemberScope(cangjieTypeRefiner: CangJieTypeRefiner) = memberScope

    override fun getDeclaredTypeParameters(): List<TypeParameterDescriptor> {
        return typeParameters
    }

    private val defaultTypeVarrayType: NotNullLazyValue<VArrayType> = storageManager.createLazyValue {
        VArrayType(
            size, argumentType.asTypeProjection(), typeConstructor, false, memberScope
        ) {
            null
        }
    }

    override fun getDefaultType(): VArrayType {
        return this.defaultTypeVarrayType.invoke()
    }

}

class VArrayClassScope(
    storageManager: StorageManager,
    containingClass: VArrayTypeDescriptor
) : GivenFunctionsMemberScope(storageManager, containingClass) {
    override fun computeDeclaredFunctions(): List<FunctionDescriptor> = emptyList()
}

