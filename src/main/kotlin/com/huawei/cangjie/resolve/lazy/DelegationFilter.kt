package com.huawei.cangjie.resolve.lazy

import com.huawei.cangjie.config.LanguageVersionSettings
import com.huawei.cangjie.container.DefaultImplementation
import com.huawei.cangjie.descriptors.CallableMemberDescriptor

@DefaultImplementation(impl = DelegationFilter.Default::class)
interface DelegationFilter {

    fun filter(interfaceMember: CallableMemberDescriptor, languageVersionSettings: LanguageVersionSettings): Boolean

    object Default : DelegationFilter {
        override fun filter(interfaceMember: CallableMemberDescriptor, languageVersionSettings: LanguageVersionSettings) = true
    }
}
