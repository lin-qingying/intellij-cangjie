package com.linqingying.cangjie.resolve.lazy

import com.linqingying.cangjie.config.LanguageVersionSettings
import com.linqingying.cangjie.container.DefaultImplementation
import com.linqingying.cangjie.descriptors.CallableMemberDescriptor

@DefaultImplementation(impl = DelegationFilter.Default::class)
interface DelegationFilter {

    fun filter(interfaceMember: CallableMemberDescriptor, languageVersionSettings: LanguageVersionSettings): Boolean

    object Default : DelegationFilter {
        override fun filter(interfaceMember: CallableMemberDescriptor, languageVersionSettings: LanguageVersionSettings) = true
    }
}
