package com.huawei.cangjie.resolve.lazy

import com.huawei.cangjie.resolve.BindingTraceFilter


enum class BodyResolveMode(val bindingTraceFilter: BindingTraceFilter, val doControlFlowAnalysis: Boolean, val resolveAdditionals: Boolean = true) {

    FULL(BindingTraceFilter.ACCEPT_ALL, doControlFlowAnalysis = true),


    PARTIAL_FOR_COMPLETION(BindingTraceFilter.NO_DIAGNOSTICS, doControlFlowAnalysis = true),


    PARTIAL_WITH_DIAGNOSTICS(BindingTraceFilter.ACCEPT_ALL, doControlFlowAnalysis = true),


    PARTIAL_WITH_CFA(BindingTraceFilter.NO_DIAGNOSTICS, doControlFlowAnalysis = true),


    PARTIAL(BindingTraceFilter.NO_DIAGNOSTICS, doControlFlowAnalysis = false),


    PARTIAL_NO_ADDITIONAL(BindingTraceFilter.NO_DIAGNOSTICS, doControlFlowAnalysis = false, resolveAdditionals = false)
    ;

    fun doesNotLessThan(other: BodyResolveMode): Boolean {
        return this <= other && this.bindingTraceFilter.includesEverythingIn(other.bindingTraceFilter)
    }
}
