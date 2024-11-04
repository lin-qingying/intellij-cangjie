package com.linqingying.cangjie.serialization.deserialization

import com.linqingying.cangjie.descriptors.ModuleDescriptor
import com.linqingying.cangjie.descriptors.NotFoundClasses
import com.linqingying.cangjie.descriptors.annotations.AnnotationDescriptor
import com.linqingying.cangjie.metadata.ProtoBuf
import com.linqingying.cangjie.metadata.deserialization.NameResolver
import com.linqingying.cangjie.metadata.deserialization.getExtensionOrNull
import com.linqingying.cangjie.resolve.constants.ConstantValue
import com.linqingying.cangjie.serialization.SerializerExtensionProtocol
import com.linqingying.cangjie.types.CangJieType

class AnnotationAndConstantLoaderImpl(
    module: ModuleDescriptor,
    notFoundClasses: NotFoundClasses,
    protocol: SerializerExtensionProtocol,
) : AbstractAnnotationLoader<AnnotationDescriptor>(protocol),
    AnnotationAndConstantLoader<AnnotationDescriptor, ConstantValue<*>> {
    private val deserializer = AnnotationDeserializer(module, notFoundClasses)

    override fun loadAnnotation(proto: ProtoBuf.Annotation, nameResolver: NameResolver): AnnotationDescriptor {
        return deserializer.deserializeAnnotation(proto, nameResolver)
    }

    override fun loadPropertyConstant(container: ProtoContainer, proto: ProtoBuf.Property, expectedType: CangJieType): ConstantValue<*>? {
        val value = proto.getExtensionOrNull(protocol.compileTimeValue) ?: return null
        return deserializer.resolveValue(expectedType, value, container.nameResolver)
    }

    override fun loadAnnotationDefaultValue(
        container: ProtoContainer,
        proto: ProtoBuf.Property,
        expectedType: CangJieType
    ): ConstantValue<*>? {
        // Implement this method to properly support Annotations Instantiation feature
        return null
    }
}
