package com.linqingying.cangjie.resolve

import com.linqingying.cangjie.utils.slicedMap.WritableSlice

interface TraceEntryFilter {

    fun accept(slice: WritableSlice<*, *>?, key: Any?): Boolean

}
