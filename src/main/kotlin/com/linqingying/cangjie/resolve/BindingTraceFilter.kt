package com.linqingying.cangjie.resolve



class BindingTraceFilter(val ignoreDiagnostics: Boolean) {
    companion object {
        val ACCEPT_ALL = BindingTraceFilter(false)
        val NO_DIAGNOSTICS = BindingTraceFilter(true)
    }

    fun includesEverythingIn(otherFilter: BindingTraceFilter): Boolean {
        return !(ignoreDiagnostics && !otherFilter.ignoreDiagnostics)
    }
}
