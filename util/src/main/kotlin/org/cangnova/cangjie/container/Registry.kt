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

import com.intellij.util.containers.MultiMap
import java.lang.reflect.Type


internal class ComponentRegistry {
    private val registrationMap = hashMapOf<Type, Any>()
    fun buildRegistrationMap(descriptors: Collection<ComponentDescriptor>): MultiMap<Type, ComponentDescriptor> {
        val registrationMap = MultiMap<Type, ComponentDescriptor>()
        for (descriptor in descriptors) {
            for (registration in descriptor.getRegistrations()) {
                registrationMap.putValue(registration, descriptor)
            }
        }
        return registrationMap
    }

    fun resolveClashesIfAny(container: ComponentContainer, clashResolvers: List<PlatformExtensionsClashResolver<*>>) {
        /*
        The idea is to create descriptor, which is very similar to other SingletonDescriptor, but instead of calling
        constructor we call 'resolveExtensionsClash' with values of clashed components as arguments.

        By mimicking the usual descriptors we get lazy evaluation and consistency checks for free.
         */
        for (resolver in clashResolvers) {
            @Suppress("UNCHECKED_CAST")
            val clashedComponents =
                registrationMap[resolver.applicableTo] as? Collection<ComponentDescriptor> ?: continue
            if (clashedComponents.size <= 1) continue

            val substituteDescriptor = ClashResolutionDescriptor(container, resolver, clashedComponents.toList())
            registrationMap[resolver.applicableTo] = substituteDescriptor
        }
    }

    fun addAll(other: ComponentRegistry) {
        if (!registrationMap.isEmpty()) {
            throw IllegalStateException("Can only copy entries from another component registry into an empty component registry")
        }
        registrationMap += other.registrationMap
    }

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
