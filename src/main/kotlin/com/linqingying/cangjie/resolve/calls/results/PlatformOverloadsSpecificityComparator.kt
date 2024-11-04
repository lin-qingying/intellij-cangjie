package com.linqingying.cangjie.resolve.calls.results

import com.linqingying.cangjie.container.DefaultImplementation
import com.linqingying.cangjie.descriptors.CallableDescriptor


@DefaultImplementation(impl = PlatformOverloadsSpecificityComparator.None::class)
interface PlatformOverloadsSpecificityComparator {
    fun isMoreSpecificShape(specific: CallableDescriptor, general: CallableDescriptor): Boolean

    object None : PlatformOverloadsSpecificityComparator {
        override fun isMoreSpecificShape(specific: CallableDescriptor, general: CallableDescriptor) = false
    }
}
