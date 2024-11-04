package com.linqingying.cangjie.container

import java.lang.reflect.Type

internal interface ComponentDescriptor : ValueDescriptor {
    fun getRegistrations(): Iterable<Type>
    fun getDependencies(context: ValueResolveContext): Collection<Type>
    val shouldInjectProperties: Boolean
        get() = false
}
class IterableDescriptor(val descriptors: Iterable<ValueDescriptor>) : ValueDescriptor {
    override fun getValue(): Any {
        return descriptors.map { it.getValue() }
    }

    override fun toString(): String = "Iterable: $descriptors"
}
