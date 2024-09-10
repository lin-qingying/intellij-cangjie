package com.huawei.cangjie.resolve.caches

import com.google.common.collect.ImmutableMap
import com.huawei.cangjie.analyzer.AnalysisResult
import com.huawei.cangjie.container.ComponentProvider
import com.huawei.cangjie.container.get
import com.huawei.cangjie.context.GlobalContext
import com.huawei.cangjie.context.ModuleContext
import com.huawei.cangjie.context.withModule
import com.huawei.cangjie.context.withProject
import com.huawei.cangjie.descriptors.*
import com.huawei.cangjie.diagnostics.DiagnosticFactoryWithPsiElement
import com.huawei.cangjie.diagnostics.DiagnosticUtils
import com.huawei.cangjie.frontend.createContainerForLazyBodyResolve
import com.huawei.cangjie.ide.cache.trackers.clearInBlockModifications
import com.huawei.cangjie.ide.cache.trackers.inBlockModifications
import com.huawei.cangjie.ide.cache.trackers.removeInBlockModifications
import com.huawei.cangjie.ide.projectStructure.languageVersionSettings
import com.huawei.cangjie.ide.stubindex.resolve.PluginDeclarationProviderFactory
import com.huawei.cangjie.psi.*
import com.huawei.cangjie.psi.psiUtil.parentsWithSelf
import com.huawei.cangjie.resolve.*
import com.huawei.cangjie.resolve.lazy.IdeaAbsentDescriptorHandler
import com.huawei.cangjie.resolve.lazy.ResolveSession
import com.huawei.cangjie.resolve.source.getPsi
import com.huawei.cangjie.storage.CancellableSimpleLock
import com.huawei.cangjie.storage.guarded
import com.huawei.cangjie.types.CangJieType
import com.huawei.cangjie.utils.CodeFragmentUtils
import com.huawei.cangjie.utils.checkWithAttachment
import com.huawei.cangjie.utils.safeAs
import com.huawei.cangjie.utils.slicedMap.ReadOnlySlice
import com.huawei.cangjie.utils.slicedMap.WritableSlice
import com.intellij.openapi.diagnostic.ControlFlowException
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.progress.ProgressIndicatorProvider
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.IndexNotReadyException
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.ModificationTracker
import com.intellij.psi.PsiElement
import com.intellij.psi.util.findParentInFile
import com.intellij.psi.util.findTopmostParentInFile
import com.intellij.psi.util.findTopmostParentOfType
import com.intellij.psi.util.parents
import java.util.concurrent.locks.ReentrantLock

private inline fun Throwable.throwAsInvalidModuleException(crossinline action: (InvalidModuleException) -> Throwable = { it }) {
    asInvalidModuleException()?.let {
        throw action(it)
    }
}

private fun Throwable.asInvalidModuleException(): InvalidModuleException? {
    return when (this) {
        is InvalidModuleException -> this
        is AssertionError ->

            if (message?.contains("contained in his own dependencies, this is probably a misconfiguration") == true)
                InvalidModuleException(message!!)
            else null

        else -> cause?.takeIf { it != this }?.asInvalidModuleException()
    }
}

internal class PerFileAnalysisCache(val file: CjFile, componentProvider: ComponentProvider) {
    private val globalContext = componentProvider.get<GlobalContext>()
    private var fileResult: AnalysisResult? = null
    private val moduleDescriptor = componentProvider.get<ModuleDescriptor>()
    private val bodyResolveCache = componentProvider.get<BodyResolveCache>()
//    private val codeFragmentAnalyzer = componentProvider.get<CodeFragmentAnalyzer>()

    private val resolveSession = componentProvider.get<ResolveSession>()
    private val lock = ReentrantLock()
    private val cache = HashMap<PsiElement, AnalysisResult>()
    private val pluginDeclarationProviderFactory = componentProvider.get<PluginDeclarationProviderFactory>()

    private val guardLock = CancellableSimpleLock(lock,
        checkCancelled = {
            ProgressIndicatorProvider.checkCanceled()
        },
        interruptedExceptionHandler = { throw ProcessCanceledException(it) })
    internal val isValid: Boolean get() = true

    internal fun fetchAnalysisResults(element: CjElement): AnalysisResult? {
        check(element)

        if (lock.tryLock()) {
            try {
                updateFileResultFromCache()

                return fileResult?.takeIf { file.inBlockModifications.isEmpty() }
            } finally {
                lock.unlock()
            }
        }
        return null
    }

    private fun lookUp(analyzableElement: CjElement): AnalysisResult? {
        // Looking for parent elements that are already analyzed
        // Also removing all elements whose parents are already analyzed, to guarantee consistency
        val descendantsOfCurrent = arrayListOf<PsiElement>()
        val toRemove = hashSetOf<PsiElement>()

        var result: AnalysisResult? = null
        for (current in analyzableElement.parentsWithSelf) {
            val cached = cache[current]
            if (cached != null) {
                result = cached
                toRemove.addAll(descendantsOfCurrent)
                descendantsOfCurrent.clear()
            }

            descendantsOfCurrent.add(current)
        }

        cache.keys.removeAll(toRemove)

        return result
    }

    internal fun getAnalysisResults(
        element: CjElement,
        callback: DiagnosticSink.DiagnosticsCallback? = null
    ): AnalysisResult {
        check(element)
        val analyzableParent = CangJieResolveDataProvider.findAnalyzableParent(element) ?: return AnalysisResult.EMPTY
        fun handleResult(result: AnalysisResult, callback: DiagnosticSink.DiagnosticsCallback?): AnalysisResult {
            callback?.let { result.bindingContext.diagnostics.forEach(it::callback) }
            return result
        }
        return guardLock.guarded {
            // It is necessary to ignore the codeFragment used in the evaluator for compilation
            // because caching can lead to data consistency bugs (see KTIJ-22496). However, the code fragments that come from the evaluator,
            // which are not intended for compilation (e.g., for highlighting), should be cached in order to maintain the current performance of the evaluator.
            if (analyzableParent.isUsedForCompilationInEvaluator()) return@guarded performAnalyze(element, callback)

            // step 1: perform incremental analysis IF it is applicable
            getIncrementalAnalysisResult(callback)?.let {
                return@guarded handleResult(it, callback)
            }

            // cache does not contain AnalysisResult per each kt/psi element
            // instead it looks up analysis for its parents - see lookUp(analyzableElement)

            // step 2: return result if it is cached
            lookUp(analyzableParent)?.let {
                return@guarded handleResult(it, callback)
            }

            // step 3: perform analyze of analyzableParent as nothing has been cached yet

            val result = performAnalyze(analyzableParent, callback)
            cache[analyzableParent] = result

            return@guarded result

        }

    }

    private fun CjElement.isUsedForCompilationInEvaluator(): Boolean =
        containingFile is CjCodeFragment && containingFile.getCopyableUserData(CodeFragmentUtils.USED_FOR_COMPILATION_IN_IR_EVALUATOR) ?: false

    private fun updateFileResultFromCache() {
        // move fileResult from cache if it is stored there
        if (fileResult == null && cache.containsKey(file)) {
            fileResult = cache[file]

            // drop existed results for entire cache:
            // if incremental analysis is applicable it will produce a single value for file
            // otherwise those results are potentially stale
            cache.clear()
        }
    }


    private fun getIncrementalAnalysisResult(callback: DiagnosticSink.DiagnosticsCallback?): AnalysisResult? {
        updateFileResultFromCache()
        val inBlockModifications = file.inBlockModifications

        if (inBlockModifications.isNotEmpty()) {
            try {
                // IF there is a cached result for ktFile and there are inBlockModifications
                fileResult = fileResult?.let { result ->
                    var analysisResult = result
                    // Force full analysis when existed is erroneous
                    if (analysisResult.isError()) return@let null
                    for (inBlockModification in inBlockModifications) {
                        val resultCtx = analysisResult.bindingContext

                        val stackedCtx =
                            if (resultCtx is StackedCompositeBindingContextTrace.StackedCompositeBindingContext) resultCtx else null

                        // no incremental analysis IF it is not applicable
                        if (stackedCtx?.isIncrementalAnalysisApplicable() == false) return@let null

                        val trace: StackedCompositeBindingContextTrace =
                            if (stackedCtx != null && stackedCtx.element() == inBlockModification) {
                                val trace = stackedCtx.bindingTrace()
                                trace.clear()
                                trace
                            } else {
                                // to reflect a depth of stacked binding context
                                val depth = (stackedCtx?.depth() ?: 0) + 1

                                StackedCompositeBindingContextTrace(
                                    depth,
                                    element = inBlockModification,
                                    resolveContext = resolveSession.bindingContext,
                                    parentContext = resultCtx
                                )
                            }

                        callback?.let { trace.parentDiagnosticsApartElement.forEach(it::callback) }

                        val newResult = analyze(inBlockModification, trace, callback)
                        analysisResult = wrapResult(result, newResult, trace)
                    }
                    file.removeInBlockModifications(inBlockModifications)

                    analysisResult
                }
            } catch (e: Throwable) {
                e.throwAsInvalidModuleException {
                    clearFileResultCache()
                    ProcessCanceledException(it)
                }
                if (e !is ControlFlowException) {
                    clearFileResultCache()
                }
                throw e
            }
        }

        if (fileResult == null) {
            file.clearInBlockModifications()
        }
        return fileResult
    }

    private fun clearFileResultCache() {
        file.clearInBlockModifications()
        fileResult = null
    }

    private fun wrapResult(
        oldResult: AnalysisResult,
        newResult: AnalysisResult,
        elementBindingTrace: StackedCompositeBindingContextTrace
    ): AnalysisResult {
        val newBindingCtx = elementBindingTrace.stackedContext
        return when {
            oldResult.isError() -> {
                oldResult.error.throwAsInvalidModuleException()
                AnalysisResult.internalError(newBindingCtx, oldResult.error)
            }

            newResult.isError() -> {
                newResult.error.throwAsInvalidModuleException()
                AnalysisResult.internalError(newBindingCtx, newResult.error)
            }

            else -> {
                AnalysisResult.success(
                    newBindingCtx,
                    oldResult.moduleDescriptor,
                    oldResult.shouldGenerateCode
                )
            }
        }
    }

    private fun check(element: CjElement) {
        checkWithAttachment(element.containingFile == file, {
            "Expected $file, but was ${element.containingFile} for ${if (element.isValid) "valid" else "invalid"} $element "
        }) {
            it.withPsiAttachment("element.kt", element)
            it.withPsiAttachment("file.kt", element.containingFile)
            it.withPsiAttachment("original.kt", file)
        }
    }

    private fun performAnalyze(
        element: CjElement,
        callback: DiagnosticSink.DiagnosticsCallback? = null
    ): AnalysisResult {
        val localDiagnostics = mutableSetOf<Diagnostic>()
        val localCallback = if (callback != null) { d: Diagnostic ->
            localDiagnostics.add(d)
            callback.callback(d)
        } else null
        val result = try {
            analyze(element, null, localCallback)
        } catch (e: Throwable) {
            e.throwAsInvalidModuleException {
                ProcessCanceledException(it)
            }
            throw e
        }

        // 某些诊断无法通过回调处理-发送其余诊断
        callback?.let { c ->
            result.bindingContext.diagnostics.filterNot { it in localDiagnostics }.forEach(c::callback)
        }
        return result
    }

    private fun analyze(
        analyzableElement: CjElement,
        bindingTrace: BindingTrace?,
        callback: DiagnosticSink.DiagnosticsCallback?
    ): AnalysisResult {

        val project = analyzableElement.project
        if (DumbService.isDumb(project)) {
            return AnalysisResult.EMPTY
        }
        try {
            return CangJieResolveDataProvider.analyze(
                project,
                globalContext.withProject(project).withModule(moduleDescriptor),
                moduleDescriptor,
                resolveSession,
//                codeFragmentAnalyzer,
                pluginDeclarationProviderFactory,
                bodyResolveCache,
                analyzableElement,
                bindingTrace,
                callback
            )
        } catch (e: ProcessCanceledException) {
            throw e
        } catch (e: IndexNotReadyException) {
            throw e
        } catch (e: Throwable) {
            e.throwAsInvalidModuleException()

            DiagnosticUtils.throwIfRunningOnServer(e)
            LOG.warn(e)

            return AnalysisResult.internalError(BindingContext.EMPTY, e)
        }

    }
}

object CangJieResolveDataProvider {

    fun findAnalyzableParent(element: CjElement): CjElement? {
        if (element is CjFile) return element

        val topmostElement = element.findTopmostParentInFile {
            it is CjNamedFunction ||
                    it is CjAnonymousInitializer ||
                    it is CjProperty ||
                    it is CjImportDirective ||
                    it is CjPackageDirective ||
                    it is CjCodeFragment ||
                    // TODO: Non-analyzable so far, add more granular analysis
                    it is CjAnnotationEntry ||
                    it is CjTypeConstraint ||
                    it is CjSuperTypeList ||
                    it is CjTypeParameter ||
                    it is CjParameter ||
                    it is CjTypeAlias
        } as CjElement?

        // parameters and supertype lists are not analyzable by themselves, but if we don't count them as topmost, we'll stop inside, say,
        // object expressions inside arguments of super constructors of classes (note that classes themselves are not topmost elements)
        val analyzableElement = when (topmostElement) {
            is CjAnnotationEntry,
            is CjTypeConstraint,
            is CjSuperTypeList,
            is CjTypeParameter,
            is CjParameter -> topmostElement.findParentInFile { it is CjTypeStatement || it is CjCallableDeclaration } as? CjElement?

            else -> topmostElement
        }
        // Primary constructor should never be returned
        if (analyzableElement is CjPrimaryConstructor) return analyzableElement.getContainingTypeStatement()
        // Class initializer should be replaced by containing class to provide full analysis
        if (analyzableElement is CjClassInitializer) return analyzableElement.containingDeclaration
        return analyzableElement
        // if none of the above worked, take the outermost declaration
            ?: element.findTopmostParentOfType<CjDeclaration>()
            // if even that didn't work, take the whole file
            ?: element.containingFile as? CjFile
    }

    fun analyze(
        project: Project,
        projectContext: ModuleContext,
        moduleDescriptor: ModuleDescriptor,
        resolveSession: ResolveSession,
//        codeFragmentAnalyzer: CodeFragmentAnalyzer,
        pluginDeclarationProviderFactory: PluginDeclarationProviderFactory,
        bodyResolveCache: BodyResolveCache,
        analyzableElement: CjElement,
        bindingTrace: BindingTrace?,
        callback: DiagnosticSink.DiagnosticsCallback?
    ): AnalysisResult {


        var callbackSet = false
        val trace = bindingTrace ?: BindingTraceForBodyResolve(
            resolveSession.bindingContext,
            "Trace for resolution of $analyzableElement"
        )
        try {
            val lazyTopDownAnalyzer = createContainerForLazyBodyResolve(
                //TODO: should get ModuleContext
                projectContext,
                resolveSession,
                trace,
//                targetPlatform,
                bodyResolveCache,
//                targetPlatform.findAnalyzerServices(project),
                PlatformDependentAnalyzerServicesImpl,
//                pluginDeclarationProviderFactory,
                analyzableElement.languageVersionSettings,
//                IdeaModuleStructureOracle(),
//                IdeMainFunctionDetectorFactory(),
//                IdeSealedClassInheritorsProvider,
//                ControlFlowInformationProviderImpl.Factory,
                absentDescriptorHandler = IdeaAbsentDescriptorHandler(pluginDeclarationProviderFactory),
//                optimizingOptions = null
            ).get<LazyTopDownAnalyzer>()
//            val lazyTopDownAnalyzer = LazyTopDownAnalyzer()
            lazyTopDownAnalyzer.analyzeDeclarations(TopDownAnalysisMode.TopLevelDeclarations, listOf(analyzableElement))

        } finally {
            if (callbackSet) {
                trace.resetCallback()
            }
        }
        return AnalysisResult.success(trace.bindingContext, moduleDescriptor)


    }

}


/**
 * Keep in mind: trace fallbacks to [resolveContext] (is used during resolve) that does not have any
 * traces of earlier resolve for this [element]
 *
 * When trace turned into [BindingContext] it fallbacks to [parentContext]:
 * It is expected that all slices specific to [element] (and its descendants) are stored in this binding context
 * and for the rest elements it falls back to [parentContext].
 */
private class StackedCompositeBindingContextTrace(
    val depth: Int, // depth of stack over original ktFile bindingContext
    val element: CjElement,
    val resolveContext: BindingContext,
    val parentContext: BindingContext
) : DelegatingBindingTrace(
    resolveContext,
    "Stacked trace for resolution of $element",
    allowSliceRewrite = true
) {
    /**
     * Effectively StackedCompositeBindingContext holds up-to-date and partially outdated contexts (parentContext)
     *
     * The most up-to-date results for element are stored here (in a DelegatingBindingTrace#map)
     *
     * Note: It does not delete outdated results rather hide it therefore there is some extra memory footprint.
     *
     * Note: stackedContext differs from DelegatingBindingTrace#bindingContext:
     *      if result is not present in this context it goes to parentContext rather to resolveContext
     *      diagnostics are aggregated from this context and parentContext
     */
    val stackedContext = StackedCompositeBindingContext()

    /**
     * All diagnostics from parentContext apart this diagnostics this belongs to the element or its descendants
     */
    val parentDiagnosticsApartElement: Collection<Diagnostic> =
        (resolveContext.diagnostics.all() + parentContext.diagnostics.all()).filterApartElement()

    val parentDiagnosticsNoSuppressionApartElement: Collection<Diagnostic> =
        (resolveContext.diagnostics.noSuppression() + parentContext.diagnostics.noSuppression()).filterApartElement()

    private fun Collection<Diagnostic>.filterApartElement() =
        toSet().let { s ->
            s.filter { it.psiElement == element && selfDiagnosticToHold(it) } +
                    s.filter { it.psiElement.parentsWithSelf.none { e -> e == element } }
        }

    inner class StackedCompositeBindingContext : BindingContext {
        var cachedDiagnostics: Diagnostics? = null

        fun bindingTrace(): StackedCompositeBindingContextTrace = this@StackedCompositeBindingContextTrace

        fun element(): CjElement = this@StackedCompositeBindingContextTrace.element

        fun depth(): Int = this@StackedCompositeBindingContextTrace.depth

        // to prevent too deep stacked binding context
        fun isIncrementalAnalysisApplicable(): Boolean = this@StackedCompositeBindingContextTrace.depth < 16

        // Predicate to check if the receiver is a PsiElement that was reanalyzed and therefore should
        // have a result in the reanalysis context. We should not look such elements up in the
        // parent context when there is no information for it in the current context. Because of mutations
        // to PsiElements, that could result in incorrect information

        private fun <K : Any?> K.containedInReanalyzedElement(): Boolean {
            return when (element) {
                is CjDeclarationWithBody -> {
                    // Psi elements within the body of a reanalyzed function should have
                    // information only in reanalysis context.
                    val body = element.bodyExpression ?: return false
                    (this as? PsiElement)?.parentsWithSelf?.contains(body) == true
                }

                is CjTypeStatement -> {
                    // Psi elements within anonymous initializers and secondary constructors should have information
                    // only in the reanalysis context.
                    (this as? PsiElement)?.parents(withSelf = false)?.any {
                        /*it in element.getAnonymousInitializers() ||*/ it in element.secondaryConstructors
                    } == true
                }

                else -> false
            }
        }

        override fun getDiagnostics(): Diagnostics {
            if (cachedDiagnostics == null) {
                val mergedDiagnostics = mutableSetOf<Diagnostic>()
                mergedDiagnostics.addAll(parentDiagnosticsApartElement)
                this@StackedCompositeBindingContextTrace.mutableDiagnostics?.all()?.let {
                    mergedDiagnostics.addAll(it)
                }

                val mergedNoSuppressionDiagnostics = mutableSetOf<Diagnostic>()
                mergedNoSuppressionDiagnostics += parentDiagnosticsNoSuppressionApartElement
                this@StackedCompositeBindingContextTrace.mutableDiagnostics?.noSuppression()?.let {
                    mergedNoSuppressionDiagnostics.addAll(it)
                }

                cachedDiagnostics = MergedDiagnostics(
                    mergedDiagnostics,
                    mergedNoSuppressionDiagnostics,
                    parentContext.diagnostics.modificationTracker
                )
            }
            return cachedDiagnostics!!
        }

        override fun <K : Any?, V : Any?> get(slice: ReadOnlySlice<K, V>, key: K): V? {
            selfGet(slice, key)?.let { return it }
            if (!key.containedInReanalyzedElement()) {
                return parentContext.get(slice, key)?.takeIf {
                    (it as? DeclarationDescriptorWithSource)?.source?.getPsi()?.isValid != false
                }
            }
            return null
        }

        override fun getType(expression: CjExpression): CangJieType? {
            val typeInfo = get(BindingContext.EXPRESSION_TYPE_INFO, expression)
            return typeInfo?.type
        }

        override fun <K, V> getKeys(slice: WritableSlice<K, V>): Collection<K> {
            val keys = map.getKeys(slice)
            val fromParent = parentContext.getKeys(slice).filter {
                !it.containedInReanalyzedElement()
            }
            if (keys.isEmpty()) return fromParent
            if (fromParent.isEmpty()) return keys

            return keys + fromParent
        }

        override fun <K : Any?, V : Any?> getSliceContents(slice: ReadOnlySlice<K, V>): ImmutableMap<K, V> {
            val parentSliceContents = parentContext.getSliceContents(slice).filter {
                !it.key.containedInReanalyzedElement()
            }
            val mapSliceContents = map.getSliceContents(slice)
            return ImmutableMap.copyOf(parentSliceContents + mapSliceContents)
        }

        override fun addOwnDataTo(trace: BindingTrace, commitDiagnostics: Boolean) =
            throw UnsupportedOperationException()
    }

    override fun <K : Any?, V : Any?> get(slice: ReadOnlySlice<K, V>, key: K): V? =
        if (slice == BindingContext.ANNOTATION) {
            selfGet(slice, key) ?: parentContext.get(slice, key)
        } else {
            super.get(slice, key)
        }

    override fun clear() {
        super.clear()
        stackedContext.cachedDiagnostics = null
    }

    companion object {
        private fun selfDiagnosticToHold(d: Diagnostic): Boolean {
            val positioningStrategy = d.factory.safeAs<DiagnosticFactoryWithPsiElement<*, *>>()?.positioningStrategy
//            return when (positioningStrategy) {
//                DECLARATION_WITH_BODY -> false
//                else -> true
//            }
            return true
        }
    }
}

private class MergedDiagnostics(
    val diagnostics: Collection<Diagnostic>,
    val noSuppressionDiagnostics: Collection<Diagnostic>,
    override val modificationTracker: ModificationTracker
) : Diagnostics {
    private val elementsCache = DiagnosticsElementsCache(this) { true }

    override fun all() = diagnostics

    override fun forElement(psiElement: PsiElement): MutableCollection<Diagnostic> =
        elementsCache.getDiagnostics(psiElement)

    override fun noSuppression() = if (noSuppressionDiagnostics.isEmpty()) {
        this
    } else {
        MergedDiagnostics(noSuppressionDiagnostics, emptyList(), modificationTracker)
    }


}

