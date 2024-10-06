//package com.huawei.cangjie.contracts.model
//
//import com.huawei.cangjie.contracts.model.structure.ESEqual
//import com.huawei.cangjie.contracts.model.structure.ESIs
//
//
//interface ESExpressionVisitor<out T> {
//    fun visitIs(isOperator: ESIs): T
//    fun visitEqual(equal: ESEqual): T
//    fun visitAnd(and: ESAnd): T
//    fun visitNot(not: ESNot): T
//    fun visitOr(or: ESOr): T
//
//    fun visitVariable(esVariable: ESVariable): T
//    fun visitConstant(esConstant: ESConstant): T
//
//    fun visitReceiver(esReceiver: ESReceiver): T
//
//    // ESLambda is invisible in this module
//    fun visitLambda(lambda: ESValue): T
//}
