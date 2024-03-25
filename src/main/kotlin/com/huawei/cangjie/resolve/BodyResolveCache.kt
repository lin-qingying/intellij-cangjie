package com.huawei.cangjie.resolve

import com.huawei.cangjie.psi.CjNamedFunction

interface BodyResolveCache {
    fun resolveFunctionBody(function: CjNamedFunction): BindingContext

    object ThrowException : BodyResolveCache {
        override fun resolveFunctionBody(function: CjNamedFunction): BindingContext {
            throw UnsupportedOperationException()
        }
    }
}
