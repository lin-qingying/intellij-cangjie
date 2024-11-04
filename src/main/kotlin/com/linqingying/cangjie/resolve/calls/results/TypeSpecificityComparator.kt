package com.linqingying.cangjie.resolve.calls.results

import com.linqingying.cangjie.container.DefaultImplementation
import com.linqingying.cangjie.container.PlatformSpecificExtension
import com.linqingying.cangjie.types.model.CangJieTypeMarker

@DefaultImplementation(impl = TypeSpecificityComparator.NONE::class)
interface TypeSpecificityComparator : PlatformSpecificExtension<TypeSpecificityComparator> {
    fun isDefinitelyLessSpecific(specific: CangJieTypeMarker, general: CangJieTypeMarker): Boolean

    object NONE : TypeSpecificityComparator {
        override fun isDefinitelyLessSpecific(specific: CangJieTypeMarker, general: CangJieTypeMarker) = false
    }
}
