package com.huawei.cangjie.resolve.calls.results

import com.huawei.cangjie.container.DefaultImplementation
import com.huawei.cangjie.container.PlatformSpecificExtension
import com.huawei.cangjie.types.model.CangJieTypeMarker

@DefaultImplementation(impl = TypeSpecificityComparator.NONE::class)
interface TypeSpecificityComparator : PlatformSpecificExtension<TypeSpecificityComparator> {
    fun isDefinitelyLessSpecific(specific: CangJieTypeMarker, general: CangJieTypeMarker): Boolean

    object NONE : TypeSpecificityComparator {
        override fun isDefinitelyLessSpecific(specific: CangJieTypeMarker, general: CangJieTypeMarker) = false
    }
}
