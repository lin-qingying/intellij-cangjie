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

package com.linqingying.cangjie.serialization.deserialization

import com.jetbrains.jsonSchema.impl.nestedCompletions.letIf
import com.linqingying.cangjie.builtins.CangJieBuiltIns
import com.linqingying.cangjie.descriptors.*
import com.linqingying.cangjie.descriptors.annotations.AnnotationDescriptor
import com.linqingying.cangjie.descriptors.annotations.AnnotationDescriptorImpl
import com.linqingying.cangjie.metadata.ProtoBuf
import com.linqingying.cangjie.metadata.deserialization.NameResolver
import com.linqingying.cangjie.name.ClassId
import com.linqingying.cangjie.name.Name
import com.linqingying.cangjie.resolve.DescriptorUtils
import com.linqingying.cangjie.types.ErrorUtils
import com.linqingying.cangjie.metadata.ProtoBuf.Annotation
import com.linqingying.cangjie.types.CangJieType
import com.linqingying.cangjie.metadata.ProtoBuf.Annotation.Argument.Value.Type
import com.linqingying.cangjie.metadata.deserialization.Flags
import com.linqingying.cangjie.resolve.constants.*

class AnnotationDeserializer(private val module: ModuleDescriptor, private val notFoundClasses: NotFoundClasses)
{
    private val builtIns: CangJieBuiltIns
        get() = module.builtIns

    private inline fun <T, R> T.letIf(predicate: Boolean, f: (T) -> R, g: (T) -> R): R =
        if (predicate) f(this) else g(this)

    private fun resolveClass(classId: ClassId): ClassDescriptor {
        return module.findNonGenericClassAcrossDependencies(classId, notFoundClasses)
    }
    fun resolveValue(expectedType: CangJieType, value: Annotation.Argument.Value, nameResolver: NameResolver): ConstantValue<*> {
        val isUnsigned = Flags.IS_UNSIGNED.get(value.flags)

        return when (value.type) {
            Type.INT8 -> value.intValue.toByte().letIf(isUnsigned, ::UInt8Value, ::Int8Value)
            Type.RUNE -> RuneValue(value.intValue.toInt().toChar())
            Type.INT16 -> value.intValue.toShort().letIf(isUnsigned, ::UInt16Value, ::Int16Value)
            Type.INT32 -> value.intValue.toInt().letIf(isUnsigned, ::UInt32Value, ::Int32Value)
            Type.INT64 -> value.intValue.letIf(isUnsigned, ::UInt64Value, ::Int64Value)
            Type.FLOAT16 -> Float16Value(value.floatValue)
            Type.FLOAT32 -> Float32Value(value.floatValue)
            Type.FLOAT64 -> Float64Value(value.doubleValue)

            Type.BOOLEAN -> BoolValue(value.intValue != 0L)

            else -> error("Unsupported annotation argument type: ${value.type} (expected $expectedType)")
        }
    }
    private fun resolveValueAndCheckExpectedType(expectedType: CangJieType, value: Annotation.Argument.Value, nameResolver: NameResolver): ConstantValue<*> {
        return resolveValue(expectedType, value, nameResolver).takeIf {
            doesValueConformToExpectedType(it, expectedType, value)
        } ?: ErrorValue.create("Unexpected argument value: actual type ${value.type} != expected type $expectedType")
    }
    private fun doesValueConformToExpectedType(result: ConstantValue<*>, expectedType: CangJieType, value: Annotation.Argument.Value): Boolean {
        return when (value.type) {
//            Type.CLASS -> {
//                val expectedClass = expectedType.constructor.declarationDescriptor as? ClassDescriptor
//                // We could also check that the class value's type is a subtype of the expected type, but loading the definition of the
//                // referenced class here is undesirable and may even be incorrect (because the module might be different at the
//                // destination where these constant values are read). This can lead to slightly incorrect model in some edge cases.
//                expectedClass == null || CangJieBuiltIns.isKClass(expectedClass)
//            }
//            Type.ARRAY -> {
//                check(result is ArrayValue && result.value.size == value.arrayElementList.size) {
//                    "Deserialized ArrayValue should have the same number of elements as the original array value: $result"
//                }
//                val expectedElementType = builtIns.getArrayElementType(expectedType)
//                result.value.indices.all { i ->
//                    doesValueConformToExpectedType(result.value[i], expectedElementType, value.getArrayElement(i))
//                }
//            }
            else -> result.getType(module) == expectedType
        }
    }
    private fun resolveArgument(
        proto: Annotation.Argument,
        parameterByName: Map<Name, ValueParameterDescriptor>,
        nameResolver: NameResolver
    ): Pair<Name, ConstantValue<*>>? {
        val parameter = parameterByName[nameResolver.getName(proto.nameId)] ?: return null
        return Pair(nameResolver.getName(proto.nameId), resolveValueAndCheckExpectedType(parameter.type, proto.value, nameResolver))
    }
    fun deserializeAnnotation(proto: Annotation, nameResolver: NameResolver): AnnotationDescriptor {
        val annotationClass = resolveClass(nameResolver.getClassId(proto.id))

        var arguments = emptyMap<Name, ConstantValue<*>>()
        if (proto.argumentCount != 0 && !ErrorUtils.isError(annotationClass) && DescriptorUtils.isAnnotationClass(annotationClass)) {
            val constructor = annotationClass.constructors.singleOrNull()
            if (constructor != null) {
                val parameterByName = constructor.valueParameters.associateBy { it.name }
                arguments = proto.argumentList.mapNotNull { resolveArgument(it, parameterByName, nameResolver) }.toMap()
            }
        }

        return AnnotationDescriptorImpl(annotationClass.defaultType, arguments, SourceElement.NO_SOURCE)
    }
}
