package com.linqingying.cangjie.resolve.calls.tower

import com.linqingying.cangjie.container.DefaultImplementation
import com.linqingying.cangjie.resolve.scopes.HierarchicalScope

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
