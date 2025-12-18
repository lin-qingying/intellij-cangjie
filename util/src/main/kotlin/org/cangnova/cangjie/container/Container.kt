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
 *
 * 定义依赖注入容器的基本契约。容器负责管理组件的生命周期和依赖关系解析。
 *
 * @property containerId 容器的唯一标识符
 */
interface ComponentContainer {
    val containerId: String

    /**
     * 创建值解析上下文
     *
     * 为指定的值描述符创建解析上下文,用于追踪依赖解析过程。
     *
     * @param requestingDescriptor 请求解析的值描述符
     * @return 值解析上下文
     */
    fun createResolveContext(requestingDescriptor: ValueDescriptor): ValueResolveContext
}

/**
 * 组件提供者接口
 *
 * 定义组件查询和创建的契约。
 */
interface ComponentProvider {
    /**
     * 解析类型对应的值描述符
     *
     * @param request 请求的类型
     * @return 对应的值描述符,如果没有找到则返回 null
     */
    fun resolve(request: Type): ValueDescriptor?

    /**
     * 创建指定类型的实例
     *
     * @param T 实例类型
     * @param request 要实例化的类
     * @return 创建的实例
     */
    fun <T> create(request: Class<T>): T
}

// ==================== 存储组件容器 ====================

/**
 * 存储组件容器 - 核心依赖注入容器实现
 *
 * 提供完整的依赖注入功能,包括组件注册、解析、生命周期管理和父子容器支持。
 *
 * @property id 容器标识符
 * @param parent 父容器,如果有的话
 */
class StorageComponentContainer(
    private val id: String,
    parent: StorageComponentContainer? = null
) : ComponentContainer, ComponentProvider, Closeable {

    /**
     * 未知组件的解析上下文
     *
     * 用于缓存动态组件描述符的解析上下文,避免重复创建。
     */
    val unknownContext: ComponentResolveContext by lazy {
        val parentContext = parent?.let { ComponentResolveContext(it, DynamicComponentDescriptor) }
        ComponentResolveContext(this, DynamicComponentDescriptor, parentContext)
    }
    private val componentStorage: ComponentStorage = ComponentStorage(id, parent?.componentStorage)

    override val containerId
        get() = "Container: $id"

    override fun createResolveContext(requestingDescriptor: ValueDescriptor): ValueResolveContext {
        if (requestingDescriptor == DynamicComponentDescriptor) // 缓存未知组件描述符
            return unknownContext
        return ComponentResolveContext(this, requestingDescriptor)
    }

    /**
     * 注册冲突解决器
     *
     * @param resolvers 冲突解决器列表
     * @return 容器自身,支持链式调用
     */
    internal fun registerClashResolvers(resolvers: List<PlatformExtensionsClashResolver<*>>): StorageComponentContainer {
        componentStorage.registerClashResolvers(resolvers)
        return this
    }

    /**
     * 注册组件描述符
     *
     * @param descriptors 组件描述符列表
     * @return 容器自身,支持链式调用
     */
    internal fun registerDescriptors(descriptors: List<ComponentDescriptor>): StorageComponentContainer {
        componentStorage.registerDescriptors(unknownContext, descriptors)
        return this
    }

    /**
     * 组合容器
     *
     * 完成容器的初始化过程,准备好提供服务。
     *
     * @return 容器自身,支持链式调用
     */
    fun compose(): StorageComponentContainer {
        componentStorage.compose(unknownContext)
        return this
    }

    /**
     * 解析可迭代类型
     *
     * 特殊处理 Iterable<T> 类型的依赖,返回所有匹配类型 T 的组件集合。
     *
     * @param request 请求的类型
     * @param context 值解析上下文
     * @return 可迭代描述符,如果请求类型不是 Iterable 则返回 null
     */
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

    /**
     * 解析多个匹配的组件
     *
     * @param request 请求的类型
     * @param context 值解析上下文,默认为 unknownContext
     * @return 所有匹配的值描述符
     */
    fun resolveMultiple(request: Class<*>, context: ValueResolveContext = unknownContext): Iterable<ValueDescriptor> {
        return componentStorage.resolveMultiple(request, context)
    }

    /**
     * 解析指定类型的组件
     *
     * @param request 请求的类型
     * @param context 值解析上下文
     * @return 对应的值描述符,如果没有找到则返回 null
     */
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
