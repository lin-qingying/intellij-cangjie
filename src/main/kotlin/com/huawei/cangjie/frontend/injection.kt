package com.huawei.cangjie.frontend

//fun StorageComponentContainer.configureModule(
//    moduleContext: ModuleContext,
//    platform: TargetPlatform,
//    analyzerServices: PlatformDependentAnalyzerServices,
//    trace: BindingTrace,
//    languageVersionSettings: LanguageVersionSettings,
//    sealedProvider: SealedClassInheritorsProvider = CliSealedClassInheritorsProvider,
//    optimizingOptions: OptimizingOptions?,
//    absentDescriptorHandlerClass: Class<out AbsentDescriptorHandler>?
//) {
//    useInstance(sealedProvider)
//    useInstance(moduleContext)
//    useInstance(moduleContext.module)
//    useInstance(moduleContext.project)
//    useInstance(moduleContext.storageManager)
//    useInstance(moduleContext.module.builtIns)
//    useInstance(trace)
//    useInstance(languageVersionSettings)
//
//    useInstanceIfNotNull(optimizingOptions)
//
//    if (absentDescriptorHandlerClass != null) {
//        registerSingleton(absentDescriptorHandlerClass)
//    }
//
//    useInstance(platform)
//    useInstance(analyzerServices)
//
//    val nonTrivialPlatformVersion = platform
//        .mapNotNull { it.targetPlatformVersion.takeIf { it != TargetPlatformVersion.NoVersion } }
//        .singleOrNull()
//
//    useInstance(nonTrivialPlatformVersion ?: TargetPlatformVersion.NoVersion)
//
//    analyzerServices.platformConfigurator.configureModuleComponents(this)
//    analyzerServices.platformConfigurator.configureModuleDependentCheckers(this)
//
//    useInstance(TypeAttributeTranslatorExtension.createTranslators(moduleContext.project))
//
//    for (extension in StorageComponentContainerContributor.getInstances(moduleContext.project)) {
//        extension.registerModuleComponents(this, platform, moduleContext.module)
//    }
//
//    useImpl<NewKotlinTypeCheckerImpl>()
//
//    if (moduleContext.module.isTypeRefinementEnabled()) {
//        useImpl<KotlinTypeRefinerImpl>()
//    } else {
//        useInstance(KotlinTypeRefiner.Default)
//    }
//
//    useInstance(KotlinTypePreparator.Default)
//
//    configurePlatformIndependentComponents()
//}
//
//fun createContainerForLazyBodyResolve(
//    moduleContext: ModuleContext,
//    kotlinCodeAnalyzer: CangJieCodeAnalyzer,
//    bindingTrace: BindingTrace,
//    platform: TargetPlatform,
//    bodyResolveCache: BodyResolveCache,
//    analyzerServices: PlatformDependentAnalyzerServices,
//    languageVersionSettings: LanguageVersionSettings,
//    moduleStructureOracle: ModuleStructureOracle,
//    mainFunctionDetectorFactory: MainFunctionDetector.Factory,
//    sealedProvider: SealedClassInheritorsProvider,
//    controlFlowInformationProviderFactory: ControlFlowInformationProvider.Factory,
//    optimizingOptions: OptimizingOptions?,
//    absentDescriptorHandler: AbsentDescriptorHandler?,
//): StorageComponentContainer = createContainer("LazyBodyResolve", analyzerServices) {
//    configureModule(
//        moduleContext,
//        platform,
//        analyzerServices,
//        bindingTrace,
//        languageVersionSettings,
//        sealedProvider,
//        optimizingOptions,
//        absentDescriptorHandlerClass = BasicAbsentDescriptorHandler::class.java.takeIf { absentDescriptorHandler == null }
//    )
//    useInstanceIfNotNull(absentDescriptorHandler)
//    useInstance(mainFunctionDetectorFactory)
//    useInstance(kotlinCodeAnalyzer)
//    useInstance(kotlinCodeAnalyzer.fileScopeProvider)
//    useInstance(bodyResolveCache)
////    useImpl<AnnotationResolverImpl>()
//    useImpl<LazyTopDownAnalyzer>()
//    useInstance(moduleStructureOracle)
//    useInstance(controlFlowInformationProviderFactory)
//    useInstance(InlineConstTracker.DoNothing)
//
//    // All containers except common inject ExpectedActualDeclarationChecker, so for common we do that
//    // explicitly.
//    // Note that it is not possible to move this code to [CommonPlatformConfigurator], because during
//    // compilation of common-module to metadata we should skip those checks
//    if (platform.isCommon()) useImpl<ExpectedActualDeclarationChecker>()
//}