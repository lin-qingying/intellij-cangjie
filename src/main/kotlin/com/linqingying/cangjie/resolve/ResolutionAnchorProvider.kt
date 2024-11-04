package com.linqingying.cangjie.resolve

import com.linqingying.cangjie.descriptors.ModuleCapability
import com.linqingying.cangjie.descriptors.ModuleDescriptor
interface ResolutionAnchorProvider {
    fun getResolutionAnchor(moduleDescriptor: ModuleDescriptor): ModuleDescriptor?
}

val RESOLUTION_ANCHOR_PROVIDER_CAPABILITY = ModuleCapability<ResolutionAnchorProvider>("ResolutionAnchorProvider")

fun ModuleDescriptor.getResolutionAnchorIfAny(): ModuleDescriptor? =
    getCapability(RESOLUTION_ANCHOR_PROVIDER_CAPABILITY)?.getResolutionAnchor(this)
