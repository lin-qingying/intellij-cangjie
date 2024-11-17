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

package com.linqingying.cangjie.metadata.internal

import com.linqingying.cangjie.metadata.MemberKind
import com.linqingying.cangjie.metadata.Modality
import com.linqingying.cangjie.metadata.Visibility
import com.linqingying.cangjie.metadata.node.*
import kotlin.enums.EnumEntries
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.KProperty
import com.google.protobuf.Internal.EnumLite as ProtoEnumLite
import com.linqingying.cangjie.metadata.deserialization.Flags as ProtoFlags
import com.linqingying.cangjie.metadata.deserialization.Flags.FlagField as ProtoFlagSet
import com.linqingying.cangjie.metadata.internal.FlagImpl as Flag


internal class EnumFlagDelegate<Node, E : Enum<E>>(
    val flags: KMutableProperty1<Node, Int>,
    private val protoSet: ProtoFlagSet<out ProtoEnumLite>,
    private val entries: EnumEntries<E>,
    private val flagValues: List<Flag>
) {
    // Pre-built permutation ProtoEnum <> E to allow reordering of enum entries?
    // Concern: if new enum values are added to metadata proto, everything (including existing flags) will break
    operator fun getValue(thisRef: Node, property: KProperty<*>): E = entries[protoSet.get(flags.get(thisRef)).number]

    operator fun setValue(thisRef: Node, property: KProperty<*>, value: E) {
        flags.set(thisRef, flagValues[value.ordinal] + flags.get(thisRef))
    }
}

// Public in internal package - for reuse in JvmFlags
class BooleanFlagDelegate<Node>(private val flags: KMutableProperty1<Node, Int>, private val flag: Flag) {
    private val mask: Int

    init {
        require(flag.bitWidth == 1 && flag.value == 1) { "BooleanFlagDelegate can work only with boolean flags (bitWidth = 1 and value = 1), but $flag was passed" }
        mask = 1 shl flag.offset
    }

    operator fun getValue(thisRef: Node, property: KProperty<*>): Boolean = flag(flags.get(thisRef))

    operator fun setValue(thisRef: Node, property: KProperty<*>, value: Boolean) {
        val newValue = if (value) flags.get(thisRef) or mask else flags.get(thisRef) and mask.inv()
        flags.set(thisRef, newValue)
    }
}


internal fun <Node> visibilityDelegate(flags: KMutableProperty1<Node, Int>) =
    EnumFlagDelegate(flags, ProtoFlags.VISIBILITY, Visibility.entries, Visibility.entries.map { it.flag })

internal fun <Node> modalityDelegate(flags: KMutableProperty1<Node, Int>) =
    EnumFlagDelegate(flags, ProtoFlags.MODALITY, Modality.entries, Modality.entries.map { it.flag })

internal fun <Node> memberKindDelegate(flags: KMutableProperty1<Node, Int>) =
    EnumFlagDelegate(flags, ProtoFlags.MEMBER_KIND, MemberKind.entries, MemberKind.entries.map { it.flag })

internal fun classBooleanFlag(flag: Flag) = BooleanFlagDelegate(CmClass::flags, flag)

internal fun functionBooleanFlag(flag: Flag) = BooleanFlagDelegate(CmFunction::flags, flag)

internal fun constructorBooleanFlag(flag: Flag) = BooleanFlagDelegate(CmConstructor::flags, flag)

internal fun propertyBooleanFlag(flag: Flag) = BooleanFlagDelegate(CmProperty::flags, flag)

internal fun propertyAccessorBooleanFlag(flag: Flag) = BooleanFlagDelegate(CmPropertyAccessorAttributes::flags, flag)

internal fun typeBooleanFlag(flag: Flag) = BooleanFlagDelegate(CmType::flags, flag)

internal fun valueParameterBooleanFlag(flag: Flag) = BooleanFlagDelegate(CmValueParameter::flags, flag)

internal fun <Node> annotationsOn(flags: KMutableProperty1<Node, Int>) =
    BooleanFlagDelegate(flags, Flag(ProtoFlags.HAS_ANNOTATIONS))

// Used for kotlin-metadata-jvm tests:
fun _flagAccess(kmClass: CmClass): Int = kmClass.flags

fun _flagAccess(kmFunc: CmFunction): Int = kmFunc.flags

fun _flagAccess(kmType: CmType): Int = kmType.flags

fun _flagAccess(kmConstr: CmConstructor): Int = kmConstr.flags
