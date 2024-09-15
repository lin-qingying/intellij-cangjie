package com.huawei.cangjie.resolve

import com.huawei.cangjie.psi.CjMainFunction
import com.huawei.cangjie.psi.CjNamedFunction

interface BodyResolveCache {
    fun resolveFunctionBody(function: CjNamedFunction): BindingContext
    fun resolveMainFunctionBody(function: CjMainFunction): BindingContext

    object ThrowException : BodyResolveCache {
        override fun resolveMainFunctionBody(function: CjMainFunction): BindingContext {
            throw UnsupportedOperationException()
        }
        override fun resolveFunctionBody(function: CjNamedFunction): BindingContext {
            throw UnsupportedOperationException()
        }
    }
}
