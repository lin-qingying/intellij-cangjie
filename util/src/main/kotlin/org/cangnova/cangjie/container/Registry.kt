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
import java.lang.reflect.Type


/**
 * 组件注册表
 *
 * 管理类型到组件描述符的映射关系。支持同一类型的多个实现和冲突解决。
 */
internal class ComponentRegistry {
    private val registrationMap = hashMapOf<Type, Any>()

    /**
     * 构建注册映射表
     *
     * 将组件描述符按其注册的类型进行分组。
     *
     * @param descriptors 组件描述符集合
     * @return 类型到组件描述符的多值映射
     */
    fun buildRegistrationMap(descriptors: Collection<ComponentDescriptor>): MultiMap<Type, ComponentDescriptor> {
        val registrationMap = MultiMap<Type, ComponentDescriptor>()
        for (descriptor in descriptors) {
            for (registration in descriptor.getRegistrations()) {
                registrationMap.putValue(registration, descriptor)
            }
        }
        return registrationMap
    }

    /**
     * 解决组件冲突(如果有)
     *
     * 思路是创建一个类似于其他 SingletonDescriptor 的描述符,但不是调用构造函数,
     * 而是使用冲突组件的值作为参数调用 'resolveExtensionsClash'。
     *
     * 通过模仿常规描述符,我们可以免费获得延迟求值和一致性检查。
     *
     * @param container 组件容器
     * @param clashResolvers 冲突解决器列表
     */
    fun resolveClashesIfAny(container: ComponentContainer, clashResolvers: List<PlatformExtensionsClashResolver<*>>) {
        for (resolver in clashResolvers) {
            @Suppress("UNCHECKED_CAST")
            val clashedComponents =
                registrationMap[resolver.applicableTo] as? Collection<ComponentDescriptor> ?: continue
            if (clashedComponents.size <= 1) continue

            val substituteDescriptor = ClashResolutionDescriptor(container, resolver, clashedComponents.toList())
            registrationMap[resolver.applicableTo] = substituteDescriptor
        }
    }

    /**
     * 添加另一个注册表的所有条目
     *
     * 只能从另一个注册表复制条目到空的注册表。
     *
     * @param other 源注册表
     * @throws IllegalStateException 如果当前注册表不为空
     */
    fun addAll(other: ComponentRegistry) {
        if (!registrationMap.isEmpty()) {
            throw IllegalStateException("只能将条目从另一个组件注册表复制到空的组件注册表")
        }
        registrationMap += other.registrationMap
    }

    /**
     * 添加组件描述符集合
     *
     * 将新的组件描述符添加到注册表中。如果同一类型已有注册,会合并为列表。
     *
     * @param descriptors 要添加的组件描述符集合
     */
    fun addAll(descriptors: Collection<ComponentDescriptor>) {
        val newRegistrationMap = buildRegistrationMap(descriptors)
        for (entry in newRegistrationMap.entrySet()) {
            val oldEntries = registrationMap[entry.key]
            if (oldEntries != null || entry.value.size > 1) {
                val list = mutableListOf<ComponentDescriptor>()
                if (oldEntries is Collection<*>) {
                    @Suppress("UNCHECKED_CAST")
                    list.addAll(oldEntries as Collection<ComponentDescriptor>)
                } else if (oldEntries != null) {
                    list.add(oldEntries as ComponentDescriptor)
                }
                list.addAll(entry.value)
                registrationMap[entry.key] = list.singleOrNull() ?: list
            } else {
                registrationMap[entry.key] = entry.value.single()
            }
        }
    }

    /**
     * 尝试获取指定类型的组件条目
     *
     * @param request 请求的类型
     * @return 匹配的组件描述符集合,如果没有则返回空列表
     */
    fun tryGetEntry(request: Type): Collection<ComponentDescriptor> {
        val value = registrationMap[request]
        @Suppress("UNCHECKED_CAST")
        return when (value) {
            is Collection<*> -> value as Collection<ComponentDescriptor>
            null -> emptyList()
            else -> listOf(value as ComponentDescriptor)
        }
    }
}
