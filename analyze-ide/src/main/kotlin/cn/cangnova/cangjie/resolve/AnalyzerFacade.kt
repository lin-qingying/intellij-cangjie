/*
 * Copyright 2024 LinQingYing. and contributors.
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

package cn.cangnova.cangjie.resolve

import com.intellij.openapi.project.Project
import com.intellij.openapi.util.ModificationTracker
import cn.cangnova.cangjie.config.LanguageVersionSettings
import cn.cangnova.cangjie.config.LanguageVersionSettingsImpl
import cn.cangnova.cangjie.container.ComponentProvider
import cn.cangnova.cangjie.container.get
import cn.cangnova.cangjie.context.ModuleContext
import cn.cangnova.cangjie.descriptors.CompositePackageFragmentProvider
import cn.cangnova.cangjie.descriptors.ModuleDescriptor
import cn.cangnova.cangjie.descriptors.ModuleInfo
import cn.cangnova.cangjie.descriptors.PackageFragmentProvider
import cn.cangnova.cangjie.descriptors.impl.ModuleDependencies
import cn.cangnova.cangjie.descriptors.impl.ModuleDescriptorImpl
import cn.cangnova.cangjie.frontend.createContainerForLazyResolve
import cn.cangnova.cangjie.resolve.caches.ModuleContent
import cn.cangnova.cangjie.resolve.lazy.AbsentDescriptorHandler
import cn.cangnova.cangjie.resolve.lazy.ResolveSession
import cn.cangnova.cangjie.resolve.lazy.declarations.DeclarationProviderFactoryService
import cn.cangnova.cangjie.resolve.scopes.optimization.OptimizingOptions
import cn.cangnova.cangjie.storage.StorageManager

interface TrackableModuleInfo : ModuleInfo {
    fun createModificationTracker(): ModificationTracker
}

fun ModuleInfo.flatten(): List<ModuleInfo> = when (this) {
//    is CombinedModuleInfo -> listOf(this) + containedModules
    else -> listOf(this)
}
//interface PackageOracleFactory {
//    fun createOracle(moduleInfo: ModuleInfo): PackageOracle
//
//    object OptimisticFactory : PackageOracleFactory {
//        override fun createOracle(moduleInfo: ModuleInfo) = PackageOracle.Optimistic
//    }
//}
/**
 * Special-purpose module info that allows implementors to provide different behavior compared to the [originalModule]'s.
 * E.g. may be used to resolve common code as if it were target-specific, or to change the dependencies visible to the code.
 *
 * Resolvers should accept a derived module info, iff the [originalModule] is accepted.
 */
interface DerivedModuleInfo : ModuleInfo {
    val originalModule: ModuleInfo
}

class ResolverForModule(
    val packageFragmentProvider: PackageFragmentProvider,
    val componentProvider: ComponentProvider
)

class EmptyResolverForProject<M : ModuleInfo> : ResolverForProject<M>() {
    override val name: String
        get() = "Empty resolver"

    override fun tryGetResolverForModule(moduleInfo: M): ResolverForModule? = null
    override fun resolverForModuleDescriptor(descriptor: ModuleDescriptor): ResolverForModule =
        throw IllegalStateException("$descriptor is not contained in this resolver")

    override fun descriptorForModule(moduleInfo: M) = diagnoseUnknownModuleInfo(listOf(moduleInfo))
    override val allModules: Collection<M> = listOf()
    override fun diagnoseUnknownModuleInfo(infos: List<ModuleInfo>) =
        throw IllegalStateException("Should not be called for $infos")


//    override fun moduleInfoForModuleDescriptor(moduleDescriptor: ModuleDescriptor): M {
//        throw IllegalStateException("$moduleDescriptor is not contained in this resolver")
//    }
}

abstract class ResolverForProject<M : ModuleInfo>  {
    abstract val allModules: Collection<M>
    fun resolverForModule(moduleInfo: M): ResolverForModule =
        resolverForModuleDescriptor(descriptorForModule(moduleInfo))

    abstract val name: String
    abstract fun descriptorForModule(moduleInfo: M): ModuleDescriptor
    abstract fun resolverForModuleDescriptor(descriptor: ModuleDescriptor): ResolverForModule

    abstract fun diagnoseUnknownModuleInfo(infos: List<ModuleInfo>): Nothing
    override fun toString() = name

    abstract fun tryGetResolverForModule(moduleInfo: M): ResolverForModule?


    companion object {
        const val resolverForLibrariesName = "project libraries"
        const val resolverForModulesName = "project source roots and libraries"
        const val resolverForSpecialInfoName = "completion/highlighting in "
        const val resolverForSdkName = "sdk"

    }
}

interface ResolverForModuleComputationTracker {

    fun onResolverComputed(moduleInfo: ModuleInfo)

    companion object {
        fun getInstance(project: Project): ResolverForModuleComputationTracker? =
            project.getComponent(ResolverForModuleComputationTracker::class.java) ?: null
    }
}
abstract class ResolverForModuleFactory {
    open fun <M : ModuleInfo> createResolverForModule(
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
    open fun <M : ModuleInfo> createResolverForModule(
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
    open fun <M : ModuleInfo> createResolverForModule(
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
    override fun <M : ModuleInfo> createResolverForModule(
        moduleDescriptor: ModuleDescriptorImpl,
        moduleContext: ModuleContext,
        moduleContent: ModuleContent<M>,
        resolverForProject: ResolverForProject<M>,
        languageVersionSettings: LanguageVersionSettings,
        sealedInheritorsProvider: SealedClassInheritorsProvider,
        resolveOptimizingOptions: OptimizingOptions?,
        absentDescriptorHandlerClass: Class<out AbsentDescriptorHandler>?
    ): ResolverForModule{

        val project = moduleContext.project
        val (moduleInfo, syntheticFiles, moduleContentScope) = moduleContent

        val declarationProviderFactory = DeclarationProviderFactoryService.createDeclarationProviderFactory(
            project, moduleContext.storageManager, syntheticFiles,
            moduleContentScope,
            moduleInfo
        )
        val trace = CodeAnalyzerInitializer.getInstance(project).createTrace()


        val container = createContainerForLazyResolve(

            moduleContext,
            trace,
            declarationProviderFactory,
            moduleContentScope,
//            moduleClassResolver,
//            targetEnvironment,
//            lookupTracker,
//            ExpectActualTracker.DoNothing,
//            InlineConstTracker.DoNothing,
//            EnumWhenTracker.DoNothing,
//            packagePartProvider,
            languageVersionSettings,
//            sealedInheritorsProvider = sealedInheritorsProvider,
//            useBuiltInsProvider = platformParameters.useBuiltinsProviderForModule(moduleInfo),
//            optimizingOptions = resolveOptimizingOptions,
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
        moduleInfo: ModuleInfo,
        project: Project
    ): LanguageVersionSettings


    object Default : LanguageSettingsProvider {
        override fun getLanguageVersionSettings(
            moduleInfo: ModuleInfo,
            project: Project
        ) = LanguageVersionSettingsImpl.DEFAULT

    }
}


class LazyModuleDependencies<M : ModuleInfo>(
    storageManager: StorageManager,
    private val module: M,
    firstDependency: M?,
    private val resolverForProject: AbstractResolverForProject<M>
) : ModuleDependencies {
    companion object {
        private fun ModuleInfo.assertModuleDependencyIsCorrect(dependency: ModuleDescriptor) {
            assertModuleDependencyIsCorrect(dependency.getCapability(ModuleInfo.Capability) ?: return)
        }

        private fun ModuleInfo.assertModuleDependencyIsCorrect(dependency: ModuleInfo) {
            assert(dependency !is DerivedModuleInfo || this is DerivedModuleInfo) {
                "Derived module infos may not be referenced from regular ones"
            }
        }
    }

    private val dependencies = storageManager.createLazyValue {
        val moduleDescriptors = mutableSetOf<ModuleDescriptorImpl>()
        firstDependency?.let {
            module.assertModuleDependencyIsCorrect(it)
            moduleDescriptors.add(resolverForProject.descriptorForModule(it))
        }
        val moduleDescriptor = resolverForProject.descriptorForModule(module)
        val dependencyOnBuiltIns = module.dependencyOnBuiltIns()
        if (dependencyOnBuiltIns == ModuleInfo.DependencyOnBuiltIns.AFTER_SDK) {
            val builtInsModule = moduleDescriptor.builtIns.builtInsModule
            module.assertModuleDependencyIsCorrect(builtInsModule)
            moduleDescriptors.add(builtInsModule)
        }
        for (dependency in module.dependencies()) {
            if (dependency == firstDependency) continue
            module.assertModuleDependencyIsCorrect(dependency)

            @Suppress("UNCHECKED_CAST")
            moduleDescriptors.add(resolverForProject.descriptorForModule(dependency as M))
        }
        if (dependencyOnBuiltIns == ModuleInfo.DependencyOnBuiltIns.LAST) {
            val builtInsModule = moduleDescriptor.builtIns.builtInsModule
            module.assertModuleDependencyIsCorrect(builtInsModule)
            moduleDescriptors.add(builtInsModule)
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
