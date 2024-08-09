package com.linqingying.cangjie.container

import java.io.Closeable
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type
import java.lang.reflect.WildcardType


interface ComponentContainer {
    val containerId: String

    fun createResolveContext(requestingDescriptor: ValueDescriptor): ValueResolveContext
}

interface ComponentProvider {
    fun resolve(request: Type): ValueDescriptor?
    fun <T> create(request: Class<T>): T
}

interface ValueDescriptor {
    fun getValue(): Any
}


fun StorageComponentContainer.registerSingleton(klass: Class<*>): StorageComponentContainer {
    return registerDescriptors(listOf(SingletonTypeComponentDescriptor(this, klass)))
}

class ContainerConsistencyException(message: String) : Exception(message)
object DynamicComponentDescriptor : ValueDescriptor {
    override fun getValue(): Any = throw UnsupportedOperationException()
    override fun toString(): String = "Dynamic"
}

class StorageComponentContainer(
    private val id: String,
    parent: StorageComponentContainer? = null
) : ComponentContainer, ComponentProvider, Closeable {

    val unknownContext: ComponentResolveContext by lazy {
        val parentContext = parent?.let { ComponentResolveContext(it, DynamicComponentDescriptor) }
        ComponentResolveContext(this, DynamicComponentDescriptor, parentContext)
    }
    private val componentStorage: ComponentStorage = ComponentStorage(id, parent?.componentStorage)

    override val containerId
        get() = "Container: $id"

    override fun createResolveContext(requestingDescriptor: ValueDescriptor): ValueResolveContext {
        if (requestingDescriptor == DynamicComponentDescriptor) // cache unknown component descriptor
            return unknownContext
        return ComponentResolveContext(this, requestingDescriptor)
    }
    internal fun registerDescriptors(descriptors: List<ComponentDescriptor>): StorageComponentContainer {
        componentStorage.registerDescriptors(unknownContext, descriptors)
        return this
    }
    fun compose(): StorageComponentContainer {
        componentStorage.compose(unknownContext)
        return this
    }
    private fun resolveIterable(request: Type, context: ValueResolveContext): ValueDescriptor? {
        if (request !is ParameterizedType) return null
        val rawType = request.rawType
        if (rawType != Iterable::class.java) return null
        val typeArguments = request.actualTypeArguments
        if (typeArguments.size != 1) return null
        val iterableType = when (val iterableTypeArgument = typeArguments[0]) {
            is WildcardType -> {
                val upperBounds = iterableTypeArgument.upperBounds
                if (upperBounds.size != 1) return null
                upperBounds[0]
            }
            is Class<*> -> iterableTypeArgument
            is ParameterizedType -> iterableTypeArgument
            else -> return null
        }
        return IterableDescriptor(componentStorage.resolveMultiple(iterableType, context))
    }

    fun resolveMultiple(request: Class<*>, context: ValueResolveContext = unknownContext): Iterable<ValueDescriptor> {
        return componentStorage.resolveMultiple(request, context)
    }
    fun resolve(request: Type, context: ValueResolveContext): ValueDescriptor? {
        return componentStorage.resolve(request, context) ?: resolveIterable(request, context)
    }

    override fun resolve(request: Type): ValueDescriptor? {
        return resolve(request, unknownContext)
    }

    override fun <T> create(request: Class<T>): T {
        val constructorBinding = request.bindToConstructor(containerId, unknownContext)
        val args = constructorBinding.argumentDescriptors.map { it.getValue() }.toTypedArray()
        return runWithUnwrappingInvocationException {
            @Suppress("UNCHECKED_CAST")
            constructorBinding.constructor.newInstance(*args) as T
        }
    }
    override fun close() = componentStorage.dispose()


}
fun StorageComponentContainer.registerInstance(instance: Any): StorageComponentContainer {
    return registerDescriptors(listOf(InstanceComponentDescriptor(instance)))
}
