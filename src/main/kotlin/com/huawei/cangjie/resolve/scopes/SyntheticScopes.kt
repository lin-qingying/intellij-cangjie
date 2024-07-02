package com.huawei.cangjie.resolve.scopes

import com.huawei.cangjie.container.DefaultImplementation
import com.huawei.cangjie.resolve.scopes.synthetic.FunInterfaceConstructorsScopeProvider

interface SyntheticScope
@DefaultImplementation(impl = FunInterfaceConstructorsScopeProvider::class)
interface SyntheticScopes {
    val scopes: Collection<SyntheticScope>

    object Empty : SyntheticScopes {
        override val scopes: Collection<SyntheticScope> = emptyList()
    }
}
