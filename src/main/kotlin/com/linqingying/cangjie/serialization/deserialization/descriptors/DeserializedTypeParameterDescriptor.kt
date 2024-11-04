package com.linqingying.cangjie.serialization.deserialization.descriptors

import com.linqingying.cangjie.descriptors.SourceElement
import com.linqingying.cangjie.descriptors.SupertypeLoopChecker
import com.linqingying.cangjie.descriptors.annotations.Annotations
import com.linqingying.cangjie.descriptors.impl.AbstractLazyTypeParameterDescriptor
import com.linqingying.cangjie.metadata.ProtoBuf
import com.linqingying.cangjie.metadata.deserialization.upperBounds
import com.linqingying.cangjie.resolve.descriptorUtil.builtIns
import com.linqingying.cangjie.serialization.deserialization.DeserializationContext
import com.linqingying.cangjie.serialization.deserialization.DeserializedAnnotations
import com.linqingying.cangjie.serialization.deserialization.ProtoEnumFlags
import  com.linqingying.cangjie.serialization.deserialization.getName
import com.linqingying.cangjie.types.CangJieType

class DeserializedTypeParameterDescriptor(
    private val c: DeserializationContext,
    val proto: ProtoBuf.TypeParameter,
    index: Int
) : AbstractLazyTypeParameterDescriptor(
    c.storageManager, c.containingDeclaration,
      Annotations.EMPTY,
    c.nameResolver.getName(proto.name),
    ProtoEnumFlags.variance(proto.variance),  index, SourceElement.NO_SOURCE, SupertypeLoopChecker.EMPTY,
) {
    override val annotations = DeserializedAnnotations(c.storageManager) {
        c.components.annotationAndConstantLoader.loadTypeParameterAnnotations(proto, c.nameResolver).toList()
    }

    override fun resolveUpperBounds(): List<CangJieType> {
        val upperBounds = proto.upperBounds(c.typeTable)
        if (upperBounds.isEmpty()) {
            return listOf(this.builtIns.defaultBound)
        }
        return upperBounds.map(c.typeDeserializer::type)
    }

    override fun reportSupertypeLoopError(type: CangJieType) = throw IllegalStateException(
        "There should be no cycles for deserialized type parameters, but found for: $this"
    )
}
