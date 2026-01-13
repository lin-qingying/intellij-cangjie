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

package org.cangnova.cangjie.frontend

import org.cangnova.cangjie.resolve.controlFlow.ControlFlowInformationProvider
import org.cangnova.cangjie.resolve.controlFlow.ControlFlowInformationProviderImpl
import org.cangnova.cangjie.config.LanguageVersionSettings
import org.cangnova.cangjie.container.*
import org.cangnova.cangjie.context.ModuleContext
import org.cangnova.cangjie.extensions.TypeAttributeTranslatorExtension
import org.cangnova.cangjie.resolve.*
import org.cangnova.cangjie.resolve.calls.components.ClassicTypeSystemContextForCS
import org.cangnova.cangjie.resolve.calls.inference.components.ClassicConstraintSystemUtilContext
import org.cangnova.cangjie.resolve.calls.smartcasts.DataFlowValueFactoryImpl
import org.cangnova.cangjie.resolve.calls.tower.CangJieResolutionStatelessCallbacksImpl
import org.cangnova.cangjie.resolve.lazy.*
import org.cangnova.cangjie.resolve.lazy.declarations.DeclarationProviderFactory
import org.cangnova.cangjie.types.checker.CangJieTypePreparator
import org.cangnova.cangjie.types.checker.CangJieTypeRefiner

import org.cangnova.cangjie.types.expressions.DeclarationScopeProviderForLocalClassifierAnalyzer
import org.cangnova.cangjie.types.expressions.LocalLazyDeclarationResolver
import com.intellij.psi.search.GlobalSearchScope
import org.cangnova.cangjie.extensions.StorageComponentContainerContributor
import org.cangnova.cangjie.resolve.binding.BindingTrace
import org.cangnova.cangjie.resolve.scopes.optimization.OptimizingOptions
import org.cangnova.cangjie.serialization.deserialization.CompilerDeserializationConfiguration
import org.cangnova.cangjie.types.checker.DefaultCangJieTypeChecker
import org.cangnova.cangjie.utils.ProgressManagerBasedCancellationChecker


/**
 * 配置标准解析组件
 *
 * 注册代码解析所需的标准组件。这些组件在大多数解析场景中都需要用到。
 *
 * **注意**: 这些组件理论上应该在 `configurePlatformIndependentComponents` 中配置，
 * 但由于某些轻量级容器（如 `createContainerForBodyResolve`）缺少部分依赖，
 * 因此单独提取出来作为可选配置。
 *
 * ## 注册的组件
 *
 * - [ResolveSession]: 解析会话，管理解析状态
 * - [LazyTopDownAnalyzer]: 自顶向下的懒加载分析器
 * - [AnnotationResolverImpl]: 注解解析器
 * - [DeclarationScopeProviderForLocalClassifierAnalyzer]: 本地分类器作用域提供者
 *
 * @receiver 存储组件容器
 */
fun StorageComponentContainer.configureStandardResolveComponents() {
////    useImpl<LazyTopDownAnalyzer>()


//
//    useImpl<SupertypeLoopCheckerImpl>()
//
//    useImpl<ResolveElementCache>()
//
//    useImpl<CompilerLocalDescriptorResolver>()
    useImpl<DeclarationScopeProviderForLocalClassifierAnalyzer>()


    useImpl<ResolveSession>()
    useImpl<LazyTopDownAnalyzer>()
    useImpl<AnnotationResolverImpl>()

}

/**
 * 配置平台无关组件
 *
 * 注册与平台无关的核心组件，这些组件在所有平台上的行为都是一致的。
 *
 * ## 注册的组件
 *
 * - [SupertypeLoopCheckerImpl]: 父类型循环检查器，防止继承循环
 * - [CangJieResolutionStatelessCallbacksImpl]: 无状态解析回调
 * - [DataFlowValueFactoryImpl]: 数据流值工厂，用于数据流分析
 * - [CompilerDeserializationConfiguration]: 反序列化配置
 * - [ClassicTypeSystemContextForCS]: 约束系统的类型系统上下文
 * - [ClassicConstraintSystemUtilContext]: 约束系统工具上下文
 * - [LookupTracker.DO_NOTHING]: 查找追踪器，用于增量编译的符号查找追踪
 *
 * @receiver 存储组件容器
 */
private fun StorageComponentContainer.configurePlatformIndependentComponents() {
    useImpl<SupertypeLoopCheckerImpl>()
    useImpl<CangJieResolutionStatelessCallbacksImpl>()
    useImpl<DataFlowValueFactoryImpl>()

    useImpl<CompilerDeserializationConfiguration>()
    useImpl<ClassicTypeSystemContextForCS>()
    useImpl<ClassicConstraintSystemUtilContext>()
    useInstance(ProgressManagerBasedCancellationChecker)
    useInstance(org.cangnova.cangjie.incremental.components.LookupTracker.DO_NOTHING)

}

/**
 * 创建函数体解析容器
 *
 * 为单个函数体或代码块的解析创建轻量级容器。
 * 这个容器专门用于分析局部代码，不需要完整的模块分析能力。
 *
 * ## 使用场景
 *
 * - IDE 中的实时代码分析
 * - 单个函数的类型检查
 * - 局部变量的作用域分析
 * - 快速错误检测
 *
 * ## 容器特点
 *
 * - **轻量级**: 只包含必要的组件，启动快速
 * - **局部性**: 只分析指定的代码块，不影响全局
 * - **实时性**: 适合 IDE 实时分析场景
 *
 * @param moduleContext 模块上下文，提供模块级别的信息
 * @param bindingTrace 绑定追踪器，记录解析结果
 * @param statementFilter 语句过滤器，控制哪些语句需要分析
 * @param analyzerServices 平台相关的分析器服务
 * @param languageVersionSettings 语言版本设置
 * @param controlFlowInformationProviderFactory 控制流信息提供者工厂
 * @param absentDescriptorHandler 缺失描述符处理器（可选）
 * @return 配置好的存储组件容器
 *
 * @see createContainerForLazyResolve
 * @see createContainerForLazyBodyResolve
 */
fun createContainerForBodyResolve(
    moduleContext: ModuleContext,
    bindingTrace: BindingTrace,
//    platform: TargetPlatform,
    statementFilter: StatementFilter,
    analyzerServices: PlatformDependentAnalyzerServices,
//    declarationProviderFactory: DeclarationProviderFactory,

    languageVersionSettings: LanguageVersionSettings,
//    moduleStructureOracle: ModuleStructureOracle,
//    sealedProvider: SealedClassInheritorsProvider,
    controlFlowInformationProviderFactory: ControlFlowInformationProvider.Factory,
    absentDescriptorHandler: AbsentDescriptorHandler?
): StorageComponentContainer = createContainer("BodyResolve", analyzerServices) {
    configureModule(
        moduleContext,
        analyzerServices,
        bindingTrace,
        languageVersionSettings,
        sealedProvider = CliSealedClassInheritorsProvider,
        optimizingOptions = null,
        absentDescriptorHandlerClass = if (absentDescriptorHandler == null) BasicAbsentDescriptorHandler::class.java else null
    )
    useInstanceIfNotNull(absentDescriptorHandler)

    useInstance(statementFilter)

    useInstance(BodyResolveCache.ThrowException)
    useImpl<AnnotationResolverImpl>()

    useImpl<BodyResolver>()
//    useInstance(moduleStructureOracle)
    useInstance(controlFlowInformationProviderFactory)
//    useInstance(InlineConstTracker.DoNothing)
}

/**
 * 配置模块级组件
 *
 * 为模块容器配置所有必要的组件。这是模块级分析的核心配置函数。
 *
 * ## 配置内容
 *
 * 1. **核心依赖注入**:
 *    - ModuleContext: 模块上下文
 *    - Project: IntelliJ 项目实例
 *    - StorageManager: 存储管理器
 *    - CangJieBuiltIns: 内置类型（从 moduleContext.module.builtIns 获取）
 *    - BindingTrace: 绑定追踪器
 *    - LanguageVersionSettings: 语言版本设置
 *
 * 2. **平台配置**:
 *    - 调用平台配置器配置模块组件
 *    - 注册平台相关的检查器
 *
 * 3. **扩展点支持**:
 *    - TypeAttributeTranslatorExtension: 类型属性转换器
 *    - StorageComponentContainerContributor: 容器贡献者扩展
 *
 * 4. **类型系统**:
 *    - NewCangJieTypeCheckerImpl: 新的类型检查器
 *    - CangJieTypeRefiner: 类型精化器
 *    - CangJieTypePreparator: 类型预处理器
 *
 * **重要**: CangJieBuiltIns 通过 `useInstance` 注入已创建的实例，
 * 而不是通过 `useImpl` 自动创建，避免循环依赖问题。
 *
 * @receiver 存储组件容器
 * @param moduleContext 模块上下文
 * @param analyzerServices 平台相关的分析器服务
 * @param trace 绑定追踪器
 * @param languageVersionSettings 语言版本设置
 * @param sealedProvider 密封类继承者提供者
 * @param optimizingOptions 优化选项（可选）
 * @param absentDescriptorHandlerClass 缺失描述符处理器类（可选）
 *
 * @see configurePlatformIndependentComponents
 */
fun StorageComponentContainer.configureModule(
    moduleContext: ModuleContext,

    analyzerServices: PlatformDependentAnalyzerServices,
    trace: BindingTrace,
    languageVersionSettings: LanguageVersionSettings,
    sealedProvider: SealedClassInheritorsProvider = CliSealedClassInheritorsProvider,
    optimizingOptions: OptimizingOptions?,
    absentDescriptorHandlerClass: Class<out AbsentDescriptorHandler>?
) {
    useInstance(sealedProvider)
    useInstance(moduleContext)
    useInstance(moduleContext.module)
    useInstance(moduleContext.project)
    useInstance(moduleContext.storageManager)
    useInstance(moduleContext.module.builtIns)
    useInstance(moduleContext.module.projectDescriptor)

    useInstance(trace)
    useInstance(languageVersionSettings)

    useInstanceIfNotNull(optimizingOptions)

    if (absentDescriptorHandlerClass != null) {
        registerSingleton(absentDescriptorHandlerClass)
    }


    useInstance(analyzerServices)




    analyzerServices.platformConfigurator.configureModuleComponents(this)
    analyzerServices.platformConfigurator.configureModuleDependentCheckers(this)

    useInstance(TypeAttributeTranslatorExtension.createTranslators(moduleContext.project))

    for (extension in StorageComponentContainerContributor.EP_NAME.extensionList) {
        extension.registerModuleComponents(this, moduleContext.module)
    }



//    if (moduleContext.module.isTypeRefinementEnabled()) {
////        useImpl<CangJieTypeRefinerImpl>()
//    } else {
    useInstance(CangJieTypeRefiner.Default)
//    }

    useInstance(CangJieTypePreparator.Default)
    useImpl<DefaultCangJieTypeChecker>()
    configurePlatformIndependentComponents()
}


/**
 * 创建懒加载解析容器
 *
 * 创建一个支持懒加载的完整模块解析容器。这是最常用的容器配置，
 * 用于 IDE 中的模块级代码分析。
 *
 * ## 容器特性
 *
 * - **懒加载**: 按需加载和解析声明，提高性能
 * - **完整性**: 支持完整的模块分析功能
 * - **缓存**: 自动缓存解析结果
 *
 * ## 使用场景
 *
 * - IDE 中的全局代码分析
 * - 模块级别的类型检查
 * - 代码导航和引用查找
 * - 重构操作
 *
 * ## 关键组件
 *
 * - **DeclarationProviderFactory**: 声明提供者工厂，负责查找声明
 * - **ResolveSession**: 解析会话，管理解析过程
 * - **LazyTopDownAnalyzer**: 懒加载的自顶向下分析器
 * - **ResolveElementCache**: 解析元素缓存
 *
 * **注意**: CangJieBuiltIns 从 `context.module.builtIns` 获取，
 * 这是一个已经创建好的实例，不会通过容器构造。
 *
 * @param context 模块上下文
 * @param bindingTrace 绑定追踪器，记录解析结果
 * @param declarationProviderFactory 声明提供者工厂
 * @param moduleContentScope 模块内容范围
 * @param languageVersionSettings 语言版本设置
 * @param absentDescriptorHandlerClass 缺失描述符处理器类（可选）
 * @param sealedProvider 密封类继承者提供者
 * @return 配置好的存储组件容器
 *
 * @see createContainerForBodyResolve
 * @see createContainerForLazyBodyResolve
 */
fun createContainerForLazyResolve(

    context: ModuleContext,
    bindingTrace: BindingTrace,
    declarationProviderFactory: DeclarationProviderFactory,
    moduleContentScope: GlobalSearchScope,

    languageVersionSettings: LanguageVersionSettings,

    absentDescriptorHandlerClass: Class<out AbsentDescriptorHandler>? = null,
    sealedProvider: SealedClassInheritorsProvider = CliSealedClassInheritorsProvider,

    ) = createContainer("LazyResolve", PlatformDependentAnalyzerServicesImpl)
{
    configureModule(
        context,
        PlatformDependentAnalyzerServicesImpl,
        bindingTrace,
        languageVersionSettings,
        sealedProvider = sealedProvider,
        optimizingOptions = null,
        absentDescriptorHandlerClass = absentDescriptorHandlerClass
    )
    useInstance(moduleContentScope)

    val builtIns = context.module.builtIns


    useInstance(declarationProviderFactory)
    configureStandardResolveComponents()
////////////////////////////////////////////////////////

    useImpl<LocalLazyDeclarationResolver>()
    useImpl<ResolveElementCache>()

    useImpl<CompilerLocalDescriptorResolver>()
    useInstance(ControlFlowInformationProviderImpl.Factory)
}


/**
 * 创建懒加载函数体解析容器
 *
 * 创建一个支持懒加载的函数体解析容器。这个容器结合了懒加载和局部分析的优点，
 * 适用于需要缓存解析结果的场景。
 *
 * ## 容器特性
 *
 * - **懒加载**: 按需解析函数体
 * - **局部性**: 只分析函数体，不影响全局
 * - **缓存支持**: 通过 BodyResolveCache 缓存解析结果
 *
 * ## 使用场景
 *
 * - IDE 中的增量分析
 * - 函数体修改后的重新分析
 * - 需要缓存的局部分析
 *
 * ## 与其他容器的区别
 *
 * - vs `createContainerForBodyResolve`: 添加了懒加载和缓存支持
 * - vs `createContainerForLazyResolve`: 专注于函数体，更轻量级
 *
 * ## 关键组件
 *
 * - **CangJieCodeAnalyzer**: 代码分析器
 * - **BodyResolveCache**: 函数体解析缓存
 * - **LazyTopDownAnalyzer**: 懒加载分析器
 * - **ControlFlowInformationProvider**: 控制流信息提供者
 *
 * @param context 模块上下文
 * @param cangjieCodeAnalyzer 仓颉代码分析器
 * @param bindingTrace 绑定追踪器
 * @param bodyResolveCache 函数体解析缓存
 * @param analyzerServices 平台相关的分析器服务
 * @param languageVersionSettings 语言版本设置
 * @param controlFlowInformationProviderFactory 控制流信息提供者工厂
 * @param absentDescriptorHandler 缺失描述符处理器（可选）
 * @return 配置好的存储组件容器
 *
 * @see createContainerForBodyResolve
 * @see createContainerForLazyResolve
 */
fun createContainerForLazyBodyResolve(

    context: ModuleContext,

    cangjieCodeAnalyzer: CangJieCodeAnalyzer,
    bindingTrace: BindingTrace,
    bodyResolveCache: BodyResolveCache,
    analyzerServices: PlatformDependentAnalyzerServices,

    languageVersionSettings: LanguageVersionSettings,
    controlFlowInformationProviderFactory: ControlFlowInformationProvider.Factory,
    absentDescriptorHandler: AbsentDescriptorHandler?,
): StorageComponentContainer = createContainer("LazyBodyResolve", analyzerServices) {

    configureModule(
        context,
        analyzerServices,
        bindingTrace,
        languageVersionSettings,
        sealedProvider = CliSealedClassInheritorsProvider,
        optimizingOptions = null,
        absentDescriptorHandlerClass = BasicAbsentDescriptorHandler::class.java.takeIf { absentDescriptorHandler == null }
    )

    useInstanceIfNotNull(absentDescriptorHandler)

    useInstance(cangjieCodeAnalyzer)

    useInstance(bodyResolveCache)

    useImpl<LazyTopDownAnalyzer>()
    useImpl<DeclarationScopeProviderForLocalClassifierAnalyzer>()
    useImpl<AnnotationResolverImpl>()
    useInstance(controlFlowInformationProviderFactory)
}
