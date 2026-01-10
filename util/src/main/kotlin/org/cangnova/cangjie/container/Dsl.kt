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
 * 依赖注入容器 DSL
 *
 * 本文件提供了用于配置依赖注入容器的 DSL（Domain Specific Language）工具函数。
 * 这些函数简化了容器配置，使代码更加简洁和易读。
 *
 * ## 核心概念
 *
 * - **容器组合**: 通过 `composeContainer` 创建和初始化容器
 * - **类型注册**: 通过 `useImpl` 注册需要自动实例化的类型
 * - **实例注册**: 通过 `useInstance` 注册已创建的实例
 * - **冲突解析**: 通过 `useClashResolver` 处理扩展冲突
 *
 * ## 使用示例
 *
 * ```kotlin
 * val container = composeContainer("MyContainer") {
 *     useImpl<MyService>()           // 自动实例化
 *     useInstance(myConfig)           // 注册实例
 *     useClashResolver(myResolver)    // 注册冲突解析器
 * }
 * ```
 *
 * @see StorageComponentContainer
 * @see PlatformExtensionsClashResolver
 */

/**
 * 创建并组合容器
 *
 * 这是创建依赖注入容器的主要入口函数。它创建一个新容器，
 * 执行初始化配置，然后组合所有已注册的组件。
 *
 * ## 工作流程
 *
 * 1. **创建容器**: 使用给定的 ID 和父容器创建新容器实例
 * 2. **执行初始化**: 调用 `init` 闭包配置容器（注册组件）
 * 3. **组合容器**: 调用 `compose()` 解析所有依赖关系
 * 4. **返回容器**: 返回配置完成的容器实例
 *
 * ## 容器层次结构
 *
 * 容器支持父子关系，子容器可以访问父容器中的组件：
 *
 * ```
 * 父容器 (parent)
 *   ├─> 组件 A
 *   └─> 组件 B
 *       ↓
 * 子容器 (child)
 *   ├─> 组件 C
 *   └─> 可以访问 A 和 B
 * ```
 *
 * ## 使用示例
 *
 * ```kotlin
 * // 创建独立容器
 * val container = composeContainer("MyContainer") {
 *     useImpl<ServiceA>()
 *     useImpl<ServiceB>()
 * }
 *
 * // 创建子容器
 * val childContainer = composeContainer("ChildContainer", parent = container) {
 *     useImpl<ServiceC>()  // 可以依赖父容器中的 ServiceA 和 ServiceB
 * }
 * ```
 *
 * ## 注意事项
 *
 * - 容器 ID 应该具有描述性，便于调试和错误报告
 * - `init` 闭包在 compose 之前执行，所有注册必须在此完成
 * - compose 之后容器就不能再添加新组件了
 *
 * @param id 容器标识符，用于调试和错误消息
 * @param parent 父容器（可选），子容器可以访问父容器的组件
 * @param init 容器初始化闭包，在此注册所有需要的组件
 * @return 配置并组合完成的容器实例
 *
 * @throws UnresolvedDependenciesException 如果存在无法解析的依赖
 *
 * @see StorageComponentContainer
 * @see StorageComponentContainer.compose
 */
fun composeContainer(
    id: String,
    parent: StorageComponentContainer? = null,
    init: StorageComponentContainer.() -> Unit
): StorageComponentContainer {
    val c = StorageComponentContainer(id, parent)
    c.init()
    c.compose()
    return c
}

/**
 * 注册实现类（自动实例化）
 *
 * 通过类型参数注册一个需要容器自动实例化的类。
 * 容器会通过反射找到构造函数，并自动解析和注入所有依赖。
 *
 * ## 工作原理
 *
 * 1. **类型推断**: 使用 `reified` 获取实际的类型信息
 * 2. **注册单例**: 将类型注册为单例组件
 * 3. **延迟实例化**: 在 compose 阶段才实例化
 * 4. **依赖注入**: 自动解析构造函数参数并注入
 *
 * ## 使用场景
 *
 * - 注册服务类，如 `LazyTopDownAnalyzer`
 * - 注册工具类，如 `DataFlowValueFactoryImpl`
 * - 注册检查器，如 `AnnotationResolverImpl`
 *
 * ## 依赖解析规则
 *
 * 构造函数参数会按以下顺序查找：
 * 1. 当前容器中已注册的实例或类型
 * 2. 父容器中已注册的实例或类型
 * 3. 如果找不到，抛出 `UnresolvedDependenciesException`
 *
 * ## 使用示例
 *
 * ```kotlin
 * composeContainer("Example") {
 *     // 注册服务
 *     useImpl<ResolveSession>()
 *     useImpl<LazyTopDownAnalyzer>()
 *     useImpl<AnnotationResolverImpl>()
 * }
 * ```
 *
 * ## 注意事项
 *
 * - **循环依赖**: 会导致实例化失败，需要重新设计依赖关系
 * - **构造函数**: 类必须有一个可访问的构造函数
 * - **单例**: 每个类型在容器中只会创建一个实例
 * - **线程安全**: 实例化过程是线程安全的
 *
 * ## 对比 useInstance
 *
 * | useImpl | useInstance |
 * |---------|-------------|
 * | 容器自动创建实例 | 使用已有实例 |
 * | 支持依赖注入 | 无依赖注入 |
 * | 延迟实例化 | 立即可用 |
 * | 适用于服务类 | 适用于配置对象 |
 *
 * @param T 要注册的类型，必须有可访问的构造函数
 *
 * @throws IllegalStateException 如果类没有可用的构造函数
 * @throws UnresolvedDependenciesException 如果构造函数的依赖无法满足
 *
 * @see useInstance
 * @see registerSingleton
 */
inline fun <reified T : Any> StorageComponentContainer.useImpl() {
    registerSingleton(T::class.java)
}

/**
 * 注册冲突解析器
 *
 * 注册一个平台扩展冲突解析器，用于处理多个平台扩展之间的冲突。
 * 当多个扩展提供相同功能时，解析器决定使用哪一个。
 *
 * ## 使用场景
 *
 * - 处理不同平台的扩展函数冲突
 * - 处理不同模块提供的同名成员
 * - 实现平台特定的优先级策略
 *
 * ## 工作原理
 *
 * 当解析过程中遇到冲突时：
 * 1. 容器会查找所有已注册的冲突解析器
 * 2. 按注册顺序依次尝试解析
 * 3. 第一个能够解析的解析器的结果会被采用
 *
 * ## 使用示例
 *
 * ```kotlin
 * composeContainer("Example") {
 *     useClashResolver(MyPlatformClashResolver())
 *     useClashResolver(FallbackClashResolver())
 * }
 * ```
 *
 * @param clashResolver 冲突解析器实例
 *
 * @see PlatformExtensionsClashResolver
 * @see registerClashResolvers
 */
fun StorageComponentContainer.useClashResolver(clashResolver: PlatformExtensionsClashResolver<*>) {
    registerClashResolvers(listOf(clashResolver))
}


/**
 * 注册已创建的实例
 *
 * 将一个已经创建好的对象实例注册到容器中。
 * 容器会根据实例的类型进行索引，其他组件可以通过类型引用它。
 *
 * ## 使用场景
 *
 * - 注册配置对象，如 `LanguageVersionSettings`
 * - 注册上下文对象，如 `ModuleContext`
 * - 注册外部创建的实例，如 `BindingTrace`
 * - 注册单例对象，如 `CangJieTypeRefiner.Default`
 *
 * ## 特点
 *
 * - **无依赖注入**: 实例已经创建，不会进行构造函数注入
 * - **立即可用**: 注册后立即可以被其他组件引用
 * - **类型索引**: 根据实例的运行时类型建立索引
 * - **多类型**: 一个实例可以被注册为多个类型（通过继承）
 *
 * ## 使用示例
 *
 * ```kotlin
 * val config = MyConfig(...)
 * val context = ModuleContext(...)
 *
 * composeContainer("Example") {
 *     useInstance(config)     // 注册配置
 *     useInstance(context)    // 注册上下文
 *     useImpl<MyService>()    // MyService 可以依赖 config 和 context
 * }
 * ```
 *
 * ## 特殊用法：避免循环依赖
 *
 * 当遇到循环依赖时，可以使用 `useInstance` 注入已创建的实例：
 *
 * ```kotlin
 * // 错误：循环依赖
 * useImpl<CangJieBuiltIns>()  // 需要 ProjectDescriptor
 * useImpl<ProjectDescriptor>() // 需要 CangJieBuiltIns
 *
 * // 正确：使用 useInstance
 * val projectDescriptor = ProjectDescriptorImpl(...)
 * val builtIns = projectDescriptor.builtIns  // 通过 lazy 创建
 * useInstance(projectDescriptor)
 * useInstance(builtIns)
 * ```
 *
 * ## 注意事项
 *
 * - 实例的生命周期由调用者管理，容器不负责销毁
 * - 如果注册同一类型的多个实例，后者会覆盖前者
 * - 不能注册 null 值，使用 `useInstanceIfNotNull` 处理可空情况
 *
 * @param instance 要注册的实例，不能为 null
 *
 * @see useInstanceIfNotNull
 * @see useImpl
 * @see registerInstance
 */
fun StorageComponentContainer.useInstance(instance: Any) {
    registerInstance(instance)
}

/**
 * 条件注册实例
 *
 * 如果实例不为 null，则将其注册到容器中。
 * 这是 `useInstance` 的安全版本，专门用于处理可空实例。
 *
 * ## 使用场景
 *
 * - 注册可选的配置对象
 * - 注册平台特定的组件（某些平台可能不提供）
 * - 注册用户可配置的扩展组件
 *
 * ## 使用示例
 *
 * ```kotlin
 * composeContainer("Example") {
 *     // 可选的优化选项
 *     useInstanceIfNotNull(optimizingOptions)  // 可能为 null
 *
 *     // 可选的描述符处理器
 *     useInstanceIfNotNull(absentDescriptorHandler)  // 可能为 null
 *
 *     // 必需的组件
 *     useInstance(moduleContext)  // 不能为 null
 * }
 * ```
 *
 * ## 对比 useInstance
 *
 * | useInstance | useInstanceIfNotNull |
 * |-------------|---------------------|
 * | 参数不能为 null | 参数可以为 null |
 * | 总是注册 | null 时跳过注册 |
 * | 适用于必需组件 | 适用于可选组件 |
 *
 * ## 典型用法
 *
 * ```kotlin
 * fun configureContainer(
 *     required: RequiredService,
 *     optional: OptionalService? = null
 * ) = composeContainer("Example") {
 *     useInstance(required)           // 必需
 *     useInstanceIfNotNull(optional)  // 可选
 * }
 * ```
 *
 * @param instance 要注册的实例，可以为 null（null 时不执行注册）
 *
 * @see useInstance
 * @see registerInstance
 */
fun StorageComponentContainer.useInstanceIfNotNull(instance: Any?) {
    if (instance != null) registerInstance(instance)
}

