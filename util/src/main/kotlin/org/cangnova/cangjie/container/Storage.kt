/*
 * Copyright 2026 LinQingYing. and contributors.
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

import com.intellij.util.containers.MultiMap
import java.io.Closeable
import java.lang.reflect.Modifier
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type

// ==================== 组件存储状态 ====================

/**
 * 组件存储状态枚举
 *
 * 定义了组件存储的生命周期状态，确保容器在正确的状态下执行操作
 */
enum class ComponentStorageState {
    /** 初始状态 - 容器已创建但未初始化 */
    Initial,

    /** 已初始化状态 - 容器已组合完成，可以正常使用 */
    Initialized,

    /** 正在销毁状态 - 容器正在执行销毁操作 */
    Disposing,

    /** 已销毁状态 - 容器已完全销毁，不可再使用 */
    Disposed
}

// ==================== 组件存储核心类 ====================

/**
 * 组件存储 - 依赖注入容器的核心存储实现
 *
 * 负责管理组件描述符的生命周期，包括注册、解析、依赖注入和销毁。
 * 支持父子容器继承、隐式依赖解析、冲突解决等高级特性。
 *
 * @property myId 容器标识符
 * @property parent 父容器存储（可选），用于构建容器层次结构
 *
 * @constructor 创建组件存储实例
 */
class ComponentStorage(private val myId: String, parent: ComponentStorage?) : ValueResolver {
    /** 当前存储状态 */
    var state = ComponentStorageState.Initial

    /** 已注册的组件描述符集合 */
    private val descriptors = LinkedHashSet<ComponentDescriptor>()

    /** 组件依赖关系映射表，记录每个组件依赖的类型 */
    private val dependencies = MultiMap.createLinkedSet<ComponentDescriptor, Type>()

    /** 平台扩展冲突解析器列表 */
    private val clashResolvers = ArrayList<PlatformExtensionsClashResolver<*>>()

    /** 组件注册表，负责类型到描述符的映射 */
    private val registry = ComponentRegistry()

    init {
        // 继承父容器的注册表和冲突解析器
        parent?.let {
            registry.addAll(it.registry)
            clashResolvers.addAll(it.clashResolvers)
        }
    }

    /**
     * 注册冲突解析器
     *
     * 用于处理同一类型有多个实现时的冲突情况
     *
     * @param resolvers 冲突解析器列表
     */
    internal fun registerClashResolvers(resolvers: List<PlatformExtensionsClashResolver<*>>) {
        clashResolvers.addAll(resolvers)
    }


    /**
     * 销毁单个组件描述符
     *
     * 如果描述符实现了 Closeable 接口，则调用其 close 方法进行资源清理
     *
     * @param descriptor 要销毁的组件描述符
     */
    private fun disposeDescriptor(descriptor: ComponentDescriptor) {
        if (descriptor is Closeable)
            descriptor.close()
    }

    /**
     * 销毁整个组件存储
     *
     * 按照依赖关系的逆序销毁所有组件，确保被依赖的组件最后销毁。
     * 只能在 Initialized 或 Initial 状态下调用。
     *
     * @throws ContainerConsistencyException 如果在非法状态下调用
     */
    fun dispose() {
        if (state != ComponentStorageState.Initialized) {
            if (state == ComponentStorageState.Initial)
                return // 允许销毁未初始化的容器
            throw ContainerConsistencyException("Component container cannot be disposed in the $state state.")
        }

        state = ComponentStorageState.Disposing
        val disposeList = getDescriptorsInDisposeOrder()
        for (descriptor in disposeList)
            disposeDescriptor(descriptor)
        state = ComponentStorageState.Disposed
    }

    /**
     * 获取按销毁顺序排列的描述符列表
     *
     * 使用拓扑排序算法，根据依赖关系确定销毁顺序。
     * 被依赖的组件会排在后面，确保依赖者先销毁。
     *
     * @return 按销毁顺序排列的组件描述符列表
     */
    private fun getDescriptorsInDisposeOrder(): List<ComponentDescriptor> {
        return topologicalSort(descriptors) {
            val dependent = ArrayList<ComponentDescriptor>()
            for (interfaceType in dependencies[it]) {
                for (dependency in registry.tryGetEntry(interfaceType)) {
                    dependent.add(dependency)
                }
            }
            dependent
        }
    }

    /**
     * 解析多个符合条件的值描述符
     *
     * 返回所有注册为指定类型的组件描述符
     *
     * @param request 请求的类型
     * @param context 值解析上下文
     * @return 符合条件的值描述符集合
     */
    fun resolveMultiple(request: Type, context: ValueResolveContext): Iterable<ValueDescriptor> {
        registerDependency(request, context)
        return registry.tryGetEntry(request)
    }

    /**
     * 获取隐式定义的依赖
     *
     * 当容器中没有显式注册某个类型时，尝试自动创建其实例：
     * 1. 如果是具体类（非抽象、非基本类型），创建隐式单例
     * 2. 如果有 @DefaultImplementation 注解，使用默认实现
     * 3. 如果默认实现是 Kotlin object，使用其 INSTANCE 字段
     *
     * @param context 组件解析上下文
     * @param rawType 原始类型
     * @return 隐式组件描述符，如果无法创建则返回 null
     */
    private fun getImplicitlyDefinedDependency(
        context: ComponentResolveContext,
        rawType: Class<*>
    ): ComponentDescriptor? {
        // 1. 尝试创建具体类的隐式单例
        if (!Modifier.isAbstract(rawType.modifiers) && !rawType.isPrimitive) {
            return ImplicitSingletonTypeComponentDescriptor(context.container, rawType)
        }

        // 2. 尝试使用默认实现（如果有构造函数）
        val defaultImplementation = rawType.getInfo().defaultImplementation
        if (defaultImplementation != null && defaultImplementation.getInfo().constructorInfo != null) {
            return DefaultSingletonTypeComponentDescriptor(context.container, defaultImplementation)
        }

        // 3. 尝试使用 Kotlin object 的 INSTANCE 字段
        if (defaultImplementation != null) {
            return defaultImplementation.getField("INSTANCE").get(null)?.let(::DefaultInstanceComponentDescriptor)
        }

        return null
    }

    /**
     * 收集临时组件（即隐式依赖）
     *
     * 递归地检查组件的所有依赖，对于未注册的依赖，尝试创建隐式组件。
     * 防止重复访问已处理的类型。
     *
     * @param context 组件解析上下文
     * @param descriptor 要检查的组件描述符
     * @param visitedTypes 已访问的类型集合，用于防止循环
     * @param adhocDescriptors 收集到的临时组件描述符集合
     */
    private fun collectAdhocComponents(
        context: ComponentResolveContext, descriptor: ComponentDescriptor,
        visitedTypes: HashSet<Type>, adhocDescriptors: LinkedHashSet<ComponentDescriptor>
    ) {
        val dependencies = descriptor.getDependencies(context)
        for (type in dependencies) {
            if (!visitedTypes.add(type))
                continue

            val entry = registry.tryGetEntry(type)
            if (entry.isEmpty()) {
                // 提取原始类型
                val rawType: Class<*>? = when (type) {
                    is Class<*> -> type
                    is ParameterizedType -> type.rawType as? Class<*>
                    else -> null
                }

                // 尝试创建隐式依赖
                val implicitDependency = rawType?.let { getImplicitlyDefinedDependency(context, it) } ?: continue

                adhocDescriptors.add(implicitDependency)
                // 递归收集隐式依赖的依赖
                collectAdhocComponents(context, implicitDependency, visitedTypes, adhocDescriptors)
            }
        }
    }

    /**
     * 注册组件描述符
     *
     * 将组件描述符添加到容器中。如果容器已初始化，立即组合新的描述符。
     *
     * @param context 组件解析上下文
     * @param items 要注册的组件描述符列表
     * @throws ContainerConsistencyException 如果容器已销毁
     */
    internal fun registerDescriptors(context: ComponentResolveContext, items: List<ComponentDescriptor>) {
        if (state == ComponentStorageState.Disposed) {
            throw ContainerConsistencyException("Cannot register descriptors in $state state")
        }

        for (descriptor in items)
            descriptors.add(descriptor)

        if (state == ComponentStorageState.Initialized)
            composeDescriptors(context, items)
    }

    /**
     * 检查依赖并注册临时组件
     *
     * 检查所有组件的依赖关系，对于缺失的依赖自动创建临时组件
     *
     * @param context 组件解析上下文
     * @param descriptors 要检查的组件描述符集合
     * @return 创建的临时组件描述符集合
     */
    private fun inspectDependenciesAndRegisterAdhoc(
        context: ComponentResolveContext,
        descriptors: Collection<ComponentDescriptor>
    ): LinkedHashSet<ComponentDescriptor> {
        val adhoc = LinkedHashSet<ComponentDescriptor>()
        val visitedTypes = HashSet<Type>()
        for (descriptor in descriptors) {
            collectAdhocComponents(context, descriptor, visitedTypes, adhoc)
        }
        registry.addAll(adhoc)
        return adhoc
    }

    /**
     * 为组件集合注入属性
     *
     * 对所有需要属性注入的组件执行注入操作
     *
     * @param context 组件解析上下文
     * @param components 要注入的组件描述符集合
     */
    private fun injectProperties(context: ComponentResolveContext, components: Collection<ComponentDescriptor>) {
        for (component in components) {
            if (component.shouldInjectProperties) {
                injectProperties(component.getValue(), context.container.createResolveContext(component))
            }
        }
    }

    /**
     * 为单个实例注入属性
     *
     * 查找实例类的所有带 @Inject 注解的 setter 方法，并调用它们进行依赖注入
     *
     * @param instance 要注入的实例对象
     * @param context 值解析上下文
     */
    private fun injectProperties(instance: Any, context: ValueResolveContext) {
        val classInfo = instance::class.java.getInfo()

        classInfo.setterInfos.forEach { (method) ->
            val methodBinding = method.bindToMethod(containerId, context)
            methodBinding.invoke(instance)
        }
    }

    /**
     * 组合组件描述符
     *
     * 完成组件的注册、依赖收集、冲突解决和属性注入的完整流程
     *
     * @param context 组件解析上下文
     * @param descriptors 要组合的组件描述符集合
     */
    private fun composeDescriptors(context: ComponentResolveContext, descriptors: Collection<ComponentDescriptor>) {
        if (descriptors.isEmpty()) return

        // 1. 注册组件到注册表
        registry.addAll(descriptors)

        // 2. 检查并注册隐式依赖
        val implicits = inspectDependenciesAndRegisterAdhoc(context, descriptors)

        // 3. 解决类型冲突
        registry.resolveClashesIfAny(context.container, clashResolvers)

        // 4. 执行属性注入
        injectProperties(context, descriptors + implicits)
    }

    /**
     * 组合容器
     *
     * 初始化容器，完成所有组件的注册和组合。
     * 只能在 Initial 状态下调用一次。
     *
     * @param context 组件解析上下文
     * @throws ContainerConsistencyException 如果容器已经组合过
     */
    fun compose(context: ComponentResolveContext) {
        if (state != ComponentStorageState.Initial)
            throw ContainerConsistencyException("$containerId $myId was already composed.")

        state = ComponentStorageState.Initialized
        composeDescriptors(context, descriptors)
    }

    /**
     * 容器标识符
     */
    val containerId
        get() = "Container: $myId"

    /**
     * 注册依赖关系
     *
     * 记录组件对特定类型的依赖，用于后续的依赖分析和销毁顺序确定
     *
     * @param request 请求的类型
     * @param context 值解析上下文
     */
    private fun registerDependency(request: Type, context: ValueResolveContext) {
        if (context is ComponentResolveContext) {
            val descriptor = context.requestingDescriptor
            if (descriptor is ComponentDescriptor) {
                dependencies.putValue(descriptor, request)
            }
        }
    }

    /**
     * 解析值描述符
     *
     * 根据请求的类型从注册表中查找对应的值描述符。
     * 支持默认实现的自动区分：
     * - 如果有非默认实现，优先选择非默认实现
     * - 如果只有默认实现，选择第一个默认实现
     * - 如果有多个非默认实现，抛出冲突异常
     *
     * @param request 请求的类型
     * @param context 值解析上下文
     * @return 值描述符，如果未找到则返回 null
     * @throws ContainerConsistencyException 如果容器未组合
     * @throws InvalidCardinalityException 如果有多个非默认实现冲突
     */
    override fun resolve(request: Type, context: ValueResolveContext): ValueDescriptor? {
        // 辅助函数：判断是否为默认组件
        fun ComponentDescriptor.isDefaultComponent(): Boolean =
            this is DefaultInstanceComponentDescriptor || this is DefaultSingletonTypeComponentDescriptor

        if (state == ComponentStorageState.Initial)
            throw ContainerConsistencyException("Container was not composed before resolving")

        val entry = registry.tryGetEntry(request)
        if (entry.isNotEmpty()) {
            registerDependency(request, context)

            // 单个注册，直接返回
            if (entry.size == 1) return entry.single()

            // 多个注册，区分默认和非默认实现
            val nonDefault = entry.filterNot { it.isDefaultComponent() }
            if (nonDefault.isEmpty()) return entry.first()

            // 只有一个非默认实现，返回
            return nonDefault.singleOrNull()
                ?: throw InvalidCardinalityException(
                    "$containerId: Request $request cannot be satisfied because there is more than one type registered\n" +
                            "Clashed registrations: ${entry.joinToString()}"
                )
        }
        return null
    }
}

// ==================== 隐式单例描述符 ====================

/**
 * 隐式单例类型组件描述符
 *
 * 用于自动创建未显式注册的具体类的单例实例
 *
 * @property container 所属容器
 * @property klass 要实例化的类
 */
class ImplicitSingletonTypeComponentDescriptor(container: ComponentContainer, klass: Class<*>) :
    SingletonTypeComponentDescriptor(container, klass) {
    override fun toString(): String {
        return "Implicit: ${klass.simpleName}"
    }
}
