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

import java.lang.reflect.Type

// ==================== 动态组件描述符 ====================

/**
 * 动态组件描述符 - 用于未知组件的占位符
 *
 * 当容器需要处理未知类型的组件时,使用此描述符作为占位符。
 * 调用 [getValue] 会抛出 [UnsupportedOperationException] 异常。
 */
object DynamicComponentDescriptor : ValueDescriptor {
    override fun getValue(): Any = throw UnsupportedOperationException()
    override fun toString(): String = "Dynamic"
}

// ==================== 实例组件描述符 ====================

/**
 * 实例组件描述符 - 用于已存在的实例对象
 *
 * 此描述符包装一个已创建的实例对象,允许将其注册到容器中。
 * 适用于需要向容器注册外部创建的对象的场景。
 *
 * @property instance 被包装的实例对象
 */
open class InstanceComponentDescriptor(val instance: Any) : ComponentDescriptor {

    override fun getValue(): Any = instance
    override fun getRegistrations(): Iterable<Type> = instance::class.java.getInfo().registrations

    override fun getDependencies(context: ValueResolveContext): Collection<Class<*>> = emptyList()

    override fun toString(): String {
        return "Instance: ${instance::class.java.simpleName}"
    }
}

/**
 * 默认实例组件描述符
 *
 * 与 [InstanceComponentDescriptor] 功能相同,但在字符串表示中明确标识为"默认"实例。
 * 用于区分默认提供的实例和自定义注册的实例。
 *
 * @param instance 被包装的实例对象
 */
class DefaultInstanceComponentDescriptor(instance: Any) : InstanceComponentDescriptor(instance) {
    override fun toString() = "Default instance: ${instance.javaClass.simpleName}"
}
