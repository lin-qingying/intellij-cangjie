package com.huawei.cangjie.container

class UnresolvedServiceException(container: ComponentProvider, request: Class<*>) :
    IllegalArgumentException("Unresolved service: $request in $container")

fun <T : Any> ComponentProvider.getService(request: Class<T>): T {
    return tryGetService(request) ?: throw UnresolvedServiceException(this, request)
}

@Suppress("UNCHECKED_CAST")
fun <T : Any> ComponentProvider.tryGetService(request: Class<T>): T? {
    return resolve(request)?.getValue() as T?
}

inline fun <reified T : Any> ComponentProvider.get(): T {
    return getService(T::class.java)
}