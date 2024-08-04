package com.linqingying.cangjie.utils

/**
 * Translate exception to unchecked exception.
 *
 * Return type is specified to make it possible to use it like this:
 *     throw ExceptionUtils.rethrow(e);
 * In this case compiler knows that code after this rethrowing won't be executed.
 */
fun rethrow(e: Throwable): RuntimeException {
    throw e
}
