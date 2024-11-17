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
import com.linqingying.cangjie.metadata.ProtoBuf
import com.linqingying.cangjie.metadata.decompiler.AnnotationWithArgs
import com.linqingying.cangjie.metadata.deserialization.NameResolver
import com.linqingying.cangjie.serialization.SerializerExtensionProtocol

abstract class AbstractAnnotationLoader<out A : Any>(
    protected val protocol: SerializerExtensionProtocol,
) : AnnotationLoader<A>
{

    override fun loadClassAnnotations(container: ProtoContainer.Class): List<A> {
        val annotations = container.classProto.getExtension(protocol.classAnnotation).orEmpty()
        return annotations.map { proto -> loadAnnotation(proto, container.nameResolver) }
    }

    override fun loadCallableAnnotations(
        container: ProtoContainer,
        proto: MessageLite,
        kind: AnnotatedCallableKind
    ): List<A> {
        val annotations = when (proto) {
            is ProtoBuf.Constructor -> proto.getExtension(protocol.constructorAnnotation)
            is ProtoBuf.Function -> proto.getExtension(protocol.functionAnnotation)
            is ProtoBuf.Property -> when (kind) {
                AnnotatedCallableKind.PROPERTY -> proto.getExtension(protocol.propertyAnnotation)
                AnnotatedCallableKind.PROPERTY_GETTER -> proto.getExtension(protocol.propertyGetterAnnotation)
                AnnotatedCallableKind.PROPERTY_SETTER -> proto.getExtension(protocol.propertySetterAnnotation)
                else -> error("Unsupported callable kind with property proto")
            }
            else -> error("Unknown message: $proto")
        }.orEmpty()
        return annotations.map { annotationProto ->
            loadAnnotation(annotationProto, container.nameResolver)
        }
    }
//
//    override fun loadPropertyBackingFieldAnnotations(container: ProtoContainer, proto: ProtoBuf.Property): List<A> {
//        val annotations = protocol.propertyBackingFieldAnnotation?.let { proto.getExtension(it) }.orEmpty()
//        return annotations.map { annotationProto ->
//            loadAnnotation(annotationProto, container.nameResolver)
//        }
//    }
//
//    override fun loadPropertyDelegateFieldAnnotations(container: ProtoContainer, proto: ProtoBuf.Property): List<A> {
//        val annotations = protocol.propertyDelegatedFieldAnnotation?.let { proto.getExtension(it) }.orEmpty()
//        return annotations.map { annotationProto ->
//            loadAnnotation(annotationProto, container.nameResolver)
//        }
//    }

    override fun loadEnumEntryAnnotations(container: ProtoContainer, proto: ProtoBuf.EnumEntry): List<A> {
        val annotations = proto.getExtension(protocol.enumEntryAnnotation).orEmpty()
        return annotations.map { annotationProto ->
            loadAnnotation(annotationProto, container.nameResolver)
        }
    }

    override fun loadValueParameterAnnotations(
        container: ProtoContainer,
        callableProto: MessageLite,
        kind: AnnotatedCallableKind,
        parameterIndex: Int,
        proto: ProtoBuf.ValueParameter
    ): List<A> {
        val annotations = proto.getExtension(protocol.parameterAnnotation).orEmpty()
        return annotations.map { annotationProto ->
            loadAnnotation(annotationProto, container.nameResolver)
        }
    }

    override fun loadExtensionReceiverParameterAnnotations(
        container: ProtoContainer,
        proto: MessageLite,
        kind: AnnotatedCallableKind
    ): List<A> {
        val annotations = when (proto) {
            is ProtoBuf.Function -> protocol.functionExtensionReceiverAnnotation?.let { proto.getExtension(it) }
            is ProtoBuf.Property -> when (kind) {
                AnnotatedCallableKind.PROPERTY, AnnotatedCallableKind.PROPERTY_GETTER, AnnotatedCallableKind.PROPERTY_SETTER -> {
                    protocol.propertyExtensionReceiverAnnotation?.let { proto.getExtension(it) }
                }
                else -> error("Unsupported callable kind with property proto for receiver annotations: $kind")
            }
            else -> error("Unknown message: $proto")
        }.orEmpty()
        return annotations.map { annotationProto ->
            loadAnnotation(annotationProto, container.nameResolver)
        }
    }

    override fun loadTypeAnnotations(proto: ProtoBuf.Type, nameResolver: NameResolver): List<A> {
        return proto.getExtension(protocol.typeAnnotation).orEmpty().map { loadAnnotation(it, nameResolver) }
    }

    override fun loadTypeParameterAnnotations(proto: ProtoBuf.TypeParameter, nameResolver: NameResolver): List<A> {
        return proto.getExtension(protocol.typeParameterAnnotation).orEmpty().map { loadAnnotation(it, nameResolver) }
    }
}
