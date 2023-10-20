package com.huawei.cangjie.utils

inline fun <R> runIf(condition: Boolean, block: () -> R): R? = if (condition) block() else null
inline fun <reified T : Any> Sequence<*>.firstIsInstanceOrNull(): T? {
    for (element in this) if (element is T) return element
    return null
}
