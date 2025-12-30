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
import java.lang.reflect.Type

/**
 * 组件状态枚举
 *
 * 定义单例组件在其生命周期中可能的状态。
 */
enum class ComponentState {
    /** 空状态 - 组件尚未初始化 */
    Null,
    /** 初始化中 - 组件正在被创建 */
    Initializing,
    /** 已初始化 - 组件创建完成,可正常使用 */
    Initialized,
    /** 已损坏 - 组件初始化失败,处于不可用状态 */
    Corrupted,
    /** 销毁中 - 组件正在被销毁 */
    Disposing,
    /** 已销毁 - 组件已被销毁,不可再使用 */
    Disposed
}

/**
 * 计算参数值
 *
 * 从值描述符列表中提取实际值。
 *
 * @param argumentDescriptors 参数描述符列表
 * @return 实际参数值列表
 */
fun computeArguments(argumentDescriptors: List<ValueDescriptor>): List<Any> = argumentDescriptors.map { it.getValue() }

/**
 * 冲突解决描述符
 *
 * 当容器中存在多个平台特定扩展的冲突实现时,使用此描述符来解决冲突。
 * 通过调用 [PlatformExtensionsClashResolver] 来选择最终使用的实现。
 *
 * @param E 平台特定扩展的类型
 * @param container 所属的组件容器
 * @param resolver 冲突解决器
 * @param clashedComponents 发生冲突的组件列表
 */
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
        throw IllegalStateException("不应被调用")
    }

    override fun getDependencies(context: ValueResolveContext): Collection<Type> {
        throw IllegalStateException("不应被调用")
    }
}

/**
 * 单例类型组件描述符
 *
 * 基于类类型创建单例实例的描述符。通过反射机制调用类的构造函数创建实例,
 * 并管理其依赖关系和生命周期。
 *
 * @property klass 要实例化的类
 * @param container 所属的组件容器
 */
open class SingletonTypeComponentDescriptor(container: ComponentContainer, val klass: Class<*>) :
    SingletonDescriptor(container) {
    override fun createInstance(context: ValueResolveContext): Any = createInstanceOf(klass, context)
    override fun getRegistrations(): Iterable<Type> = klass.getInfo().registrations

    /**
     * 创建指定类的实例
     *
     * @param klass 要实例化的类
     * @param context 值解析上下文
     * @return 创建的实例对象
     */
    private fun createInstanceOf(klass: Class<*>, context: ValueResolveContext): Any {
        val binding = klass.bindToConstructor(container.containerId, context)
        state = ComponentState.Initializing
        // 注册可销毁的依赖对象(排除单例描述符本身)
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

        // 合并构造函数参数和 setter 注入参数
        return if (setterInfos.isEmpty())
            constructorParameters
        else
            constructorParameters + setterInfos.flatMap { it.parameters }
    }

    override fun toString(): String = "Singleton: ${klass.simpleName}"
}

/**
 * 单例描述符抽象基类
 *
 * 管理单例组件的生命周期,包括延迟初始化、状态管理和资源销毁。
 * 确保组件在整个容器生命周期内只创建一次。
 *
 * @property container 所属的组件容器
 */
abstract class SingletonDescriptor(val container: ComponentContainer) : ComponentDescriptor, Closeable {
    private var instance: Any? = null
    protected var state: ComponentState = ComponentState.Null
    private val disposableObjects by lazy { ArrayList<Closeable>() }

    override fun getValue(): Any {
        when {
            state == ComponentState.Corrupted -> throw ContainerConsistencyException("组件描述符 $this 已损坏,无法访问")
            state == ComponentState.Disposed -> throw ContainerConsistencyException("组件描述符 $this 已销毁,无法访问")
            instance == null -> createInstance(container)
        }
        return instance!!
    }

    /**
     * 注册需要随组件一起销毁的对象
     *
     * @param ownedObject 需要销毁的对象
     */
    protected fun registerDisposableObject(ownedObject: Closeable) {
        disposableObjects.add(ownedObject)
    }

    /**
     * 创建组件实例
     *
     * 子类需实现此方法以定义具体的实例创建逻辑。
     *
     * @param context 值解析上下文
     * @return 创建的实例对象
     */
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
                throw ContainerConsistencyException("无法创建组件 $this,因为它正在初始化中。是否存在未检测到的循环依赖?")

            ComponentState.Initialized ->
                throw ContainerConsistencyException("无法获取组件 $this。实例在已初始化状态下为 null")

            ComponentState.Corrupted ->
                throw ContainerConsistencyException("无法获取组件 $this,因为它已损坏")

            ComponentState.Disposing ->
                throw ContainerConsistencyException("无法获取组件 $this,因为它正在销毁中")

            ComponentState.Disposed ->
                throw ContainerConsistencyException("无法获取组件 $this,因为它已被销毁")
        }
    }

    private fun disposeImpl() {
        val wereInstance = instance
        state = ComponentState.Disposing
        instance = null // 之后无法再获取实例
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
            } // 已损坏的组件处于未定义状态,忽略
            ComponentState.Null -> {
            } // 可以移除空组件,它可能从未被使用过

            ComponentState.Initializing ->
                throw ContainerConsistencyException("组件正在初始化中,无法销毁。")

            ComponentState.Disposing ->
                throw ContainerConsistencyException("组件已处于销毁中状态。")

            ComponentState.Disposed ->
                throw ContainerConsistencyException("组件已被销毁。")
        }
    }

    override val shouldInjectProperties: Boolean
        get() = true
}


/**
 * 默认单例类型组件描述符
 *
 * 与 [SingletonTypeComponentDescriptor] 功能相同,但在字符串表示中明确标识为"默认"组件。
 * 用于区分默认提供的组件和自定义注册的组件。
 *
 * @param container 所属的组件容器
 * @param klass 要实例化的类
 */
class DefaultSingletonTypeComponentDescriptor(container: ComponentContainer, klass: Class<*>) :
    SingletonTypeComponentDescriptor(container, klass) {
    override fun toString(): String {
        return "Default: ${klass.simpleName}"
    }
}
