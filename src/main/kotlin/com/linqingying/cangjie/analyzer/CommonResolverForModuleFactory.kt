package com.linqingying.cangjie.analyzer


import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import com.linqingying.cangjie.builtins.createBuiltIns
import com.linqingying.cangjie.config.LanguageFeature
import com.linqingying.cangjie.config.LanguageVersionSettings
import com.linqingying.cangjie.container.StorageComponentContainer
import com.linqingying.cangjie.container.get
import com.linqingying.cangjie.container.useImpl
import com.linqingying.cangjie.container.useInstance
import com.linqingying.cangjie.context.ModuleContext
import com.linqingying.cangjie.context.ProjectContext
import com.linqingying.cangjie.descriptors.BindingTrace
import com.linqingying.cangjie.descriptors.CompositePackageFragmentProvider
import com.linqingying.cangjie.descriptors.ModuleCapability
import com.linqingying.cangjie.descriptors.impl.ModuleDescriptorImpl
import com.linqingying.cangjie.frontend.configureModule
import com.linqingying.cangjie.frontend.configureStandardResolveComponents
import com.linqingying.cangjie.ide.vfilefinder.VirtualFileFinderFactory
import com.linqingying.cangjie.name.Name
import com.linqingying.cangjie.platform.TargetPlatform
import com.linqingying.cangjie.psi.CjFile
import com.linqingying.cangjie.resolve.*
import com.linqingying.cangjie.resolve.caches.ModuleContent
import com.linqingying.cangjie.resolve.controlFlow.ControlFlowInformationProviderImpl
import com.linqingying.cangjie.resolve.extensions.AnalysisHandlerExtension
import com.linqingying.cangjie.resolve.lazy.AbsentDescriptorHandler
import com.linqingying.cangjie.resolve.lazy.CompilerLocalDescriptorResolver
import com.linqingying.cangjie.resolve.lazy.ResolveSession
import com.linqingying.cangjie.resolve.lazy.declarations.DeclarationProviderFactory
import com.linqingying.cangjie.resolve.lazy.declarations.DeclarationProviderFactoryService
import com.linqingying.cangjie.resolve.scopes.optimization.OptimizingOptions
import com.linqingying.cangjie.serialization.builtins.CommonDependenciesContainer
import com.linqingying.cangjie.serialization.deserialization.CangJieBuiltInsPackageFragmentProvider
import com.linqingying.cangjie.serialization.deserialization.MetadataPackageFragmentProvider
import com.linqingying.cangjie.serialization.deserialization.MetadataPartProvider
import com.linqingying.cangjie.types.expressions.LocalLazyDeclarationResolver

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
                AnalysisResult.success(trace.bindingContext, moduleDescriptor)
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
