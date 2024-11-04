package com.linqingying.cangjie.resolve

import com.linqingying.cangjie.container.DefaultImplementation
import com.linqingying.cangjie.descriptors.DeclarationDescriptorNonRoot


@DefaultImplementation(impl = OverloadFilter.Default::class)
interface OverloadFilter {
    fun filterPackageMemberOverloads(overloads: Collection<DeclarationDescriptorNonRoot>): Collection<DeclarationDescriptorNonRoot>

    object Default : OverloadFilter {
        override fun filterPackageMemberOverloads(overloads: Collection<DeclarationDescriptorNonRoot>): Collection<DeclarationDescriptorNonRoot> =
            overloads
    }
}
