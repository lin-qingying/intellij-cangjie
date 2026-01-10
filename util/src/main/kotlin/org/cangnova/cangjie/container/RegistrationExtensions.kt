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

// ==================== 组件注册扩展函数 ====================

/**
 * 注册单例组件
 *
 * 将指定的类注册为单例组件到容器中。容器会在首次请求时创建实例，
 * 之后的所有请求都返回同一个实例。
 *
 * **特点**：
 * - 延迟初始化：首次使用时才创建实例
 * - 单例模式：整个容器生命周期内只有一个实例
 * - 自动依赖注入：构造函数参数和 @Inject 属性会自动注入
 * - 生命周期管理：容器销毁时自动清理（如果实现了 Closeable）
 *
 * **注册要求**：
 * - 类必须有一个可访问的构造函数（带 @Inject 注解或无参构造函数）
 * - 构造函数的所有参数都必须能从容器中解析
 * - 类不能是抽象类
 *
 * **使用示例**：
 * ```kotlin
 * // 注册服务类
 * container.registerSingleton(DatabaseService::class.java)
 *          .registerSingleton(UserRepository::class.java)
 *
 * // 使用服务
 * val dbService = container.get<DatabaseService>()
 * ```
 *
 * @receiver 存储组件容器
 * @param klass 要注册的类类型
 * @return 容器自身（支持链式调用）
 *
 * @see SingletonTypeComponentDescriptor 单例组件的底层实现
 * @see registerInstance 注册已存在的实例
 */
fun StorageComponentContainer.registerSingleton(klass: Class<*>): StorageComponentContainer {
    return registerDescriptors(listOf(SingletonTypeComponentDescriptor(this, klass)))
}

/**
 * 注册实例组件
 *
 * 将已经创建的实例注册到容器中。该实例可以通过其类型从容器中获取。
 * 适用于已经初始化的对象或外部创建的依赖。
 *
 * **特点**：
 * - 立即可用：无需创建，直接使用提供的实例
 * - 外部管理：实例的创建和初始化由外部控制
 * - 类型推导：自动注册为实例的运行时类型
 * - 生命周期：容器不负责清理（除非实例实现了特定的销毁接口）
 *
 * **使用场景**：
 * - 注册配置对象
 * - 注册外部库创建的对象
 * - 注册需要特殊初始化的实例
 * - 单元测试中注册 Mock 对象
 *
 * **使用示例**：
 * ```kotlin
 * // 注册配置实例
 * val config = AppConfig.load("config.json")
 * container.registerInstance(config)
 *
 * // 注册外部对象
 * val httpClient = OkHttpClient.Builder().build()
 * container.registerInstance(httpClient)
 *
 * // 使用实例
 * val appConfig = container.get<AppConfig>()
 * ```
 *
 * @receiver 存储组件容器
 * @param instance 要注册的实例对象
 * @return 容器自身（支持链式调用）
 *
 * @see InstanceComponentDescriptor 实例组件的底层实现
 * @see registerSingleton 注册类型让容器创建单例
 */
fun StorageComponentContainer.registerInstance(instance: Any): StorageComponentContainer {
    return registerDescriptors(listOf(InstanceComponentDescriptor(instance)))
}
