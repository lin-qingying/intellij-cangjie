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

import com.intellij.openapi.project.Project
import com.intellij.openapi.util.ModificationTracker
import org.cangnova.cangjie.config.LanguageVersionSettings
import org.cangnova.cangjie.config.LanguageVersionSettingsImpl
import org.cangnova.cangjie.container.ComponentProvider
import org.cangnova.cangjie.container.get
import org.cangnova.cangjie.context.ModuleContext
import org.cangnova.cangjie.descriptors.AnalysisContext
import org.cangnova.cangjie.descriptors.ModuleDescriptor
import org.cangnova.cangjie.descriptors.PackageFragmentProvider
import org.cangnova.cangjie.descriptors.impl.CompositePackageFragmentProvider
import org.cangnova.cangjie.descriptors.impl.ModuleDependencies
import org.cangnova.cangjie.descriptors.impl.ModuleDescriptorImpl
import org.cangnova.cangjie.frontend.createContainerForLazyResolve
import org.cangnova.cangjie.resolve.LazyModuleDependencies.Companion.assertModuleDependencyIsCorrect
import org.cangnova.cangjie.resolve.caches.ModuleContent
import org.cangnova.cangjie.resolve.lazy.AbsentDescriptorHandler
import org.cangnova.cangjie.resolve.lazy.ResolveSession
import org.cangnova.cangjie.resolve.lazy.declarations.DeclarationProviderFactoryService
import org.cangnova.cangjie.resolve.scopes.optimization.OptimizingOptions
import org.cangnova.cangjie.storage.StorageManager

/**
 * 可追踪的分析上下文
 *
 * 扩展 AnalysisContext，添加修改追踪能力。
 * 用于增量分析和缓存失效。
 */
interface TrackableAnalysisContext : AnalysisContext {
    fun createModificationTracker(): ModificationTracker
}

/**
 * 派生的分析上下文
 *
 * 允许实现者提供与原始上下文不同的行为。
 * 例如：将通用代码解析为特定平台代码，或改变可见的依赖关系。
 */
interface DerivedAnalysisContext : AnalysisContext {
    val originalContext: AnalysisContext
}

class ResolverForModule(
    val packageFragmentProvider: PackageFragmentProvider,
    val componentProvider: ComponentProvider
)

class EmptyResolverForProject<M : AnalysisContext> : ResolverForProject<M>() {
    override val name: String
        get() = "Empty resolver"

    override fun tryGetResolverForModule(context: M): ResolverForModule? = null
    override fun resolverForModuleDescriptor(descriptor: ModuleDescriptor): ResolverForModule =
        throw IllegalStateException("$descriptor is not contained in this resolver")

    override fun descriptorForModule(context: M) = diagnoseUnknownContext(listOf(context))
    override val allModules: Collection<M> = listOf()
    override fun diagnoseUnknownContext(contexts: List<AnalysisContext>) =
        throw IllegalStateException("Should not be called for $contexts")
}

abstract class ResolverForProject<M : AnalysisContext> {
    abstract val allModules: Collection<M>
    fun resolverForModule(context: M): ResolverForModule =
        resolverForModuleDescriptor(descriptorForModule(context))

    abstract val name: String
    abstract fun descriptorForModule(context: M): ModuleDescriptor
    abstract fun resolverForModuleDescriptor(descriptor: ModuleDescriptor): ResolverForModule

    abstract fun diagnoseUnknownContext(contexts: List<AnalysisContext>): Nothing
    override fun toString() = name

    abstract fun tryGetResolverForModule(context: M): ResolverForModule?

    companion object {
        const val resolverForLibrariesName = "project libraries"
        const val resolverForModulesName = "project source roots and libraries"
        const val resolverForSpecialInfoName = "completion/highlighting in "
        const val resolverForSdkName = "sdk"
    }
}

interface ResolverForModuleComputationTracker {

    fun onResolverComputed(context: AnalysisContext)

    companion object {
        fun getInstance(project: Project): ResolverForModuleComputationTracker? =
            project.getComponent(ResolverForModuleComputationTracker::class.java) ?: null
    }
}

abstract class ResolverForModuleFactory {
    open fun <M : AnalysisContext> createResolverForModule(
        moduleDescriptor: ModuleDescriptorImpl,
        moduleContext: ModuleContext,
        moduleContent: ModuleContent<M>,
        resolverForProject: ResolverForProject<M>,
        languageVersionSettings: LanguageVersionSettings,
        sealedInheritorsProvider: SealedClassInheritorsProvider,
        resolveOptimizingOptions: OptimizingOptions?,
        absentDescriptorHandlerClass: Class<out AbsentDescriptorHandler>?
    ): ResolverForModule {
        @Suppress("DEPRECATION")
        return createResolverForModule(
            moduleDescriptor,
            moduleContext,
            moduleContent,
            resolverForProject,
            languageVersionSettings,
            sealedInheritorsProvider,
            resolveOptimizingOptions
        )
    }

    @Deprecated(
        "Left only for compatibility, please use full version",
        ReplaceWith("createResolverForModule(moduleDescriptor, moduleContext, moduleContent, resolverForProject, languageVersionSettings, sealedInheritorsProvider, null, null)")
    )
    open fun <M : AnalysisContext> createResolverForModule(
        moduleDescriptor: ModuleDescriptorImpl,
        moduleContext: ModuleContext,
        moduleContent: ModuleContent<M>,
        resolverForProject: ResolverForProject<M>,
        languageVersionSettings: LanguageVersionSettings,
        sealedInheritorsProvider: SealedClassInheritorsProvider,
        resolveOptimizingOptions: OptimizingOptions?,
    ): ResolverForModule {
        @Suppress("DEPRECATION")
        return createResolverForModule(
            moduleDescriptor,
            moduleContext,
            moduleContent,
            resolverForProject,
            languageVersionSettings,
            sealedInheritorsProvider
        )
    }

    @Deprecated(
        "Left only for compatibility, please use full version",
        ReplaceWith("createResolverForModule(moduleDescriptor, moduleContext, moduleContent, resolverForProject, languageVersionSettings, sealedInheritorsProvider, null, null)")
    )
    open fun <M : AnalysisContext> createResolverForModule(
        moduleDescriptor: ModuleDescriptorImpl,
        moduleContext: ModuleContext,
        moduleContent: ModuleContent<M>,
        resolverForProject: ResolverForProject<M>,
        languageVersionSettings: LanguageVersionSettings,
        sealedInheritorsProvider: SealedClassInheritorsProvider
    ): ResolverForModule {
        return createResolverForModule(
            moduleDescriptor,
            moduleContext,
            moduleContent,
            resolverForProject,
            languageVersionSettings,
            sealedInheritorsProvider,
            resolveOptimizingOptions = null,
            absentDescriptorHandlerClass = null
        )
    }
}


class CangJieResolverForModuleFactory : ResolverForModuleFactory() {
    override fun <M : AnalysisContext> createResolverForModule(
        moduleDescriptor: ModuleDescriptorImpl,
        moduleContext: ModuleContext,
        moduleContent: ModuleContent<M>,
        resolverForProject: ResolverForProject<M>,
        languageVersionSettings: LanguageVersionSettings,
        sealedInheritorsProvider: SealedClassInheritorsProvider,
        resolveOptimizingOptions: OptimizingOptions?,
        absentDescriptorHandlerClass: Class<out AbsentDescriptorHandler>?
    ): ResolverForModule {

        val project = moduleContext.project
        val (context, syntheticFiles, moduleContentScope) = moduleContent

        val declarationProviderFactory = DeclarationProviderFactoryService.createDeclarationProviderFactory(
            project, moduleContext.storageManager, syntheticFiles,
            moduleContentScope,
            context
        )
        val trace = CodeAnalyzerInitializer.getInstance(project).createTrace()


        val container = createContainerForLazyResolve(

            moduleContext,
            trace,
            declarationProviderFactory,
            moduleContentScope,
            languageVersionSettings,
            absentDescriptorHandlerClass = absentDescriptorHandlerClass
        )
        val providersForModule = arrayListOf(
            container.get<ResolveSession>().getPackageFragmentProvider(),

            )
        return ResolverForModule(
            CompositePackageFragmentProvider(providersForModule, "CompositeProvider for $moduleDescriptor"),
            container
        )
    }

}

interface LanguageSettingsProvider {
    fun getLanguageVersionSettings(
        context: AnalysisContext,
        project: Project
    ): LanguageVersionSettings


    object Default : LanguageSettingsProvider {
        override fun getLanguageVersionSettings(
            context: AnalysisContext,
            project: Project
        ) = LanguageVersionSettingsImpl.DEFAULT

    }
}


class LazyModuleDependencies<M : AnalysisContext>(
    storageManager: StorageManager,
    private val module: M,
    firstDependency: M?,
    private val resolverForProject: AbstractResolverForProject<M>
) : ModuleDependencies {
    companion object {
        private fun AnalysisContext.assertModuleDependencyIsCorrect(dependency: ModuleDescriptor) {
            assertModuleDependencyIsCorrect(dependency.getCapability(AnalysisContextCapability) ?: return)
        }

        private fun AnalysisContext.assertModuleDependencyIsCorrect(dependency: AnalysisContext) {
            assert(dependency !is DerivedAnalysisContext || this is DerivedAnalysisContext) {
                "Derived analysis contexts may not be referenced from regular ones"
            }
        }
    }

    private val dependencies = storageManager.createLazyValue {
        val moduleDescriptors = mutableSetOf<ModuleDescriptorImpl>()
        firstDependency?.let {
            module.assertModuleDependencyIsCorrect(it)
            moduleDescriptors.add(resolverForProject.descriptorForModule(it))
        }

        // 处理所有依赖
        for (dependency in module.dependencies) {
            if (dependency == firstDependency) continue
            module.assertModuleDependencyIsCorrect(dependency)

            @Suppress("UNCHECKED_CAST")
            moduleDescriptors.add(resolverForProject.descriptorForModule(dependency as M))
        }

        moduleDescriptors.toList()
    }

    override val allDependencies: List<ModuleDescriptorImpl>
        get() = dependencies()
    override val modulesWhoseInternalsAreVisible: Set<ModuleDescriptorImpl>
        get() = emptySet()
    override val directExpectedByDependencies: List<ModuleDescriptorImpl>
        get() = emptyList()
    override val allExpectedByDependencies: Set<ModuleDescriptorImpl>
        get() = emptySet()
}

/**
 * AnalysisContext 的 ModuleDescriptor 能力键
 *
 * 用于在 ModuleDescriptor 中存储和检索关联的 AnalysisContext。
 */
val AnalysisContextCapability = ModuleCapability<AnalysisContext>("AnalysisContext")
