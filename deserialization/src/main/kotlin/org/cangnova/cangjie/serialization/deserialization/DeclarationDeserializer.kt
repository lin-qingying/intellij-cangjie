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

import org.cangnova.cangjie.descriptors.*
import org.cangnova.cangjie.descriptors.annotations.Annotations
import org.cangnova.cangjie.descriptors.impl.*
import org.cangnova.cangjie.metadata.model.fb.FbAnno
import org.cangnova.cangjie.metadata.model.fb.FbDecl
import org.cangnova.cangjie.metadata.model.fb.FbDeclInfo
import org.cangnova.cangjie.metadata.model.fb.FbDeclKind
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.serialization.deserialization.descriptors.DeserializedClassConstructorDescriptor
import org.cangnova.cangjie.serialization.deserialization.descriptors.DeserializedSimpleFunctionDescriptor
import org.cangnova.cangjie.types.CangJieType
import org.cangnova.cangjie.types.error.ErrorPropertyDescriptor
import org.cangnova.cangjie.types.error.ErrorTypeAliasDescriptor
import org.cangnova.cangjie.types.error.ErrorVariableDescriptor

enum class AnnotatedCallableKind {
    FUNCTION,
    PROPERTY,
    VARIABLE,
    PROPERTY_GETTER,
    PROPERTY_SETTER
}


class DeclarationDeserializer(private val c: DeserializationContext) {
    private fun getAnnotations(a: List<FbAnno>): Annotations {
        return Annotations.EMPTY
    }

    private fun getCallableMemberDescriptorKind(decl: FbDecl): CallableMemberDescriptor.Kind {

//        TODO 暂时返回固定值
        return CallableMemberDescriptor.Kind.DECLARATION
    }

    fun loadConstructor(decl: FbDecl): ClassConstructorDescriptor {

        val info = decl.info as FbDeclInfo.FuncInfo

        val classDescriptor = c.containingDeclaration as ClassDescriptor
        val descriptor = DeserializedClassConstructorDescriptor(
            classDescriptor,
            null,
            getAnnotations(
                decl.annotations
            ),
            false,
            CallableMemberDescriptor.Kind.DECLARATION,
            decl,


            c.containerSource
        )

        val local = c.childContext(descriptor)
        descriptor.initialize(
            local.declDeserializer.valueParameters(
                c.declTable.get(info.funcBody.params),
            ),
        )
        descriptor.setReturnType(classDescriptor.defaultType)

//        descriptor.setHasStableParameterNames(!Flags.IS_CONSTRUCTOR_WITH_NON_STABLE_PARAMETER_NAMES.get(proto.flags))

        return descriptor
    }

    fun loadProperty(decl: FbDecl): PropertyDescriptor {

        return ErrorPropertyDescriptor()
    }

    fun loadTypeAlias(decl: FbDecl): TypeAliasDescriptor {
        return ErrorTypeAliasDescriptor()
    }

    fun loadVariable(decl: FbDecl): VariableDescriptor {

        return ErrorVariableDescriptor()
    }

    fun loadClass(decl: FbDecl): ClassDescriptor? {
        val classId = ClassId(c.`package`.packageName, decl.name)
//        val fragments = c.components.packageFragmentProvider.packageFragments(classId.packageFqName)
//        val fragment = fragments.firstOrNull { it !is DeserializedPackageFragment /*|| it.hasTopLevelClass(classId.shortClassName)*/ }
//            ?: return null
////
//    c.    components.createContext(
//            fragment, c.`package`,
//
//
//            metadataVersion,
//            containerSource = null
//        )
//        DeserializedClassDescriptor(c, decl)

        return c.components.deserializeClass(classId)
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

    fun loadFunction(decl: FbDecl): SimpleFunctionDescriptor {
        assert(decl.kind == FbDeclKind.FuncDecl) { "Expected function, but $decl found" }
        val info = decl.info as FbDeclInfo.FuncInfo


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
            unsubstitutedValueParameters = local.declDeserializer.valueParameters(c.declTable.get(info.funcBody.params)),
            unsubstitutedReturnType = local.typeDeserializer.type(c.typeTable.get(info.funcBody.retType)),
            userDataMap = emptyMap()
        )

//        function.isOperator =

        return function
    }


    private fun valueParameters(
        valueParameters: List<FbDecl>,

        ): List<ValueParameterDescriptor> {
        if (valueParameters.isEmpty()) return emptyList()
        assert(valueParameters.any { it.kind == FbDeclKind.FuncParam }) { "Expected value parameters, but $valueParameters found" }
        val callableDescriptor = c.containingDeclaration as CallableDescriptor

        return valueParameters.mapIndexed { i, decl ->

            val info = decl.info as FbDeclInfo.ParamInfo
            ValueParameterDescriptorImpl(
                callableDescriptor, null, i,
                getAnnotations(decl.annotations),
                decl.name, info.isNamedParam,
                c.typeDeserializer.type(c.typeTable.get(decl.type)),

                info.defaultVal != 0,
                SourceElement.NO_SOURCE
            )


        }.toList()
    }
}
