/*
 * Copyright 2025 LinQingYing. and contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * The use of this source code is governed by the Apache License 2.0,
 * which allows users to freely use, modify, and distribute the code,
 * provided they adhere to the terms of the license.
 *
 * The software is provided "as-is", and the authors are not responsible for
 * any damages or issues arising from its use.
 *
 */

package org.cangnova.cangjie.container

import java.io.Closeable
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type
import java.lang.reflect.WildcardType

// ==================== 核心容器接口 ====================

/**
 * 组件容器接口
 */
interface ComponentContainer {
    val containerId: String

    fun createResolveContext(requestingDescriptor: ValueDescriptor): ValueResolveContext
}

/**
 * 组件提供者接口
 */
interface ComponentProvider {
    fun resolve(request: Type): ValueDescriptor?
    fun <T> create(request: Class<T>): T
}

// ==================== 存储组件容器 ====================

/**
 * 存储组件容器 - 核心 DI 容器实现
 */
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

    internal fun registerClashResolvers(resolvers: List<PlatformExtensionsClashResolver<*>>): StorageComponentContainer {
        componentStorage.registerClashResolvers(resolvers)
        return this
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
