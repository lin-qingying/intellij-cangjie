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

package org.cangnova.cangjie.resolve

import org.cangnova.cangjie.container.*
import org.cangnova.cangjie.resolve.caches.DeclarationChecker
import org.cangnova.cangjie.resolve.calls.checkers.AdditionalTypeChecker
import org.cangnova.cangjie.resolve.calls.checkers.AssignmentChecker
import org.cangnova.cangjie.resolve.calls.checkers.CallChecker
import org.cangnova.cangjie.resolve.calls.checkers.configureDefaultCheckers
import org.cangnova.cangjie.resolve.lazy.DelegationFilter
import org.cangnova.cangjie.types.DynamicTypesSettings

/**
 * 平台配置器工具函数
 *
 * 本文件提供了平台相关的配置器基类和工具函数，用于配置不同平台的代码分析行为。
 *
 * ## 核心概念
 *
 * - **平台配置器**: 封装平台特定的分析规则和检查器
 * - **检查器**: 各种代码检查器，如类型检查器、调用检查器等
 * - **扩展性**: 通过参数化配置支持不同平台的自定义需求
 *
 * @see PlatformConfigurator
 * @see PlatformConfiguratorBase
 */

/**
 * 创建平台特定的容器
 *
 * 这是一个便捷函数，用于创建包含平台特定组件的依赖注入容器。
 * 它会自动组合平台配置器提供的容器和用户自定义的初始化逻辑。
 *
 * ## 工作原理
 *
 * 1. 获取平台配置器的特定容器（`platformSpecificContainer`）
 * 2. 执行用户提供的初始化逻辑（`init`）
 * 3. 组合成一个完整的容器
 *
 * ## 使用场景
 *
 * - 创建 LazyResolve 容器
 * - 创建 BodyResolve 容器
 * - 创建 LazyBodyResolve 容器
 *
 * @param id 容器标识符，用于调试和错误报告
 * @param analyzerServices 平台相关的分析器服务，提供平台配置器
 * @param init 容器初始化闭包，用于注册额外的组件
 * @return 配置好的存储组件容器
 *
 * @see composeContainer
 * @see StorageComponentContainer
 */
fun createContainer(
    id: String,
    analyzerServices: PlatformDependentAnalyzerServices,
    init: StorageComponentContainer.() -> Unit
) =
    composeContainer(id, analyzerServices.platformConfigurator.platformSpecificContainer, init)

/**
 * 平台配置器基类
 *
 * 这是一个抽象基类，提供了平台特定配置的通用实现。
 * 通过构造函数参数接收各种检查器和过滤器，支持灵活的平台配置。
 *
 * ## 设计目标
 *
 * 1. **可扩展性**: 通过参数化配置支持不同平台的特殊需求
 * 2. **模块化**: 将不同类型的检查器分离，便于管理和扩展
 * 3. **复用性**: 提供通用的配置逻辑，减少平台实现的重复代码
 *
 * ## 支持的配置类型
 *
 * ### 检查器 (Checkers)
 * - **声明检查器** ([DeclarationChecker]): 检查声明的有效性
 * - **调用检查器** ([CallChecker]): 检查函数/方法调用
 * - **赋值检查器** ([AssignmentChecker]): 检查赋值操作
 * - **类型检查器** ([AdditionalTypeChecker]): 额外的类型检查
 *
 * ### 扩展组件
 * - **动态类型设置** ([DynamicTypesSettings]): 动态类型相关配置
 * - **标识符检查器** ([IdentifierChecker]): 检查标识符命名规范
 * - **重载过滤器** ([OverloadFilter]): 过滤重载候选
 * - **委托过滤器** ([DelegationFilter]): 过滤委托目标
 * - **冲突解析器** ([PlatformExtensionsClashResolver]): 解决扩展冲突
 * - **向后兼容助手** ([OverridesBackwardCompatibilityHelper]): 处理重写的兼容性
 * - **返回类型清理器** ([DeclarationReturnTypeSanitizer]): 清理和标准化返回类型
 *
 * ## 使用示例
 *
 * ```kotlin
 * class MyPlatformConfigurator : PlatformConfiguratorBase(
 *     additionalCallCheckers = listOf(MyCustomCallChecker()),
 *     identifierChecker = MyIdentifierChecker()
 * ) {
 *     override fun configureModuleComponents(container: StorageComponentContainer) {
 *         // 配置模块特定组件
 *     }
 * }
 * ```
 *
 * @property dynamicTypesSettings 动态类型设置（可选）
 * @property additionalDeclarationCheckers 额外的声明检查器列表
 * @property additionalCallCheckers 额外的调用检查器列表
 * @property additionalAssignmentCheckers 额外的赋值检查器列表
 * @property additionalTypeCheckers 额外的类型检查器列表
 * @property additionalClashResolvers 额外的冲突解析器列表
 * @property identifierChecker 标识符检查器（可选）
 * @property overloadFilter 重载过滤器（可选）
 * @property delegationFilter 委托过滤器（可选）
 * @property overridesBackwardCompatibilityHelper 重写向后兼容助手（可选）
 * @property declarationReturnTypeSanitizer 声明返回类型清理器（可选）
 *
 * @see PlatformConfigurator
 * @see CangJiePlatformConfigurator
 */
//根据参数可扩展
abstract class PlatformConfiguratorBase(
    private val dynamicTypesSettings: DynamicTypesSettings? = null,
    private val additionalDeclarationCheckers: List<DeclarationChecker> = emptyList(),
    private val additionalCallCheckers: List<CallChecker> = emptyList(),
    private val additionalAssignmentCheckers: List<AssignmentChecker> = emptyList(),
    private val additionalTypeCheckers: List<AdditionalTypeChecker> = emptyList(),
//    private val additionalClassifierUsageCheckers: List<ClassifierUsageChecker> = emptyList(),
//    private val additionalAnnotationCheckers: List<AdditionalAnnotationChecker> = emptyList(),
    private val additionalClashResolvers: List<PlatformExtensionsClashResolver<*>> = emptyList(),
    private val identifierChecker: IdentifierChecker? = null,
    private val overloadFilter: OverloadFilter? = null,
//    private val platformToCangJieClassMapper: PlatformToCangJieClassMapper? = null,
//    private val platformSpecificCastChecker: PlatformSpecificCastChecker? = null,
    private val delegationFilter: DelegationFilter? = null,
    private val overridesBackwardCompatibilityHelper: OverridesBackwardCompatibilityHelper? = null,
    private val declarationReturnTypeSanitizer: DeclarationReturnTypeSanitizer? = null
) : PlatformConfigurator {

    /**
     * 配置模块相关的检查器
     *
     * 这个方法在每个模块创建时被调用，用于注册模块级别的检查器。
     * 基类提供了空实现，子类可以根据需要重写。
     *
     * ## 使用场景
     *
     * - 注册平台特定的声明检查器
     * - 注册依赖模块信息的检查器
     * - 配置模块级别的验证规则
     *
     * ## 实现示例
     *
     * ```kotlin
     * override fun configureModuleDependentCheckers(container: StorageComponentContainer) {
     *     container.useImpl<MyModuleSpecificChecker>()
     * }
     * ```
     *
     * @param container 存储组件容器，用于注册检查器
     */
    override fun configureModuleDependentCheckers(container: StorageComponentContainer) {
//        container.useImpl<OptInMarkerDeclarationAnnotationChecker>()
    }


    /**
     * 配置扩展和检查器
     *
     * 将构造函数中传入的所有检查器和扩展组件注册到容器中。
     * 这是一个内部方法，被 `platformSpecificContainer` 使用。
     *
     * ## 注册过程
     *
     * 1. 注册动态类型设置（如果有）
     * 2. 注册所有额外的检查器（声明、调用、赋值、类型）
     * 3. 注册所有冲突解析器
     * 4. 注册可选的扩展组件（标识符检查器、过滤器等）
     *
     * ## 为什么使用 useInstance 而不是 useImpl？
     *
     * 因为这些检查器都是通过构造函数传入的已创建实例，
     * 而不是需要容器自动实例化的类型。
     *
     * @param container 存储组件容器
     *
     * @see useInstance
     * @see useInstanceIfNotNull
     * @see useClashResolver
     */
    fun configureExtensionsAndCheckers(container: StorageComponentContainer) {
        with(container) {
            useInstanceIfNotNull(dynamicTypesSettings)
            additionalDeclarationCheckers.forEach { useInstance(it) }
            additionalCallCheckers.forEach { useInstance(it) }
            additionalAssignmentCheckers.forEach { useInstance(it) }
            additionalTypeCheckers.forEach { useInstance(it) }
//            additionalClassifierUsageCheckers.forEach { useInstance(it) }
//            additionalAnnotationCheckers.forEach { useInstance(it) }
            additionalClashResolvers.forEach { useClashResolver(it) }
            useInstanceIfNotNull(identifierChecker)
            useInstanceIfNotNull(overloadFilter)
//            useInstanceIfNotNull(platformToCangJieClassMapper)
//            useInstanceIfNotNull(platformSpecificCastChecker)
            useInstanceIfNotNull(delegationFilter)
            useInstanceIfNotNull(overridesBackwardCompatibilityHelper)
            useInstanceIfNotNull(declarationReturnTypeSanitizer)
        }
    }

    /**
     * 平台特定容器
     *
     * 这是一个预配置的容器，包含了平台特定的所有检查器和扩展。
     * 它会被 `createContainer` 函数用于组合成最终的容器。
     *
     * ## 配置内容
     *
     * 1. **默认检查器**: 通过 `configureDefaultCheckers()` 注册
     * 2. **自定义扩展**: 通过 `configureExtensionsAndCheckers()` 注册
     *
     * ## 工作流程
     *
     * ```
     * platformSpecificContainer
     *   ├─> configureDefaultCheckers()      // 注册默认检查器
     *   └─> configureExtensionsAndCheckers() // 注册自定义扩展
     * ```
     *
     * @see composeContainer
     * @see configureDefaultCheckers
     * @see configureExtensionsAndCheckers
     */
    override val platformSpecificContainer = composeContainer(this::class.java.simpleName) {
        configureDefaultCheckers()
        configureExtensionsAndCheckers(this)
    }

}

/**
 * 仓颉平台配置器
 *
 * 这是仓颉语言的默认平台配置器实现，继承自 [PlatformConfiguratorBase]。
 * 目前提供了最基础的配置，没有额外的平台特定检查器。
 *
 * ## 设计说明
 *
 * - 使用默认的检查器配置（来自父类）
 * - 不添加额外的平台特定检查器
 * - 作为标准实现供编译器和 IDE 使用
 *
 * ## 未来扩展
 *
 * 如果需要添加仓颉特定的检查逻辑，可以在这里：
 * - 重写 `configureModuleComponents` 添加模块组件
 * - 重写 `configureModuleDependentCheckers` 添加检查器
 * - 通过父类构造函数传入自定义检查器
 *
 * ## 使用场景
 *
 * - 编译器中作为默认平台配置
 * - IDE 插件中作为标准分析配置
 * - 测试环境中作为基准配置
 *
 * @see PlatformConfiguratorBase
 * @see PlatformDependentAnalyzerServices
 */
object CangJiePlatformConfigurator : PlatformConfiguratorBase() {

    /**
     * 配置模块组件
     *
     * 注册仓颉平台特定的模块组件，包括：
     * - SyntheticScopes: 合成作用域，用于提供合成的构造函数和扩展
     *
     * @param container 存储组件容器
     */
    override fun configureModuleComponents(container: StorageComponentContainer) {
        container.useImpl<org.cangnova.cangjie.resolve.scopes.synthetic.FunInterfaceConstructorsScopeProvider>()
    }

    /**
     * 配置模块相关检查器
     *
     * 仓颉平台目前没有额外的模块相关检查器。
     * 未来可以在这里添加平台特定的检查逻辑。
     *
     * @param container 存储组件容器
     */
    override fun configureModuleDependentCheckers(container: StorageComponentContainer) {
//        container.useImpl<OptInMarkerDeclarationAnnotationChecker>()
    }


}
