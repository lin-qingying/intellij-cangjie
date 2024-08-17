package com.huawei.cangjie.frontend

import com.huawei.cangjie.config.LanguageVersionSettings
import com.huawei.cangjie.container.*
import com.huawei.cangjie.context.ModuleContext
import com.huawei.cangjie.descriptors.BindingTrace
import com.huawei.cangjie.extensions.TypeAttributeTranslatorExtension
import com.huawei.cangjie.incremental.components.LookupTracker
import com.huawei.cangjie.resolve.*
import com.huawei.cangjie.resolve.calls.components.ClassicTypeSystemContextForCS
import com.huawei.cangjie.resolve.calls.inference.components.ClassicConstraintSystemUtilContext
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
import com.intellij.psi.search.GlobalSearchScope

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
//    useImpl<ContractDeserializerImpl>()
//    useImpl<CompilerDeserializationConfiguration>()
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
//    controlFlowInformationProviderFactory: ControlFlowInformationProvider.Factory,
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
//    useInstance(controlFlowInformationProviderFactory)
//    useInstance(InlineConstTracker.DoNothing)
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

    absentDescriptorHandlerClass: Class<out AbsentDescriptorHandler>? = null

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

//    val builtIns = context.module.builtIns
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
//    declarationProviderFactory: DeclarationProviderFactory,

    languageVersionSettings: LanguageVersionSettings,
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
        languageVersionSettings,

        absentDescriptorHandlerClass = BasicAbsentDescriptorHandler::class.java.takeIf { absentDescriptorHandler == null }
    )

    useInstanceIfNotNull(absentDescriptorHandler)

    useInstance(cangjieCodeAnalyzer)

    useInstance(bodyResolveCache)

    useImpl<LazyTopDownAnalyzer>()
    useImpl<DeclarationScopeProviderForLocalClassifierAnalyzer>()
    useImpl<AnnotationResolverImpl>()

}
