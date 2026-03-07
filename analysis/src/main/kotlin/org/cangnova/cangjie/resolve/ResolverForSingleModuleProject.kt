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

import org.cangnova.cangjie.builtins.CangJieBuiltIns
import org.cangnova.cangjie.config.LanguageVersionSettings
import org.cangnova.cangjie.config.LanguageVersionSettingsImpl
import org.cangnova.cangjie.context.ProjectContext
import org.cangnova.cangjie.context.withModule
import org.cangnova.cangjie.descriptors.ModuleDescriptor
import org.cangnova.cangjie.descriptors.ProjectDescriptor
import org.cangnova.cangjie.descriptors.impl.ModuleDescriptorImpl
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.resolve.caches.ModuleContent
import com.intellij.psi.search.GlobalSearchScope

import org.cangnova.cangjie.moduleinfo.ModuleInfo

/**
 * 单模块项目的解析器实现
 *
 * 这是一个专门为单模块项目设计的解析器，主要用于测试场景和简单项目分析。
 * 它简化了多模块项目解析器的复杂性，只关注单个模块及其已知依赖。
 *
 * ## 主要职责
 *
 * 1. **模块管理**: 管理单个主模块及其依赖模块的解析
 * 2. **模块内容提供**: 为模块提供源文件（包括合成文件）和搜索范围
 * 3. **解析器创建**: 为每个模块创建对应的 ResolverForModule 实例
 * 4. **依赖处理**: 管理已知的模块依赖关系和描述符
 *
 * ## 使用场景
 *
 * - 单元测试中的代码分析
 * - 独立文件或代码片段的解析
 * - 简单项目的快速分析
 * - 不需要完整项目结构的场景
 *
 * ## 工作流程
 *
 * ```
 * 1. 初始化时注册已知依赖模块描述符
 * 2. 为主模块和依赖模块提供内容 (modulesContent)
 * 3. 按需创建每个模块的解析器 (createResolverForModule)
 * 4. 解析器使用 resolverForModuleFactory 创建具体实例
 * ```
 *
 * ## 与多模块解析器的区别
 *
 * - **简化的依赖管理**: 只处理显式提供的依赖，不进行复杂的依赖发现
 * - **固定搜索范围**: 所有模块使用相同的 searchScope
 * - **无 SDK 依赖**: sdkDependency 参数已废弃
 * - **乐观的包预言**: 使用 OptimisticFactory，假设包总是存在
 *
 * @param M 模块信息类型，必须实现 ModuleInfo 接口
 * @param debugName 调试名称，用于日志和诊断
 * @param projectContext 项目上下文，提供项目级别的服务和配置
 * @param projectDescriptor 项目描述符，包含项目的元数据
 * @param module 要解析的主模块
 * @param resolverForModuleFactory 模块解析器工厂，用于创建具体的 ResolverForModule
 * @param searchScope 搜索范围，定义了查找符号的文件范围
 * @param builtIns 内置类型定义，默认使用标准内置类型
 * @param languageVersionSettings 语言版本设置，控制语言特性的启用
 * @param syntheticFiles 合成文件集合，通常用于测试或生成的代码
 * @param sdkDependency SDK 依赖模块（已废弃，不再使用）
 * @param knownDependencyModuleDescriptors 已知的依赖模块描述符映射
 *
 * @see AbstractResolverForProject 抽象解析器基类
 * @see ResolverForModuleFactory 模块解析器工厂接口
 * @see ModuleContent 模块内容数据类
 */
class ResolverForSingleModuleProject<M : ModuleInfo>(
    debugName: String,
    projectContext: ProjectContext,
    projectDescriptor: ProjectDescriptor,
    private val module: M,
    private val resolverForModuleFactory: ResolverForModuleFactory,
    private val searchScope: GlobalSearchScope,
    private val languageVersionSettings: LanguageVersionSettings = LanguageVersionSettingsImpl.Companion.DEFAULT,
    private val syntheticFiles: Collection<CjFile> = emptyList(),
    private val sdkDependency: M? = null,
    knownDependencyModuleDescriptors: Map<M, ModuleDescriptor> = emptyMap()
) : AbstractResolverForProject<M>(
    debugName,
    projectContext,
    projectDescriptor,
    listOf(module) + knownDependencyModuleDescriptors.keys,
    null,
    EmptyResolverForProject(),
    PackageOracleFactory.OptimisticFactory
) {
//    override fun sdkDependency(module: M): M? = sdkDependency

    init {
        knownDependencyModuleDescriptors.forEach { (module, descriptor) ->
            descriptorByModule[module] = ModuleData(
                descriptor as ModuleDescriptorImpl,
                (module as? TrackableModuleInfo)?.createModificationTracker() ?: fallbackModificationTracker
            )
        }
    }

    private val builtIns: CangJieBuiltIns get() = projectDescriptor.builtIns
    override fun modulesContent(module: M): ModuleContent<M> = when (module) {
        this.module -> ModuleContent(module, syntheticFiles, searchScope)
        else -> ModuleContent(module, emptyList(), searchScope)
    }


    override fun doCreateResolverForModule(descriptor: ModuleDescriptor, context: M): ResolverForModule =
        resolverForModuleFactory.createResolverForModule(
            descriptor as ModuleDescriptorImpl,
            projectContext.withModule(descriptor),
            modulesContent(context),
            this,
            languageVersionSettings,
            CliSealedClassInheritorsProvider,
            resolveOptimizingOptions = null,
            absentDescriptorHandlerClass = null
        )
}