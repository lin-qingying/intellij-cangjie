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

package org.cangnova.cangjie.frontend

import org.cangnova.cangjie.resolve.controlFlow.ControlFlowInformationProvider
import org.cangnova.cangjie.resolve.controlFlow.ControlFlowInformationProviderImpl
import org.cangnova.cangjie.config.LanguageVersionSettings
import org.cangnova.cangjie.container.*
import org.cangnova.cangjie.context.ModuleContext
import org.cangnova.cangjie.extensions.TypeAttributeTranslatorExtension
import org.cangnova.cangjie.resolve.*
import org.cangnova.cangjie.resolve.calls.components.ClassicTypeSystemContextForCS
import org.cangnova.cangjie.resolve.calls.inference.components.ClassicConstraintSystemUtilContext
import org.cangnova.cangjie.resolve.calls.smartcasts.DataFlowValueFactoryImpl
import org.cangnova.cangjie.resolve.calls.tower.CangJieResolutionStatelessCallbacksImpl
import org.cangnova.cangjie.resolve.lazy.*
import org.cangnova.cangjie.resolve.lazy.declarations.DeclarationProviderFactory
import org.cangnova.cangjie.types.checker.CangJieTypePreparator
import org.cangnova.cangjie.types.checker.CangJieTypeRefiner
import org.cangnova.cangjie.types.checker.NewCangJieTypeCheckerImpl
import org.cangnova.cangjie.types.expressions.DeclarationScopeProviderForLocalClassifierAnalyzer
import org.cangnova.cangjie.types.expressions.LocalLazyDeclarationResolver
import com.intellij.psi.search.GlobalSearchScope
import org.cangnova.cangjie.extensions.StorageComponentContainerContributor
import org.cangnova.cangjie.resolve.binding.BindingTrace
import org.cangnova.cangjie.resolve.scopes.optimization.OptimizingOptions
import org.cangnova.cangjie.serialization.deserialization.CompilerDeserializationConfiguration

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
    useImpl<CompilerDeserializationConfiguration>()
//
    useImpl<ClassicTypeSystemContextForCS>()
    useImpl<ClassicConstraintSystemUtilContext>()
//    useInstance(ProgressManagerBasedCancellationChecker)
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


    useInstance(analyzerServices)




    analyzerServices.platformConfigurator.configureModuleComponents(this)
    analyzerServices.platformConfigurator.configureModuleDependentCheckers(this)

    useInstance(TypeAttributeTranslatorExtension.createTranslators(moduleContext.project))

    for (extension in StorageComponentContainerContributor.EP_NAME.extensionList ) {
        extension.registerModuleComponents(this,   moduleContext.module)
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

//    useInstance(builtIns.customizer)
//    useImpl<CangJieBuiltInsPackageFragmentProvider>()
//    useInstance(VirtualFileFinderFactory.getInstance(context.project).create(moduleContentScope))

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
