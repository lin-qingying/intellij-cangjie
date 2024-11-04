package com.linqingying.cangjie.resolve

import com.linqingying.cangjie.container.DefaultImplementation
import com.linqingying.cangjie.descriptors.CallableMemberDescriptor


@DefaultImplementation(impl = OverridesBackwardCompatibilityHelper.Default::class)
interface OverridesBackwardCompatibilityHelper {
    fun overrideCanBeOmitted(overridingDescriptor: CallableMemberDescriptor): Boolean

    object Default : OverridesBackwardCompatibilityHelper {
        override fun overrideCanBeOmitted(overridingDescriptor: CallableMemberDescriptor): Boolean =
            false
    }
}
