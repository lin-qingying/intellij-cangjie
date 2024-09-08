//package com.huawei.cangjie.contracts.model.structure
//
//import com.huawei.cangjie.contracts.description.EventOccurrencesRange
//import com.huawei.cangjie.contracts.model.ESEffect
//import com.huawei.cangjie.contracts.model.ESValue
//import com.huawei.cangjie.contracts.model.SimpleEffect
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
