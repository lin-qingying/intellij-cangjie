package com.linqingying.cangjie.frontend

import com.linqingying.cangjie.resolve.controlFlow.ControlFlowInformationProvider
import com.linqingying.cangjie.resolve.controlFlow.ControlFlowInformationProviderImpl
import com.linqingying.cangjie.config.LanguageVersionSettings
import com.linqingying.cangjie.container.*
import com.linqingying.cangjie.context.ModuleContext
import com.linqingying.cangjie.descriptors.BindingTrace
import com.linqingying.cangjie.extensions.TypeAttributeTranslatorExtension
import com.linqingying.cangjie.resolve.*
import com.linqingying.cangjie.resolve.calls.components.ClassicTypeSystemContextForCS
import com.linqingying.cangjie.resolve.calls.inference.components.ClassicConstraintSystemUtilContext
import com.linqingying.cangjie.resolve.calls.smartcasts.DataFlowValueFactoryImpl
import com.linqingying.cangjie.resolve.calls.tower.CangJieResolutionStatelessCallbacksImpl
import com.linqingying.cangjie.resolve.lazy.*
import com.linqingying.cangjie.resolve.lazy.declarations.DeclarationProviderFactory
import com.linqingying.cangjie.types.checker.CangJieTypePreparator
import com.linqingying.cangjie.types.checker.CangJieTypeRefiner
import com.linqingying.cangjie.types.checker.NewCangJieTypeCheckerImpl
import com.linqingying.cangjie.types.expressions.DeclarationScopeProviderForLocalClassifierAnalyzer
import com.linqingying.cangjie.types.expressions.LocalLazyDeclarationResolver
import com.linqingying.cangjie.utils.ProgressManagerBasedCancellationChecker
import com.intellij.psi.search.GlobalSearchScope
import com.linqingying.cangjie.builtins.CangJieBuiltIns
import com.linqingying.cangjie.contracts.ContractDeserializerImpl
import com.linqingying.cangjie.extensions.StorageComponentContainerContributor
import com.linqingying.cangjie.ide.vfilefinder.VirtualFileFinderFactory
import com.linqingying.cangjie.platform.TargetPlatform
import com.linqingying.cangjie.platform.TargetPlatformVersion
import com.linqingying.cangjie.resolve.descriptorUtil.isTypeRefinementEnabled
import com.linqingying.cangjie.resolve.scopes.optimization.OptimizingOptions
import com.linqingying.cangjie.serialization.deserialization.CangJieBuiltInsPackageFragmentProvider
import com.linqingying.cangjie.serialization.deserialization.CompilerDeserializationConfiguration

/**
 * Actually, those should be present in 'configurePlatformIndependentComponents',
 * but, unfortunately, this is currently impossible, because in some lightweight
 * containers (see [createContainerForBodyResolve] and similar) some dependencies
 * are missing
 *
 * If you're not doing some trickery with containers, you should use them.
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

private fun StorageComponentContainer.configurePlatformIndependentComponents() {
    useImpl<SupertypeLoopCheckerImpl>()
    useImpl<CangJieResolutionStatelessCallbacksImpl>()
    useImpl<DataFlowValueFactoryImpl>()
//
//    useImpl<OptInUsageChecker>()
//    useImpl<OptInUsageChecker.Overrides>()
//    useImpl<OptInUsageChecker.ClassifierUsage>()
//
    useImpl<ContractDeserializerImpl>()
    useImpl<CompilerDeserializationConfiguration>()
//
    useImpl<ClassicTypeSystemContextForCS>()
    useImpl<ClassicConstraintSystemUtilContext>()
    useInstance(ProgressManagerBasedCancellationChecker)
}

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
    configure(
        moduleContext, analyzerServices, bindingTrace,
        languageVersionSettings,
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

fun StorageComponentContainer.configureModule(
    moduleContext: ModuleContext,
    platform: TargetPlatform,
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
    useInstance(trace)
    useInstance(languageVersionSettings)

    useInstanceIfNotNull(optimizingOptions)

    if (absentDescriptorHandlerClass != null) {
        registerSingleton(absentDescriptorHandlerClass)
    }

    useInstance(platform)
    useInstance(analyzerServices)

    val nonTrivialPlatformVersion = platform
        .mapNotNull { it.targetPlatformVersion.takeIf { it != TargetPlatformVersion.NoVersion } }
        .singleOrNull()

    useInstance(nonTrivialPlatformVersion ?: TargetPlatformVersion.NoVersion)

    analyzerServices.platformConfigurator.configureModuleComponents(this)
    analyzerServices.platformConfigurator.configureModuleDependentCheckers(this)

    useInstance(TypeAttributeTranslatorExtension.createTranslators(moduleContext.project))

    for (extension in StorageComponentContainerContributor.getInstances(moduleContext.project)) {
        extension.registerModuleComponents(this, platform, moduleContext.module)
    }

    useImpl<NewCangJieTypeCheckerImpl>()

    if (moduleContext.module.isTypeRefinementEnabled()) {
//        useImpl<CangJieTypeRefinerImpl>()
    } else {
        useInstance(CangJieTypeRefiner.Default)
    }

    useInstance(CangJieTypePreparator.Default)

    configurePlatformIndependentComponents()
}

fun StorageComponentContainer.configure(
//    context: GlobalContext,
    context: ModuleContext,

//    platform: TargetPlatform,
    analyzerServices: PlatformDependentAnalyzerServices,
    trace: BindingTrace,
    languageVersionSettings: LanguageVersionSettings,
//    sealedProvider: SealedClassInheritorsProvider = CliSealedClassInheritorsProvider,
//    optimizingOptions: OptimizingOptions?,
    absentDescriptorHandlerClass: Class<out AbsentDescriptorHandler>?
) {
    if (absentDescriptorHandlerClass != null) {
        registerSingleton(absentDescriptorHandlerClass)
    }
    useInstance(trace)
    useInstance(context)
    useInstance(context.module)
    useInstance(languageVersionSettings)

    useInstance(context.project)
    useInstance(context.storageManager)
    useInstance(analyzerServices)
    useImpl<NewCangJieTypeCheckerImpl>()
    useInstance(TypeAttributeTranslatorExtension.createTranslators(context.project))

//    if (context.module.isTypeRefinementEnabled()) {
//        useImpl<CangJieTypeRefinerImpl>()
//    } else {
    useInstance(CangJieTypeRefiner.Default)
//    }
    useInstance(CangJieTypePreparator.Default)

    configurePlatformIndependentComponents()

}


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
    configure(
        context,
        PlatformDependentAnalyzerServicesImpl,
        bindingTrace,
        languageVersionSettings,

        absentDescriptorHandlerClass,

        )
    useInstance(moduleContentScope)
//    useInstance(VirtualFileFinderFactory.getInstance(context.project).create(moduleContentScope))
    useInstance(sealedProvider)

    val builtIns = context.module.builtIns

    useInstance(builtIns.customizer)
    useImpl<CangJieBuiltInsPackageFragmentProvider>()
    useInstance(VirtualFileFinderFactory.getInstance(context.project).create(moduleContentScope))

    useInstance(declarationProviderFactory)
    configureStandardResolveComponents()
////////////////////////////////////////////////////////

    useImpl<LocalLazyDeclarationResolver>()
    useImpl<ResolveElementCache>()

    useImpl<CompilerLocalDescriptorResolver>()
    useInstance(ControlFlowInformationProviderImpl.Factory )
}


fun createContainerForLazyBodyResolve(

    context: ModuleContext,

    cangjieCodeAnalyzer: CangJieCodeAnalyzer,
    bindingTrace: BindingTrace,
//    platform: TargetPlatform,
    bodyResolveCache: BodyResolveCache,
    analyzerServices: PlatformDependentAnalyzerServices,
//    declarationProviderFactory: DeclarationProviderFactory,

    languageVersionSettings: LanguageVersionSettings,
//    moduleStructureOracle: ModuleStructureOracle,
//    mainFunctionDetectorFactory: MainFunctionDetector.Factory,
//    sealedProvider: SealedClassInheritorsProvider,
    controlFlowInformationProviderFactory: ControlFlowInformationProvider.Factory,
//    optimizingOptions: OptimizingOptions?,
    absentDescriptorHandler: AbsentDescriptorHandler?,
): StorageComponentContainer = createContainer("LazyBodyResolve", analyzerServices) {

    configure(
        context,
        analyzerServices,
        bindingTrace,
        languageVersionSettings,

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
