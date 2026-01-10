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

/**
 * 组件描述符内部接口
 *
 * 扩展 [ValueDescriptor],添加组件注册和依赖管理的能力。
 */
internal interface ComponentDescriptor : ValueDescriptor {
    /**
     * 获取组件的注册类型
     *
     * 返回此组件可以作为哪些类型被解析(包括所有超类和接口)。
     *
     * @return 注册的类型列表
     */
    fun getRegistrations(): Iterable<Type>

    /**
     * 获取组件的依赖
     *
     * 返回此组件依赖的其他组件的类型。
     *
     * @param context 值解析上下文
     * @return 依赖类型集合
     */
    fun getDependencies(context: ValueResolveContext): Collection<Type>

    /**
     * 是否应该注入属性
     *
     * 指示是否应该对此组件执行 setter 注入。
     */
    val shouldInjectProperties: Boolean
        get() = false
}

/**
 * 可迭代描述符
 *
 * 包装多个值描述符,在获取值时返回所有描述符对应值的列表。
 * 用于处理 Iterable<T> 类型的依赖注入。
 *
 * @property descriptors 包含的值描述符集合
 */
class IterableDescriptor(val descriptors: Iterable<ValueDescriptor>) : ValueDescriptor {
    override fun getValue(): Any {
        return descriptors.map { it.getValue() }
    }

    override fun toString(): String = "Iterable: $descriptors"
}
