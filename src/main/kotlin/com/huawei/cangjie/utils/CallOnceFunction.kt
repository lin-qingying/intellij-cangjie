package com.huawei.cangjie.utils

import java.util.concurrent.atomic.AtomicReference

class CallOnceFunction<F, T>(private val defaultValue: T, delegate: Function1<F, T>) : Function1<F, T> {
    private val functionRef: AtomicReference<Function1<F, T>?> = AtomicReference(delegate)

    override fun invoke(p1: F): T = functionRef.getAndSet(null)?.invoke(p1) ?: defaultValue
}
