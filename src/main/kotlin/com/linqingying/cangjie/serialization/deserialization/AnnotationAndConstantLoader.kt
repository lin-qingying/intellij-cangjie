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
