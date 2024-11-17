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



import com.linqingying.cangjie.metadata.CmAnnotation
import com.linqingying.cangjie.metadata.CmAnnotationArgument
import com.linqingying.cangjie.metadata.ProtoBuf
import com.linqingying.cangjie.metadata.ProtoBuf.Annotation.Argument.Value.Type.*
import com.linqingying.cangjie.metadata.deserialization.Flags
import com.linqingying.cangjie.metadata.deserialization.NameResolver
import com.linqingying.cangjie.metadata.node.ClassName

public fun ProtoBuf.Annotation.readAnnotation(strings: NameResolver): CmAnnotation =
    CmAnnotation(
        strings.getClassName(id),
        argumentList.mapNotNull { argument ->
            argument.value.readAnnotationArgument(strings)?.let { value ->
                strings.getString(argument.nameId) to value
            }
        }.toMap()
    )

public fun ProtoBuf.Annotation.Argument.Value.readAnnotationArgument(strings: NameResolver): CmAnnotationArgument? {
    if (Flags.IS_UNSIGNED[flags]) {
        return when (type) {
            INT8 -> CmAnnotationArgument.UByteValue(intValue.toByte().toUByte())
            INT16 -> CmAnnotationArgument.UShortValue(intValue.toShort().toUShort())
            INT32 -> CmAnnotationArgument.UIntValue(intValue.toInt().toUInt())
            INT64 -> CmAnnotationArgument.ULongValue(intValue.toULong())
            else -> error("Cannot read value of unsigned type: $type")
        }
    }

    return when (type) {
        INT8 -> CmAnnotationArgument.ByteValue(intValue.toByte())
        RUNE -> CmAnnotationArgument.CharValue(intValue.toInt().toChar())
        INT16 -> CmAnnotationArgument.ShortValue(intValue.toShort())
        INT32 -> CmAnnotationArgument.IntValue(intValue.toInt())
        INT64 -> CmAnnotationArgument.LongValue(intValue)
        FLOAT16 -> CmAnnotationArgument.FloatValue(floatValue)
        FLOAT32 -> CmAnnotationArgument.FloatValue(floatValue)
        FLOAT64 -> CmAnnotationArgument.DoubleValue(doubleValue)

        BOOLEAN -> CmAnnotationArgument.BooleanValue(intValue != 0L)
//        STRING -> CmAnnotationArgument.StringValue(strings.getString(stringValue))
//        CLASS -> strings.getClassName(classId).let { className ->
//            if (arrayDimensionCount == 0)
//                CmAnnotationArgument.KClassValue(className)
//            else
//                CmAnnotationArgument.ArrayKClassValue(className, arrayDimensionCount)
//        }
//        ENUM -> CmAnnotationArgument.EnumValue(strings.getClassName(classId), strings.getString(enumValueId))
//        ANNOTATION -> CmAnnotationArgument.AnnotationValue(annotation.readAnnotation(strings))
//        ARRAY -> CmAnnotationArgument.ArrayValue(arrayElementList.mapNotNull { it.readAnnotationArgument(strings) })
        null -> null
    }
}

internal fun NameResolver.getClassName(index: Int): ClassName {
    val name = getQualifiedClassName(index)
    return if (isLocalClassName(index)) ".$name" else name
}
