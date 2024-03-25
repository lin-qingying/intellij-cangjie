package com.huawei.cangjie.container

import java.lang.reflect.Type

open class InstanceComponentDescriptor(val instance: Any) : ComponentDescriptor {

    override fun getValue(): Any = instance
    override fun getRegistrations(): Iterable<Type> = instance::class.java.getInfo().registrations

    override fun getDependencies(context: ValueResolveContext): Collection<Class<*>> = emptyList()

    override fun toString(): String {
        return "Instance: ${instance::class.java.simpleName}"
    }
}
class DefaultInstanceComponentDescriptor(instance: Any): InstanceComponentDescriptor(instance) {
    override fun toString() = "Default instance: ${instance.javaClass.simpleName}"
}