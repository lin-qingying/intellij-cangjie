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

package com.linqingying.cangjie.resolve.constants

import com.linqingying.cangjie.builtins.CangJieBuiltIns
import com.linqingying.cangjie.builtins.StandardNames
import com.linqingying.cangjie.builtins.UnsignedTypes
import com.linqingying.cangjie.descriptors.ModuleDescriptor
import com.linqingying.cangjie.descriptors.findClassAcrossModuleDependencies
import com.linqingying.cangjie.name.ClassId
import com.linqingying.cangjie.types.CangJieType
import com.linqingying.cangjie.types.SimpleType

internal fun CangJieType.minValue(): Long {
    if (UnsignedTypes.isUnsignedType(this)) return 0
    return when {
        CangJieBuiltIns.isInt8(this) -> Byte.MIN_VALUE.toLong()
        CangJieBuiltIns.isInt16(this) -> Short.MIN_VALUE.toLong()
        CangJieBuiltIns.isInt32(this) -> Int.MIN_VALUE.toLong()
//        CangJieBuiltIns.isInt64(this) -> Long.MIN_VALUE.toLong()

        else -> error("Can't get min value for type: $this")
    }

}

internal fun CangJieType.maxValue(): Long{
    return when {
        CangJieBuiltIns.isInt8(this) -> Byte.MAX_VALUE.toLong()
        CangJieBuiltIns.isInt16(this) -> Short.MAX_VALUE.toLong()
        CangJieBuiltIns.isInt32(this) -> Int.MAX_VALUE.toLong()
        CangJieBuiltIns.isInt64(this) -> Long.MAX_VALUE

        CangJieBuiltIns.isUInt8(this) ->255
        CangJieBuiltIns.isUInt16(this) -> 65535
        CangJieBuiltIns.isUInt32(this) -> 4294967295
//        CangJieBuiltIns.isUInt64(this) -> 18446744073709551615

        else -> error("Can't get max value for type: $this")
    }

}


internal val ModuleDescriptor.allSignedLiteralTypes: Collection<CangJieType>
    get() = listOf(builtIns.int64Type, builtIns.int32Type, builtIns.int16Type, builtIns.int8Type)

internal fun ModuleDescriptor.unsignedType(classId: ClassId): SimpleType =
    findClassAcrossModuleDependencies(classId)!!.defaultType

internal val ModuleDescriptor.uInt32Type: SimpleType
    get() = unsignedType(StandardNames.FqNames.uInt32ClassId)

internal val ModuleDescriptor.uInt64Type: SimpleType
    get() = unsignedType(StandardNames.FqNames.uInt64ClassId)

internal val ModuleDescriptor.uInt8Type: SimpleType
    get() = unsignedType(StandardNames.FqNames.uInt8ClassId)

internal val ModuleDescriptor.uInt16Type: SimpleType
    get() = unsignedType(StandardNames.FqNames.uInt16ClassId)
