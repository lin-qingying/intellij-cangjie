//package com.huawei.cangjie.contracts.model.structure
//
//import com.huawei.cangjie.contracts.model.ESExpressionVisitor
//import com.huawei.cangjie.contracts.model.ESValue
//import com.huawei.cangjie.resolve.scopes.receivers.ReceiverValue
//import java.util.*
//interface ESReceiver : ESValue {
//    val receiverValue: ReceiverValue
//
//    override fun <T> accept(visitor: ESExpressionVisitor<T>): T = visitor.visitReceiver(this)
//}
//
///**
// * [ESConstant] represent some constant is Effect System
// *
// * There is only few constants are supported (@see [ESConstant.Companion])
// */
//class ESConstant internal constructor(val constantReference: ConstantReference, override val type: ESType) : AbstractESValue(type) {
//    override fun <T> accept(visitor: ESExpressionVisitor<T>): T = visitor.visitConstant(this)
//
//    override fun equals(other: Any?): Boolean = other is ESConstant && constantReference == other.constantReference
//
//    override fun hashCode(): Int = Objects.hashCode(constantReference)
//
//    override fun toString(): String = constantReference.name
//
//    fun isNullConstant(): Boolean =
//        constantReference == ConstantReference.NULL || constantReference == ConstantReference.NOT_NULL
//}
