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

package cn.cangnova.cangjie.metadata.decompiler

import cn.cangnova.cangjie.metadata.ProtoBuf
import cn.cangnova.cangjie.metadata.deserialization.Flags
import cn.cangnova.cangjie.metadata.deserialization.NameResolver
import cn.cangnova.cangjie.parsing.Float16
import cn.cangnova.cangjie.resolve.constants.*


fun createConstantValue(value: ProtoBuf.Annotation.Argument.Value, nameResolver: NameResolver): ConstantValue<*> {
    val isUnsigned = Flags.IS_UNSIGNED.get(value.flags)

    fun <T, R> T.letIf(predicate: Boolean, f: (T) -> R, g: (T) -> R): R =
        if (predicate) f(this) else g(this)

    return when (value.type) {
        ProtoBuf.Annotation.Argument.Value.Type.INT8 -> value.intValue.toByte()
            .letIf(isUnsigned, ::UInt8Value, ::Int8Value)

        ProtoBuf.Annotation.Argument.Value.Type.RUNE -> RuneValue(value.intValue.toInt().toChar())
        ProtoBuf.Annotation.Argument.Value.Type.INT16 -> value.intValue.toShort()
            .letIf(isUnsigned, ::UInt16Value, ::Int16Value)

        ProtoBuf.Annotation.Argument.Value.Type.INT32 -> value.intValue.toInt()
            .letIf(isUnsigned, ::UInt32Value, ::Int32Value)

        ProtoBuf.Annotation.Argument.Value.Type.INT64 -> value.intValue.letIf(isUnsigned, ::UInt64Value, ::Int64Value)
        ProtoBuf.Annotation.Argument.Value.Type.FLOAT16 -> Float16Value(Float16.fromFloat(value.floatValue))
        ProtoBuf.Annotation.Argument.Value.Type.FLOAT32 -> Float32Value(value.floatValue)
        ProtoBuf.Annotation.Argument.Value.Type.FLOAT64 -> Float64Value(value.doubleValue)

        ProtoBuf.Annotation.Argument.Value.Type.BOOLEAN -> BoolValue(value.intValue != 0L)


        else -> error("Unsupported annotation argument type: ${value.type}")
    }
}
