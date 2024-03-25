package com.huawei.cangjie.storage

interface MemoizedFunctionToNotNull<in P, out R : Any> : Function1<P, R> {
    fun isComputed(key: P): Boolean
}
interface MemoizedFunctionToNullable<in P, out R : Any> : Function1<P, R?> {
    fun isComputed(key: P): Boolean
}