package com.huawei.cangjie.resolve

import com.huawei.cangjie.container.DefaultImplementation
import com.huawei.cangjie.descriptors.DeclarationDescriptorNonRoot


@DefaultImplementation(impl = OverloadFilter.Default::class)
interface OverloadFilter {
    fun filterPackageMemberOverloads(overloads: Collection<DeclarationDescriptorNonRoot>): Collection<DeclarationDescriptorNonRoot>

    object Default : OverloadFilter {
        override fun filterPackageMemberOverloads(overloads: Collection<DeclarationDescriptorNonRoot>): Collection<DeclarationDescriptorNonRoot> =
            overloads
    }
}
