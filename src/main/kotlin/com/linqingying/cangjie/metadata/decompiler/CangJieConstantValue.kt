package com.linqingying.cangjie.metadata.decompiler

import com.linqingying.cangjie.metadata.ProtoBuf
import com.linqingying.cangjie.metadata.deserialization.Flags
import com.linqingying.cangjie.metadata.deserialization.NameResolver
import com.linqingying.cangjie.resolve.constants.ConstantValue
import com.linqingying.cangjie.resolve.constants.*
import com.linqingying.cangjie.resolve.constants.RuneValue


fun createConstantValue(value: ProtoBuf.Annotation.Argument.Value, nameResolver: NameResolver): ConstantValue<*> {
    val isUnsigned = Flags.IS_UNSIGNED.get(value.flags)

    fun <T, R> T.letIf(predicate: Boolean, f: (T) -> R, g: (T) -> R): R =
        if (predicate) f(this) else g(this)

    return when (value.type) {
        ProtoBuf.Annotation.Argument.Value.Type.INT8 -> value.intValue.toByte().letIf(isUnsigned, ::UInt8Value, ::Int8Value)
        ProtoBuf.Annotation.Argument.Value.Type.RUNE -> RuneValue(value.intValue.toInt().toChar())
        ProtoBuf.Annotation.Argument.Value.Type.INT16 -> value.intValue.toShort().letIf(isUnsigned, ::UInt16Value, :: Int16Value)
        ProtoBuf.Annotation.Argument.Value.Type.INT32 -> value.intValue.toInt().letIf(isUnsigned, ::UInt32Value, ::Int32Value)
        ProtoBuf.Annotation.Argument.Value.Type.INT64 -> value.intValue.letIf(isUnsigned, ::UInt64Value, :: Int64Value)
        ProtoBuf.Annotation.Argument.Value.Type.FLOAT16 -> Float16Value(value.floatValue)
        ProtoBuf.Annotation.Argument.Value.Type.FLOAT32 -> Float32Value(value.floatValue)
        ProtoBuf.Annotation.Argument.Value.Type.FLOAT64 -> Float64Value(value.doubleValue)

        ProtoBuf.Annotation.Argument.Value.Type.BOOLEAN -> BoolValue(value.intValue != 0L)


        else -> error("Unsupported annotation argument type: ${value.type}")
    }
}
