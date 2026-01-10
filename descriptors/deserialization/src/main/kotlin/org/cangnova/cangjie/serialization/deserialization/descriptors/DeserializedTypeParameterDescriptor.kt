/*
 * Copyright 2026 LinQingYing. and contributors.
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

package org.cangnova.cangjie.serialization.deserialization.descriptors

import org.cangnova.cangjie.descriptors.SourceElement
import org.cangnova.cangjie.descriptors.SupertypeLoopChecker
import org.cangnova.cangjie.descriptors.annotations.Annotations
import org.cangnova.cangjie.descriptors.impl.AbstractLazyTypeParameterDescriptor
import org.cangnova.cangjie.metadata.model.wrapper.TypeParameterWrapper
import org.cangnova.cangjie.resolve.builtIns
import org.cangnova.cangjie.serialization.deserialization.DeserializationContext
import org.cangnova.cangjie.types.CangJieType

class DeserializedTypeParameterDescriptor(
    private val c: DeserializationContext,
    private val typeParameter: TypeParameterWrapper,
    index: Int,
) : AbstractLazyTypeParameterDescriptor(
    c.storageManager,
    c.containingDeclaration,
    Annotations.EMPTY,
    typeParameter.name, // Generate a name based on ID
    index,
    SourceElement.NO_SOURCE,
    SupertypeLoopChecker.EMPTY
) {
    override val annotations = Annotations.EMPTY // TODO: Load annotations from constraint if needed

    override fun resolveUpperBounds(): List<CangJieType> {
        if (typeParameter.uppers.isEmpty()) {
            return listOf(this.builtIns.defaultBound)
        }

        return typeParameter.uppers.map { upperBound ->
            c.typeDeserializer.type(upperBound)
        }
    }

    override fun reportSupertypeLoopError(type: CangJieType) = throw IllegalStateException(
        "There should be no cycles for deserialized type parameters, but found for: $this"
    )
}
