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

// ==================== 组件提供者扩展函数 ====================

/**
 * 获取服务实例（强制）
 *
 * 从容器中获取指定类型的服务实例。如果服务不存在，抛出异常。
 * 这是最常用的服务获取方法，适用于必需的依赖项。
 *
 * **使用场景**：
 * - 获取核心服务，确保其存在
 * - 在初始化阶段获取必需的依赖
 *
 * @param T 服务类型
 * @param request 请求的服务类类型
 * @return 服务实例（保证非 null）
 * @throws UnresolvedServiceException 如果服务未注册或无法解析
 *
 * @see tryGetService 获取服务的非抛出版本
 */
fun <T : Any> ComponentProvider.getService(request: Class<T>): T {
    return tryGetService(request) ?: throw UnresolvedServiceException(this, request)
}

/**
 * 尝试获取服务实例（可选）
 *
 * 从容器中获取指定类型的服务实例。如果服务不存在，返回 null 而不抛出异常。
 * 适用于可选的依赖项。
 *
 * **使用场景**：
 * - 获取可选服务，不强制要求其存在
 * - 提供降级功能（服务不存在时使用默认实现）
 * - 避免异常处理的性能开销
 *
 * @param T 服务类型
 * @param request 请求的服务类类型
 * @return 服务实例，如果不存在则返回 null
 *
 * @see getService 获取服务的强制版本
 */
@Suppress("UNCHECKED_CAST")
fun <T : Any> ComponentProvider.tryGetService(request: Class<T>): T? {
    return resolve(request)?.getValue() as T?
}

/**
 * 获取服务实例（内联泛型版本）
 *
 * 使用 Kotlin 泛型具体化（reified）的便捷方法，无需显式传递 Class 对象。
 * 这是 Kotlin 代码中最方便的服务获取方式。
 *
 * **使用示例**：
 * ```kotlin
 * // Java 风格（需要显式 Class 对象）
 * val service = container.getService(MyService::class.java)
 *
 * // Kotlin 风格（使用泛型具体化）
 * val service = container.get<MyService>()
 * ```
 *
 * @param T 服务类型
 * @return 服务实例（保证非 null）
 * @throws UnresolvedServiceException 如果服务未注册或无法解析
 *
 * @see getService 底层实现方法
 */
inline fun <reified T : Any> ComponentProvider.get(): T {
    return getService(T::class.java)
}
