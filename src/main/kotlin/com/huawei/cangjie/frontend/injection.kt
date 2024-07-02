package com.huawei.cangjie.frontend

import com.huawei.cangjie.container.*
import com.huawei.cangjie.context.ModuleContext
import com.huawei.cangjie.descriptors.BindingTrace
import com.huawei.cangjie.incremental.components.LookupTracker
import com.huawei.cangjie.resolve.*
import com.huawei.cangjie.resolve.calls.components.ClassicTypeSystemContextForCS
import com.huawei.cangjie.resolve.calls.smartcasts.DataFlowValueFactoryImpl
import com.huawei.cangjie.resolve.calls.tower.CangJieResolutionStatelessCallbacksImpl
import com.huawei.cangjie.resolve.lazy.*
import com.huawei.cangjie.resolve.lazy.declarations.DeclarationProviderFactory
import com.huawei.cangjie.types.checker.CangJieTypePreparator
import com.huawei.cangjie.types.checker.CangJieTypeRefiner
import com.huawei.cangjie.types.checker.NewCangJieTypeCheckerImpl
import com.huawei.cangjie.types.expressions.DeclarationScopeProviderForLocalClassifierAnalyzer
import com.huawei.cangjie.types.expressions.LocalClassDescriptorHolder
import com.huawei.cangjie.types.expressions.LocalLazyDeclarationResolver
import com.huawei.cangjie.utils.ProgressManagerBasedCancellationChecker

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
//    useImpl<AnnotationResolverImpl>()
////    useImpl<ResolveSession>()
//
//    useImpl<SupertypeLoopCheckerImpl>()
//
//    useImpl<ResolveElementCache>()
//
//    useImpl<CompilerLocalDescriptorResolver>()
//    useImpl<DeclarationScopeProviderForLocalClassifierAnalyzer>()


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
//    useImpl<ContractDeserializerImpl>()
//    useImpl<CompilerDeserializationConfiguration>()
//
    useImpl<ClassicTypeSystemContextForCS>()
//    useImpl<ClassicConstraintSystemUtilContext>()
    useInstance(ProgressManagerBasedCancellationChecker)
}

fun createContainerForBodyResolve(
    moduleContext: ModuleContext,
    bindingTrace: BindingTrace,
//    platform: TargetPlatform,
    statementFilter: StatementFilter,
    analyzerServices: PlatformDependentAnalyzerServices,
//    declarationProviderFactory: DeclarationProviderFactory,

//    languageVersionSettings: LanguageVersionSettings,
//    moduleStructureOracle: ModuleStructureOracle,
//    sealedProvider: SealedClassInheritorsProvider,
//    controlFlowInformationProviderFactory: ControlFlowInformationProvider.Factory,
    absentDescriptorHandler: AbsentDescriptorHandler?
): StorageComponentContainer = createContainer("BodyResolve", analyzerServices) {
    configure(
        moduleContext, analyzerServices, bindingTrace,

        absentDescriptorHandlerClass = if (absentDescriptorHandler == null) BasicAbsentDescriptorHandler::class.java else null
    )
    useInstanceIfNotNull(absentDescriptorHandler)

    useInstance(statementFilter)

    useInstance(BodyResolveCache.ThrowException)
    useImpl<AnnotationResolverImpl>()

    useImpl<BodyResolver>()
//    useInstance(moduleStructureOracle)
//    useInstance(controlFlowInformationProviderFactory)
//    useInstance(InlineConstTracker.DoNothing)
}

fun StorageComponentContainer.configure(
//    context: GlobalContext,
    context: ModuleContext,

//    platform: TargetPlatform,
    analyzerServices: PlatformDependentAnalyzerServices,
    trace: BindingTrace,
//    languageVersionSettings: LanguageVersionSettings,
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

    useInstance(context.project)
    useInstance(context.storageManager)
    useInstance(analyzerServices)
    useImpl<NewCangJieTypeCheckerImpl>()

//    if (context.module.isTypeRefinementEnabled()) {
//        useImpl<CangJieTypeRefinerImpl>()
//    } else {
    useInstance(CangJieTypeRefiner.Default)
//    }
    useInstance(CangJieTypePreparator.Default)

    configurePlatformIndependentComponents()

}

fun createContainerForLazyLocalClassifierAnalyzer(
    moduleContext: ModuleContext,
    bindingTrace: BindingTrace,

    lookupTracker: LookupTracker,

//    statementFilter: StatementFilter,
    localClassDescriptorHolder: LocalClassDescriptorHolder,
    analyzerServices: PlatformDependentAnalyzerServices,
//    controlFlowInformationProviderFactory: ControlFlowInformationProvider.Factory,
    absentDescriptorHandler: AbsentDescriptorHandler?
): StorageComponentContainer = createContainer("LocalClassifierAnalyzer", analyzerServices) {
    configure(
        moduleContext,

        analyzerServices,
        bindingTrace,


        absentDescriptorHandlerClass = null
    )

    if (absentDescriptorHandler != null) {
        useInstance(absentDescriptorHandler)
    }
    useInstance(localClassDescriptorHolder)
    useInstance(lookupTracker)
    /*
        useInstance(ExpectActualTracker.DoNothing)
        useInstance(InlineConstTracker.DoNothing)
        useInstance(EnumWhenTracker.DoNothing)
    */

    useImpl<LazyTopDownAnalyzer>()

//    useInstance(NoTopLevelDescriptorProvider)
//
//    TargetEnvironment.configureCompilerEnvironment(this)
//    useInstance(controlFlowInformationProviderFactory)


    useInstance(FileScopeProvider.ThrowException)
    useImpl<AnnotationResolverImpl>()

    useImpl<DeclarationScopeProviderForLocalClassifierAnalyzer>()
    useImpl<LocalLazyDeclarationResolver>()

//    useInstance(statementFilter)
}


fun createContainerForLazyResolve(

    context: ModuleContext,
    bindingTrace: BindingTrace,
    declarationProviderFactory: DeclarationProviderFactory,
//    moduleContentScope: GlobalSearchScope,


    absentDescriptorHandlerClass: Class<out AbsentDescriptorHandler>? = null
) = createContainer("LazyResolveWith", PlatformDependentAnalyzerServicesImpl)
{
    configure(
        context,
        PlatformDependentAnalyzerServicesImpl,
        bindingTrace,
        absentDescriptorHandlerClass
    )

    val builtIns = context.module.builtIns
//    if (useBuiltInsProvider && builtIns is JvmBuiltIns) {
        // TODO(dsavvinov): make sure that useBuiltInsProvider == true <=> builtIns is JvmBuiltIns
        // Currently, that's not the case at least in IDE unit-tests, because they do not set-up
        // dependency on SDK properly, see KT-43828
//        useInstance(builtIns.customizer)
//        useImpl<JvmBuiltInsPackageFragmentProvider>()
//    }
//    configureStandardResolveComponents()

    useInstance(declarationProviderFactory)
    configureStandardResolveComponents()
////////////////////////////////////////////////////////
    useImpl<DeclarationScopeProviderForLocalClassifierAnalyzer>()
    useImpl<LocalLazyDeclarationResolver>()
    useImpl<ResolveElementCache>()

    useImpl<CompilerLocalDescriptorResolver>()
}


fun createContainerForLazyBodyResolve(

    context: ModuleContext,

    cangjieCodeAnalyzer: CangJieCodeAnalyzer,
    bindingTrace: BindingTrace,
//    platform: TargetPlatform,
    bodyResolveCache: BodyResolveCache,
    analyzerServices: PlatformDependentAnalyzerServices,
    declarationProviderFactory: DeclarationProviderFactory,

//    languageVersionSettings: LanguageVersionSettings,
//    moduleStructureOracle: ModuleStructureOracle,
//    mainFunctionDetectorFactory: MainFunctionDetector.Factory,
//    sealedProvider: SealedClassInheritorsProvider,
//    controlFlowInformationProviderFactory: ControlFlowInformationProvider.Factory,
//    optimizingOptions: OptimizingOptions?,
    absentDescriptorHandler: AbsentDescriptorHandler?,
): StorageComponentContainer = createContainer("LazyBodyResolve", analyzerServices) {

    configure(
        context,
        analyzerServices,
        bindingTrace,

        absentDescriptorHandlerClass = BasicAbsentDescriptorHandler::class.java.takeIf { absentDescriptorHandler == null }
    )

    useInstanceIfNotNull(absentDescriptorHandler)
//    useInstance(mainFunctionDetectorFactory)
    useInstance(cangjieCodeAnalyzer)
//    useInstance(cangjieCodeAnalyzer.fileScopeProvider)
    useInstance(bodyResolveCache)
    useImpl<AnnotationResolverImpl>()
    useImpl<LazyTopDownAnalyzer>()
//    useInstance(moduleStructureOracle)
//    useInstance(controlFlowInformationProviderFactory)
//    useInstance(InlineConstTracker.DoNothing)

    // All containers except common inject ExpectedActualDeclarationChecker, so for common we do that
    // explicitly.
    // Note that it is not possible to move this code to [CommonPlatformConfigurator], because during
    // compilation of common-module to metadata we should skip those checks
//    if (platform.isCommon()) useImpl<ExpectedActualDeclarationChecker>()

//    if (absentDescriptorHandler != null) {
//        useInstance(absentDescriptorHandler)
//    }
//    useInstance(cangjieCodeAnalyzer)
//
//    useInstance(declarationProviderFactory)
//
//    useImpl<LazyTopDownAnalyzer>()
//    useInstance(cangjieCodeAnalyzer.fileScopeProvider)
}
