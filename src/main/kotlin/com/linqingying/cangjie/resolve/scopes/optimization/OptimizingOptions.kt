package com.linqingying.cangjie.resolve.scopes.optimization

import com.linqingying.cangjie.container.DefaultImplementation
import com.linqingying.cangjie.descriptors.ModuleDescriptor


@DefaultImplementation(OptimizingOptions.Default::class)
interface OptimizingOptions {
    fun shouldCalculateAllNamesForLazyImportScopeOptimizing(moduleDescriptor: ModuleDescriptor?): Boolean

    object Default : OptimizingOptions {
        override fun shouldCalculateAllNamesForLazyImportScopeOptimizing(moduleDescriptor: ModuleDescriptor?): Boolean {
            return true
        }
    }
}
