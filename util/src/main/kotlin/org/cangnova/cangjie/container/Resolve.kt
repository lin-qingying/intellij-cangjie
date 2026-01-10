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

import java.lang.reflect.Constructor
import java.lang.reflect.Member
import java.lang.reflect.Method
import java.lang.reflect.Type

// ==================== 值解析接口 ====================

/**
 * 值解析上下文接口
 *
 * 提供依赖解析的上下文环境，用于从容器中查找和获取依赖项。
 * 这是依赖注入系统中最基础的接口之一。
 */
interface ValueResolveContext {
    /**
     * 解析指定类型的值描述符
     *
     * @param registration 要解析的类型
     * @return 值描述符，如果无法解析则返回 null
     */
    fun resolve(registration: Type): ValueDescriptor?
}

/**
 * 值解析器接口
 *
 * 定义了值解析的标准协议，实现类负责在给定的上下文中解析特定类型的依赖。
 */
interface ValueResolver {
    /**
     * 在给定上下文中解析请求的类型
     *
     * @param request 请求的类型
     * @param context 值解析上下文
     * @return 值描述符，如果无法解析则返回 null
     */
    fun resolve(request: Type, context: ValueResolveContext): ValueDescriptor?
}

// ==================== 组件解析上下文 ====================

/**
 * 组件解析上下文 - 值解析上下文的具体实现
 *
 * 提供了完整的依赖解析环境，包括：
 * - 所属容器的引用
 * - 当前请求的描述符（用于依赖追踪）
 * - 父上下文（用于跨容器依赖解析）
 *
 * 解析策略：
 * 1. 首先尝试从当前容器解析
 * 2. 如果失败且存在父上下文，则从父上下文解析
 * 3. 这实现了容器的层次结构支持
 *
 * @property container 所属的存储组件容器
 * @property requestingDescriptor 发起请求的值描述符（用于依赖关系追踪）
 * @property parentContext 父级解析上下文（可选），用于跨容器依赖解析
 *
 * @constructor 创建组件解析上下文实例
 */
class ComponentResolveContext(
    val container: StorageComponentContainer,
    val requestingDescriptor: ValueDescriptor,
    val parentContext: ValueResolveContext? = null
) : ValueResolveContext {
    /**
     * 解析类型
     *
     * 实现了两级解析策略：
     * 1. 优先从当前容器解析
     * 2. 如果当前容器无法解析且存在父上下文，则委托给父上下文
     *
     * @param registration 要解析的类型
     * @return 值描述符，如果两级都无法解析则返回 null
     */
    override fun resolve(registration: Type): ValueDescriptor? =
        container.resolve(registration, this) ?: parentContext?.resolve(registration)

    override fun toString(): String = "for $requestingDescriptor in $container"
}

// ==================== 方法绑定 ====================

/**
 * 将方法绑定到值解析上下文
 *
 * 这是属性注入的核心函数，用于：
 * 1. 解析方法的所有参数类型
 * 2. 从容器中获取对应的值描述符
 * 3. 创建可调用的方法绑定对象
 *
 * @receiver 要绑定的方法（通常是带 @Inject 注解的 setter 方法）
 * @param containerId 容器标识符（用于错误消息）
 * @param context 值解析上下文
 * @return 方法绑定对象，包含方法和已解析的参数描述符
 * @throws UnresolvedDependenciesException 如果有参数无法解析
 */
fun Method.bindToMethod(containerId: String, context: ValueResolveContext): MethodBinding {
    return MethodBinding(this, bindArguments(containerId, genericParameterTypes.toList(), context))
}

/**
 * 方法绑定 - 封装方法调用和参数解析
 *
 * 存储了方法引用和已解析的参数值描述符，提供统一的调用接口。
 *
 * @property method 要调用的方法
 * @property argumentDescriptors 已解析的参数值描述符列表
 *
 * @constructor 创建方法绑定实例
 */
class MethodBinding(val method: Method, private val argumentDescriptors: List<ValueDescriptor>) {
    /**
     * 调用方法并注入依赖
     *
     * 执行流程：
     * 1. 从参数描述符中计算实际的参数值
     * 2. 通过反射调用方法
     * 3. 自动解包 InvocationTargetException，将真正的异常抛出
     *
     * @param instance 方法调用的目标对象
     * @throws Throwable 方法执行过程中抛出的异常（已解包）
     */
    fun invoke(instance: Any) {
        val arguments = computeArguments(argumentDescriptors).toTypedArray()
        runWithUnwrappingInvocationException { method.invoke(instance, *arguments) }
    }
}

/**
 * 为成员（方法或构造函数）绑定参数
 *
 * 核心的参数解析逻辑，对每个参数类型：
 * 1. 尝试从上下文中解析对应的值描述符
 * 2. 如果解析失败，收集到未满足的依赖列表
 * 3. 如果存在未满足的依赖，抛出异常
 *
 * @receiver 成员对象（方法或构造函数）
 * @param containerId 容器标识符（用于错误消息）
 * @param parameters 参数类型列表
 * @param context 值解析上下文
 * @return 已解析的值描述符列表
 * @throws UnresolvedDependenciesException 如果有参数无法解析
 */
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
                unsatisfied = ArrayList()
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

// ==================== 构造函数绑定 ====================

/**
 * 构造函数绑定 - 封装构造函数和已解析的参数
 *
 * 用于创建组件实例时的构造函数注入。
 *
 * @property constructor 要调用的构造函数
 * @property argumentDescriptors 已解析的参数值描述符列表
 *
 * @constructor 创建构造函数绑定实例
 */
class ConstructorBinding(val constructor: Constructor<*>, val argumentDescriptors: List<ValueDescriptor>)

/**
 * 将类绑定到构造函数
 *
 * 用于组件实例化，执行以下步骤：
 * 1. 从类信息中获取构造函数元数据
 * 2. 解析构造函数的所有参数
 * 3. 创建构造函数绑定对象
 *
 * 这是构造函数注入的核心函数，由 SingletonTypeComponentDescriptor 等使用。
 *
 * @receiver 要实例化的类
 * @param containerId 容器标识符（用于错误消息）
 * @param context 值解析上下文
 * @return 构造函数绑定对象
 * @throws IllegalStateException 如果类没有可用的构造函数
 * @throws UnresolvedDependenciesException 如果构造函数参数无法解析
 */
fun Class<*>.bindToConstructor(containerId: String, context: ValueResolveContext): ConstructorBinding {
    val constructorInfo = getInfo().constructorInfo ?: error("No constructor for $this: ${getInfo()} in $containerId")
    val candidate = constructorInfo.constructor
    return ConstructorBinding(candidate, candidate.bindArguments(containerId, constructorInfo.parameters, context))
}
