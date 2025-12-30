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

package org.cangnova.cangjie.serialization.deserialization

import org.cangnova.cangjie.descriptors.*
import org.cangnova.cangjie.descriptors.annotations.Annotations
import org.cangnova.cangjie.descriptors.impl.PropertyGetterDescriptorImpl
import org.cangnova.cangjie.descriptors.impl.PropertySetterDescriptorImpl
import org.cangnova.cangjie.descriptors.impl.ValueParameterDescriptorImpl
import org.cangnova.cangjie.metadata.model.wrapper.*
import org.cangnova.cangjie.serialization.deserialization.descriptors.*
import org.cangnova.cangjie.types.CangJieType

enum class AnnotatedCallableKind {
    FUNCTION,
    PROPERTY,
    VARIABLE,
    PROPERTY_GETTER,
    PROPERTY_SETTER
}


class DeclarationDeserializer(private val c: DeserializationContext) {
    private fun getAnnotations(a: List<AnnotationWrapper>): Annotations {
        return Annotations.EMPTY
    }

    fun loadEnumConstructor(decl: EnumConstructorWrapper): EnumConstructorDescriptor {


        val classDescriptor = c.containingDeclaration as EnumDescriptor
        val descriptor = DeserializedEnumConstructorDescriptor(
            classDescriptor,
            null,
            getAnnotations(
                decl.annotations
            ),
            decl,


            c.containerSource
        )

        val local = c.childContext(descriptor, listOf())
        descriptor.initialize(
            unsubstitutedValueParameters = local.declDeserializer.valueParameters(
                decl.valueParameters,
            ),
            visibility = decl.visibility,
            unsubstitutedReturnType = classDescriptor.defaultType
        )

        return descriptor
    }

    fun loadConstructor(decl: ConstructorWrapper, isPrimary: Boolean): ClassConstructorDescriptor {


        val classDescriptor = c.containingDeclaration as ClassDescriptor
        val descriptor = DeserializedClassConstructorDescriptor(
            classDescriptor,
            null,
            getAnnotations(
                decl.annotations
            ),
            isPrimary,
            CallableMemberDescriptor.Kind.DECLARATION,
            decl,


            c.containerSource
        )

        val local = c.childContext(descriptor, listOf())
        descriptor.initialize(
            local.declDeserializer.valueParameters(
                decl.valueParameters,
            ),
            visibility = decl.visibility
        )
        descriptor.setReturnType(classDescriptor.defaultType)

//        descriptor.setHasStableParameterNames(!Flags.IS_CONSTRUCTOR_WITH_NON_STABLE_PARAMETER_NAMES.get(proto.flags))

        return descriptor
    }

    fun loadProperty(decl: PropertyWrapper): PropertyDescriptor {

        val property = DeserializedPropertyDescriptor(
            c.containingDeclaration, null,
            getAnnotations(

                decl.annotations
            ),
            decl.modality,
            decl.visibility,
            decl.isVar,
            decl.name,
            decl.kind,


            decl,

            c.containerSource
        )

        val local = c.childContext(property, emptyList())

        val receiverAnnotations = Annotations.EMPTY

        property.setType(
            local.typeDeserializer.type(decl.returnType),
            local.typeDeserializer.ownTypeParameters,
            getDispatchReceiverParameter()

        )


        val getter = if (decl.getter != null) {

            val isExternal = false
            val isInline = decl.getter!!.isInLine
            val annotations = getAnnotations(decl.getter!!.annotations)
            val getter =
                PropertyGetterDescriptorImpl(
                    property,
                    annotations,
                    decl.getter!!.modality,
                    decl.getter!!.visibility,
                    /* isDefault = */


                    property.kind, null, SourceElement.NO_SOURCE
                )

            getter.initialize(property.returnType)
            getter
        } else {
            null
        }

        val setter = if (decl.setter != null) {

            val isInline = decl.setter!!.isInLine
            val annotations = getAnnotations(

                decl.setter!!.annotations
            )

            val setter = PropertySetterDescriptorImpl(
                property,
                annotations,
                decl.setter!!.modality,
                decl.setter!!.visibility,
                /*        *//* isDefault = *//* !isNotDefault,
*/
                property.kind, null, SourceElement.NO_SOURCE
            )
            val setterLocal = local.childContext(setter, listOf())
            val valueParameters = setterLocal.declDeserializer.valueParameters(
                decl.setter!!.valueParameters,
            )
            setter.initialize(valueParameters.single())
            setter

        } else {
            null
        }


        property.initialize(
            getter, setter,

            )

        return property
    }

    fun loadTypeAlias(decl: TypeAliasWrapper): TypeAliasDescriptor {
        val annotations = getAnnotations(decl.annotations)
        val visibility = decl.visibility
        val typeAlias = DeserializedTypeAliasDescriptor(
            c.storageManager, c.containingDeclaration, annotations, decl.name,
            visibility, decl, c.containerSource
        )

        val local = c.childContext(typeAlias, listOf())
        typeAlias.initialize(
            local.typeDeserializer.ownTypeParameters,
            local.typeDeserializer.simpleType(decl.underlyingType, expandTypeAliases = false),
            local.typeDeserializer.simpleType(decl.expandedType, expandTypeAliases = false)
        )

        return typeAlias
    }

    fun loadVariable(decl: VariableWrapper): VariableDescriptor {
        val variable = DeserializedVariableDescriptor(
            c.containingDeclaration, null,
            getAnnotations(

                decl.annotations
            ),
            decl.modality,
            decl.visibility,
            decl.isVar,
            decl.name,
            decl.kind,
            decl.declaresDefaultValue,
            decl,


            c.containerSource
        )

        val local = c.childContext(variable, listOf())
        val receiverAnnotations =
            Annotations.EMPTY
        variable.setType(
            local.typeDeserializer.type(decl.returnType),
            local.typeDeserializer.ownTypeParameters,
            getDispatchReceiverParameter()
        )
        return variable
    }

    private fun getDispatchReceiverParameter(): ReceiverParameterDescriptor? {
        return (c.containingDeclaration as? ClassDescriptor)?.thisAsReceiverParameter
    }

    fun loadClass(decl: ClassDeclWrapper): ClassDescriptor? {
//        val classId = ClassId(c.`package`.packageName, decl.name)
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

        return c.components.deserializeClass(decl.classId)
    }

    private fun DeserializedSimpleFunctionDescriptor.initializeWithCoroutinesExperimentalityStatus(
        dispatchReceiverParameter: ReceiverParameterDescriptor? = null,
        typeParameters: List<TypeParameterDescriptor> = emptyList(),
        unsubstitutedValueParameters: List<ValueParameterDescriptor> = emptyList(),
        unsubstitutedReturnType: CangJieType? = null,
        modality: Modality? = null,
        visibility: DescriptorVisibility = DescriptorVisibilities.PUBLIC,
        userDataMap: Map<out CallableDescriptor.UserDataKey<*>, Any>? = null
    ) {
        initialize(
            dispatchReceiverParameter,
            typeParameters,
            unsubstitutedValueParameters,
            unsubstitutedReturnType,
            modality,
            visibility,
            userDataMap
        )
    }

    fun loadFunction(decl: FunctionWrapper): SimpleFunctionDescriptor {


        val annotations = getAnnotations(decl.annotations)

        val callableMemberDescriptorKind = decl.kind
        val function = DeserializedSimpleFunctionDescriptor(
            c.containingDeclaration, /* original = */
            null,
            annotations,
            decl.name,
            callableMemberDescriptorKind,
            decl,
            c.containerSource


        )

        val local = c.childContext(function, decl.typeParameters)
        function.initializeWithCoroutinesExperimentalityStatus(
            unsubstitutedValueParameters = local.declDeserializer.valueParameters(decl.valueParameters),
            unsubstitutedReturnType = local.typeDeserializer.type(decl.returnType),
            userDataMap = emptyMap(),
            typeParameters = local.typeDeserializer.ownTypeParameters,
            modality = decl.modality,
            visibility = decl.visibility,
            dispatchReceiverParameter = getDispatchReceiverParameter(),
        )

        function.isOperator = decl.isOperator
        function.isStatic = decl.isStatic
        function.isConst = decl.isConst

        return function
    }


    private fun valueParameters(
        valueParameters: List<ValueParameterWrapper>,

        ): List<ValueParameterDescriptor> {
        if (valueParameters.isEmpty()) return emptyList()
        val callableDescriptor = c.containingDeclaration as CallableDescriptor

        return valueParameters.mapIndexed { i, decl ->

            ValueParameterDescriptorImpl(
                callableDescriptor, null, i,
                getAnnotations(decl.annotations),
                decl.name, decl.isNamedParam,
                c.typeDeserializer.type(decl.type),

                decl.declaresDefaultValue,
                SourceElement.NO_SOURCE
            )


        }.toList()
    }
}
