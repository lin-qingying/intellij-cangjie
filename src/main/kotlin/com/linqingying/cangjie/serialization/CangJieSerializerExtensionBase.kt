package com.linqingying.cangjie.serialization

import com.google.protobuf.GeneratedMessage
import com.google.protobuf.GeneratedMessageLite
import com.linqingying.cangjie.descriptors.*
import com.linqingying.cangjie.metadata.ProtoBuf
import com.linqingying.cangjie.metadata.serialization.MutableVersionRequirementTable
import com.linqingying.cangjie.name.FqName
import com.linqingying.cangjie.resolve.nonSourceAnnotations
import com.linqingying.cangjie.types.CangJieType


abstract class CangJieSerializerExtensionBase(private val protocol: SerializerExtensionProtocol) : SerializerExtension() {
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
            proto.addExtensionOrNull(protocol.constructorAnnotation, annotationSerializer.serializeAnnotation(annotation))
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
            proto.addExtensionOrNull(protocol.propertyGetterAnnotation, annotationSerializer.serializeAnnotation(annotation))
        }
        for (annotation in descriptor.setter?.nonSourceAnnotations.orEmpty()) {
            proto.addExtensionOrNull(protocol.propertySetterAnnotation, annotationSerializer.serializeAnnotation(annotation))
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
            proto.addExtensionOrNull(protocol.typeParameterAnnotation, annotationSerializer.serializeAnnotation(annotation))
        }
    }

    override fun serializeTypeAlias(typeAlias: TypeAliasDescriptor, proto: ProtoBuf.TypeAlias.Builder) {
        // TODO serialize annotations on type aliases?
        // (this requires more extensive protobuf scheme modifications)
    }

    @Suppress("Reformat")
    private fun <
            MessageType : GeneratedMessage.ExtendableMessage<MessageType>,
            BuilderType : GeneratedMessage.ExtendableBuilder<MessageType, BuilderType>,
            Type
            > GeneratedMessage.ExtendableBuilder<MessageType, BuilderType>.addExtensionOrNull(
        extension: GeneratedMessage.GeneratedExtension<MessageType, List<Type>>,
        value: Type?
    ) {
        if (value != null) {
            addExtension(extension, value)
        }
    }
}
