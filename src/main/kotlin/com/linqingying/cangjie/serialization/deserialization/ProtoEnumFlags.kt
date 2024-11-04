/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
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
