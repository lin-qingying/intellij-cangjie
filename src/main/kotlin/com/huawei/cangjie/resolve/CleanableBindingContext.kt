package com.huawei.cangjie.resolve


interface CleanableBindingContext : BindingContext {
    /**
     * Removes all recorded data except diagnostics.
     */
    fun clear()
}
