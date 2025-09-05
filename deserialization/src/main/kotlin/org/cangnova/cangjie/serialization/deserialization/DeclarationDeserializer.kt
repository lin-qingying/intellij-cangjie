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

package org.cangnova.cangjie.serialization.deserialization

import org.cangnova.cangjie.descriptors.annotations.AnnotationsImpl
import com.google.protobuf.MessageLite
import org.cangnova.cangjie.metadata.model.*
import org.cangnova.cangjie.descriptors.*
import org.cangnova.cangjie.descriptors.annotations.AnnotationDescriptorImpl
import org.cangnova.cangjie.descriptors.annotations.Annotations
import org.cangnova.cangjie.descriptors.impl.*
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.serialization.deserialization.descriptors.DeserializedSimpleFunctionDescriptor
import org.cangnova.cangjie.types.CangJieType
import org.cangnova.cangjie.types.error.ErrorClassDescriptor
import org.cangnova.cangjie.types.error.ErrorPropertyDescriptor
import org.cangnova.cangjie.types.error.ErrorTypeAliasDescriptor
import org.cangnova.cangjie.types.error.ErrorVariableDescriptor
import kotlin.collections.get

enum class AnnotatedCallableKind {
    FUNCTION,
    PROPERTY,
    VARIABLE,
    PROPERTY_GETTER,
    PROPERTY_SETTER
}


class DeclarationDeserializer(private val c: DeserializationContext) {
    private fun getAnnotations(a: List<Anno>): Annotations {
        return Annotations.EMPTY
    }

    private fun getCallableMemberDescriptorKind(decl: Decl): CallableMemberDescriptor.Kind {

//        TODO 暂时返回固定值
        return CallableMemberDescriptor.Kind.DECLARATION
    }

    fun loadProperty(decl: Decl): PropertyDescriptor {

        return ErrorPropertyDescriptor()
    }

    fun loadTypeAlias(decl: Decl): TypeAliasDescriptor {
        return ErrorTypeAliasDescriptor()
    }

    fun loadVariable(decl: Decl): VariableDescriptor {

        return ErrorVariableDescriptor()
    }

    fun loadClass(decl: Decl): ClassDescriptor? {
        return ErrorClassDescriptor()
    }

    private fun DeserializedSimpleFunctionDescriptor.initializeWithCoroutinesExperimentalityStatus(
        extensionReceiverParameter: ReceiverParameterDescriptor? = null,
        dispatchReceiverParameter: ReceiverParameterDescriptor? = null,
        contextReceiverParameters: List<ReceiverParameterDescriptor> = emptyList(),
        typeParameters: List<TypeParameterDescriptor> = emptyList(),
        unsubstitutedValueParameters: List<ValueParameterDescriptor> = emptyList(),
        unsubstitutedReturnType: CangJieType? = null,
        modality: Modality? = null,
        visibility: DescriptorVisibility = DescriptorVisibilities.PUBLIC,
        userDataMap: Map<out CallableDescriptor.UserDataKey<*>, Any>? = null
    ) {
        initialize(
            extensionReceiverParameter,
            dispatchReceiverParameter,
            contextReceiverParameters,
            typeParameters,
            unsubstitutedValueParameters,
            unsubstitutedReturnType,
            modality,
            visibility,
            userDataMap
        )
    }

    fun loadFunction(decl: Decl): SimpleFunctionDescriptor {
        if (decl.kind != DeclKind.FuncDecl) error("Expected function, but $decl found")


        val annotations = getAnnotations(decl.annotations)

        val callableMemberDescriptorKind = getCallableMemberDescriptorKind(decl)
        val function = DeserializedSimpleFunctionDescriptor(
            c.containingDeclaration, /* original = */
            null,
            annotations,
            decl.name,
            callableMemberDescriptorKind,
            decl,
            c.containerSource


        )

        val local = c.childContext(function)
        function.initializeWithCoroutinesExperimentalityStatus(

            userDataMap = emptyMap()
        )

//        function.isOperator =

        return function
    }
}
