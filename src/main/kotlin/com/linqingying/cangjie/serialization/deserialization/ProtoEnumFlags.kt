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

import com.linqingying.cangjie.descriptors.ClassKind
import com.linqingying.cangjie.descriptors.ClassKind.*
import com.linqingying.cangjie.descriptors.Modality
import com.linqingying.cangjie.descriptors.Visibilities
import com.linqingying.cangjie.descriptors.Visibility

import com.linqingying.cangjie.metadata.ProtoBuf
import com.linqingying.cangjie.types.Variance

object ProtoEnumFlags {
    fun modality(modality: ProtoBuf.Modality?): Modality = when (modality) {
        ProtoBuf.Modality.FINAL -> Modality.FINAL
        ProtoBuf.Modality.OPEN -> Modality.OPEN
        ProtoBuf.Modality.ABSTRACT -> Modality.ABSTRACT
        ProtoBuf.Modality.SEALED -> Modality.SEALED
        else -> Modality.FINAL
    }

    fun modality(modality: Modality): ProtoBuf.Modality = when (modality) {
        Modality.FINAL -> ProtoBuf.Modality.FINAL
        Modality.OPEN -> ProtoBuf.Modality.OPEN
        Modality.ABSTRACT -> ProtoBuf.Modality.ABSTRACT
        Modality.SEALED -> ProtoBuf.Modality.SEALED
    }

    fun visibility(visibility: ProtoBuf.Visibility?): Visibility = when (visibility) {
        ProtoBuf.Visibility.INTERNAL -> Visibilities.Internal
        ProtoBuf.Visibility.PRIVATE -> Visibilities.Private
        ProtoBuf.Visibility.PRIVATE_TO_THIS -> Visibilities.PrivateToThis
        ProtoBuf.Visibility.PROTECTED -> Visibilities.Protected
        ProtoBuf.Visibility.PUBLIC -> Visibilities.Public
        ProtoBuf.Visibility.LOCAL -> Visibilities.Local
        else -> Visibilities.Private
    }

    fun visibility(visibility: Visibility): ProtoBuf.Visibility = when (visibility) {
        Visibilities.Internal -> ProtoBuf.Visibility.INTERNAL
        Visibilities.Public -> ProtoBuf.Visibility.PUBLIC
        Visibilities.Private -> ProtoBuf.Visibility.PRIVATE
        Visibilities.PrivateToThis -> ProtoBuf.Visibility.PRIVATE_TO_THIS
        Visibilities.Protected -> ProtoBuf.Visibility.PROTECTED
        Visibilities.Local -> ProtoBuf.Visibility.LOCAL
        else -> throw IllegalArgumentException("Unknown visibility: $visibility")
    }

    fun classKind(kind: ProtoBuf.Class.Kind?): ClassKind = when (kind) {
        ProtoBuf.Class.Kind.CLASS -> CLASS
        ProtoBuf.Class.Kind.INTERFACE -> INTERFACE
        ProtoBuf.Class.Kind.ENUM -> ENUM
        ProtoBuf.Class.Kind.ENUM_ENTRY -> ENUM_ENTRY
        ProtoBuf.Class.Kind.ANNOTATION_CLASS -> ANNOTATION_CLASS
        ProtoBuf.Class.Kind.STRUCT  -> STRUCT
        else -> CLASS
    }

    fun classKind(kind: ClassKind ): ProtoBuf.Class.Kind {

        return when (kind) {
            CLASS -> ProtoBuf.Class.Kind.CLASS
            INTERFACE -> ProtoBuf.Class.Kind.INTERFACE
            ENUM -> ProtoBuf.Class.Kind.ENUM
            ENUM_ENTRY -> ProtoBuf.Class.Kind.ENUM_ENTRY
            ANNOTATION_CLASS -> ProtoBuf.Class.Kind.ANNOTATION_CLASS
            STRUCT -> ProtoBuf.Class.Kind.STRUCT
            TUPLE -> TODO()
            EXTEND -> TODO()
            BASIC -> ProtoBuf.Class.Kind.CLASS
        }
    }

    fun variance(variance: ProtoBuf.TypeParameter.Variance): Variance = when (variance) {

        ProtoBuf.TypeParameter.Variance.INV -> Variance.INVARIANT
    }

    fun variance(projection: ProtoBuf.Type.Argument.Projection): Variance = when (projection) {

        ProtoBuf.Type.Argument.Projection.INV -> Variance.INVARIANT

    }

    fun variance(variance: Variance):  ProtoBuf.TypeParameter.Variance = when (variance) {

        Variance.INVARIANT -> ProtoBuf.TypeParameter.Variance.INV
    }
}
