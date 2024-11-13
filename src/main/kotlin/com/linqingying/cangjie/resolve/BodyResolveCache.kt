package com.linqingying.cangjie.resolve

import com.linqingying.cangjie.psi.CjMacroDeclaration
import com.linqingying.cangjie.psi.CjMainFunction
import com.linqingying.cangjie.psi.CjNamedFunction

interface BodyResolveCache {
    fun resolveFunctionBody(function: CjNamedFunction): BindingContext
    fun resolveMainFunctionBody(function: CjMainFunction): BindingContext
    fun resolveMacroBody(macro: CjMacroDeclaration): BindingContext

    object ThrowException : BodyResolveCache {
        override fun resolveMainFunctionBody(function: CjMainFunction): BindingContext {
            throw UnsupportedOperationException()
        }

        override fun resolveMacroBody(macro: CjMacroDeclaration): BindingContext {
            throw UnsupportedOperationException()

        }

        override fun resolveFunctionBody(function: CjNamedFunction): BindingContext {
            throw UnsupportedOperationException()
        }
    }
}
