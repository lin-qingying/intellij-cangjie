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

//import com.linqingying.cangjie.descriptors.ModuleDescriptor
import com.linqingying.cangjie.builtins.StandardNames
import com.linqingying.cangjie.descriptors.ModuleDescriptor
import com.linqingying.cangjie.descriptors.annotations.AnnotationArgumentVisitor
import com.linqingying.cangjie.descriptors.findClassAcrossModuleDependencies
import com.linqingying.cangjie.name.ClassId
import com.linqingying.cangjie.types.CangJieType
import com.linqingying.cangjie.types.ErrorUtils
import com.linqingying.cangjie.types.error.ErrorTypeKind


abstract class ConstantValue<out T>(open val value: T) {
    abstract fun getType(module: ModuleDescriptor): CangJieType

    abstract fun <R, D> accept(visitor: AnnotationArgumentVisitor<R, D>, data: D): R

    override fun equals(other: Any?): Boolean = this === other || value == (other as? ConstantValue<*>)?.value

    override fun hashCode(): Int = value?.hashCode() ?: 0

    override fun toString(): String = value.toString()

    open fun boxedValue(): Any? = value
}

open class ArrayValue(
    value: List<ConstantValue<*>>,
    private val computeType: (ModuleDescriptor) -> CangJieType
) : ConstantValue<List<ConstantValue<*>>>(value) {
    //    override fun getType(module: ModuleDescriptor): CangJieType = computeType(module).also { type ->
//        assert(CangJieBuiltIns.isArray(type) || CangJieBuiltIns.isPrimitiveArray(type) || CangJieBuiltIns.isUnsignedArrayType(type)) {
//            "Type should be an array, but was $type: $value"
//        }
//    }
    override fun getType(module: ModuleDescriptor): CangJieType {
        return module.builtIns.unitType
    }

    override fun <R, D> accept(visitor: AnnotationArgumentVisitor<R, D>, data: D) = visitor.visitArrayValue(this, data)
}

object UnitValue : ConstantValue<Unit>(Unit) {
    //    override fun getType(module: ModuleDescriptor) = module.builtIns.stringType
    override fun getType(module: ModuleDescriptor) = module.builtIns.unitType

    override fun <R, D> accept(visitor: AnnotationArgumentVisitor<R, D>, data: D) = visitor.visitUnitValue(this, data)

    override fun toString() = "\"$value\""
}

class StringValue(value: String) : ConstantValue<String>(value) {
    //    override fun getType(module: ModuleDescriptor) = module.builtIns.stringType
    override fun getType(module: ModuleDescriptor) = module.builtIns.stringType

    override fun <R, D> accept(visitor: AnnotationArgumentVisitor<R, D>, data: D) = visitor.visitStringValue(this, data)

    override fun toString() = "\"$value\""
}

abstract class IntegerValueConstant<out T> protected constructor(value: T) : ConstantValue<T>(value)


abstract class ErrorValue : ConstantValue<Unit>(Unit) {
    init {
    }

    @Deprecated("Should not be called, for this is not a real value, but an indication of an error")
    override val value: Unit
        get() = throw UnsupportedOperationException()

    override fun <R, D> accept(visitor: AnnotationArgumentVisitor<R, D>, data: D) = visitor.visitErrorValue(this, data)

    class ErrorValueWithMessage(val message: String) : ErrorValue() {
        override fun getType(module: ModuleDescriptor) =
            ErrorUtils.createErrorType(ErrorTypeKind.ERROR_CONSTANT_VALUE, message)

        override fun toString() = message
    }

    companion object {
        fun create(message: String): ErrorValue {
            return ErrorValueWithMessage(message)
        }
    }
}

abstract class IntValue<T>(value: Int) : IntegerValueConstant<Int>(value)
class Int8Value(value: Int8) : IntegerValueConstant<Int8>(value) {
    override fun getType(module: ModuleDescriptor) = module.builtIns.int8Type

    override fun <R, D> accept(visitor: AnnotationArgumentVisitor<R, D>, data: D) = visitor.visitInt8Value(this, data)
}

class Int32Value(value: Int32) : IntegerValueConstant<Int32>(value) {
    override fun getType(module: ModuleDescriptor) = module.builtIns.int32Type

    override fun <R, D> accept(visitor: AnnotationArgumentVisitor<R, D>, data: D) = visitor.visitInt32Value(this, data)
}

class Int64Value(value: Int64) : IntegerValueConstant<Int64>(value) {
    override fun getType(module: ModuleDescriptor) = module.builtIns.int64Type

    override fun <R, D> accept(visitor: AnnotationArgumentVisitor<R, D>, data: D) = visitor.visitInt64Value(this, data)
}

class Int16Value(value: Int16) : IntegerValueConstant<Int16>(value) {
    override fun getType(module: ModuleDescriptor) = module.builtIns.int16Type

    override fun <R, D> accept(visitor: AnnotationArgumentVisitor<R, D>, data: D) = visitor.visitInt16Value(this, data)
}

class RuneValue(value: Rune) : IntegerValueConstant<Rune>(value) {
    override fun getType(module: ModuleDescriptor) = module.builtIns.runeType

    override fun <R, D> accept(visitor: AnnotationArgumentVisitor<R, D>, data: D) = visitor.visitRuneValue(this, data)

    override fun toString() = "\\u%04X ('%s')".format(value.code, getPrintablePart(value))

    private fun getPrintablePart(c: Rune): String = when (c) {
        '\b' -> "\\b"
        '\t' -> "\\t"
        '\n' -> "\\n"

        12.toChar() -> "\\f"
        '\r' -> "\\r"
        else -> if (isPrintableUnicode(c)) c.toString() else "?"
    }

    private fun isPrintableUnicode(c: Rune): Boolean {
        val t = Character.getType(c).toByte()
        return t != Character.UNASSIGNED &&
                t != Character.LINE_SEPARATOR &&
                t != Character.PARAGRAPH_SEPARATOR &&
                t != Character.CONTROL &&
                t != Character.FORMAT &&
                t != Character.PRIVATE_USE &&
                t != Character.SURROGATE
    }
}
class Float16Value(value: Float32) : ConstantValue<Float32>(value) {
    override fun getType(module: ModuleDescriptor) = module.builtIns.float32Type

    override fun <R, D> accept(visitor: AnnotationArgumentVisitor<R, D>, data: D) =
        visitor.visitFloat16Value(this, data)

    override fun toString() = "$value.toFloat()"
}

class Float32Value(value: Float32) : ConstantValue<Float32>(value) {
    override fun getType(module: ModuleDescriptor) = module.builtIns.float32Type

    override fun <R, D> accept(visitor: AnnotationArgumentVisitor<R, D>, data: D) =
        visitor.visitFloat32Value(this, data)

    override fun toString() = "$value.toFloat()"
}

class BoolValue(value: Bool) : ConstantValue<Bool>(value) {
    override fun getType(module: ModuleDescriptor) = module.builtIns.boolType
    override fun <R, D> accept(visitor: AnnotationArgumentVisitor<R, D>, data: D) = visitor.visitBoolValue(this, data)
}

class Float64Value(value: Float64) : ConstantValue<Float64>(value) {
    override fun getType(module: ModuleDescriptor) = module.builtIns.float64Type

    override fun <R, D> accept(visitor: AnnotationArgumentVisitor<R, D>, data: D) =
        visitor.visitFloat64Value(this, data)

    override fun toString() = "$value.toDouble()"
}

abstract class UnsignedValueConstant<out T> protected constructor(value: T) : ConstantValue<T>(value)

class UInt32Value(intValue: Int32) : UnsignedValueConstant<Int32>(intValue) {
    override fun getType(module: ModuleDescriptor): CangJieType {
        return module.findClassAcrossModuleDependencies(StandardNames.FqNames.uInt32ClassId)?.defaultType
            ?: ErrorUtils.createErrorType(ErrorTypeKind.NOT_FOUND_UNSIGNED_TYPE, "UInt32")
    }

    override fun <R, D> accept(visitor: AnnotationArgumentVisitor<R, D>, data: D) = visitor.visitUInt32Value(this, data)

    override fun toString() = "$value.toUInt32()"

    override fun boxedValue(): Any = value.toUInt()
}

class UInt16Value(intValue: Int16) : UnsignedValueConstant<Int16>(intValue) {
    override fun getType(module: ModuleDescriptor): CangJieType {
        return module.findClassAcrossModuleDependencies(StandardNames.FqNames.uInt16ClassId)?.defaultType
            ?: ErrorUtils.createErrorType(ErrorTypeKind.NOT_FOUND_UNSIGNED_TYPE, "UInt16")
    }

    override fun <R, D> accept(visitor: AnnotationArgumentVisitor<R, D>, data: D) = visitor.visitUInt16Value(this, data)

    override fun toString() = "$value.toUInt16()"

    override fun boxedValue(): Any = value.toUInt()
}

class UInt64Value(intValue: Int64) : UnsignedValueConstant<Int64>(intValue) {
    override fun getType(module: ModuleDescriptor): CangJieType {
        return module.findClassAcrossModuleDependencies(StandardNames.FqNames.uInt64ClassId)?.defaultType
            ?: ErrorUtils.createErrorType(ErrorTypeKind.NOT_FOUND_UNSIGNED_TYPE, "UInt64")
    }

    override fun <R, D> accept(visitor: AnnotationArgumentVisitor<R, D>, data: D) = visitor.visitUInt64Value(this, data)

    override fun toString() = "$value.toUInt64()"

    override fun boxedValue(): Any = value.toUInt()
}

class UInt8Value(intValue: Int8) : UnsignedValueConstant<Int8>(intValue) {
    override fun getType(module: ModuleDescriptor): CangJieType {
        return module.findClassAcrossModuleDependencies(StandardNames.FqNames.uInt8ClassId)?.defaultType
            ?: ErrorUtils.createErrorType(ErrorTypeKind.NOT_FOUND_UNSIGNED_TYPE, "UInt8")
    }

    override fun <R, D> accept(visitor: AnnotationArgumentVisitor<R, D>, data: D) = visitor.visitUInt8Value(this, data)

    override fun toString() = "$value.toUInt8()"

    override fun boxedValue(): Any = value.toUInt()
}

data class ClassLiteralValue(val classId: ClassId, val arrayNestedness: Int) {
    override fun toString(): String = buildString {

        append(classId)

    }
}

//class CClassValue(value: Value) : ConstantValue<CClassValue.Value>(value) {
//    sealed class Value {
//        data class NormalClass(val value: ClassLiteralValue) : Value() {
//            val classId: ClassId get() = value.classId
//            val arrayDimensions: Int get() = value.arrayNestedness
//        }
//
//        data class LocalClass(val type: CangJieType) : Value()
//    }
//    constructor(value: ClassLiteralValue) : this(Value.NormalClass(value))
//
//    constructor(classId: ClassId, arrayDimensions: Int) : this(ClassLiteralValue(classId, arrayDimensions))
//
//    override fun getType(module: ModuleDescriptor): CangJieType =
//        CangJieTypeFactory.simpleNotNullType(TypeAttributes.Empty, module.builtIns.kClass, listOf(TypeProjectionImpl(getArgumentType(module))))
//
//}
