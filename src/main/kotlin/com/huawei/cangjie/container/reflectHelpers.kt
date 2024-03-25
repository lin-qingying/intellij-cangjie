package com.huawei.cangjie.container

import java.lang.reflect.InvocationTargetException


inline fun <T> runWithUnwrappingInvocationException(block: () -> T) =
    try {
        block()
    } catch (e: InvocationTargetException) {
        throw e.targetException ?: e
    }
