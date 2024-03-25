package com.huawei.cangjie.container

import java.io.Closeable
import java.lang.reflect.Type

enum class ComponentState {
    Null,
    Initializing,
    Initialized,
    Corrupted,
    Disposing,
    Disposed
}

fun computeArguments(argumentDescriptors: List<ValueDescriptor>): List<Any> = argumentDescriptors.map { it.getValue() }
internal class ClashResolutionDescriptor<E : PlatformSpecificExtension<E>>(
    container: ComponentContainer,
    private val resolver: PlatformExtensionsClashResolver<E>,
    private val clashedComponents: List<ComponentDescriptor>
) : SingletonDescriptor(container) {

    override fun createInstance(context: ValueResolveContext): Any {
        state = ComponentState.Initializing
        @Suppress("UNCHECKED_CAST")
        val extensions = computeArguments(clashedComponents) as List<E>
        val resolution = resolver.resolveExtensionsClash(extensions)
        state = ComponentState.Initialized
        return resolution
    }

    override fun getRegistrations(): Iterable<Type> {
        throw IllegalStateException("Shouldn't be called")
    }

    override fun getDependencies(context: ValueResolveContext): Collection<Type> {
        throw IllegalStateException("Shouldn't be called")
    }
}

open class SingletonTypeComponentDescriptor(container: ComponentContainer, val klass: Class<*>) :
    SingletonDescriptor(container) {
    override fun createInstance(context: ValueResolveContext): Any = createInstanceOf(klass, context)
    override fun getRegistrations(): Iterable<Type> = klass.getInfo().registrations

    private fun createInstanceOf(klass: Class<*>, context: ValueResolveContext): Any {
        val binding = klass.bindToConstructor(container.containerId, context)
        state = ComponentState.Initializing
        for (argumentDescriptor in binding.argumentDescriptors) {
            if (argumentDescriptor is Closeable && argumentDescriptor !is SingletonDescriptor) {
                registerDisposableObject(argumentDescriptor)
            }
        }

        val constructor = binding.constructor
        val arguments = computeArguments(binding.argumentDescriptors)

        val instance = runWithUnwrappingInvocationException { constructor.newInstance(*arguments.toTypedArray())!! }
        state = ComponentState.Initialized
        return instance
    }

    override fun getDependencies(context: ValueResolveContext): Collection<Type> {
        val classInfo = klass.getInfo()
        val constructorParameters = classInfo.constructorInfo?.parameters.orEmpty()
        val setterInfos = classInfo.setterInfos

        // In most cases, setterInfos is empty (KT-52756)
        return if (setterInfos.isEmpty())
            constructorParameters
        else
            constructorParameters + setterInfos.flatMap { it.parameters }
    }

    override fun toString(): String = "Singleton: ${klass.simpleName}"
}

abstract class SingletonDescriptor(val container: ComponentContainer) : ComponentDescriptor, Closeable {
    private var instance: Any? = null
    protected var state: ComponentState = ComponentState.Null
    private val disposableObjects by lazy { ArrayList<Closeable>() }

    override fun getValue(): Any {
        when {
            state == ComponentState.Corrupted -> throw ContainerConsistencyException("Component descriptor $this is corrupted and cannot be accessed")
            state == ComponentState.Disposed -> throw ContainerConsistencyException("Component descriptor $this is disposed and cannot be accessed")
            instance == null -> createInstance(container)
        }
        return instance!!
    }

    protected fun registerDisposableObject(ownedObject: Closeable) {
        disposableObjects.add(ownedObject)
    }

    protected abstract fun createInstance(context: ValueResolveContext): Any

    private fun createInstance(container: ComponentContainer) {
        when (state) {
            ComponentState.Null -> {
                try {
                    instance = createInstance(container.createResolveContext(this))
                    return
                } catch (ex: Throwable) {
                    state = ComponentState.Corrupted
                    for (disposable in disposableObjects)
                        disposable.close()
                    throw ex
                }
            }

            ComponentState.Initializing ->
                throw ContainerConsistencyException("Could not create the component $this because it is being initialized. Do we have undetected circular dependency?")

            ComponentState.Initialized ->
                throw ContainerConsistencyException("Could not get the component $this. Instance is null in Initialized state")

            ComponentState.Corrupted ->
                throw ContainerConsistencyException("Could not get the component $this because it is corrupted")

            ComponentState.Disposing ->
                throw ContainerConsistencyException("Could not get the component $this because it is being disposed")

            ComponentState.Disposed ->
                throw ContainerConsistencyException("Could not get the component $this because it is already disposed")
        }
    }

    private fun disposeImpl() {
        val wereInstance = instance
        state = ComponentState.Disposing
        instance = null // cannot get instance any more
        try {
            if (wereInstance is Closeable)
                wereInstance.close()
            for (disposable in disposableObjects)
                disposable.close()
        } catch (ex: Throwable) {
            state = ComponentState.Corrupted
            throw ex
        }
        state = ComponentState.Disposed
    }

    override fun close() {
        when (state) {
            ComponentState.Initialized ->
                disposeImpl()

            ComponentState.Corrupted -> {
            } // corrupted component is in the undefined state, ignore
            ComponentState.Null -> {
            } // it's ok to to remove null component, it may have been never needed

            ComponentState.Initializing ->
                throw ContainerConsistencyException("The component is being initialized and cannot be disposed.")

            ComponentState.Disposing ->
                throw ContainerConsistencyException("The component is already in disposing state.")

            ComponentState.Disposed ->
                throw ContainerConsistencyException("The component has already been destroyed.")
        }
    }

    override val shouldInjectProperties: Boolean
        get() = true
}


class DefaultSingletonTypeComponentDescriptor(container: ComponentContainer, klass: Class<*>) :
    SingletonTypeComponentDescriptor(container, klass) {
    override fun toString(): String {
        return "Default: ${klass.simpleName}"
    }
}