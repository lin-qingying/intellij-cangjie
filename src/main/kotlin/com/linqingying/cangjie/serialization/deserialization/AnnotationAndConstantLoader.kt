package com.linqingying.cangjie.serialization.deserialization

import com.google.protobuf.MessageLite
import com.linqingying.cangjie.metadata.ProtoBuf
import com.linqingying.cangjie.metadata.deserialization.NameResolver
import com.linqingying.cangjie.types.CangJieType

interface AnnotationLoader<out A : Any>{

    fun loadExtensionReceiverParameterAnnotations(
        container: ProtoContainer,
        proto: MessageLite,
        kind: AnnotatedCallableKind
    ): List<A>
    fun loadClassAnnotations(
        container: ProtoContainer.Class
    ): List<A>

    fun loadCallableAnnotations(
        container: ProtoContainer,
        proto: MessageLite,
        kind: AnnotatedCallableKind
    ): List<A>

    fun loadEnumEntryAnnotations(
        container: ProtoContainer,
        proto: ProtoBuf.EnumEntry
    ): List<A>
    fun loadTypeAnnotations(
        proto: ProtoBuf.Type,
        nameResolver: NameResolver
    ): List<A>
    fun loadTypeParameterAnnotations(
        proto: ProtoBuf.TypeParameter,
        nameResolver: NameResolver
    ): List<A>
    fun loadValueParameterAnnotations(
        container: ProtoContainer,
        callableProto: MessageLite,
        kind: AnnotatedCallableKind,
        parameterIndex: Int,
        proto: ProtoBuf.ValueParameter
    ): List<A>
    fun loadAnnotation(proto: ProtoBuf.Annotation, nameResolver: NameResolver): A

}
interface AnnotationAndConstantLoader<out A : Any, out C : Any> : AnnotationLoader<A> {
    fun loadPropertyConstant(
        container: ProtoContainer,
        proto: ProtoBuf.Property,
        expectedType: CangJieType
    ): C?

    fun loadAnnotationDefaultValue(
        container: ProtoContainer,
        proto: ProtoBuf.Property,
        expectedType: CangJieType
    ): C?
}
