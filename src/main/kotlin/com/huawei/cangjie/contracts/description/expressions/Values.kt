//package com.huawei.cangjie.contracts.description.expressions
//
//open class ConstantReference(val name: String) : ContractDescriptionValue {
//    override fun <R, D> accept(contractDescriptionVisitor: ContractDescriptionVisitor<R, D>, data: D): R =
//        contractDescriptionVisitor.visitConstantDescriptor(this, data)
//
//    companion object {
//        val NULL = ConstantReference("NULL")
//        val WILDCARD = ConstantReference("WILDCARD")
//        val NOT_NULL = ConstantReference("NOT_NULL")
//    }
//}
