package com.huawei.cangjie.utils

import com.huawei.cangjie.descriptors.DeclarationDescriptor

interface ModuleVisibilityHelper {
    fun isInFriendModule(what: DeclarationDescriptor, from: DeclarationDescriptor): Boolean

    object EMPTY: ModuleVisibilityHelper {
        override fun isInFriendModule(what: DeclarationDescriptor, from: DeclarationDescriptor) = true
    }
}
class ModuleVisibilityHelperImpl : ModuleVisibilityHelper {
    override fun isInFriendModule(what: DeclarationDescriptor, from: DeclarationDescriptor): Boolean {
        TODO("Not yet implemented")
    }


}
