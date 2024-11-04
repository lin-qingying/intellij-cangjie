/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package com.linqingying.cangjie.serialization

import com.linqingying.cangjie.descriptors.*
import com.linqingying.cangjie.extensions.ProjectExtensionDescriptor
import com.linqingying.cangjie.metadata.ProtoBuf
import com.linqingying.cangjie.metadata.serialization.MutableVersionRequirementTable

interface DescriptorSerializerPlugin {
    fun afterClass(
        descriptor: ClassDescriptor,
        proto: ProtoBuf.Class.Builder,
        versionRequirementTable: MutableVersionRequirementTable,
        childSerializer: DescriptorSerializer,
        extension: SerializerExtension
    ) {
    }

    fun afterFunction(
        descriptor: FunctionDescriptor,
        proto: ProtoBuf.Function.Builder,
        versionRequirementTable: MutableVersionRequirementTable?,
        childSerializer: DescriptorSerializer,
        extension: SerializerExtension
    ) {
    }

    fun afterConstructor(
        descriptor: ConstructorDescriptor,
        proto: ProtoBuf.Constructor.Builder,
        versionRequirementTable: MutableVersionRequirementTable?,
        childSerializer: DescriptorSerializer,
        extension: SerializerExtension
    ) {
    }
    fun afterVariable(
        descriptor: VariableDescriptor,
        proto: ProtoBuf.Variable.Builder,
        versionRequirementTable: MutableVersionRequirementTable?,
        childSerializer: DescriptorSerializer,
        extension: SerializerExtension
    ) {
    }
    fun afterProperty(
        descriptor: PropertyDescriptor,
        proto: ProtoBuf.Property.Builder,
        versionRequirementTable: MutableVersionRequirementTable?,
        childSerializer: DescriptorSerializer,
        extension: SerializerExtension
    ) {
    }

    fun afterTypealias(
        descriptor: TypeAliasDescriptor,
        proto: ProtoBuf.TypeAlias.Builder,
        versionRequirementTable: MutableVersionRequirementTable?,
        childSerializer: DescriptorSerializer,
        extension: SerializerExtension
    ) {
    }

    companion object : ProjectExtensionDescriptor<DescriptorSerializerPlugin>(
        "com.linqingying.cangjie.DescriptorSerializerPlugin", DescriptorSerializerPlugin::class.java)
}
