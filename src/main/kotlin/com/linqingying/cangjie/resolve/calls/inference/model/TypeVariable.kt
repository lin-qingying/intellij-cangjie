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

package com.linqingying.cangjie.resolve.calls.inference.model

import com.linqingying.cangjie.builtins.CangJieBuiltIns
import com.linqingying.cangjie.descriptors.ClassifierDescriptor
import com.linqingying.cangjie.descriptors.TypeParameterDescriptor
import com.linqingying.cangjie.name.Name
import com.linqingying.cangjie.name.SpecialNames
import com.linqingying.cangjie.resolve.calls.inference.CallHandle
import com.linqingying.cangjie.resolve.calls.model.PostponableCangJieCallArgument
import com.linqingying.cangjie.resolve.descriptorUtil.builtIns
import com.linqingying.cangjie.resolve.descriptorUtil.hasOnlyInputTypesAnnotation
import com.linqingying.cangjie.types.*
import com.linqingying.cangjie.types.checker.CangJieTypeRefiner
import com.linqingying.cangjie.types.checker.NewTypeVariableConstructor
import com.linqingying.cangjie.types.model.TypeVariableMarker
import com.linqingying.cangjie.types.model.TypeVariableTypeConstructorMarker

class TypeVariableTypeConstructor(
    private val builtIns: CangJieBuiltIns,
    val debugName: String,
    override val originalTypeParameter: TypeParameterDescriptor?
) : NewTypeVariableConstructor, TypeVariableTypeConstructorMarker {
    override fun getParameters(): List<TypeParameterDescriptor> = emptyList()
    override fun getSupertypes(): Collection<CangJieType> = emptyList()

        override fun isFinal(): Boolean = false
    override fun isDenotable(): Boolean = false
    override fun getDeclarationDescriptor(): ClassifierDescriptor? = null

    override fun getBuiltIns() = builtIns

    @TypeRefinement
    override fun refine(cangjieTypeRefiner: CangJieTypeRefiner): TypeConstructor = this

    override fun toString() = "TypeVariable($debugName)"

    var isContainedInInvariantOrContravariantPositions: Boolean = false
}

fun TypeConstructor.typeForTypeVariable(): SimpleType {
    require(this is TypeVariableTypeConstructor)
    return CangJieTypeFactory.simpleTypeWithNonTrivialMemberScope(
        TypeAttributes.Empty, this, arguments = emptyList(),
        nullable = false, memberScope = builtIns.any.unsubstitutedMemberScope
    )
}

class TypeVariableFromCallableDescriptor(
    val originalTypeParameter: TypeParameterDescriptor
) : NewTypeVariable(
    originalTypeParameter.builtIns,
    SpecialNames.safeIdentifier(originalTypeParameter.name).identifier,
    originalTypeParameter
) {
    override fun hasOnlyInputTypesAnnotation(): Boolean = originalTypeParameter.hasOnlyInputTypesAnnotation()
}

sealed class NewTypeVariable(
    builtIns: CangJieBuiltIns,
    name: String,
    originalTypeParameter: TypeParameterDescriptor? = null
) : TypeVariableMarker {
    val freshTypeConstructor = TypeVariableTypeConstructor(builtIns, name, originalTypeParameter)

    // member scope is used if we have receiver with type TypeVariable(T)
    // todo add to member scope methods from supertypes for type variable
    val defaultType: SimpleType = freshTypeConstructor.typeForTypeVariable()
    abstract fun hasOnlyInputTypesAnnotation(): Boolean

    override fun toString() = freshTypeConstructor.toString()
}
class TypeVariable(
    val call: CallHandle,
    internal val freshTypeParameter: TypeParameterDescriptor,
    val originalTypeParameter: TypeParameterDescriptor,
    val isExternal: Boolean
) {
    val name: Name get() = originalTypeParameter.name

    val type: CangJieType get() = freshTypeParameter.defaultType

    fun hasOnlyInputTypesAnnotation(): Boolean =
        originalTypeParameter.hasOnlyInputTypesAnnotation()
}

class TypeVariableForCallableReferenceParameterType(
    builtIns: CangJieBuiltIns,
    name: String
) : NewTypeVariable(builtIns, name) {
    override fun hasOnlyInputTypesAnnotation(): Boolean = false
}

class TypeVariableForCallableReferenceReturnType(
    builtIns: CangJieBuiltIns,
    name: String
) : NewTypeVariable(builtIns, name) {
    override fun hasOnlyInputTypesAnnotation(): Boolean = false
}
class TypeVariableForLambdaReturnType(
    builtIns: CangJieBuiltIns,
    name: String
) : NewTypeVariable(builtIns, name) {
    override fun hasOnlyInputTypesAnnotation(): Boolean = false
}
class TypeVariableForLambdaParameterType(
    val atom: PostponableCangJieCallArgument,
    val index: Int,
    builtIns: CangJieBuiltIns,
    name: String
) : NewTypeVariable(builtIns, name) {
    override fun hasOnlyInputTypesAnnotation(): Boolean = false
}
