package com.huawei.cangjie.resolve.calls.results

import com.huawei.cangjie.container.DefaultImplementation
import com.huawei.cangjie.descriptors.CallableDescriptor


@DefaultImplementation(impl = PlatformOverloadsSpecificityComparator.None::class)
interface PlatformOverloadsSpecificityComparator {
    fun isMoreSpecificShape(specific: CallableDescriptor, general: CallableDescriptor): Boolean

    object None : PlatformOverloadsSpecificityComparator {
        override fun isMoreSpecificShape(specific: CallableDescriptor, general: CallableDescriptor) = false
    }
}
