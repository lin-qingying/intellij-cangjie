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

package com.linqingying.cangjie.serialization.deserialization

import com.google.protobuf.MessageLite
import com.linqingying.cangjie.descriptors.*
import com.linqingying.cangjie.descriptors.annotations.Annotations
import com.linqingying.cangjie.descriptors.impl.PropertyGetterDescriptorImpl
import com.linqingying.cangjie.descriptors.impl.PropertySetterDescriptorImpl
import com.linqingying.cangjie.descriptors.impl.ValueParameterDescriptorImpl
import com.linqingying.cangjie.metadata.ProtoBuf
import com.linqingying.cangjie.metadata.deserialization.*
import com.linqingying.cangjie.resolve.DescriptorFactory
import com.linqingying.cangjie.serialization.deserialization.descriptors.*
import com.linqingying.cangjie.types.CangJieType

enum class AnnotatedCallableKind {
    FUNCTION,
    PROPERTY,
    VARIABLE,
    PROPERTY_GETTER,
    PROPERTY_SETTER
}

class MemberDeserializer(private val c: DeserializationContext) {
//    private val annotationDeserializer = AnnotationDeserializer(c.components.moduleDescriptor, c.components.notFoundClasses)

    fun loadProperty(proto: ProtoBuf.Property): PropertyDescriptor {
        val flags = if (proto.hasFlags()) proto.flags else loadOldFlags(proto.oldFlags)

        val property = DeserializedPropertyDescriptor(
            c.containingDeclaration, null,
            getAnnotations(proto, flags, AnnotatedCallableKind.PROPERTY),
            ProtoEnumFlags.modality(Flags.MODALITY.get(flags)),
            ProtoEnumFlags.descriptorVisibility(Flags.VISIBILITY.get(flags)),
            Flags.IS_VAR.get(flags),
            c.nameResolver.getName(proto.name),
            ProtoEnumFlags.memberKind(Flags.MEMBER_KIND.get(flags)),


            proto,
            c.nameResolver,
            c.typeTable,
            c.versionRequirementTable,
            c.containerSource
        )

        val local = c.childContext(property, proto.typeParameterList)

        val hasGetter = Flags.HAS_GETTER.get(flags)
        val receiverAnnotations = if (hasGetter && proto.hasReceiver())
            getReceiverParameterAnnotations(proto, AnnotatedCallableKind.PROPERTY_GETTER)
        else
            Annotations.EMPTY

        property.setType(
            local.typeDeserializer.type(proto.returnType(c.typeTable)),
            local.typeDeserializer.ownTypeParameters,
            getDispatchReceiverParameter(),
            proto.receiverType(c.typeTable)?.let(local.typeDeserializer::type)?.let { receiverType ->
                DescriptorFactory.createExtensionReceiverParameterForCallable(
                    property,
                    receiverType,
                    receiverAnnotations
                )
            },
            proto.contextReceiverTypes(c.typeTable)
                .mapIndexedNotNull { index, type -> type.toContextReceiver(local, property, index) }
        )

        // Per documentation on Property.getter_flags in metadata.proto, if an accessor flags field is absent, its value should be computed
        // by taking hasAnnotations/visibility/modality from property flags, and using false for the rest
        val defaultAccessorFlags = Flags.getAccessorFlags(
            Flags.HAS_ANNOTATIONS.get(flags),
            Flags.VISIBILITY.get(flags),
            Flags.MODALITY.get(flags),
            false
        )

        val getter = if (hasGetter) {
            val getterFlags = if (proto.hasGetterFlags()) proto.getterFlags else defaultAccessorFlags
            val isNotDefault = Flags.IS_NOT_DEFAULT.get(getterFlags)
            val isExternal = Flags.IS_EXTERNAL_ACCESSOR.get(getterFlags)
            val isInline = Flags.IS_INLINE_ACCESSOR.get(getterFlags)
            val annotations = getAnnotations(proto, getterFlags, AnnotatedCallableKind.PROPERTY_GETTER)
            val getter = if (isNotDefault) {
                PropertyGetterDescriptorImpl(
                    property,
                    annotations,
                    ProtoEnumFlags.modality(Flags.MODALITY.get(getterFlags)),
                    ProtoEnumFlags.descriptorVisibility(Flags.VISIBILITY.get(getterFlags)),
                    /* isDefault = */ !isNotDefault,


                    property.kind, null, SourceElement.NO_SOURCE
                )
            } else {
                DescriptorFactory.createDefaultGetter(property, annotations)
            }
            getter.initialize(property.returnType)
            getter
        } else {
            null
        }

        val setter = if (Flags.HAS_SETTER.get(flags)) {
            val setterFlags = if (proto.hasSetterFlags()) proto.setterFlags else defaultAccessorFlags
            val isNotDefault = Flags.IS_NOT_DEFAULT.get(setterFlags)
            val isExternal = Flags.IS_EXTERNAL_ACCESSOR.get(setterFlags)
            val isInline = Flags.IS_INLINE_ACCESSOR.get(setterFlags)
            val annotations = getAnnotations(proto, setterFlags, AnnotatedCallableKind.PROPERTY_SETTER)
            if (isNotDefault) {
                val setter = PropertySetterDescriptorImpl(
                    property,
                    annotations,
                    ProtoEnumFlags.modality(Flags.MODALITY.get(setterFlags)),
                    ProtoEnumFlags.descriptorVisibility(Flags.VISIBILITY.get(setterFlags)),
                    /* isDefault = */ !isNotDefault,

                    property.kind, null, SourceElement.NO_SOURCE
                )
                val setterLocal = local.childContext(setter, listOf())
                val valueParameters = setterLocal.memberDeserializer.valueParameters(
                    listOf(proto.setterValueParameter), proto, AnnotatedCallableKind.PROPERTY_SETTER
                )
                setter.initialize(valueParameters.single())
                setter
            } else {
                DescriptorFactory.createDefaultSetter(
                    property, annotations,
                    Annotations.EMPTY /* Otherwise the setter is not default, see DescriptorResolver.resolvePropertySetterDescriptor */
                )
            }
        } else {
            null
        }

//        if (Flags.HAS_CONSTANT.get(flags)) {
//            property.setCompileTimeInitializerFactory {
//                c.storageManager.createNullableLazyValue {
//                    val container = c.containingDeclaration.asProtoContainer()!!
//                    c.components.annotationAndConstantLoader.loadPropertyConstant(container, proto, property.returnType)
//                }
//            }
//        }

//        if ((c.containingDeclaration as? ClassDescriptor)?.kind == ClassKind.ANNOTATION_CLASS) {
//            property.setCompileTimeInitializerFactory {
//                c.storageManager.createNullableLazyValue {
//                    val container = c.containingDeclaration.asProtoContainer()!!
//                    c.components.annotationAndConstantLoader.loadAnnotationDefaultValue(container, proto, property.returnType)
//                }
//            }
//        }

        property.initialize(
            getter, setter,

            )

        return property
    }

    fun loadVariable(proto: ProtoBuf.Variable): VariableDescriptor {
        val flags = if (proto.hasFlags()) proto.flags else loadOldFlags(proto.oldFlags)

        val variable = DeserializedVariableDescriptor(
            c.containingDeclaration, null,
            getAnnotations(proto, flags, AnnotatedCallableKind.PROPERTY),
            ProtoEnumFlags.modality(Flags.MODALITY.get(flags)),
            ProtoEnumFlags.descriptorVisibility(Flags.VISIBILITY.get(flags)),
            Flags.IS_VAR.get(flags),
            c.nameResolver.getName(proto.name),
            ProtoEnumFlags.memberKind(Flags.MEMBER_KIND.get(flags)),


            proto,
            c.nameResolver,
            c.typeTable,
            c.versionRequirementTable,
            c.containerSource
        )

        val local = c.childContext(variable, proto.typeParameterList)

        val hasGetter = Flags.HAS_GETTER.get(flags)
        val receiverAnnotations =
            Annotations.EMPTY

        variable.setType(
            local.typeDeserializer.type(proto.returnType(c.typeTable)),
            local.typeDeserializer.ownTypeParameters,
            getDispatchReceiverParameter(),
            proto.receiverType(c.typeTable)?.let(local.typeDeserializer::type)?.let { receiverType ->
                DescriptorFactory.createExtensionReceiverParameterForCallable(
                    variable,
                    receiverType,
                    receiverAnnotations
                )
            },
            proto.contextReceiverTypes(c.typeTable)
                .mapIndexedNotNull { index, type -> type.toContextReceiver(local, variable, index) }
        )

        // Per documentation on Property.getter_flags in metadata.proto, if an accessor flags field is absent, its value should be computed
        // by taking hasAnnotations/visibility/modality from property flags, and using false for the rest
        val defaultAccessorFlags = Flags.getAccessorFlags(
            Flags.HAS_ANNOTATIONS.get(flags),
            Flags.VISIBILITY.get(flags),
            Flags.MODALITY.get(flags),
            false
        )


//        if (Flags.HAS_CONSTANT.get(flags)) {
//            variable.setCompileTimeInitializerFactory {
//                c.storageManager.createNullableLazyValue {
//                    val container = c.containingDeclaration.asProtoContainer()!!
//                    c.components.annotationAndConstantLoader.loadPropertyConstant(container, proto, variable.returnType)
//                }
//            }
//        }

//        if ((c.containingDeclaration as? ClassDescriptor)?.kind == ClassKind.ANNOTATION_CLASS) {
//            variable.setCompileTimeInitializerFactory {
//                c.storageManager.createNullableLazyValue {
//                    val container = c.containingDeclaration.asProtoContainer()!!
//                    c.components.annotationAndConstantLoader.loadAnnotationDefaultValue(container, proto, variable.returnType)
//                }
//            }
//        }


        return variable
    }


    private fun DeserializedSimpleFunctionDescriptor.initializeWithCoroutinesExperimentalityStatus(
        extensionReceiverParameter: ReceiverParameterDescriptor?,
        dispatchReceiverParameter: ReceiverParameterDescriptor?,
        contextReceiverParameters: List<ReceiverParameterDescriptor>,
        typeParameters: List<TypeParameterDescriptor>,
        unsubstitutedValueParameters: List<ValueParameterDescriptor>,
        unsubstitutedReturnType: CangJieType?,
        modality: Modality?,
        visibility: DescriptorVisibility,
        userDataMap: Map<out CallableDescriptor.UserDataKey<*>, *>
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

    private fun loadOldFlags(oldFlags: Int): Int {
        val lowSixBits = oldFlags and 0x3f
        val rest = (oldFlags shr 8) shl 6
        return lowSixBits + rest
    }

    fun loadFunction(proto: ProtoBuf.Function): SimpleFunctionDescriptor {
        val flags = if (proto.hasFlags()) proto.flags else loadOldFlags(proto.oldFlags)
        val annotations = getAnnotations(proto, flags, AnnotatedCallableKind.FUNCTION)
        val receiverAnnotations = if (proto.hasReceiver())
            getReceiverParameterAnnotations(proto, AnnotatedCallableKind.FUNCTION)
        else Annotations.EMPTY
        val versionRequirementTable =

            c.versionRequirementTable
        val function = DeserializedSimpleFunctionDescriptor(
            c.containingDeclaration, /* original = */
            null,
            annotations,
            c.nameResolver.getName(proto.name),
            ProtoEnumFlags.memberKind(Flags.MEMBER_KIND.get(flags)),
            proto,
            c.nameResolver,
            c.typeTable,
            versionRequirementTable,
            c.containerSource
        )

        val local = c.childContext(function, proto.typeParameterList)

        function.initializeWithCoroutinesExperimentalityStatus(
            proto.receiverType(c.typeTable)?.let(local.typeDeserializer::type)?.let { receiverType ->
                DescriptorFactory.createExtensionReceiverParameterForCallable(
                    function,
                    receiverType,
                    receiverAnnotations
                )
            },
            getDispatchReceiverParameter(),
            proto.contextReceiverTypes(c.typeTable)
                .mapIndexedNotNull { index, type -> type.toContextReceiver(local, function, index) },
            local.typeDeserializer.ownTypeParameters,
            local.memberDeserializer.valueParameters(proto.valueParameterList, proto, AnnotatedCallableKind.FUNCTION),
            local.typeDeserializer.type(proto.returnType(c.typeTable)),
            ProtoEnumFlags.modality(Flags.MODALITY.get(flags)),
            ProtoEnumFlags.descriptorVisibility(Flags.VISIBILITY.get(flags)),
            emptyMap<CallableDescriptor.UserDataKey<*>, Any?>()
        )
        function.isOperator = Flags.IS_OPERATOR.get(flags)


//        function.setHasStableParameterNames(!Flags.IS_FUNCTION_WITH_NON_STABLE_PARAMETER_NAMES.get(flags))

        val mapValueForContract =
            c.components.contractDeserializer.deserializeContractFromFunction(
                proto,
                function,
                c.typeTable,
                local.typeDeserializer
            )
        if (mapValueForContract != null) {
            function.putInUserDataMap(mapValueForContract.first, mapValueForContract.second)
        }

        return function
    }

    fun loadTypeAlias(proto: ProtoBuf.TypeAlias): TypeAliasDescriptor {
//        val annotations = Annotations.create(
//            proto.annotationList.map { annotationDeserializer.deserializeAnnotation(it, c.nameResolver) }
//        )
        val annotations = Annotations.EMPTY
        val visibility = ProtoEnumFlags.descriptorVisibility(Flags.VISIBILITY.get(proto.flags))
        val typeAlias = DeserializedTypeAliasDescriptor(
            c.storageManager, c.containingDeclaration, annotations, c.nameResolver.getName(proto.name),
            visibility, proto, c.nameResolver, c.typeTable, c.versionRequirementTable, c.containerSource
        )

        val local = c.childContext(typeAlias, proto.typeParameterList)
        typeAlias.initialize(
            local.typeDeserializer.ownTypeParameters,
            local.typeDeserializer.simpleType(proto.underlyingType(c.typeTable), expandTypeAliases = false),
            local.typeDeserializer.simpleType(proto.expandedType(c.typeTable), expandTypeAliases = false)
        )

        return typeAlias
    }

    private fun getDispatchReceiverParameter(): ReceiverParameterDescriptor? {
        return (c.containingDeclaration as? ClassDescriptor)?.thisAsReceiverParameter
    }

    fun loadConstructor(proto: ProtoBuf.Constructor, isPrimary: Boolean): ClassConstructorDescriptor {
        val classDescriptor = c.containingDeclaration as ClassDescriptor
        val descriptor = DeserializedClassConstructorDescriptor(
            classDescriptor,
            null,
            getAnnotations(proto, proto.flags, AnnotatedCallableKind.FUNCTION),
            isPrimary,
            CallableMemberDescriptor.Kind.DECLARATION,
            proto,
            c.nameResolver,
            c.typeTable,
            c.versionRequirementTable,
            c.containerSource
        )

        val local = c.childContext(descriptor, listOf())
        descriptor.initialize(
            local.memberDeserializer.valueParameters(proto.valueParameterList, proto, AnnotatedCallableKind.FUNCTION),
            ProtoEnumFlags.descriptorVisibility(Flags.VISIBILITY.get(proto.flags))
        )
        descriptor.returnType = classDescriptor.defaultType
        descriptor.isExpect = classDescriptor.isExpect
        descriptor.setHasStableParameterNames(!Flags.IS_CONSTRUCTOR_WITH_NON_STABLE_PARAMETER_NAMES.get(proto.flags))

        return descriptor
    }

    private fun getAnnotations(proto: MessageLite, flags: Int, kind: AnnotatedCallableKind): Annotations {
        return Annotations.EMPTY
//        if (!Flags.HAS_ANNOTATIONS.get(flags)) {
//            return Annotations.EMPTY
//        }
//        return NonEmptyDeserializedAnnotations(c.storageManager) {
//            c.containingDeclaration.asProtoContainer()?.let {
//                c.components.annotationAndConstantLoader.loadCallableAnnotations(it, proto, kind).toList()
//            }.orEmpty()
//        }
    }

//    private fun getPropertyFieldAnnotations(proto: ProtoBuf.Property, isDelegate: Boolean): Annotations {
//        if (!Flags.HAS_ANNOTATIONS.get(proto.flags)) {
//            return Annotations.EMPTY
//        }
//        return NonEmptyDeserializedAnnotations(c.storageManager) {
//            c.containingDeclaration.asProtoContainer()?.let {
//                if (isDelegate) {
//                    c.components.annotationAndConstantLoader.loadPropertyDelegateFieldAnnotations(it, proto).toList()
//                } else {
//                    c.components.annotationAndConstantLoader.loadPropertyBackingFieldAnnotations(it, proto).toList()
//                }
//            }.orEmpty()
//        }
//    }

    private fun getReceiverParameterAnnotations(proto: MessageLite, kind: AnnotatedCallableKind): Annotations =
        DeserializedAnnotations(c.storageManager) {
            c.containingDeclaration.asProtoContainer()?.let {
                c.components.annotationAndConstantLoader.loadExtensionReceiverParameterAnnotations(it, proto, kind)
            }.orEmpty()
        }

    private fun valueParameters(
        valueParameters: List<ProtoBuf.ValueParameter>,
        callable: MessageLite,
        kind: AnnotatedCallableKind
    ): List<ValueParameterDescriptor> {
        val callableDescriptor = c.containingDeclaration as CallableDescriptor
        val containerOfCallable = callableDescriptor.containingDeclaration.asProtoContainer()

        return valueParameters.mapIndexed { i, proto ->
            val flags = if (proto.hasFlags()) proto.flags else 0
            val annotations = if (containerOfCallable != null && Flags.HAS_ANNOTATIONS.get(flags)) {
                NonEmptyDeserializedAnnotations(c.storageManager) {
                    c.components.annotationAndConstantLoader
                        .loadValueParameterAnnotations(containerOfCallable, callable, kind, i, proto)
                        .toList()
                }
            } else Annotations.EMPTY
            ValueParameterDescriptorImpl(
                callableDescriptor, null, i,
                annotations,
                c.nameResolver.getName(proto.name),

                Flags.IS_NAMED.get(flags),
                c.typeDeserializer.type(proto.type(c.typeTable)),
                Flags.DECLARES_DEFAULT_VALUE.get(flags),

//                proto.varargElementType(c.typeTable)?.let { c.typeDeserializer.type(it) },
                SourceElement.NO_SOURCE
            )
        }.toList()
    }

    private fun DeclarationDescriptor.asProtoContainer(): ProtoContainer? = when (this) {
        is PackageFragmentDescriptor -> ProtoContainer.Package(fqName, c.nameResolver, c.typeTable, c.containerSource)
        is DeserializedClassDescriptor -> thisAsProtoContainer
        else -> null // TODO: support annotations on lambdas and their parameters
    }

    private fun ProtoBuf.Type.toContextReceiver(
        deserializationContext: DeserializationContext,
        callableDescriptor: CallableDescriptor,
        index: Int
    ): ReceiverParameterDescriptor? {
        val contextReceiverType = deserializationContext.typeDeserializer.type(this)
        return DescriptorFactory.createContextReceiverParameterForCallable(
            callableDescriptor,
            contextReceiverType,
            /* customLabelName = */ null/*todo store custom label name in metadata?*/,
            Annotations.EMPTY,
            index
        )
    }
}
