package com.huawei.cangjie.resolve.calls.tower

import com.huawei.cangjie.container.DefaultImplementation
import com.huawei.cangjie.resolve.scopes.HierarchicalScope

@DefaultImplementation(ImplicitsExtensionsResolutionFilter.Default::class)
interface ImplicitsExtensionsResolutionFilter {
    fun getScopesWithInfo(
        scopes: Sequence<HierarchicalScope>
    ): Sequence<ScopeWithImplicitsExtensionsResolutionInfo>

    object Default : ImplicitsExtensionsResolutionFilter {
        override fun getScopesWithInfo(
            scopes: Sequence<HierarchicalScope>
        ): Sequence<ScopeWithImplicitsExtensionsResolutionInfo> = scopes.map { scope ->
            ScopeWithImplicitsExtensionsResolutionInfo(scope, true)
        }
    }
}

class ScopeWithImplicitsExtensionsResolutionInfo(
    val scope: HierarchicalScope,
    val resolveExtensionsForImplicitReceiver: Boolean,
)
