package com.huawei.cangjie.resolve

import com.huawei.cangjie.container.get
import com.huawei.cangjie.context.SimpleGlobalContext
import com.huawei.cangjie.context.withModule
import com.huawei.cangjie.context.withProject
import com.huawei.cangjie.descriptors.BindingTrace
import com.huawei.cangjie.descriptors.FunctionDescriptor
import com.huawei.cangjie.frontend.createContainerForBodyResolve
import com.huawei.cangjie.idea.projectStructure.languageVersionSettings
import com.huawei.cangjie.name.Name
import com.huawei.cangjie.psi.*
import com.huawei.cangjie.psi.psiUtil.getElementTextWithContext
import com.huawei.cangjie.resolve.calls.smartcasts.DataFlowInfo
import com.huawei.cangjie.resolve.lazy.*
import com.huawei.cangjie.resolve.lazy.BodyResolveMode.*
import com.huawei.cangjie.utils.getNonStrictParentOfType
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.psi.util.*
import com.intellij.util.containers.CollectionFactory

class ResolveElementCache(
    private val resolveSession: ResolveSession,
    private val project: Project,

//    private val codeFragmentAnalyzer: CodeFragmentAnalyzer
) : BodyResolveCache {

    private fun findElementOfAdditionalResolve(element: CjElement, bodyResolveMode: BodyResolveMode): CjElement? {
        if (element is CjAnnotationEntry && bodyResolveMode == PARTIAL_NO_ADDITIONAL)
            return element

        val elementOfAdditionalResolve = element.findTopmostParentInFile {
            it is CjNamedFunction ||
                    it is CjAnonymousInitializer ||
                    it is CjPrimaryConstructor ||
                    it is CjSecondaryConstructor ||
                    it is CjProperty ||
                    it is CjSuperTypeList ||

                    it is CjImportList ||
                    it is CjAnnotationEntry ||
                    it is CjTypeParameter ||
                    it is CjTypeConstraint ||
                    it is CjPackageDirective ||
                    it is CjCodeFragment ||
                    it is CjTypeAlias ||
                    it is CjDestructuringDeclaration
        } as CjElement?

        when (elementOfAdditionalResolve) {
            null -> {
                // Case of JetAnnotationEntry on top level class
                if (element is CjAnnotationEntry) {
                    return element
                }

//                if (element is CjFileAnnotationList) {
//                    return element
//                }

                // Case of pure script element, like val (x, y) = ... on top of the script
//                return element.findParentOfType<CjScript>(strict = false)
                return null
            }

            is CjPackageDirective -> return element

            is CjDeclaration -> {
                if (element is CjParameter && !CjPsiUtil.isLocal(element)) {
                    return null
                }

                return elementOfAdditionalResolve
            }

            else -> return elementOfAdditionalResolve
        }
    }


//    // drop whole cache after change "out of code block", each entry is checked with own modification stamp
//    private val fullResolveCache: CachedValue<MutableMap<CjElement, CachedFullResolve>> =
//        CachedValuesManager.getManager(project).createCachedValue(
//            CachedValueProvider {
//                CachedValueProvider.Result.create(
//                    CollectionFactory.createConcurrentWeakKeySoftValueMap(),
//                    cacheDependencies
//                )
//            },
//            false
//        )
//    private fun getElementsAdditionalResolve(
//        resolveElement: CjElement,
//        contextElement: CjElement?,
//        contextElements: Collection<CjElement>? = null,
//        bodyResolveMode: BodyResolveMode
//    ): BindingContext {
//        if (contextElements == null && contextElement == null) {
//            assert(bodyResolveMode == BodyResolveMode.FULL)
//        }
////        if (bodyResolveMode != BodyResolveMode.FULL &&
////            bodyResolveMode != BodyResolveMode.PARTIAL_FOR_COMPLETION &&
////            (!isUnitTestMode() || forceFullAnalysisModeInTests) &&
////            forcedFullResolveOnHighlighting && DaemonCodeAnalyzerStatusService.getInstance(project).daemonRunning
////        ) {
////            val virtualFile = resolveElement.containingFile.virtualFile
////            // applicable for real (physical) files only
////            if (virtualFile != null && FileEditorManager.getInstance(resolveElement.project)?.selectedFiles?.any { it == virtualFile } == true) {
////                return getElementsAdditionalResolve(
////                    resolveElement,
////                    contextElement,
////                    contextElements,
////                    BodyResolveMode.FULL
////                )
////            }
////        }
//
//        // check if full additional resolve already performed and is up-to-date
//        val fullResolveMap = fullResolveCache.value
//        val cachedFullResolve = fullResolveMap[resolveElement]
//        if (cachedFullResolve != null) {
//            if (cachedFullResolve.isUpToDate(resolveElement)) {
//                return cachedFullResolve.bindingContext
//            } else {
//                fullResolveMap.remove(resolveElement) // remove outdated cache entry
//            }
//        }
//
//        when (bodyResolveMode) {
//            BodyResolveMode.FULL -> {
//                val bindingContext = performElementAdditionalResolve(resolveElement, null, BodyResolveMode.FULL).first
//                fullResolveMap[resolveElement] = CachedFullResolve(bindingContext, resolveElement)
//                return bindingContext
//            }
//
//            else -> {
//                if (resolveElement !is CjDeclaration) {
//                    return getElementsAdditionalResolve(
//                        resolveElement,
//                        contextElement = null,
//                        contextElements = null,
//                        bodyResolveMode = BodyResolveMode.FULL
//                    )
//                }
//
//                val file = resolveElement.getContainingCjFile()
//                val statementsToResolve =
//                    contextElement?.run {
//                        listOf(
//                            PartialBodyResolveFilter.findStatementToResolve(
//                                this,
//                                resolveElement
//                            )
//                        )
//                    } ?: contextElements!!.map { PartialBodyResolveFilter.findStatementToResolve(it, resolveElement) }
//                        .distinct()
//                val statementsToResolveByCjFile =
//                    statementsToResolve.groupBy { (it ?: resolveElement).getContainingCjFile() }
//                val cachedResults =
//                    statementsToResolveByCjFile.flatMap { (file, expressions) ->
//                        val partialBodyResolveCacheValue = partialBodyResolveCache.value
//                        val expressionsMap = synchronized(partialBodyResolveCacheValue) {
//                            partialBodyResolveCacheValue[file]
//                        }
//                        expressions.map { expressionsMap[it ?: resolveElement] }
//                    }
//
//                // a bit of problem here that several threads come to analyze same resolveElement
//
//                if (cachedResults.all { it != null && it.isUpToDate(file, bodyResolveMode) }) {
//                    // partial resolve is already cached for these statements
//                    return CompositeBindingContext.create(cachedResults.map { it!!.bindingContext }.distinct())
//                }
//
//                val (bindingContext, statementFilter) = performElementAdditionalResolve(
//                    resolveElement,
//                    contextElements ?: listOfNotNull(contextElement),
//                    bodyResolveMode
//                )
//
//                if (statementFilter == StatementFilter.NONE &&
//                    bodyResolveMode.doControlFlowAnalysis && !bodyResolveMode.bindingTraceFilter.ignoreDiagnostics
//                ) {
//                    // Without statement filter, we analyze everything, so we can count partial resolve result as full resolve
//                    // But we can do this only if our resolve mode also provides *both* CFA and diagnostics
//                    // This is true only for PARTIAL_WITH_DIAGNOSTICS resolve mode
//                    fullResolveMap[resolveElement] = CachedFullResolve(bindingContext, resolveElement)
//                    return bindingContext
//                }
//
//                val partialBodyResolveCacheValue = partialBodyResolveCache.value
//                val expressionsMap = synchronized(partialBodyResolveCacheValue) {
//                    partialBodyResolveCacheValue[file]
//                }
//
//                val resolveToCache = CachedPartialResolve(bindingContext, file, bodyResolveMode)
//
//                if (statementFilter is PartialBodyResolveFilter) {
//                    for (statement in statementFilter.allStatementsToResolve) {
//                        if (bindingContext[BindingContext.PROCESSED, statement] == true) {
//                            expressionsMap.putIfAbsent(statement, resolveToCache)?.let { oldResolveToCache ->
//                                if (!oldResolveToCache.isUpToDate(file, bodyResolveMode)) {
//                                    expressionsMap[statement] = resolveToCache
//                                } else if (!oldResolveToCache.mode.doesNotLessThan(bodyResolveMode)) {
//                                    expressionsMap.replace(statement, oldResolveToCache, resolveToCache)
//                                }
//                                return@let
//                            }
//                        }
//                    }
//                }
//
//                // we use the whole declaration key in the map to obtain resolve not inside any block (e.g. default parameter values)
//                expressionsMap[resolveElement] = resolveToCache
//
//                return bindingContext
//            }
//        }
//    }

    fun resolveToElement(element: CjElement, bodyResolveMode: BodyResolveMode = FULL): BindingContext {
        val elementOfAdditionalResolve = findElementOfAdditionalResolve(element, bodyResolveMode)

        val bindingContext = if (elementOfAdditionalResolve != null) {
            if (elementOfAdditionalResolve is CjParameter) {
                throw AssertionError(
                    "ResolveElementCache: Element of additional resolve should not be CjParameter: " +
                            "${elementOfAdditionalResolve.text} for context element ${element.text}"
                )
            }
//            resolveSession.bindingContext
            getElementsAdditionalResolve(elementOfAdditionalResolve, element, null, bodyResolveMode)
        } else {
            element.getNonStrictParentOfType<CjDeclaration>()?.takeIf {
                it !is CjAnonymousInitializer && it !is CjDestructuringDeclaration && it !is CjDestructuringDeclarationEntry
            }?.let { resolveSession.resolveToDescriptor(it) }
            resolveSession.bindingContext
        }

        return bindingContext

    }

    private fun getElementsAdditionalResolve(
        resolveElement: CjElement,
        contextElement: CjElement?,
        contextElements: Collection<CjElement>? = null,
        bodyResolveMode: BodyResolveMode
    ): BindingContext {

//        TODO return bindingContext
//        if (contextElements == null && contextElement == null) {
//            assert(bodyResolveMode == BodyResolveMode.FULL)
//        }

        // Force perform FULL analysis to avoid redundant analysis for the current selected files.
        if (bodyResolveMode != FULL &&
            bodyResolveMode != PARTIAL_FOR_COMPLETION
//            &&
//            (!isUnitTestMode() || forceFullAnalysisModeInTests) &&
//            forcedFullResolveOnHighlighting && DaemonCodeAnalyzerStatusService.getInstance(project).daemonRunning
        ) {
            val virtualFile = resolveElement.containingFile.virtualFile
            // applicable for real (physical) files only
            if (virtualFile != null && FileEditorManager.getInstance(resolveElement.project)?.selectedFiles?.any { it == virtualFile } == true) {
                return getElementsAdditionalResolve(resolveElement, contextElement, contextElements, FULL)
            }
        }


        when (bodyResolveMode) {
            FULL -> {
                val bindingContext = performElementAdditionalResolve(resolveElement, null, FULL).first
//                fullResolveMap[resolveElement] = CachedFullResolve(bindingContext, resolveElement)
                return bindingContext
            }

            PARTIAL_FOR_COMPLETION -> TODO()
            PARTIAL_WITH_DIAGNOSTICS -> TODO()
            PARTIAL_WITH_CFA -> TODO()
            PARTIAL -> TODO()
            PARTIAL_NO_ADDITIONAL -> TODO()
        }


        return resolveSession.bindingContext
    }

    private fun performElementAdditionalResolve(
        resolveElement: CjElement,
        contextElements: Collection<CjElement>?,
        bodyResolveMode: BodyResolveMode
    ): Pair<BindingContext, StatementFilter> {
        if (contextElements == null) {
            assert(bodyResolveMode == FULL)
        }

        val file = resolveElement.getContainingCjFile()

        var statementFilterUsed = StatementFilter.NONE

        fun createStatementFilter(): StatementFilter {
            assert(resolveElement is CjDeclaration)
            if (bodyResolveMode != FULL) {
                statementFilterUsed = PartialBodyResolveFilter(
                    contextElements!!,
                    resolveElement as CjDeclaration,
                    bodyResolveMode == PARTIAL_FOR_COMPLETION
                )
            }
            return statementFilterUsed
        }


        val trace: BindingTrace = when (resolveElement) {

            is CjNamedFunction -> functionAdditionalResolve(
                resolveSession,
                resolveElement,
                file,
                createStatementFilter(),
                bodyResolveMode.bindingTraceFilter
            )

            else -> {
                if (resolveElement.findParentOfType<CjPackageDirective>(true) != null) {
                    packageRefAdditionalResolve(resolveSession, resolveElement, bodyResolveMode.bindingTraceFilter)
                } else {
                    error("Invalid type of the topmost parent: $resolveElement\n${resolveElement.getElementTextWithContext()}")
                }
            }
        }

//        if (bodyResolveMode.doControlFlowAnalysis) {
//            analyzeControlFlow(resolveSession, resolveElement, trace)
//        }

        return Pair(trace.bindingContext, statementFilterUsed)
    }

    private fun createBodyResolver(
        resolveSession: ResolveSession,
        trace: BindingTrace,
        file: CjFile,
        statementFilter: StatementFilter
    ): BodyResolver {
        val globalContext = SimpleGlobalContext(resolveSession.storageManager, resolveSession.exceptionTracker)
        val module = resolveSession.moduleDescriptor
        return createContainerForBodyResolve(
            globalContext.withProject(file.project).withModule(module),
            trace,
//            targetPlatform,
            statementFilter,
            PlatformDependentAnalyzerServicesImpl,
//            targetPlatform.findAnalyzerServices(file.project),
            file.languageVersionSettings,
//            IdeaModuleStructureOracle(),
//            IdeSealedClassInheritorsProvider,
//            ControlFlowInformationProviderImpl.Factory,
            IdeaAbsentDescriptorHandler(resolveSession.declarationProviderFactory)
        ).get()
    }

    private fun functionAdditionalResolve(
        resolveSession: ResolveSession, namedFunction: CjNamedFunction, file: CjFile,
        statementFilter: StatementFilter,
        bindingTraceFilter: BindingTraceFilter
    ): BindingTrace {
        val trace = createDelegatingTrace(namedFunction, bindingTraceFilter)

        val scope = resolveSession.declarationScopeProvider.getResolutionScopeForDeclaration(namedFunction)
        val functionDescriptor = resolveSession.resolveToDescriptor(namedFunction) as FunctionDescriptor
        ForceResolveUtil.forceResolveAllContents(functionDescriptor)

        val bodyResolver = createBodyResolver(resolveSession, trace, file, statementFilter)
        bodyResolver.resolveFunctionBody(DataFlowInfo.EMPTY, trace, namedFunction, functionDescriptor, scope, null)

//        forceResolveAnnotationsInside(namedFunction)

        return trace
    }

    /*
    Note that currently we *have* to re-create container with custom trace in order to disallow resolution of
    bodies in top-level trace (trace from DI-container).
    Resolving bodies in top-level trace may lead to memory leaks and incorrect resolution, because top-level
    trace isn't invalidated on in-block modifications (while body resolution surely does)
    */
    private fun createDelegatingTrace(resolveElement: CjElement, filter: BindingTraceFilter): BindingTrace {
        return resolveSession.storageManager.createSafeTrace(
            BindingTraceForBodyResolve(
                resolveSession.bindingContext,
                AnalyzingUtils.formDebugNameForBindingTrace("trace to resolve element", resolveElement),
                filter
            )
        )
    }

    private fun packageRefAdditionalResolve(
        resolveSession: ResolveSession, ktElement: CjElement,
        bindingTraceFilter: BindingTraceFilter
    ): BindingTrace {
        val trace = createDelegatingTrace(ktElement, bindingTraceFilter)

        if (ktElement is CjSimpleNameExpression) {
            val header = ktElement.findParentOfType<CjPackageDirective>(true)!!

            if (Name.isValidIdentifier(ktElement.getReferencedName())) {
//                if (trace.bindingContext[BindingContext.REFERENCE_TARGET, ktElement] == null) {
//                    val fqName = header.getFqName(ktElement)
//                    val packageDescriptor = resolveSession.moduleDescriptor.getPackage(fqName)
//                    trace.record(BindingContext.REFERENCE_TARGET, ktElement, packageDescriptor)
//                }
            }
        }

        return trace
    }

    private fun getElementsAdditionalResolve(
        resolveElement: CjElement,
        contextElements: Collection<CjElement>?,
        bodyResolveMode: BodyResolveMode
    ): BindingContext = getElementsAdditionalResolve(resolveElement, null, contextElements, bodyResolveMode)

    override fun resolveFunctionBody(function: CjNamedFunction): BindingContext {
        return getElementsAdditionalResolve(function, null, FULL)
    }
}
