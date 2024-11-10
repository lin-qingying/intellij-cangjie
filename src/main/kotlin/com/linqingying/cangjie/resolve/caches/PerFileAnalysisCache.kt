package com.linqingying.cangjie.resolve.caches

import com.google.common.collect.ImmutableMap
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
import com.linqingying.cangjie.analyzer.AnalysisResult
import com.linqingying.cangjie.container.ComponentProvider
import com.linqingying.cangjie.container.get
import com.linqingying.cangjie.context.GlobalContext
import com.linqingying.cangjie.context.ModuleContext
import com.linqingying.cangjie.context.withModule
import com.linqingying.cangjie.context.withProject
import com.linqingying.cangjie.descriptors.BindingTrace
import com.linqingying.cangjie.descriptors.DeclarationDescriptorWithSource
import com.linqingying.cangjie.descriptors.InvalidModuleException
import com.linqingying.cangjie.descriptors.ModuleDescriptor
import com.linqingying.cangjie.diagnostics.*
import com.linqingying.cangjie.frontend.createContainerForLazyBodyResolve
import com.linqingying.cangjie.ide.cache.trackers.clearInBlockModifications
import com.linqingying.cangjie.ide.cache.trackers.inBlockModifications
import com.linqingying.cangjie.ide.cache.trackers.removeInBlockModifications
import com.linqingying.cangjie.ide.projectStructure.languageVersionSettings
import com.linqingying.cangjie.ide.stubindex.resolve.PluginDeclarationProviderFactory
import com.linqingying.cangjie.psi.*
import com.linqingying.cangjie.psi.psiUtil.parentsWithSelf
import com.linqingying.cangjie.resolve.*
import com.linqingying.cangjie.resolve.controlFlow.ControlFlowInformationProviderImpl
import com.linqingying.cangjie.resolve.lazy.BodyResolveMode
import com.linqingying.cangjie.resolve.lazy.IdeaAbsentDescriptorHandler
import com.linqingying.cangjie.resolve.lazy.ResolveSession
import com.linqingying.cangjie.resolve.source.getPsi
import com.linqingying.cangjie.storage.CancellableSimpleLock
import com.linqingying.cangjie.storage.guarded
import com.linqingying.cangjie.types.CangJieType
import com.linqingying.cangjie.utils.CodeFragmentUtils
import com.linqingying.cangjie.utils.checkWithAttachment
import com.linqingying.cangjie.utils.safeAs
import com.linqingying.cangjie.utils.slicedMap.ReadOnlySlice
import com.linqingying.cangjie.utils.slicedMap.WritableSlice
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
    private val codeFragmentAnalyzer = componentProvider.get<CodeFragmentAnalyzer>()

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

    /**
     * 获取给定元素的分析结果。
     *
     * 该函数负责分析提供的 [element] 并返回分析结果。
     * 如果提供了 [callback]，则会将诊断信息通过回调传递。
     *
     * @param element 需要分析的代码元素。
     * @param callback 可选的诊断信息回调函数，用于处理分析过程中产生的诊断信息。
     * @return 分析结果，包含绑定上下文和诊断信息。
     */
    internal fun getAnalysisResults(
        element: CjElement,
        callback: DiagnosticSink.DiagnosticsCallback? = null
    ): AnalysisResult {
        check(element)
        val analyzableParent = CangJieResolveDataProvider.findAnalyzableParent(element) ?: return AnalysisResult.EMPTY

        /**
         * 处理分析结果并调用回调函数。
         *
         * @param result 分析结果。
         * @param callback 诊断信息回调函数。
         * @return 处理后的分析结果。
         */
        fun handleResult(result: AnalysisResult, callback: DiagnosticSink.DiagnosticsCallback?): AnalysisResult {
            callback?.let { result.bindingContext.diagnostics.forEach(it::callback) }
            return result
        }

        return guardLock.guarded {
            // 忽略评估器中用于编译的代码片段，因为缓存可能导致数据一致性问题（见 KTIJ-22496）。
            // 但是，来自评估器的不用于编译的代码片段（例如，用于高亮显示）应该缓存以保持评估器的当前性能。
            if (analyzableParent.isUsedForCompilationInEvaluator()) return@guarded performAnalyze(element, callback)

            // 第一步：如果适用，执行增量分析
            getIncrementalAnalysisResult(callback)?.let {
                return@guarded handleResult(it, callback)
            }

            // 第二步：如果结果已缓存，直接返回缓存结果
            lookUp(analyzableParent)?.let {
                return@guarded handleResult(it, callback)
            }

            // 第三步：如果没有任何缓存结果，执行分析并缓存结果
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
            it.withPsiAttachment("element.cj", element)
            it.withPsiAttachment("file.cj", element.containingFile)
            it.withPsiAttachment("original.cj", file)
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
                codeFragmentAnalyzer,
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

    /**
     * 分析给定的可分析元素。
     *
     * @param project 项目实例，表示当前分析的项目。
     * @param projectContext 模块上下文，包含模块的相关信息。
     * @param moduleDescriptor 模块描述符，用于描述模块的结构。
     * @param resolveSession 解析会话，用于解析过程中的上下文管理。
     * @param codeFragmentAnalyzer 代码片段分析器，用于分析代码片段。
     * @param pluginDeclarationProviderFactory 插件声明提供者工厂，用于生成插件声明提供者。
     * @param bodyResolveCache 体解析缓存，用于缓存解析结果。
     * @param analyzableElement 可分析元素，可以是代码片段或其他可分析的元素。
     * @param bindingTrace 绑定跟踪，用于记录解析过程中的绑定信息。
     * @param callback 诊断回调，用于处理诊断信息。
     * @return 返回分析结果，包含绑定上下文和模块描述符。
     */
    fun analyze(
        project: Project,
        projectContext: ModuleContext,
        moduleDescriptor: ModuleDescriptor,
        resolveSession: ResolveSession,
        codeFragmentAnalyzer: CodeFragmentAnalyzer,
        pluginDeclarationProviderFactory: PluginDeclarationProviderFactory,
        bodyResolveCache: BodyResolveCache,
        analyzableElement: CjElement,
        bindingTrace: BindingTrace?,
        callback: DiagnosticSink.DiagnosticsCallback?
    ): AnalysisResult {
        try {
            // 如果可分析元素是代码片段，则使用部分解析模式进行分析
            if (analyzableElement is CjCodeFragment) {
                val bodyResolveMode = BodyResolveMode.PARTIAL_FOR_COMPLETION
                val trace: BindingTrace = codeFragmentAnalyzer.analyzeCodeFragment(analyzableElement, bodyResolveMode)
                val bindingContext = trace.bindingContext
                return AnalysisResult.success(bindingContext, moduleDescriptor)
            }

            // 如果没有提供绑定跟踪，则创建一个新的绑定跟踪
            val trace = bindingTrace ?: BindingTraceForBodyResolve(
                resolveSession.bindingContext,
                "Trace for resolution of $analyzableElement"
            )

            var callbackSet = false
            try {
                // 设置诊断回调
                callbackSet = callback?.let(trace::setCallbackIfNotSet) ?: false

                // 创建懒惰的自上而下分析器，用于分析声明
                val lazyTopDownAnalyzer = createContainerForLazyBodyResolve(
                    projectContext,
                    resolveSession,
                    trace,
                    bodyResolveCache,
                    PlatformDependentAnalyzerServicesImpl,
                    analyzableElement.languageVersionSettings,
                    ControlFlowInformationProviderImpl.Factory,
                    absentDescriptorHandler = IdeaAbsentDescriptorHandler(pluginDeclarationProviderFactory)
                ).get<LazyTopDownAnalyzer>()

                // 分析声明
                lazyTopDownAnalyzer.analyzeDeclarations(
                    TopDownAnalysisMode.TopLevelDeclarations,
                    listOf(analyzableElement)
                )
            } finally {
                // 重置诊断回调
                if (callbackSet) {
                    trace.resetCallback()
                }
            }

            // 返回分析结果
            return AnalysisResult.success(trace.bindingContext, moduleDescriptor)
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


/**
 * Keep in mind: trace fallbacks to [resolveContext] (is used during resolve) that does not have any
 * traces of earlier resolve for this [element]
 *
 * When trace turned into [BindingContext] it fallbacks to [parentContext]:
 * It is expected that all slices specific to [element] (and its descendants) are stored in this binding context
 * and for the rest elements it falls back to [parentContext].
 */
private class StackedCompositeBindingContextTrace(
    val depth: Int, // depth of stack over original cjFile bindingContext
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
     *来自 parentContext 的所有诊断，除了属于该元素或其后代的诊断
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

