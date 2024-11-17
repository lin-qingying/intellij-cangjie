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
//import com.linqingying.cangjie.contracts.description.EventOccurrencesRange
//import com.linqingying.cangjie.contracts.model.ESEffect
//import com.linqingying.cangjie.contracts.model.ESValue
//import com.linqingying.cangjie.contracts.model.SimpleEffect
//
//
//data class ESCalls(val callable: ESValue, val kind: EventOccurrencesRange) : SimpleEffect() {
//    override fun isImplies(other: ESEffect): Boolean? {
//        if (other !is ESCalls) return null
//
//        if (callable != other.callable) return null
//
//        return kind == other.kind
//    }
//
//}
//
//data class ESReturns(val value: ESValue) : SimpleEffect() {
//    override fun isImplies(other: ESEffect): Boolean? {
//        if (other !is ESReturns) return null
//
//        if (this.value !is ESConstant || other.value !is ESConstant) return this.value == other.value
//
//        // ESReturns(x) implies ESReturns(?) for any 'x'
//        if (other.value.isWildcard) return true
//
//        return value == other.value
//    }
//}
//
//inline fun ESEffect.isReturns(block: ESReturns.() -> Boolean): Boolean =
//    this is ESReturns && block()
