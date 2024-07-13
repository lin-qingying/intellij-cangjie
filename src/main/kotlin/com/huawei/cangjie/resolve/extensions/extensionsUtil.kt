package com.huawei.cangjie.resolve.extensions

import com.intellij.openapi.diagnostic.Logger

internal inline fun <T : Any, R> withLinkageErrorLogger(receiver: T, block: T.() -> R): R {
    try {
        return receiver.block()
    } catch (e: LinkageError) {
        val logger = Logger.getInstance(receiver::class.java)
        logger.error("${receiver::class.java.name} caused LinkageError", e)
        throw e
    }
}

