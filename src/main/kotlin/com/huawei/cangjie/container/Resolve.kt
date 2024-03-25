package com.huawei.cangjie.container

import java.lang.reflect.Constructor
import java.lang.reflect.Member
import java.lang.reflect.Method
import java.lang.reflect.Type

interface ValueResolveContext {
    fun resolve(registration: Type): ValueDescriptor?
}

interface ValueResolver {
    fun resolve(request: Type, context: ValueResolveContext): ValueDescriptor?
}

class ComponentResolveContext(
    val container: StorageComponentContainer,
    val requestingDescriptor: ValueDescriptor,
    val parentContext: ValueResolveContext? = null
) : ValueResolveContext {
    override fun resolve(registration: Type): ValueDescriptor? =
        container.resolve(registration, this) ?: parentContext?.resolve(registration)

    override fun toString(): String = "for $requestingDescriptor in $container"
}

fun Method.bindToMethod(containerId: String, context: ValueResolveContext): MethodBinding {
    return MethodBinding(this, bindArguments(containerId, genericParameterTypes.toList(), context))
}

class MethodBinding(val method: Method, private val argumentDescriptors: List<ValueDescriptor>) {
    fun invoke(instance: Any) {
        val arguments = computeArguments(argumentDescriptors).toTypedArray()
        runWithUnwrappingInvocationException { method.invoke(instance, *arguments) }
    }
}

private fun Member.bindArguments(
    containerId: String,
    parameters: List<Type>,
    context: ValueResolveContext
): List<ValueDescriptor> {
    val bound = ArrayList<ValueDescriptor>(parameters.size)
    var unsatisfied: MutableList<Type>? = null

    for (parameter in parameters) {
        val descriptor = context.resolve(parameter)
        if (descriptor == null) {
            if (unsatisfied == null)
                unsatisfied = java.util.ArrayList<Type>()
            unsatisfied.add(parameter)
        } else {
            bound.add(descriptor)
        }
    }
    if (unsatisfied != null) {
        throw UnresolvedDependenciesException("$containerId: Dependencies for `$this` cannot be satisfied:\n  $unsatisfied")
    }
    return bound
}

class UnresolvedDependenciesException(message: String) : Exception(message)

class ConstructorBinding(val constructor: Constructor<*>, val argumentDescriptors: List<ValueDescriptor>)

fun Class<*>.bindToConstructor(containerId: String, context: ValueResolveContext): ConstructorBinding {
    val constructorInfo = getInfo().constructorInfo ?: error("No constructor for $this: ${getInfo()} in $containerId")
    val candidate = constructorInfo.constructor
    return ConstructorBinding(candidate, candidate.bindArguments(containerId, constructorInfo.parameters, context))
}