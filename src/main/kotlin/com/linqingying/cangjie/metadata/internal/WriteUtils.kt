/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package com.linqingying.cangjie.metadata.internal

import com.linqingying.cangjie.metadata.CmAnnotation
import com.linqingying.cangjie.metadata.CmAnnotationArgument
import com.linqingying.cangjie.metadata.ProtoBuf
import com.linqingying.cangjie.metadata.deserialization.Flags
import com.linqingying.cangjie.metadata.node.ClassName
import com.linqingying.cangjie.metadata.node.isLocalClassName
import com.linqingying.cangjie.metadata.serialization.StringTable

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
