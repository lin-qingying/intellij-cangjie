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

package cn.cangnova.cangjie.analyzer

import cn.cangnova.cangjie.analyzer.components.CangJieAnalysisScopeProvider
import cn.cangnova.cangjie.analyzer.components.CangJieAnalysisScopeProviderImpl
import cn.cangnova.cangjie.analyzer.components.CjAnalysisScopeProviderMixIn
import cn.cangnova.cangjie.analyzer.lifetime.CjLifetimeOwner
import cn.cangnova.cangjie.analyzer.lifetime.CjLifetimeToken
import cn.cangnova.cangjie.builtins.CangJieBuiltIns
import cn.cangnova.cangjie.config.LanguageVersionSettings
import cn.cangnova.cangjie.descriptors.CallableDescriptor
import cn.cangnova.cangjie.descriptors.MemberDescriptor
import cn.cangnova.cangjie.descriptors.ModuleDescriptor
import cn.cangnova.cangjie.descriptors.ValueParameterDescriptor
import cn.cangnova.cangjie.descriptors.synthetic.SyntheticMemberDescriptor
import cn.cangnova.cangjie.ide.FrontendInternals
import cn.cangnova.cangjie.ide.base.projectStructure.RootKindFilter
import cn.cangnova.cangjie.ide.base.projectStructure.matches
import cn.cangnova.cangjie.ide.projectStructure.moduleInfo
import cn.cangnova.cangjie.psi.CjCodeFragment
import cn.cangnova.cangjie.psi.CjElement
import cn.cangnova.cangjie.psi.CjFile
import cn.cangnova.cangjie.psi.ValueArgument
import cn.cangnova.cangjie.resolve.BindingContext
import cn.cangnova.cangjie.resolve.DescriptorToSourceUtils
import cn.cangnova.cangjie.resolve.ResolutionFacade
import cn.cangnova.cangjie.resolve.caches.CangJieCacheService
import cn.cangnova.cangjie.resolve.caches.getResolutionFacade
import cn.cangnova.cangjie.resolve.calls.CallResolver
import cn.cangnova.cangjie.resolve.calls.inference.ConstraintSystemBuilderImpl
import cn.cangnova.cangjie.resolve.calls.model.DefaultValueArgument
import cn.cangnova.cangjie.resolve.calls.model.ResolvedCall
import cn.cangnova.cangjie.resolve.calls.model.VariableAsFunctionResolvedCallImpl
import cn.cangnova.cangjie.resolve.calls.results.*
import cn.cangnova.cangjie.resolve.calls.tower.CangJieToResolvedCallTransformer
import cn.cangnova.cangjie.resolve.deprecation.DeprecationResolver
import cn.cangnova.cangjie.resolve.lazy.BodyResolveMode
import cn.cangnova.cangjie.resolve.lazy.ResolveSession
import cn.cangnova.cangjie.types.CangJieType
import cn.cangnova.cangjie.types.checker.CangJieTypeRefiner
import cn.cangnova.cangjie.utils.CancellationChecker
import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope

@Suppress("AnalysisApiMissingLifetimeCheck")
abstract class CangJieAnalysisSession(final override val token: CjLifetimeToken) : CjLifetimeOwner,
    CjOriginalPsiProviderMixIn, CjAnalysisScopeProviderMixIn {

    override val analysisSession: CangJieAnalysisSession get() = this
    abstract val useSiteModule: CjModule


    internal val analysisScopeProvider: CangJieAnalysisScopeProvider get() = analysisScopeProviderImpl
    protected abstract val analysisScopeProviderImpl: CangJieAnalysisScopeProvider
    internal val originalPsiProvider: CangJieOriginalPsiProvider get() = originalPsiProviderImpl
    protected abstract val originalPsiProviderImpl: CangJieOriginalPsiProvider
}

class CjAnalysisContext(
    facade: CjAnalysisFacade,
    val resolveSession: ResolveSession,
    val deprecationResolver: DeprecationResolver,
    val callResolver: CallResolver,
    val cangjieToResolvedCallTransformer: CangJieToResolvedCallTransformer,
    val overloadingConflictResolver: OverloadingConflictResolver<ResolvedCall<*>>,
    val cangjieTypeRefiner: CangJieTypeRefiner,
    val token: CjLifetimeToken,
) : CjAnalysisFacade by facade {
    val builtIns: CangJieBuiltIns
        get() = resolveSession.moduleDescriptor.builtIns

    val languageVersionSettings: LanguageVersionSettings
        get() = resolveSession.languageVersionSettings
}

interface CjAnalysisFacade {
    companion object {
        fun getInstance(project: Project): CjAnalysisFacade {
            return project.getService(CjAnalysisFacade::class.java)
        }
    }

    fun getAnalysisContext(element: CjElement, token: CjLifetimeToken): CjAnalysisContext

    fun getAnalysisContext(cjModule: CjModule, token: CjLifetimeToken): CjAnalysisContext

    fun analyze(elements: List<CjElement>, mode: AnalysisMode = AnalysisMode.FULL): BindingContext

    fun analyze(element: CjElement, mode: AnalysisMode = AnalysisMode.FULL): BindingContext {
        return analyze(listOf(element), mode)
    }

//    fun getOrigin(file: VirtualFile): CjSymbolOrigin

    enum class AnalysisMode {
        ALL_COMPILER_CHECKS,
        FULL,
        PARTIAL_WITH_DIAGNOSTICS,
        PARTIAL
    }
}

fun <RC : ResolvedCall<*>> RC.createFlatSignature(): FlatSignature<RC> {
    val originalDescriptor = candidateDescriptor.original
    val originalValueParameters = originalDescriptor.valueParameters

    var numDefaults = 0
    val valueArgumentToParameterType = HashMap<ValueArgument, CangJieType>()
    for ((valueParameter, resolvedValueArgument) in valueArguments.entries) {
        if (resolvedValueArgument is DefaultValueArgument) {
            numDefaults++
        } else {
            val originalValueParameter = originalValueParameters[valueParameter.index]
            val parameterType = originalValueParameter.argumentValueType
            for (valueArgument in resolvedValueArgument.arguments) {
                valueArgumentToParameterType[valueArgument] = parameterType
            }
        }
    }

    return FlatSignature.create(
        this,
        originalDescriptor,
        numDefaults,
        call.valueArguments.map { valueArgumentToParameterType[it] })
}

val ValueParameterDescriptor.argumentValueType get() = type
fun <T> FlatSignature.Companion.create(
    origin: T,
    descriptor: CallableDescriptor,
    numDefaults: Int,
    parameterTypes: List<CangJieType?>
): FlatSignature<T> {
    val extensionReceiverType = descriptor.extensionReceiverParameter?.type
    val contextReceiverTypes = descriptor.contextReceiverParameters.mapNotNull { it.type }

    return FlatSignature(
        origin,
        descriptor.typeParameters,
        valueParameterTypes = contextReceiverTypes + listOfNotNull(extensionReceiverType) + parameterTypes,
        hasExtensionReceiver = extensionReceiverType != null,
        contextReceiverCount = contextReceiverTypes.size,
        hasVarargs = descriptor.valueParameters.any { it.varargElementType != null },
        numDefaults = numDefaults,
        isExpect = descriptor is MemberDescriptor && descriptor.isExpect,
        isSyntheticMember = descriptor is SyntheticMemberDescriptor<*>
    )
}

@JvmName("createWithConvertedTypes")
fun <T> FlatSignature.Companion.create(
    origin: T,
    descriptor: CallableDescriptor,
    numDefaults: Int,
    parameterTypes: List<TypeWithConversion?>,
): FlatSignature<T> {
    val extensionReceiverType = descriptor.extensionReceiverParameter?.type
    val contextReceiverTypes = descriptor.contextReceiverParameters.mapNotNull { TypeWithConversion(it.type) }

    return FlatSignature(
        origin,
        descriptor.typeParameters,
        valueParameterTypes = contextReceiverTypes + extensionReceiverType?.let { listOf(TypeWithConversion(it)) }
            .orEmpty() + parameterTypes,
        hasExtensionReceiver = extensionReceiverType != null,
        contextReceiverCount = contextReceiverTypes.size,
        hasVarargs =false /*descriptor.valueParameters.any { it.varargElementType != null }*/,
        numDefaults = numDefaults,
        isExpect = descriptor is MemberDescriptor && descriptor.isExpect,
        isSyntheticMember = descriptor is SyntheticMemberDescriptor<*>
    )
}

fun createOverloadingConflictResolver(
    builtIns: CangJieBuiltIns,
    module: ModuleDescriptor,
    specificityComparator: TypeSpecificityComparator,
    platformOverloadsSpecificityComparator: PlatformOverloadsSpecificityComparator,
    cancellationChecker: CancellationChecker,
    cangjieTypeRefiner: CangJieTypeRefiner,
) = OverloadingConflictResolver(
    builtIns,
    module,
    specificityComparator,
    platformOverloadsSpecificityComparator,
    cancellationChecker,
    ResolvedCall<*>::getResultingDescriptor,
    ConstraintSystemBuilderImpl.Companion::forSpecificity,
    ResolvedCall<*>::createFlatSignature,
    { (it as? VariableAsFunctionResolvedCallImpl)?.variableCall },
    { DescriptorToSourceUtils.descriptorToDeclaration(it) != null },
    null,
    cangjieTypeRefiner
)

internal class IdeCjAnalysisFacade(private val project: Project) : CjAnalysisFacade {
    override fun getAnalysisContext(element: CjElement, token: CjLifetimeToken): CjAnalysisContext {
        val resolutionFacade = element.getResolutionFacade()
        return resolutionFacade.getAnalysisContext(token)
    }

    @OptIn(FrontendInternals::class)
    private fun ResolutionFacade.getOverloadingConflictResolver(): OverloadingConflictResolver<ResolvedCall<*>> {
        val moduleDescriptor = moduleDescriptor

        return createOverloadingConflictResolver(
            moduleDescriptor.builtIns,
            moduleDescriptor,
            getFrontendService(TypeSpecificityComparator::class.java),
            getFrontendService(PlatformOverloadsSpecificityComparator::class.java),
            getFrontendService(CancellationChecker::class.java),
            getFrontendService(CangJieTypeRefiner::class.java),
        )
    }

    @OptIn(FrontendInternals::class)
    private fun ResolutionFacade.getAnalysisContext(token: CjLifetimeToken): CjAnalysisContext {
        return CjAnalysisContext(
            facade = this@IdeCjAnalysisFacade,
            resolveSession = getFrontendService(ResolveSession::class.java),
            deprecationResolver = getFrontendService(DeprecationResolver::class.java),
            callResolver = getFrontendService<CallResolver>(CallResolver::class.java),
            cangjieToResolvedCallTransformer = getFrontendService(CangJieToResolvedCallTransformer::class.java),
            overloadingConflictResolver = getOverloadingConflictResolver(),
            cangjieTypeRefiner = getFrontendService(CangJieTypeRefiner::class.java),
            token = token,
        )
    }

    override fun getAnalysisContext(cjModule: CjModule, token: CjLifetimeToken): CjAnalysisContext {
        val moduleInfo = cjModule.moduleInfo
        val resolutionFacade =
            CangJieCacheService.getInstance(project).getResolutionFacadeByModuleInfo(moduleInfo)
        return resolutionFacade.getAnalysisContext(token)
    }

    private fun getResolutionFacade(elements: List<CjElement>): ResolutionFacade? {
        val cangjieCacheService = CangJieCacheService.getInstance(project)

        if (elements.size == 1) {
            cangjieCacheService.getResolutionFacade(elements.single())
        }

        val files = elements
            .asSequence()
            .mapNotNull { it.containingFile }
            .mapNotNull { if (it is CjCodeFragment) it.context?.containingFile else it }
            .filterIsInstance<CjFile>()
            .distinct()
            .toList()

        if (files.isEmpty()) {
            return null
        }


        // The following logic is taken from 'CangJieCacheServiceImpl.filterNotInProjectSource()'
        val moduleInfo = files.first().moduleInfo
        val specialFiles = files
            .filterNot { RootKindFilter.projectSources.matches(it) && moduleInfo.contentScope.contains(it.virtualFile) }

        if (specialFiles.isNotEmpty()) {
            // 'CangJieCacheServiceImpl' is bad in getting a resolution facade for multiple files outside the so-called 'main' module root.
            // Here we artificially choose the facade for the first element. It's not quite correct, still it's better than nothing.
            return cangjieCacheService.getResolutionFacade(specialFiles.first())
        }

        return cangjieCacheService.getResolutionFacade(files)
    }

    override fun analyze(elements: List<CjElement>, mode: CjAnalysisFacade.AnalysisMode): BindingContext {
        val resolutionFacade = getResolutionFacade(elements) ?: return BindingContext.EMPTY

        if (mode == CjAnalysisFacade.AnalysisMode.ALL_COMPILER_CHECKS) {
            return resolutionFacade.analyzeWithAllCompilerChecks(elements).bindingContext
        }

        @Suppress("CangJieConstantConditions")
        val bodyResolveMode = when (mode) {
            CjAnalysisFacade.AnalysisMode.FULL -> BodyResolveMode.FULL
            CjAnalysisFacade.AnalysisMode.PARTIAL_WITH_DIAGNOSTICS -> BodyResolveMode.PARTIAL_WITH_DIAGNOSTICS
            CjAnalysisFacade.AnalysisMode.PARTIAL -> BodyResolveMode.PARTIAL
            CjAnalysisFacade.AnalysisMode.ALL_COMPILER_CHECKS -> error("Must have been handled above")
        }

        return resolutionFacade.analyze(elements, bodyResolveMode)
    }

}

class CangJieAnalysisSessionImpl(
    val analysisContext: CjAnalysisContext,
    override val useSiteModule: CjModule,
    token: CjLifetimeToken,
    ) : CangJieAnalysisSession(token) {


    override val analysisScopeProviderImpl: CangJieAnalysisScopeProvider
        get() = CangJieAnalysisScopeProviderImpl(this, token, shadowedScope = GlobalSearchScope.EMPTY_SCOPE)
    override val originalPsiProviderImpl: CangJieOriginalPsiProvider
        get() = CangJieOriginalPsiProviderImpl(this)

}

val CjModule.moduleInfo: ModuleInfo
    get() {
//        require(this is CjModuleByModuleInfoBase)
//        return ideaModuleInfo
//        return moduleInfo
        TODO()
    }
//
//abstract class CjModuleByModuleInfoBase(moduleInfo: ModuleInfo) {
//    val ideaModuleInfo = moduleInfo
//
//    open val directRegularDependencies: List<CjModule>
//        get() = ideaModuleInfo.dependenciesWithoutSelf().map { it.toCjModule() }.toList()
//
//    open val directDependsOnDependencies: List<CjModule>
//        get() = ideaModuleInfo.expectedBy.mapNotNull { (it as? IdeaModuleInfo)?.toCjModule() }
//
//    // TODO: Implement some form of caching. Also see `ProjectStructureProviderIdeImpl.getCjModuleByModuleInfo`.
//    val transitiveDependsOnDependencies: List<CjModule>
//        get() = computeTransitiveDependsOnDependencies(directDependsOnDependencies)
//
//    open val directFriendDependencies: List<CjModule>
//        get() = ideaModuleInfo.modulesWhoseInternalsAreVisible().mapNotNull { (it as? IdeaModuleInfo)?.toCjModule() }
//
//    val platform: TargetPlatform get() = ideaModuleInfo.platform
//    val analyzerServices: PlatformDependentAnalyzerServices get() = ideaModuleInfo.analyzerServices
//
//    override fun equals(other: Any?): Boolean {
//        if (this === other) return true
//
//        if (other !is CjModuleByModuleInfoBase) return false
//        if (this::class.java != other::class.java) return false
//        return this.ideaModuleInfo == other.ideaModuleInfo
//    }
//
//    override fun hashCode(): Int {
//        return ideaModuleInfo.hashCode()
//    }
//
//    override fun toString(): String {
//        return "${this::class.java.simpleName} ${(this as CjModule).moduleDescription}"
//    }
//}
