package com.huawei.cangjie.resolve



class BindingTraceFilter(val ignoreDiagnostics: Boolean) {
    companion object {
        val ACCEPT_ALL = BindingTraceFilter(false)
        val NO_DIAGNOSTICS = BindingTraceFilter(true)
    }

    fun includesEverythingIn(otherFilter: BindingTraceFilter): Boolean {
        if (ignoreDiagnostics && !otherFilter.ignoreDiagnostics) {
            return false
        }
        return true
    }
}
