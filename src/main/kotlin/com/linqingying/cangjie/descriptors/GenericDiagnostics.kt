package com.linqingying.cangjie.descriptors

import com.linqingying.cangjie.diagnostics.UnboundDiagnostic

interface GenericDiagnostics<T : UnboundDiagnostic> : Iterable<T> {
    fun all(): Collection<T>

    fun isEmpty(): Boolean = all().isEmpty()

    override fun iterator(): Iterator<T> = all().iterator()
}
