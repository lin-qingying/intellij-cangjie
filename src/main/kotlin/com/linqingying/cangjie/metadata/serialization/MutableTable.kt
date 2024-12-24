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

@file:Suppress("FINITE_BOUNDS_VIOLATION_IN_JAVA")

package com.linqingying.cangjie.metadata.serialization

import com.google.protobuf.GeneratedMessage
import com.google.protobuf.GeneratedMessageLite
import com.google.protobuf.GeneratedMessageV3
import com.linqingying.cangjie.metadata.ProtoBuf

private class TableElementWrapper<Element : GeneratedMessageV3.Builder<Element>>(val builder: Element) {
    // If you'll try to optimize it using structured equals/hashCode, pay attention to extensions present in proto messages
    private val bytes: ByteArray = builder.build().toByteArray()
    private val hashCode: Int = bytes.contentHashCode()

    override fun hashCode() = hashCode

    override fun equals(other: Any?) = other is TableElementWrapper<*> && bytes.contentEquals(other.bytes)
}

abstract class MutableTable<Element, Table, TableBuilder>
        where Element : GeneratedMessageV3.Builder<  Element>,
              Table : GeneratedMessageV3,
              TableBuilder : GeneratedMessageV3.Builder<TableBuilder> {

    private val interner = Interner<TableElementWrapper<Element>>()

    protected abstract fun createTableBuilder(): TableBuilder

    protected abstract fun addElement(builder: TableBuilder, element: Element)

    operator fun get(type: Element): Int =
        interner.intern(TableElementWrapper(type))

    @Suppress("UNCHECKED_CAST")
    fun serialize(): Table? =
        if (interner.isEmpty) null
        else createTableBuilder().apply {
            for (obj in interner.allInternedObjects) {
                addElement(this, obj.builder)
            }
        }.build() as Table
}

class MutableTypeTable : MutableTable<ProtoBuf.Type.Builder, ProtoBuf.TypeTable, ProtoBuf.TypeTable.Builder>() {
    override fun createTableBuilder(): ProtoBuf.TypeTable.Builder = ProtoBuf.TypeTable.newBuilder()

    override fun addElement(builder: ProtoBuf.TypeTable.Builder, element: ProtoBuf.Type.Builder) {
        builder.addType(element)
    }
}

class MutableVersionRequirementTable :
    MutableTable<ProtoBuf.VersionRequirement.Builder, ProtoBuf.VersionRequirementTable, ProtoBuf.VersionRequirementTable.Builder>() {
    override fun createTableBuilder(): ProtoBuf.VersionRequirementTable.Builder =
        ProtoBuf.VersionRequirementTable.newBuilder()

    override fun addElement(
        builder: ProtoBuf.VersionRequirementTable.Builder,
        element: ProtoBuf.VersionRequirement.Builder
    ) {
        builder.addRequirement(element)
    }
}
