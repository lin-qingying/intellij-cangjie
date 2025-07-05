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

package cn.cangnova.cangjie.serialization

import com.google.protobuf.GeneratedMessage
import com.google.protobuf.GeneratedMessageV3
import cn.cangnova.cangjie.descriptors.*
import cn.cangnova.cangjie.metadata.ProtoBuf
import cn.cangnova.cangjie.metadata.serialization.MutableVersionRequirementTable
import cn.cangnova.cangjie.name.FqName
import cn.cangnova.cangjie.resolve.nonSourceAnnotations
import cn.cangnova.cangjie.types.CangJieType


abstract class CangJieSerializerExtensionBase(private val protocol: SerializerExtensionProtocol) :
    SerializerExtension() {
    override val stringTable = StringTableImpl()

    override fun serializeClass(
        descriptor: ClassDescriptor,
        proto: ProtoBuf.Class.Builder,
        versionRequirementTable: MutableVersionRequirementTable,
        childSerializer: DescriptorSerializer
    ) {
        for (annotation in descriptor.nonSourceAnnotations) {
            proto.addExtensionOrNull(protocol.classAnnotation, annotationSerializer.serializeAnnotation(annotation))
        }
    }

    override fun serializePackage(packageFqName: FqName, proto: ProtoBuf.Package.Builder) {
        proto.setExtension(protocol.packageFqName, stringTable.getPackageFqNameIndex(packageFqName))
    }

    override fun serializeConstructor(
        descriptor: ConstructorDescriptor,
        proto: ProtoBuf.Constructor.Builder,
        childSerializer: DescriptorSerializer
    ) {
        for (annotation in descriptor.nonSourceAnnotations) {
            proto.addExtensionOrNull(
                protocol.constructorAnnotation,
                annotationSerializer.serializeAnnotation(annotation)
            )
        }
    }

    override fun serializeFunction(
        descriptor: FunctionDescriptor,
        proto: ProtoBuf.Function.Builder,
        versionRequirementTable: MutableVersionRequirementTable?,
        childSerializer: DescriptorSerializer
    ) {
        for (annotation in descriptor.nonSourceAnnotations) {
            proto.addExtensionOrNull(protocol.functionAnnotation, annotationSerializer.serializeAnnotation(annotation))
        }
        protocol.functionExtensionReceiverAnnotation?.let { extension ->
            for (annotation in descriptor.extensionReceiverParameter?.nonSourceAnnotations.orEmpty()) {
                proto.addExtensionOrNull(extension, annotationSerializer.serializeAnnotation(annotation))
            }
        }
    }

    override fun serializeProperty(
        descriptor: PropertyDescriptor,
        proto: ProtoBuf.Property.Builder,
        versionRequirementTable: MutableVersionRequirementTable?,
        childSerializer: DescriptorSerializer
    ) {
        for (annotation in descriptor.nonSourceAnnotations) {
            proto.addExtensionOrNull(protocol.propertyAnnotation, annotationSerializer.serializeAnnotation(annotation))
        }
        for (annotation in descriptor.getter?.nonSourceAnnotations.orEmpty()) {
            proto.addExtensionOrNull(
                protocol.propertyGetterAnnotation,
                annotationSerializer.serializeAnnotation(annotation)
            )
        }
        for (annotation in descriptor.setter?.nonSourceAnnotations.orEmpty()) {
            proto.addExtensionOrNull(
                protocol.propertySetterAnnotation,
                annotationSerializer.serializeAnnotation(annotation)
            )
        }
        protocol.propertyExtensionReceiverAnnotation?.let { extension ->
            for (annotation in descriptor.extensionReceiverParameter?.nonSourceAnnotations.orEmpty()) {
                proto.addExtensionOrNull(extension, annotationSerializer.serializeAnnotation(annotation))
            }
        }
//        protocol.propertyBackingFieldAnnotation?.let { extension ->
//            for (annotation in descriptor.backingField?.nonSourceAnnotations.orEmpty()) {
//                proto.addExtensionOrNull(extension, annotationSerializer.serializeAnnotation(annotation))
//            }
//        }
//        protocol.propertyDelegatedFieldAnnotation?.let { extension ->
//            for (annotation in descriptor.delegateField?.nonSourceAnnotations.orEmpty()) {
//                proto.addExtensionOrNull(extension, annotationSerializer.serializeAnnotation(annotation))
//            }
//        }
//        val constantInitializer = descriptor.compileTimeInitializer ?: return
//        if (constantInitializer !is NullValue) {
//            proto.setExtension(protocol.compileTimeValue, annotationSerializer.valueProto(constantInitializer).build())
//        }
    }

    override fun serializeEnumEntry(descriptor: ClassDescriptor, proto: ProtoBuf.EnumEntry.Builder) {
        for (annotation in descriptor.nonSourceAnnotations) {
            proto.addExtensionOrNull(protocol.enumEntryAnnotation, annotationSerializer.serializeAnnotation(annotation))
        }
    }

    override fun serializeValueParameter(descriptor: ValueParameterDescriptor, proto: ProtoBuf.ValueParameter.Builder) {
        for (annotation in descriptor.nonSourceAnnotations) {
            proto.addExtensionOrNull(protocol.parameterAnnotation, annotationSerializer.serializeAnnotation(annotation))
        }
    }

    override fun serializeType(type: CangJieType, proto: ProtoBuf.Type.Builder) {
        for (annotation in type.nonSourceAnnotations) {
            proto.addExtensionOrNull(protocol.typeAnnotation, annotationSerializer.serializeAnnotation(annotation))
        }
    }

    override fun serializeTypeParameter(typeParameter: TypeParameterDescriptor, proto: ProtoBuf.TypeParameter.Builder) {
        for (annotation in typeParameter.nonSourceAnnotations) {
            proto.addExtensionOrNull(
                protocol.typeParameterAnnotation,
                annotationSerializer.serializeAnnotation(annotation)
            )
        }
    }

    override fun serializeTypeAlias(typeAlias: TypeAliasDescriptor, proto: ProtoBuf.TypeAlias.Builder) {
        // TODO serialize annotations on type aliases?
        // (this requires more extensive protobuf scheme modifications)
    }

    @Suppress("Reformat")
    private fun <
            MessageType : GeneratedMessageV3.ExtendableMessage<MessageType>,
            BuilderType : GeneratedMessageV3.ExtendableBuilder<MessageType, BuilderType>,
            Type
            > GeneratedMessageV3.ExtendableBuilder<MessageType, BuilderType>.addExtensionOrNull(
        extension: GeneratedMessage.GeneratedExtension<MessageType, List<Type>>,
        value: Type?
    ) {
        if (value != null) {
            addExtension(extension, value)
        }
    }
}
