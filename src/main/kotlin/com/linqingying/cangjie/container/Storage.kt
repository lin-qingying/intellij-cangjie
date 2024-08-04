package com.linqingying.cangjie.container

import com.intellij.util.containers.MultiMap
import java.io.Closeable
import java.lang.reflect.Modifier
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type

enum class ComponentStorageState {
    Initial,
    Initialized,
    Disposing,
    Disposed
}

class ComponentStorage(private val myId: String, parent: ComponentStorage?) : ValueResolver {
    var state = ComponentStorageState.Initial

    private val descriptors = LinkedHashSet<ComponentDescriptor>()
    private val dependencies = MultiMap.createLinkedSet<ComponentDescriptor, Type>()
    private val clashResolvers = ArrayList<PlatformExtensionsClashResolver<*>>()
    private val registry = ComponentRegistry()

    init {
        parent?.let {
            registry.addAll(it.registry)
            clashResolvers.addAll(it.clashResolvers)
        }
    }


    private fun disposeDescriptor(descriptor: ComponentDescriptor) {
        if (descriptor is Closeable)
            descriptor.close()
    }

    fun dispose() {
        if (state != ComponentStorageState.Initialized) {
            if (state == ComponentStorageState.Initial)
                return // it is valid to dispose container which was not initialized
            throw ContainerConsistencyException("Component container cannot be disposed in the $state state.")
        }

        state = ComponentStorageState.Disposing
        val disposeList = getDescriptorsInDisposeOrder()
        for (descriptor in disposeList)
            disposeDescriptor(descriptor)
        state = ComponentStorageState.Disposed
    }

    private fun getDescriptorsInDisposeOrder(): List<ComponentDescriptor> {
        return topologicalSort(descriptors) {
            val dependent = java.util.ArrayList<ComponentDescriptor>()
            for (interfaceType in dependencies[it]) {
                for (dependency in registry.tryGetEntry(interfaceType)) {
                    dependent.add(dependency)
                }
            }
            dependent
        }
    }

    fun resolveMultiple(request: Type, context: ValueResolveContext): Iterable<ValueDescriptor> {
        registerDependency(request, context)
        return registry.tryGetEntry(request)
    }

    private fun getImplicitlyDefinedDependency(
        context: ComponentResolveContext,
        rawType: Class<*>
    ): ComponentDescriptor? {
        if (!Modifier.isAbstract(rawType.modifiers) && !rawType.isPrimitive) {
            return ImplicitSingletonTypeComponentDescriptor(context.container, rawType)
        }

        val defaultImplementation = rawType.getInfo().defaultImplementation
        if (defaultImplementation != null && defaultImplementation.getInfo().constructorInfo != null) {
            return DefaultSingletonTypeComponentDescriptor(context.container, defaultImplementation)
        }

        if (defaultImplementation != null) {
            return defaultImplementation.getField("INSTANCE").get(null)?.let(::DefaultInstanceComponentDescriptor)
        }

        return null
    }

    private fun collectAdhocComponents(
        context: ComponentResolveContext, descriptor: ComponentDescriptor,
        visitedTypes: HashSet<Type>, adhocDescriptors: java.util.LinkedHashSet<ComponentDescriptor>
    ) {
        val dependencies = descriptor.getDependencies(context)
        for (type in dependencies) {
            if (!visitedTypes.add(type))
                continue

            val entry = registry.tryGetEntry(type)
            if (entry.isEmpty()) {
                val rawType: Class<*>? = when (type) {
                    is Class<*> -> type
                    is ParameterizedType -> type.rawType as? Class<*>
                    else -> null
                }

                val implicitDependency = rawType?.let { getImplicitlyDefinedDependency(context, it) } ?: continue

                adhocDescriptors.add(implicitDependency)
                collectAdhocComponents(context, implicitDependency, visitedTypes, adhocDescriptors)
            }
        }
    }

    internal fun registerDescriptors(context: ComponentResolveContext, items: List<ComponentDescriptor>) {
        if (state == ComponentStorageState.Disposed) {
            throw ContainerConsistencyException("Cannot register descriptors in $state state")
        }

        for (descriptor in items)
            descriptors.add(descriptor)

        if (state == ComponentStorageState.Initialized)
            composeDescriptors(context, items)

    }

    private fun inspectDependenciesAndRegisterAdhoc(
        context: ComponentResolveContext,
        descriptors: Collection<ComponentDescriptor>
    ): java.util.LinkedHashSet<ComponentDescriptor> {
        val adhoc = java.util.LinkedHashSet<ComponentDescriptor>()
        val visitedTypes = HashSet<Type>()
        for (descriptor in descriptors) {
            collectAdhocComponents(context, descriptor, visitedTypes, adhoc)
        }
        registry.addAll(adhoc)
        return adhoc
    }

    private fun injectProperties(context: ComponentResolveContext, components: Collection<ComponentDescriptor>) {
        for (component in components) {
            if (component.shouldInjectProperties) {
                injectProperties(component.getValue(), context.container.createResolveContext(component))
            }
        }
    }

    private fun injectProperties(instance: Any, context: ValueResolveContext) {
        val classInfo = instance::class.java.getInfo()

        classInfo.setterInfos.forEach { (method) ->
            val methodBinding = method.bindToMethod(containerId, context)
            methodBinding.invoke(instance)
        }
    }

    private fun composeDescriptors(context: ComponentResolveContext, descriptors: Collection<ComponentDescriptor>) {
        if (descriptors.isEmpty()) return

        registry.addAll(descriptors)

        val implicits = inspectDependenciesAndRegisterAdhoc(context, descriptors)

        registry.resolveClashesIfAny(context.container, clashResolvers)
        injectProperties(context, descriptors + implicits)
    }

    fun compose(context: ComponentResolveContext) {
        if (state != ComponentStorageState.Initial)
            throw ContainerConsistencyException("$containerId $myId was already composed.")

        state = ComponentStorageState.Initialized
        composeDescriptors(context, descriptors)
    }

    val containerId
        get() = "Container: $myId"

    private fun registerDependency(request: Type, context: ValueResolveContext) {
        if (context is ComponentResolveContext) {
            val descriptor = context.requestingDescriptor
            if (descriptor is ComponentDescriptor) {
                dependencies.putValue(descriptor, request)
            }
        }
    }

    override fun resolve(request: Type, context: ValueResolveContext): ValueDescriptor? {
        fun ComponentDescriptor.isDefaultComponent(): Boolean =
            this is DefaultInstanceComponentDescriptor || this is DefaultSingletonTypeComponentDescriptor

        if (state == ComponentStorageState.Initial)
            throw ContainerConsistencyException("Container was not composed before resolving")

        val entry = registry.tryGetEntry(request)
        if (entry.isNotEmpty()) {
            registerDependency(request, context)

            if (entry.size == 1) return entry.single()

            val nonDefault = entry.filterNot { it.isDefaultComponent() }
            if (nonDefault.isEmpty()) return entry.first()

            return nonDefault.singleOrNull()
                ?: throw InvalidCardinalityException(
                    "$containerId: Request $request cannot be satisfied because there is more than one type registered\n" +
                            "Clashed registrations: ${entry.joinToString()}"
                )
        }
        return null
    }

}

internal class InvalidCardinalityException(message: String) : Exception(message)
class ImplicitSingletonTypeComponentDescriptor(container: ComponentContainer, klass: Class<*>) :
    SingletonTypeComponentDescriptor(container, klass) {
    override fun toString(): String {
        return "Implicit: ${klass.simpleName}"
    }
}
