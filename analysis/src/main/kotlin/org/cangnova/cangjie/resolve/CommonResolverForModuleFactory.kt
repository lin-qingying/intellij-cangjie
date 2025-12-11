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


import org.cangnova.cangjie.descriptors.ModuleInfo
import org.cangnova.cangjie.descriptors.ModuleOrigin
import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import org.cangnova.cangjie.builtins.createBuiltIns
import org.cangnova.cangjie.config.LanguageFeature
import org.cangnova.cangjie.config.LanguageVersionSettings
import org.cangnova.cangjie.container.StorageComponentContainer
import org.cangnova.cangjie.container.get
import org.cangnova.cangjie.container.useImpl
import org.cangnova.cangjie.container.useInstance
import org.cangnova.cangjie.context.ModuleContext
import org.cangnova.cangjie.context.ProjectContext
import org.cangnova.cangjie.descriptors.BindingTrace
import org.cangnova.cangjie.descriptors.CompositePackageFragmentProvider
import org.cangnova.cangjie.descriptors.ModuleCapability
import org.cangnova.cangjie.descriptors.impl.ModuleDescriptorImpl
import org.cangnova.cangjie.frontend.configureModule
import org.cangnova.cangjie.frontend.configureStandardResolveComponents
import org.cangnova.cangjie.ide.vfilefinder.VirtualFileFinderFactory
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.platform.TargetPlatform
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.resolve.caches.ModuleContent
import org.cangnova.cangjie.resolve.controlFlow.ControlFlowInformationProviderImpl
import org.cangnova.cangjie.resolve.extensions.AnalysisHandlerExtension
import org.cangnova.cangjie.resolve.lazy.AbsentDescriptorHandler
import org.cangnova.cangjie.resolve.lazy.CompilerLocalDescriptorResolver
import org.cangnova.cangjie.resolve.lazy.ResolveSession
import org.cangnova.cangjie.resolve.lazy.declarations.DeclarationProviderFactory
import org.cangnova.cangjie.resolve.lazy.declarations.DeclarationProviderFactoryService
import org.cangnova.cangjie.resolve.scopes.optimization.OptimizingOptions
import org.cangnova.cangjie.serialization.builtins.CommonDependenciesContainer
import org.cangnova.cangjie.serialization.deserialization.CangJieBuiltInsPackageFragmentProvider
import org.cangnova.cangjie.serialization.deserialization.MetadataPackageFragmentProvider
import org.cangnova.cangjie.serialization.deserialization.MetadataPartProvider
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
    private val targetPlatform: TargetPlatform,
    private val shouldCheckExpectActual: Boolean,
    private val commonDependenciesContainer: CommonDependenciesContainer? = null
) : ResolverForModuleFactory() {
    private class SourceModuleInfo(
        override val name: Name,
//        override val capabilities: Map<ModuleCapability<*>, Any?>,
        private val dependencies: Iterable<ModuleInfo>,
//        override val expectedBy: List<ModuleInfo>,
//        override val platform: TargetPlatform,
        private val modulesWhoseInternalsAreVisible: Collection<ModuleInfo>,
        private val dependOnOldBuiltIns: Boolean
    ) : ModuleInfo {
        override val project: Project
            get() = TODO("Not yet implemented")
        override val contentScope: GlobalSearchScope
            get() = TODO("Not yet implemented")
        override val moduleOrigin: ModuleOrigin
            get() = ModuleOrigin.MODULE

        override fun dependencies() = listOf(this, *dependencies.toList().toTypedArray())

//        override fun modulesWhoseInternalsAreVisible(): Collection<ModuleInfo> = modulesWhoseInternalsAreVisible

        override fun dependencyOnBuiltIns(): ModuleInfo.DependencyOnBuiltIns =
            if (dependOnOldBuiltIns) ModuleInfo.DependencyOnBuiltIns.LAST else ModuleInfo.DependencyOnBuiltIns.NONE

        override val analyzerServices: PlatformDependentAnalyzerServices
            get() = CommonPlatformAnalyzerServices
    }

    override fun <M : ModuleInfo> createResolverForModule(
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
            targetPlatform,
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

    companion object {
        fun analyzeFiles(
            files: Collection<CjFile>,
            moduleName: Name,
            dependOnBuiltIns: Boolean,
            languageVersionSettings: LanguageVersionSettings,
            targetPlatform: TargetPlatform,
            targetEnvironment: TargetEnvironment,
            capabilities: Map<ModuleCapability<*>, Any?> = emptyMap(),
            dependenciesContainer: CommonDependenciesContainer? = null,
            explicitProjectContext: ProjectContext? = null,
            metadataPartProviderFactory: (ModuleContent<ModuleInfo>) -> MetadataPartProvider
        ): AnalysisResult {
            val moduleInfo = SourceModuleInfo(
                moduleName,
//                capabilities,
                dependenciesContainer?.moduleInfos?.toList().orEmpty(),
                dependenciesContainer?.refinesModuleInfos.orEmpty(),
//                targetPlatform,
//                dependenciesContainer?.friendModuleInfos.orEmpty(),
                dependOnBuiltIns
            )
            val project = files.firstOrNull()?.project ?: throw AssertionError("No files to analyze")

            val multiplatformLanguageSettings = object : LanguageVersionSettings by languageVersionSettings {
                override fun getFeatureSupport(feature: LanguageFeature): LanguageFeature.State =
                    /* if (feature == LanguageFeature.MultiPlatformProjects) LanguageFeature.State.ENABLED
                     else */languageVersionSettings.getFeatureSupport(feature)
            }

            val resolverForModuleFactory = CommonResolverForModuleFactory(
                CommonAnalysisParameters(metadataPartProviderFactory),
                targetEnvironment,
                targetPlatform,
                shouldCheckExpectActual = false,
                dependenciesContainer
            )

            val projectContext = explicitProjectContext ?: ProjectContext(project, "metadata serializer")
//            val resolver = CangJieResolverForModuleFactory(
//
//            )
            val resolver = ResolverForSingleModuleProject(
                "sources for metadata serializer",
                projectContext,
                moduleInfo,
                resolverForModuleFactory,
                GlobalSearchScope.allScope(project),
                builtIns = createBuiltIns(projectContext),
                languageVersionSettings = multiplatformLanguageSettings,
                syntheticFiles = files,
                knownDependencyModuleDescriptors = dependenciesContainer?.moduleInfos
                    ?.associateWith(dependenciesContainer::moduleDescriptorForModuleInfo).orEmpty()
            )

            val moduleDescriptor = resolver.descriptorForModule(moduleInfo)


            dependenciesContainer?.registerDependencyForAllModules(moduleInfo, moduleDescriptor)

            val container = resolver.resolverForModule(moduleInfo).componentProvider

            val analysisHandlerExtensions = AnalysisHandlerExtension.getInstances(project)
            val trace = container.get<BindingTrace>()


            var result = analysisHandlerExtensions.firstNotNullOfOrNull { extension ->
                extension.doAnalysis(project, moduleDescriptor, projectContext, files, trace, container)
            } ?: run {
                container.get<LazyTopDownAnalyzer>()
                    .analyzeDeclarations(TopDownAnalysisMode.TopLevelDeclarations, files)
                AnalysisResult.Companion.success(trace.bindingContext, moduleDescriptor)
            }

            result = analysisHandlerExtensions.firstNotNullOfOrNull { extension ->
                extension.analysisCompleted(project, moduleDescriptor, trace, files)
            } ?: result

            return result
        }
    }
}

object CommonPlatformAnalyzerServices : PlatformDependentAnalyzerServices() {
//    override fun computePlatformSpecificDefaultImports(storageManager: StorageManager, result: MutableList<ImportPath>) {}

    override val platformConfigurator: PlatformConfigurator = CommonPlatformConfigurator

    override fun dependencyOnBuiltIns(): ModuleInfo.DependencyOnBuiltIns = ModuleInfo.DependencyOnBuiltIns.AFTER_SDK
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
    platform: TargetPlatform,
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
            platform,
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
