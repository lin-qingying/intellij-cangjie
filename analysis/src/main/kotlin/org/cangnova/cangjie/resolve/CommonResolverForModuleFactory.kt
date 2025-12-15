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


import org.cangnova.cangjie.descriptors.DependencyOnBuiltIns
import org.cangnova.cangjie.descriptors.AnalysisContext
import org.cangnova.cangjie.descriptors.ModuleOrigin
import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope

import org.cangnova.cangjie.config.LanguageFeature
import org.cangnova.cangjie.config.LanguageVersionSettings
import org.cangnova.cangjie.container.StorageComponentContainer
import org.cangnova.cangjie.container.get
import org.cangnova.cangjie.container.useImpl
import org.cangnova.cangjie.container.useInstance
import org.cangnova.cangjie.context.ModuleContext
import org.cangnova.cangjie.context.ProjectContext
import org.cangnova.cangjie.descriptors.ModuleCapability
import org.cangnova.cangjie.descriptors.impl.CompositePackageFragmentProvider
import org.cangnova.cangjie.descriptors.impl.ModuleDescriptorImpl
import org.cangnova.cangjie.frontend.configureModule
import org.cangnova.cangjie.frontend.configureStandardResolveComponents
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.resolve.binding.BindingTrace
import org.cangnova.cangjie.resolve.caches.ModuleContent
import org.cangnova.cangjie.resolve.controlFlow.ControlFlowInformationProviderImpl
import org.cangnova.cangjie.resolve.extensions.AnalysisHandlerExtension
import org.cangnova.cangjie.resolve.lazy.AbsentDescriptorHandler
import org.cangnova.cangjie.resolve.lazy.CompilerLocalDescriptorResolver
import org.cangnova.cangjie.resolve.lazy.ResolveSession
import org.cangnova.cangjie.resolve.lazy.declarations.DeclarationProviderFactory
import org.cangnova.cangjie.resolve.lazy.declarations.DeclarationProviderFactoryService
import org.cangnova.cangjie.resolve.scopes.optimization.OptimizingOptions
import org.cangnova.cangjie.types.expressions.LocalLazyDeclarationResolver

class CommonAnalysisParameters(
    val metadataPartProviderFactory: (ModuleContent<*>) -> MetadataPartProvider,

    ) : PlatformAnalysisParameters

/**
 * A facade that is used to analyze common (platform-independent) modules in multi-platform projects.
 */
class CommonResolverForModuleFactory(
    private val platformParameters: CommonAnalysisParameters,
    private val targetEnvironment: TargetEnvironment,

    private val shouldCheckExpectActual: Boolean,
    private val commonDependenciesContainer: CommonDependenciesContainer? = null
) : ResolverForModuleFactory() {

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
        val (moduleInfo, syntheticFiles, moduleContentScope) = moduleContent
        val project = moduleContext.project
        val declarationProviderFactory = DeclarationProviderFactoryService.createDeclarationProviderFactory(
            project, moduleContext.storageManager, syntheticFiles,
            moduleContentScope,
            moduleInfo
        )

        val metadataPartProvider = platformParameters.metadataPartProviderFactory(moduleContent)
        val trace = CodeAnalyzerInitializer.getInstance(project).createTrace()
        val container = createContainerToResolveCommonCode(
            moduleContext,
            trace,
            declarationProviderFactory,
            moduleContentScope,
            targetEnvironment,
            metadataPartProvider,
            languageVersionSettings,

            CommonPlatformAnalyzerServices,
            shouldCheckExpectActual,
            absentDescriptorHandlerClass
        )


        val packageFragmentProviders =
            /** If this is a dependency module that [commonDependenciesContainer] knows about, get the package fragments from there */
            commonDependenciesContainer?.packageFragmentProviderForModuleInfo(moduleInfo)?.let(::listOf)
                ?: listOfNotNull(
                    container.get<ResolveSession>().getPackageFragmentProvider(),
                    container.get<MetadataPackageFragmentProvider>(),

                    )
        return ResolverForModule(
            CompositePackageFragmentProvider(
                packageFragmentProviders,
                "CompositeProvider@CommonResolver for $moduleDescriptor"
            ),
            container
        )
    }
}

object CommonPlatformAnalyzerServices : PlatformDependentAnalyzerServices() {
//    override fun computePlatformSpecificDefaultImports(storageManager: StorageManager, result: MutableList<ImportPath>) {}

    override val platformConfigurator: PlatformConfigurator = CommonPlatformConfigurator

    override fun dependencyOnBuiltIns(): DependencyOnBuiltIns = DependencyOnBuiltIns.AFTER_SDK
}

private object CommonPlatformConfigurator : PlatformConfiguratorBase() {
    override fun configureModuleComponents(container: StorageComponentContainer) {}

}

private fun createContainerToResolveCommonCode(
    moduleContext: ModuleContext,
    bindingTrace: BindingTrace,
    declarationProviderFactory: DeclarationProviderFactory,
    moduleContentScope: GlobalSearchScope,
    targetEnvironment: TargetEnvironment,
    metadataPartProvider: MetadataPartProvider,
    languageVersionSettings: LanguageVersionSettings,

    analyzerServices: PlatformDependentAnalyzerServices,
    shouldCheckExpectActual: Boolean,
    absentDescriptorHandlerClass: Class<out AbsentDescriptorHandler>?
): StorageComponentContainer =
    createContainer("ResolveCommonCode", analyzerServices) {
//        configure(
//            moduleContext,
//
//            analyzerServices,
//            bindingTrace,
//            languageVersionSettings,
//
//            absentDescriptorHandlerClass = absentDescriptorHandlerClass
//        )
        configureModule(
            moduleContext,

            analyzerServices,
            bindingTrace,
            languageVersionSettings,
            optimizingOptions = null,
            absentDescriptorHandlerClass = absentDescriptorHandlerClass
        )
        useInstance(moduleContentScope)
        useInstance(declarationProviderFactory)

        useImpl<CompilerLocalDescriptorResolver>()
        useInstance(BodyResolveCache.ThrowException)
        useImpl<LocalLazyDeclarationResolver>()
        useInstance(ControlFlowInformationProviderImpl.Factory)
        configureStandardResolveComponents()

        val builtIns = moduleContext.module.builtIns
        useInstance(builtIns.customizer)
        useImpl<CangJieBuiltInsPackageFragmentProvider>()
        useInstance(VirtualFileFinderFactory.getInstance(moduleContext.project).create(moduleContentScope))

        configureCommonSpecificComponents()
        useInstance(metadataPartProvider)

//        val metadataFinderFactory = moduleContext.project.getService(
//            MetadataFinderFactory::class.java
//        )
//            ?: error("No MetadataFinderFactory in project")
//        useInstance(metadataFinderFactory.create(moduleContentScope))
//
//        targetEnvironment.configure(this)
//
//        if (shouldCheckExpectActual) {
//            useImpl<ExpectedActualDeclarationChecker>()
//        }
//        useInstance(InlineConstTracker.DoNothing)
    }

fun StorageComponentContainer.configureCommonSpecificComponents() {
    useImpl<MetadataPackageFragmentProvider>()
}
