package com.linqingying.cangjie.resolve.caches

import com.linqingying.cangjie.analyzer.ModuleInfo
import com.linqingying.cangjie.descriptors.ModuleDescriptor

interface ResolutionFacadeModuleDescriptorProvider {
    fun findModuleDescriptor(ideaModuleInfo: ModuleInfo): ModuleDescriptor
}
