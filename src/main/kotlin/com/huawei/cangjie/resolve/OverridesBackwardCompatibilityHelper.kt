package com.huawei.cangjie.resolve

import com.huawei.cangjie.container.DefaultImplementation
import com.huawei.cangjie.descriptors.CallableMemberDescriptor


@DefaultImplementation(impl = OverridesBackwardCompatibilityHelper.Default::class)
interface OverridesBackwardCompatibilityHelper {
    fun overrideCanBeOmitted(overridingDescriptor: CallableMemberDescriptor): Boolean

    object Default : OverridesBackwardCompatibilityHelper {
        override fun overrideCanBeOmitted(overridingDescriptor: CallableMemberDescriptor): Boolean =
            false
    }
}
