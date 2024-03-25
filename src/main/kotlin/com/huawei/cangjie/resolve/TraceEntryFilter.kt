package com.huawei.cangjie.resolve

import com.huawei.cangjie.utils.slicedMap.WritableSlice

interface TraceEntryFilter {

    fun accept(slice: WritableSlice<*, *>?, key: Any?): Boolean

}