package com.huawei.cangjie.resolve.deprecation

import com.huawei.cangjie.descriptors.CallableDescriptor
import com.huawei.cangjie.descriptors.DeclarationDescriptor

abstract class DescriptorBasedDeprecationInfo : DeprecationInfo() {
    override val propagatesToOverrides: Boolean
        get() = forcePropagationToOverrides

    /**
     * Marks deprecation as necessary to propagate to overrides
     * even if LanguageFeature.StopPropagatingDeprecationThroughOverrides is disabled or one of the overrides "undeprecated"
     * See DeprecationResolver.deprecationByOverridden for details.
     *
     * Currently, it's only expected to be true for deprecation from unsupported JDK members that might be removed in future versions:
     * we'd like to mark their overrides as unsafe as well.
     *
     * Also, there's an implicit contract that if `forcePropagationToOverrides`, then `propagatesToOverrides` should also be true
     */
    open val forcePropagationToOverrides: Boolean
        get() = false

    abstract val target: DeclarationDescriptor
}

val DEPRECATED_FUNCTION_KEY = object : CallableDescriptor.UserDataKey<DescriptorBasedDeprecationInfo> {}
