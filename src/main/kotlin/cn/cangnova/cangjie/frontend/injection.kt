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

package cn.cangnova.cangjie.frontend

import cn.cangnova.cangjie.resolve.controlFlow.ControlFlowInformationProvider
import cn.cangnova.cangjie.resolve.controlFlow.ControlFlowInformationProviderImpl
import cn.cangnova.cangjie.config.LanguageVersionSettings
import cn.cangnova.cangjie.container.*
import cn.cangnova.cangjie.context.ModuleContext
import cn.cangnova.cangjie.descriptors.BindingTrace
import cn.cangnova.cangjie.extensions.TypeAttributeTranslatorExtension
import cn.cangnova.cangjie.resolve.*
import cn.cangnova.cangjie.resolve.calls.components.ClassicTypeSystemContextForCS
import cn.cangnova.cangjie.resolve.calls.inference.components.ClassicConstraintSystemUtilContext
import cn.cangnova.cangjie.resolve.calls.smartcasts.DataFlowValueFactoryImpl
import cn.cangnova.cangjie.resolve.calls.tower.CangJieResolutionStatelessCallbacksImpl
import cn.cangnova.cangjie.resolve.lazy.*
import cn.cangnova.cangjie.resolve.lazy.declarations.DeclarationProviderFactory
import cn.cangnova.cangjie.types.checker.CangJieTypePreparator
import cn.cangnova.cangjie.types.checker.CangJieTypeRefiner
import cn.cangnova.cangjie.types.checker.NewCangJieTypeCheckerImpl
import cn.cangnova.cangjie.types.expressions.DeclarationScopeProviderForLocalClassifierAnalyzer
import cn.cangnova.cangjie.types.expressions.LocalLazyDeclarationResolver
import cn.cangnova.cangjie.utils.ProgressManagerBasedCancellationChecker
import com.intellij.psi.search.GlobalSearchScope
import cn.cangnova.cangjie.builtins.CangJieBuiltIns
import cn.cangnova.cangjie.contracts.ContractDeserializerImpl
import cn.cangnova.cangjie.extensions.StorageComponentContainerContributor
import cn.cangnova.cangjie.ide.vfilefinder.VirtualFileFinderFactory
import cn.cangnova.cangjie.platform.TargetPlatform
import cn.cangnova.cangjie.platform.TargetPlatformVersion
import cn.cangnova.cangjie.resolve.descriptorUtil.isTypeRefinementEnabled
import cn.cangnova.cangjie.resolve.scopes.optimization.OptimizingOptions
import cn.cangnova.cangjie.serialization.deserialization.CangJieBuiltInsPackageFragmentProvider
import cn.cangnova.cangjie.serialization.deserialization.CompilerDeserializationConfiguration

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
