package com.huawei.cangjie.resolve

import com.huawei.cangjie.utils.slicedMap.WritableSlice





class BindingTraceForBodyResolve(
    parentContext: BindingContext,
    debugName: String,
    filter: BindingTraceFilter = BindingTraceFilter.ACCEPT_ALL
) : DelegatingBindingTrace(parentContext, debugName, filter = filter, allowSliceRewrite = true) {
    override fun <K, V> getKeys(slice: WritableSlice<K, V>): Collection<K> {
//        if (slice == BindingContext.DEFERRED_TYPE) {
//            return map.getKeys(slice)
//        }

        return super.getKeys(slice)
    }


}