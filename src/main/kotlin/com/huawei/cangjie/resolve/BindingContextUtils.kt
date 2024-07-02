package com.huawei.cangjie.resolve

import com.huawei.cangjie.descriptors.BindingTrace
import com.huawei.cangjie.psi.CjElement
import com.huawei.cangjie.resolve.BindingContext.LEXICAL_SCOPE
import com.huawei.cangjie.resolve.scopes.LexicalScope
import com.huawei.cangjie.resolve.scopes.takeSnapshot

fun BindingTrace.recordScope(scope: LexicalScope, element: CjElement?) {
    if (element != null) {
//        TODO()
        record(LEXICAL_SCOPE, element, scope.takeSnapshot() as LexicalScope)
    }
}