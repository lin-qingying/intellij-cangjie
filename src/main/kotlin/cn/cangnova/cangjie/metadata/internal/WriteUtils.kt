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

package cn.cangnova.cangjie.metadata.internal

import cn.cangnova.cangjie.metadata.CmAnnotation
import cn.cangnova.cangjie.metadata.CmAnnotationArgument
import cn.cangnova.cangjie.metadata.ProtoBuf
import cn.cangnova.cangjie.metadata.deserialization.Flags
import cn.cangnova.cangjie.metadata.node.ClassName
import cn.cangnova.cangjie.metadata.node.isLocalClassName
import cn.cangnova.cangjie.metadata.serialization.StringTable

fun CmAnnotation.writeAnnotation(strings: StringTable): ProtoBuf.Annotation.Builder =
    ProtoBuf.Annotation.newBuilder().apply {
        id = strings.getClassNameIndex(className)
        for ((name, argument) in arguments) {
            addArgument(ProtoBuf.Annotation.Argument.newBuilder().apply {
                nameId = strings.getStringIndex(name)
                value = argument.writeAnnotationArgument(strings).build()
            })
        }
    }

fun CmAnnotationArgument.writeAnnotationArgument(strings: StringTable): ProtoBuf.Annotation.Argument.Value.Builder =
    ProtoBuf.Annotation.Argument.Value.newBuilder().apply {
        when (this@writeAnnotationArgument) {
            is CmAnnotationArgument.ByteValue -> {
                this.type = ProtoBuf.Annotation.Argument.Value.Type.INT8
                this.intValue = value.toLong()
            }

            is CmAnnotationArgument.CharValue -> {
                this.type = ProtoBuf.Annotation.Argument.Value.Type.RUNE
                this.intValue = value.code.toLong()
            }

            is CmAnnotationArgument.ShortValue -> {
                this.type = ProtoBuf.Annotation.Argument.Value.Type.INT16
                this.intValue = value.toLong()
            }

            is CmAnnotationArgument.IntValue -> {
                this.type = ProtoBuf.Annotation.Argument.Value.Type.INT32
                this.intValue = value.toLong()
            }

            is CmAnnotationArgument.LongValue -> {
                this.type = ProtoBuf.Annotation.Argument.Value.Type.INT64
                this.intValue = value
            }

            is CmAnnotationArgument.FloatValue -> {
                this.type = ProtoBuf.Annotation.Argument.Value.Type.FLOAT16
                this.floatValue = value
            }

            is CmAnnotationArgument.DoubleValue -> {
                this.type = ProtoBuf.Annotation.Argument.Value.Type.FLOAT64
                this.doubleValue = value
            }

            is CmAnnotationArgument.BooleanValue -> {
                this.type = ProtoBuf.Annotation.Argument.Value.Type.BOOLEAN
                this.intValue = if (value) 1 else 0
            }

            is CmAnnotationArgument.UByteValue -> {
                this.type = ProtoBuf.Annotation.Argument.Value.Type.INT8
                this.intValue = value.toLong()
                this.flags = Flags.IS_UNSIGNED.toFlags(true)
            }

            is CmAnnotationArgument.UShortValue -> {
                this.type = ProtoBuf.Annotation.Argument.Value.Type.INT16
                this.intValue = value.toLong()
                this.flags = Flags.IS_UNSIGNED.toFlags(true)
            }

            is CmAnnotationArgument.UIntValue -> {
                this.type = ProtoBuf.Annotation.Argument.Value.Type.INT32
                this.intValue = value.toLong()
                this.flags = Flags.IS_UNSIGNED.toFlags(true)
            }

            is CmAnnotationArgument.ULongValue -> {
                this.type = ProtoBuf.Annotation.Argument.Value.Type.INT64
                this.intValue = value.toLong()
                this.flags = Flags.IS_UNSIGNED.toFlags(true)
            }
//            is CmAnnotationArgument.StringValue -> {
//                this.type = ProtoBuf.Annotation.Argument.Value.Type.STRING
//                this.stringValue = strings.getStringIndex(value)
//            }
//            is CmAnnotationArgument.KClassValue -> {
//                this.type = ProtoBuf.Annotation.Argument.Value.Type.CLASS
//                this.classId = strings.getClassNameIndex(className)
//            }
//            is CmAnnotationArgument.ArrayKClassValue -> {
//                this.type = ProtoBuf.Annotation.Argument.Value.Type.CLASS
//                this.classId = strings.getClassNameIndex(className)
//                this.arrayDimensionCount = this@writeAnnotationArgument.arrayDimensionCount
//            }
//            is CmAnnotationArgument.EnumValue -> {
//                this.type = ProtoBuf.Annotation.Argument.Value.Type.ENUM
//                this.classId = strings.getClassNameIndex(enumClassName)
//                this.enumValueId = strings.getStringIndex(enumEntryName)
//            }
//            is CmAnnotationArgument.AnnotationValue -> {
//                this.type = ProtoBuf.Annotation.Argument.Value.Type.ANNOTATION
//                this.annotation = this@writeAnnotationArgument.annotation.writeAnnotation(strings).build()
//            }
//            is CmAnnotationArgument.ArrayValue -> {
//                this.type = ProtoBuf.Annotation.Argument.Value.Type.ARRAY
//                for (element in elements) {
//                    this.addArrayElement(element.writeAnnotationArgument(strings))
//                }
//            }

            else -> {

            }
        }
    }

internal fun StringTable.getClassNameIndex(name: ClassName): Int =
    if (name.isLocalClassName())
        getQualifiedClassNameIndex(name.substring(1), true)
    else
        getQualifiedClassNameIndex(name, false)
