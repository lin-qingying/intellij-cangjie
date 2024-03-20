package com.huawei.cangjie.resolve.constants

import com.huawei.cangjie.descriptors.ModuleDescriptor
import com.huawei.cangjie.descriptors.annotations.AnnotationArgumentVisitor
import com.huawei.cangjie.types.CangJieType


abstract class ConstantValue<out T>(open val value: T) {
    abstract fun getType(module: ModuleDescriptor): CangJieType

    abstract fun <R, D> accept(visitor: AnnotationArgumentVisitor<R, D>, data: D): R

    override fun equals(other: Any?): Boolean = this === other || value == (other as? ConstantValue<*>)?.value

    override fun hashCode(): Int = value?.hashCode() ?: 0

    override fun toString(): String = value.toString()

    open fun boxedValue(): Any? = value
}

//open class ArrayValue(
//    value: List<ConstantValue<*>>,
//    private val computeType: (ModuleDescriptor) -> CangJieType
//) : ConstantValue<List<ConstantValue<*>>>(value) {
//    override fun getType(module: ModuleDescriptor): CangJieType = computeType(module).also { type ->
//        assert(CangJieBuiltIns.isArray(type) || CangJieBuiltIns.isPrimitiveArray(type) || CangJieBuiltIns.isUnsignedArrayType(type)) {
//            "Type should be an array, but was $type: $value"
//        }
//    }
//
//    override fun <R, D> accept(visitor: AnnotationArgumentVisitor<R, D>, data: D) = visitor.visitArrayValue(this, data)
//}
