package com.huawei.cangjie.descriptors


open class SimpleGenericDiagnostics<T : UnboundDiagnostic>(diagnostics: Collection<T>) : GenericDiagnostics<T> {
    //copy to prevent external change
    private val diagnostics = ArrayList(diagnostics)

    override fun all() = diagnostics
}
