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

//package com.linqingying.cangjie.contracts.model.structure
//
//import com.linqingying.cangjie.contracts.model.ESExpressionVisitor
//import com.linqingying.cangjie.contracts.model.ESValue
//import com.linqingying.cangjie.resolve.scopes.receivers.ReceiverValue
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
////class ESConstant internal constructor(val constantReference: ConstantReference, override val type: ESType) : AbstractESValue(type) {
////    override fun <T> accept(visitor: ESExpressionVisitor<T>): T = visitor.visitConstant(this)
////
////    override fun equals(other: Any?): Boolean = other is ESConstant && constantReference == other.constantReference
////
////    override fun hashCode(): Int = Objects.hashCode(constantReference)
////
////    override fun toString(): String = constantReference.name
////
////    fun isNullConstant(): Boolean =
////        constantReference == ConstantReference.NULL || constantReference == ConstantReference.NOT_NULL
////}
