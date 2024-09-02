package com.huawei.cangjie.resolve.constants

import com.huawei.cangjie.builtins.CangJieBuiltIns
import com.huawei.cangjie.builtins.StandardNames
import com.huawei.cangjie.builtins.UnsignedTypes
import com.huawei.cangjie.descriptors.ModuleDescriptor
import com.huawei.cangjie.descriptors.findClassAcrossModuleDependencies
import com.huawei.cangjie.name.ClassId
import com.huawei.cangjie.types.CangJieType
import com.huawei.cangjie.types.SimpleType

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
