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

/**
 * 值描述符接口
 *
 * 该接口定义了获取值的基本契约。
 * 值描述符用于延迟计算和依赖注入系统中，提供统一的值访问接口。
 *
 * 使用场景：
 * - 依赖注入容器中的组件描述
 * - 延迟值计算
 * - 代理和包装器模式
 *
 * 实现类：
 * - [ComponentDescriptor]：组件描述符
 * - [IterableDescriptor]：可迭代集合描述符
 * - [SingletonDescriptor]：单例描述符
 */
interface ValueDescriptor {
    /**
     * 获取值
     *
     * 该方法返回描述符所代表的实际值。
     * 具体的实现可能会执行延迟初始化、依赖解析等操作。
     *
     * @return 描述符所代表的值
     */
    fun getValue(): Any
}
