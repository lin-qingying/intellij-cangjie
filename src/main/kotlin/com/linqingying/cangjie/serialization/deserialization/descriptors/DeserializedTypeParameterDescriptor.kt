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
