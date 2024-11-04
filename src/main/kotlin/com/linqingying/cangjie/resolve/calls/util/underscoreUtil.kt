package com.linqingying.cangjie.resolve.calls.util

import com.linqingying.cangjie.psi.CjNamedDeclaration
import com.intellij.psi.StubBasedPsiElement

/**
 * val lambda = fun(x: Int, _: String, `_`: Double) = 1
 *
 * This property is true only for second value parameter in the example above
 */
val CjNamedDeclaration.isSingleUnderscore: Boolean
    get() {
        // We don't want to call 'getNameIdentifier' on stubs to prevent text building
        // But it's fine because one-underscore names are prohibited for non-local declarations (only lambda parameters, local vars are allowed)
        if (this is StubBasedPsiElement<*> && this.stub != null) return false
        return nameIdentifier?.text == "_"
    }
