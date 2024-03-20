package com.huawei.cangjie.descriptors

interface GenericDiagnostics<T : UnboundDiagnostic> : Iterable<T> {
    fun all(): Collection<T>

    fun isEmpty(): Boolean = all().isEmpty()

    override fun iterator(): Iterator<T> = all().iterator()
}