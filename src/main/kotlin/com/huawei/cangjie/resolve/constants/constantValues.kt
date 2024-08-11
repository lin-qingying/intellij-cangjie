package com.huawei.cangjie.resolve.constants

//import com.huawei.cangjie.descriptors.ModuleDescriptor
import com.huawei.cangjie.descriptors.ModuleDescriptor
import com.huawei.cangjie.descriptors.annotations.AnnotationArgumentVisitor
import com.huawei.cangjie.types.CangJieType
import com.huawei.cangjie.types.ErrorUtils
import com.huawei.cangjie.types.error.ErrorTypeKind


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

class StringValue(value: String) : ConstantValue<String>(value) {
    //    override fun getType(module: ModuleDescriptor) = module.builtIns.stringType
    override fun getType(module: ModuleDescriptor) = module.builtIns.unitType

    override fun <R, D> accept(visitor: AnnotationArgumentVisitor<R, D>, data: D) = visitor.visitStringValue(this, data)

    override fun toString() = "\"$value\""
}

abstract class IntegerValueConstant<out T> protected constructor(value: T) : ConstantValue<T>(value)

abstract class IntValue<T>(value: Int) : IntegerValueConstant<Int>(value)

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

class Int32Value(value: Int) : IntValue<Int>(value) {
    override fun getType(module: ModuleDescriptor) = module.builtIns.int32Type

    override fun <R, D> accept(visitor: AnnotationArgumentVisitor<R, D>, data: D) = visitor.visitIntValue(this, data)
}
