package com.linqingying.cangjie.metadata.decompiler

import com.google.protobuf.MessageLite
import com.linqingying.cangjie.metadata.ProtoBuf
import com.linqingying.cangjie.metadata.deserialization.NameResolver
import com.linqingying.cangjie.serialization.SerializerExtensionProtocol
import com.linqingying.cangjie.serialization.deserialization.*

class AnnotationLoaderForStubBuilderImpl(
    protocol: SerializerExtensionProtocol,
) : AbstractAnnotationLoader<AnnotationWithArgs>(protocol) {


    override fun loadAnnotation(proto: ProtoBuf.Annotation, nameResolver: NameResolver): AnnotationWithArgs {
        val valueMap = proto.argumentList.associate { nameResolver.getName(it.nameId) to createConstantValue(it.value, nameResolver) }
        return AnnotationWithArgs(nameResolver.getClassId(proto.id), valueMap)
    }
}
