package com.huawei.cangjie.descriptors

import com.huawei.cangjie.diagnostics.UnboundDiagnostic


open class SimpleGenericDiagnostics<T : UnboundDiagnostic>(diagnostics: Collection<T>) : GenericDiagnostics<T> {
    //copy to prevent external change
    private val diagnostics = ArrayList(diagnostics)

    override fun all() = diagnostics
}
