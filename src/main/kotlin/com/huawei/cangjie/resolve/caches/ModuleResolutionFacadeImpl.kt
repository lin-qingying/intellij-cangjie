package com.huawei.cangjie.resolve.caches

import com.huawei.cangjie.analyzer.ModuleInfo
import com.huawei.cangjie.descriptors.ModuleDescriptor

interface ResolutionFacadeModuleDescriptorProvider {
    fun findModuleDescriptor(ideaModuleInfo: ModuleInfo): ModuleDescriptor
}
