package com.huawei.cangjie.container

import java.lang.reflect.Type

interface ComponentProvider {
    fun resolve(request: Type): ValueDescriptor?
    fun <T> create(request: Class<T>): T
}

interface ValueDescriptor {
    fun getValue(): Any
}