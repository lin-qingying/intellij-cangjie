package com.huawei.cangjie.resolve

import com.huawei.cangjie.analyzer.CjpmLibraryInfo
import com.huawei.cangjie.analyzer.DaemonCodeAnalyzerStatusService
import com.huawei.cangjie.analyzer.ModuleInfo
import com.huawei.cangjie.container.get
import com.huawei.cangjie.context.SimpleGlobalContext
import com.huawei.cangjie.context.withModule
import com.huawei.cangjie.context.withProject
import com.huawei.cangjie.descriptors.*
import com.huawei.cangjie.frontend.createContainerForBodyResolve
import com.huawei.cangjie.ide.cache.trackers.CangJieCodeBlockModificationListener
import com.huawei.cangjie.ide.projectStructure.languageVersionSettings
import com.huawei.cangjie.ide.stubindex.resolve.isUnitTestMode
import com.huawei.cangjie.name.Name
import com.huawei.cangjie.psi.*
import com.huawei.cangjie.psi.psiUtil.forEachDescendantOfType
import com.huawei.cangjie.psi.psiUtil.getElementTextWithContext
import com.huawei.cangjie.psi.psiUtil.getNonStrictParentOfType
import com.huawei.cangjie.resolve.caches.CodeFragmentAnalyzer
import com.huawei.cangjie.resolve.caches.analyzeControlFlow
import com.huawei.cangjie.resolve.calls.smartcasts.DataFlowInfo
import com.huawei.cangjie.resolve.controlFlow.ControlFlowInformationProviderImpl
import com.huawei.cangjie.resolve.lazy.*
import com.huawei.cangjie.resolve.lazy.BodyResolveMode.*
import com.huawei.cangjie.resolve.lazy.descriptors.LazyClassDescriptorBase
import com.huawei.cangjie.resolve.scopes.LexicalScope
import com.huawei.cangjie.types.expressions.ExpressionTypingContext
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectRootModificationTracker
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.registry.Registry
import com.intellij.psi.util.*
import com.intellij.util.containers.CollectionFactory
import com.intellij.util.containers.SLRUCache
import org.jetbrains.annotations.TestOnly
import java.util.concurrent.ConcurrentMap

private val FILE_IN_BLOCK_MODIFICATION_COUNT = Key<Long>("FILE_IN_BLOCK_MODIFICATION_COUNT")

val CjFile.inBlockModificationCount: Long by NotNullableUserDataProperty(FILE_IN_BLOCK_MODIFICATION_COUNT, 0)

//排除的类型
private val EXCLUDED_TYPES = setOf(
    CjAnonymousInitializer::class,
    CjDestructuringDeclaration::class,
    CjDestructuringDeclarationEntry::class,
    CjBindingPattern::class,
    CjTypePattern::class

)

class ResolveElementCache(
    private val resolveSession: ResolveSession,
    private val project: Project,

    private val codeFragmentAnalyzer: CodeFragmentAnalyzer
) : BodyResolveCache {
    private val cacheDependencies = listOfNotNull(
        resolveSession.exceptionTracker,
        ProjectRootModificationTracker.getInstance(project),
        if (resolveSession.moduleDescriptor.getCapability(ModuleInfo.Capability) !is CjpmLibraryInfo) {
            CangJieCodeBlockModificationListener.getInstance(project).cangjieOutOfCodeBlockTracker
        } else null
    ).toTypedArray()

    private class CachedPartialResolve(val bindingContext: BindingContext, file: CjFile, val mode: BodyResolveMode) {
        private val modificationStamp: Long = modificationStamp(file)

        fun isUpToDate(file: CjFile, newMode: BodyResolveMode): Boolean {
            return modificationStamp == modificationStamp(file) && mode.doesNotLessThan(newMode)
        }

        private fun modificationStamp(file: CjFile): Long {
            // for non-physical file we don't get MODIFICATION_COUNT increased and must reset data on any modification of the file
            return if (!file.isPhysical)
                file.modificationStamp
            else
                file.inBlockModificationCount
        }

        override fun toString(): String {
            return "{CachedPartialResolve: $mode $modificationStamp}"
        }
    }

    private fun codeFragmentAdditionalResolve(
        codeFragment: CjCodeFragment,
        bodyResolveMode: BodyResolveMode
    ): BindingTrace {
        val contextResolveMode = if (bodyResolveMode == PARTIAL)
            PARTIAL_FOR_COMPLETION
        else
            bodyResolveMode

        return codeFragmentAnalyzer.analyzeCodeFragment(codeFragment, contextResolveMode)
    }

    fun resolveToElements(elements: Collection<CjElement>, bodyResolveMode: BodyResolveMode = FULL): BindingContext {
        val elementsByAdditionalResolveElement: Map<CjElement?, List<CjElement>> =
            elements.groupBy { findElementOfAdditionalResolve(it, bodyResolveMode) }

        val bindingContexts = ArrayList<BindingContext>()
        val declarationsToResolve = ArrayList<CjDeclaration>()
        var addResolveSessionBindingContext = false

        ensureFileAnnotationsResolved(elements)
        for ((elementOfAdditionalResolve, contextElements) in elementsByAdditionalResolveElement) {
            if (elementOfAdditionalResolve != null) {
                if (elementOfAdditionalResolve is CjParameter) {
                    throw AssertionError(
                        "ResolveElementCache: Element of additional resolve should not be CjParameter: " +
                                "${elementOfAdditionalResolve.text} for context element ${contextElements.firstOrNull()?.text}"
                    )
                }
                val bindingContext =
                    getElementsAdditionalResolve(elementOfAdditionalResolve, contextElements, bodyResolveMode)
                bindingContexts.add(bindingContext)
            } else {
                contextElements
                    .mapNotNull { it.getNonStrictParentOfType<CjDeclaration>() }
                    .filterTo(declarationsToResolve) {
                        it !is CjAnonymousInitializer && it !is CjDestructuringDeclaration && it !is CjDestructuringDeclarationEntry
                    }
                addResolveSessionBindingContext = true
            }
        }

        declarationsToResolve.forEach { resolveSession.resolveToDescriptor(it) }
        if (addResolveSessionBindingContext) {
            bindingContexts.add(resolveSession.bindingContext)
        }

        //TODO: it can be slow if too many contexts
        return CompositeBindingContext.create(bindingContexts)
    }

    private val partialBodyResolveCache: CachedValue<SLRUCache<CjFile, ConcurrentMap<CjExpression, CachedPartialResolve>>> =
        CachedValuesManager.getManager(project).createCachedValue(
            CachedValueProvider {
                val slruCache = SLRUCache.slruCache<CjFile, ConcurrentMap<CjExpression, CachedPartialResolve>>(20, 20) {
                    CollectionFactory.createConcurrentWeakKeySoftValueMap()
                }

                CachedValueProvider.Result.create(slruCache, cacheDependencies)
            },
            false
        )

    private class CachedFullResolve(val bindingContext: BindingContext, resolveElement: CjElement) {
        private val modificationStamp: Long? = modificationStamp(resolveElement)

        fun isUpToDate(resolveElement: CjElement) = modificationStamp == modificationStamp(resolveElement)

        private fun modificationStamp(resolveElement: CjElement): Long? {
            val file = resolveElement.containingFile
            return when {
                // for non-physical file we don't get OUT_OF_CODE_BLOCK_MODIFICATION_COUNT increased and must reset
                // data on any modification of the file
                !file.isPhysical -> file.modificationStamp

//                resolveElement is CjDeclaration && PureCangJieCodeBlockModificationListener.isBlockDeclaration(resolveElement) -> resolveElement.getModificationStamp()
                resolveElement is CjSuperTypeList -> resolveElement.modificationStamp
                else -> null
            }
        }
    }

    // drop whole cache after change "out of code block", each entry is checked with own modification stamp
    private val fullResolveCache: CachedValue<MutableMap<CjElement, CachedFullResolve>> =
        CachedValuesManager.getManager(project).createCachedValue(
            CachedValueProvider {
                CachedValueProvider.Result.create(
                    CollectionFactory.createConcurrentWeakKeySoftValueMap(),
                    cacheDependencies
                )
            },
            false
        )

    private fun findElementOfAdditionalResolve(element: CjElement, bodyResolveMode: BodyResolveMode): CjElement? {
        if (element is CjAnnotationEntry && bodyResolveMode == PARTIAL_NO_ADDITIONAL)
            return element

        val elementOfAdditionalResolve = element.findTopmostParentInFile {
            it is CjNamedFunction ||
                    it is CjAnonymousInitializer ||
                    it is CjPrimaryConstructor ||
                    it is CjSecondaryConstructor ||
                    it is CjProperty ||
                    it is CjVariable ||
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

    private fun ensureFileAnnotationsResolved(elements: Collection<CjElement>) {
        val filesToBeAnalyzed = elements.map { it.getContainingCjFile() }.toSet()
        for (file in filesToBeAnalyzed) {
            ensureFileAnnotationsResolved(file)
        }
    }

    //
    private fun ensureFileAnnotationsResolved(file: CjFile) {
//        val fileLevelAnnotations = resolveSession.getFileAnnotations(file)
//        doResolveAnnotations(fileLevelAnnotations)
    }

    fun resolveToElement(element: CjElement, bodyResolveMode: BodyResolveMode = FULL): BindingContext {
        val elementOfAdditionalResolve = findElementOfAdditionalResolve(element, bodyResolveMode)

//        ensureFileAnnotationsResolved(element.getContainingCjFile())

        val bindingContext = if (elementOfAdditionalResolve != null) {
            if (elementOfAdditionalResolve is CjParameter) {
                throw AssertionError(
                    "ResolveElementCache: Element of additional resolve should not be CjParameter: " +
                            "${elementOfAdditionalResolve.text} for context element ${element.text}"
                )
            }
            getElementsAdditionalResolve(elementOfAdditionalResolve, element, null, bodyResolveMode)
        } else {
            element.getNonStrictParentOfType<CjDeclaration>()?.takeIf {

                !EXCLUDED_TYPES.any{ kclass->
                    kclass  == it::class

                }
//                it !is CjAnonymousInitializer && it !is CjDestructuringDeclaration && it !is CjDestructuringDeclarationEntry
            }?.let { resolveSession.resolveToDescriptor(it) }
            resolveSession.bindingContext
        }

        return bindingContext

    }

    private val forcedFullResolveOnHighlighting =
        Registry.`is`("cangjie.resolve.force.full.resolve.on.highlighting", true)

    private fun getElementsAdditionalResolve(
        resolveElement: CjElement,
        contextElement: CjElement?,
        contextElements: Collection<CjElement>? = null,
        bodyResolveMode: BodyResolveMode
    ): BindingContext {

//        TODO return bindingContext
        if (contextElements == null && contextElement == null) {
            assert(bodyResolveMode == FULL)
        }

        // Force perform FULL analysis to avoid redundant analysis for the current selected files.
        if (bodyResolveMode != FULL &&
            bodyResolveMode != PARTIAL_FOR_COMPLETION
            &&
            (!isUnitTestMode() || forceFullAnalysisModeInTests) &&
            forcedFullResolveOnHighlighting && DaemonCodeAnalyzerStatusService.getInstance(project).daemonRunning
        ) {
            val virtualFile = resolveElement.containingFile.virtualFile
            // applicable for real (physical) files only
            if (virtualFile != null && FileEditorManager.getInstance(resolveElement.project)?.selectedFiles?.any { it == virtualFile } == true) {
                return getElementsAdditionalResolve(resolveElement, contextElement, contextElements, FULL)
            }
        }

        val fullResolveMap = fullResolveCache.value

        when (bodyResolveMode) {
            FULL -> {
                val bindingContext = performElementAdditionalResolve(resolveElement, null, FULL).first
//                fullResolveMap[resolveElement] = CachedFullResolve(bindingContext, resolveElement)
                return bindingContext
            }

            else -> {


                if (resolveElement !is CjDeclaration) {
                    return getElementsAdditionalResolve(
                        resolveElement,
                        contextElement = null,
                        contextElements = null,
                        bodyResolveMode = FULL
                    )
                }

                val file = resolveElement.getContainingCjFile()
                val statementsToResolve =
                    contextElement?.run {
                        listOf(
                            PartialBodyResolveFilter.findStatementToResolve(
                                this,
                                resolveElement
                            )
                        )
                    } ?: contextElements!!.map { PartialBodyResolveFilter.findStatementToResolve(it, resolveElement) }
                        .distinct()
                val statementsToResolveByCjFile =
                    statementsToResolve.groupBy { (it ?: resolveElement).getContainingCjFile() }
                val cachedResults =
                    statementsToResolveByCjFile.flatMap { (file, expressions) ->
                        val partialBodyResolveCacheValue = partialBodyResolveCache.value
                        val expressionsMap = synchronized(partialBodyResolveCacheValue) {
                            partialBodyResolveCacheValue[file]
                        }
                        expressions.map { expressionsMap[it ?: resolveElement] }
                    }

                // a bit of problem here that several threads come to analyze same resolveElement

                if (cachedResults.all { it != null && it.isUpToDate(file, bodyResolveMode) }) {
                    // partial resolve is already cached for these statements
                    return CompositeBindingContext.create(cachedResults.map { it!!.bindingContext }.distinct())
                }

                val (bindingContext, statementFilter) = performElementAdditionalResolve(
                    resolveElement,
                    contextElements ?: listOfNotNull(contextElement),
                    bodyResolveMode
                )

                if (statementFilter == StatementFilter.NONE &&
                    bodyResolveMode.doControlFlowAnalysis && !bodyResolveMode.bindingTraceFilter.ignoreDiagnostics
                ) {
                    // Without statement filter, we analyze everything, so we can count partial resolve result as full resolve
                    // But we can do this only if our resolve mode also provides *both* CFA and diagnostics
                    // This is true only for PARTIAL_WITH_DIAGNOSTICS resolve mode
                    fullResolveMap[resolveElement] = CachedFullResolve(bindingContext, resolveElement)
                    return bindingContext
                }

                val partialBodyResolveCacheValue = partialBodyResolveCache.value
                val expressionsMap = synchronized(partialBodyResolveCacheValue) {
                    partialBodyResolveCacheValue[file]
                }

                val resolveToCache = CachedPartialResolve(bindingContext, file, bodyResolveMode)

                if (statementFilter is PartialBodyResolveFilter) {
                    for (statement in statementFilter.allStatementsToResolve) {
                        if (bindingContext[BindingContext.PROCESSED, statement] == true) {
                            expressionsMap.putIfAbsent(statement, resolveToCache)?.let { oldResolveToCache ->
                                if (!oldResolveToCache.isUpToDate(file, bodyResolveMode)) {
                                    expressionsMap[statement] = resolveToCache
                                } else if (!oldResolveToCache.mode.doesNotLessThan(bodyResolveMode)) {
                                    expressionsMap.replace(statement, oldResolveToCache, resolveToCache)
                                }
                                return@let
                            }
                        }
                    }
                }

                // we use the whole declaration key in the map to obtain resolve not inside any block (e.g. default parameter values)
                expressionsMap[resolveElement] = resolveToCache

                return bindingContext

            }
        }


    }

    private fun typealiasAdditionalResolve(
        resolveSession: ResolveSession, typeAlias: CjTypeAlias,
        bindingTraceFilter: BindingTraceFilter
    ): BindingTrace {
        val trace = createDelegatingTrace(typeAlias, bindingTraceFilter)
        val typeAliasDescriptor = resolveSession.resolveToDescriptor(typeAlias)
        ForceResolveUtil.forceResolveAllContents(typeAliasDescriptor)
//        forceResolveAnnotationsInside(typeAlias)
        return trace
    }

    private fun primaryConstructorAdditionalResolve(
        resolveSession: ResolveSession,
        constructor: CjPrimaryConstructor,
        file: CjFile, statementFilter: StatementFilter,
        bindingTraceFilter: BindingTraceFilter
    ): BindingTrace {
        return constructorAdditionalResolve(resolveSession, constructor, file, statementFilter, bindingTraceFilter)
    }

    private fun endSecondaryConstructorAdditionalResolve(
        resolveSession: ResolveSession,
        constructor: CjEndSecondaryConstructor,
        file: CjFile, statementFilter: StatementFilter,
        bindingTraceFilter: BindingTraceFilter
    ): BindingTrace {
        return constructorAdditionalResolve(resolveSession, constructor, file, statementFilter, bindingTraceFilter)
    }

    private fun secondaryConstructorAdditionalResolve(
        resolveSession: ResolveSession,
        constructor: CjSecondaryConstructor,
        file: CjFile, statementFilter: StatementFilter,
        bindingTraceFilter: BindingTraceFilter
    ): BindingTrace {
        return constructorAdditionalResolve(resolveSession, constructor, file, statementFilter, bindingTraceFilter)
    }

    private fun constructorAdditionalResolve(
        resolveSession: ResolveSession,
        constructor: CjConstructor<*>,
        file: CjFile, statementFilter: StatementFilter,
        bindingTraceFilter: BindingTraceFilter
    ): BindingTrace {
        val trace = createDelegatingTrace(constructor, bindingTraceFilter)

        val scope = resolveSession.declarationScopeProvider.getResolutionScopeForDeclaration(constructor)
        val constructorDescriptor = resolveSession.resolveToDescriptor(constructor) as ClassConstructorDescriptor
        ForceResolveUtil.forceResolveAllContents(constructorDescriptor)

        val bodyResolver = createBodyResolver(resolveSession, trace, file, statementFilter)
        bodyResolver.resolveConstructorBody(
            DataFlowInfo.EMPTY,
            trace,
            constructor,
            constructorDescriptor,
            scope,
            null
        )

        forceResolveAnnotationsInside(constructor)

        return trace
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
            is CjMainFunction -> functionAdditionalResolve(
                resolveSession,
                resolveElement,
                file,
                createStatementFilter(),
                bodyResolveMode.bindingTraceFilter
            )

            is CjNamedFunction -> functionAdditionalResolve(
                resolveSession,
                resolveElement,
                file,
                createStatementFilter(),
                bodyResolveMode.bindingTraceFilter
            )

            is CjVariable -> variableAdditionalResolve(
                resolveSession,
                resolveElement,
                file,
                createStatementFilter(),
                bodyResolveMode.bindingTraceFilter
            )

            is CjProperty -> propertyAdditionalResolve(
                resolveSession,
                resolveElement,
                file,
                createStatementFilter(),
                bodyResolveMode.bindingTraceFilter
            )

            is CjTypeAlias -> typealiasAdditionalResolve(
                resolveSession,
                resolveElement,
                bodyResolveMode.bindingTraceFilter
            )

            is CjSuperTypeList -> delegationSpecifierAdditionalResolve(
                resolveSession,
                resolveElement,
                resolveElement.getParent() as CjTypeStatement,
                file,
                bodyResolveMode.bindingTraceFilter
            )

            is CjImportList -> {
                val resolver = resolveSession.fileScopeProvider.getImportResolver(resolveElement.getContainingCjFile())
                resolver.forceResolveNonDefaultImports()
                resolveSession.trace
            }


            is CjPackageDirective -> {
                resolveSession.trace

            }

//            is CjPrimaryConstructor -> constructorAdditionalResolve(
//                resolveSession,
//                resolveElement.parent as CjTypeStatement,
//                file,
//                bodyResolveMode.bindingTraceFilter
//            )

            is CjTypeConstraint -> typeConstraintAdditionalResolve(resolveSession, resolveElement)
            is CjPrimaryConstructor -> primaryConstructorAdditionalResolve(
                resolveSession,
                resolveElement,
                file,
                createStatementFilter(),
                bodyResolveMode.bindingTraceFilter
            )

            is CjEndSecondaryConstructor -> endSecondaryConstructorAdditionalResolve(
                resolveSession,
                resolveElement,
                file,
                createStatementFilter(),
                bodyResolveMode.bindingTraceFilter
            )

            is CjSecondaryConstructor -> secondaryConstructorAdditionalResolve(
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

        if (bodyResolveMode.doControlFlowAnalysis) {
            analyzeControlFlow(resolveSession, resolveElement, trace)
        }

        return Pair(trace.bindingContext, statementFilterUsed)
    }

    private fun variableAdditionalResolve(
        resolveSession: ResolveSession, variable: CjVariable,
        file: CjFile,
        statementFilter: StatementFilter,
        bindingTraceFilter: BindingTraceFilter
    ): BindingTrace {
        val trace = createDelegatingTrace(variable, bindingTraceFilter)

        val bodyResolver = createBodyResolver(resolveSession, trace, file, statementFilter)
        val descriptor = resolveSession.resolveToDescriptor(variable) as VariableDescriptor
        ForceResolveUtil.forceResolveAllContents(descriptor)

        val bodyResolveContext = BodyResolveContextForLazy(TopDownAnalysisMode.LocalDeclarations) { declaration ->
            assert(declaration.parent == variable || declaration == variable) {
                "Must be called only for property accessors or for property, but called for $declaration"
            }
            resolveSession.declarationScopeProvider.getResolutionScopeForDeclaration(declaration)
        }

        bodyResolver.resolveVariable(bodyResolveContext, variable, descriptor)

        forceResolveAnnotationsInside(variable)

//        for (accessor in property.accessors) {
//            ControlFlowInformationProviderImpl(
//                accessor, trace, accessor.languageVersionSettings, resolveSession.platformDiagnosticSuppressor
//            ).checkDeclaration()
//        }

        return trace
    }

    private fun propertyAdditionalResolve(
        resolveSession: ResolveSession, property: CjProperty,
        file: CjFile,
        statementFilter: StatementFilter,
        bindingTraceFilter: BindingTraceFilter
    ): BindingTrace {
        val trace = createDelegatingTrace(property, bindingTraceFilter)

        val bodyResolver = createBodyResolver(resolveSession, trace, file, statementFilter)
        val descriptor = resolveSession.resolveToDescriptor(property) as PropertyDescriptor
        ForceResolveUtil.forceResolveAllContents(descriptor)

        val bodyResolveContext = BodyResolveContextForLazy(TopDownAnalysisMode.LocalDeclarations) { declaration ->
            assert(declaration.parent == property || declaration == property || declaration.parent.parent == property) {
                "Must be called only for property accessors or for property, but called for $declaration"
            }
            resolveSession.declarationScopeProvider.getResolutionScopeForDeclaration(declaration)
        }

        bodyResolver.resolveProperty(bodyResolveContext, property, descriptor)

        forceResolveAnnotationsInside(property)

        for (accessor in property.accessors) {
            ControlFlowInformationProviderImpl(
                accessor, trace, accessor.languageVersionSettings, /*resolveSession.platformDiagnosticSuppressor*/
            ).checkDeclaration()
        }

        return trace
    }

    private fun typeConstraintAdditionalResolve(
        analyzer: CangJieCodeAnalyzer,
        typeConstraint: CjTypeConstraint
    ): BindingTrace {
        val declaration = typeConstraint.findParentOfType<CjDeclaration>(true)!!
        val descriptor = analyzer.resolveToDescriptor(declaration) as ClassifierDescriptorWithTypeParameters

        for (parameterDescriptor in descriptor.declaredTypeParameters) {
            ForceResolveUtil.forceResolveAllContents(parameterDescriptor)
        }

        return resolveSession.trace
    }

    private fun constructorAdditionalResolve(
        resolveSession: ResolveSession,
        cclass: CjTypeStatement,
        file: CjFile,
        filter: BindingTraceFilter
    ): BindingTrace {
        val trace = createDelegatingTrace(cclass, filter)

        val classDescriptor = resolveSession.resolveToDescriptor(cclass) as ClassDescriptor
        val constructorDescriptor = classDescriptor.unsubstitutedPrimaryConstructor
            ?: error("Can't get primary constructor for descriptor '$classDescriptor' in from class '${cclass.getElementTextWithContext()}'")
        ForceResolveUtil.forceResolveAllContents(constructorDescriptor)

        val primaryConstructor = cclass.primaryConstructor
        if (primaryConstructor != null) {
            val scope = resolveSession.declarationScopeProvider.getResolutionScopeForDeclaration(primaryConstructor)
            val bodyResolver = createBodyResolver(resolveSession, trace, file, StatementFilter.NONE)
            bodyResolver.resolveConstructorParameterDefaultValues(
                DataFlowInfo.EMPTY,
                trace,
                primaryConstructor,
                constructorDescriptor,
                scope,
                resolveSession.inferenceSession
            )

//            forceResolveAnnotationsInside(primaryConstructor)
        }

        return trace
    }

    private fun forceResolveAnnotationsInside(element: CjAnnotated) {
        val action: (CjAnnotationEntry) -> Unit = { entry ->
            resolveSession.bindingContext[BindingContext.ANNOTATION, entry]?.let {
                ForceResolveUtil.forceResolveAllContents(it)
            }
        }
        if (element.getContainingCjFile().isCompiled) {
            element.annotationEntries.forEach(action)
        } else {
            element.forEachDescendantOfType<CjAnnotationEntry>(
                canGoInside = { it !is CjBlockExpression },
                action = action
            )
        }
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
            ControlFlowInformationProviderImpl.Factory,
            IdeaAbsentDescriptorHandler(resolveSession.declarationProviderFactory)
        ).get()
    }

    private fun functionAdditionalResolve(
        resolveSession: ResolveSession,
        namedFunction: CjFunction,
        file: CjFile,
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

    private fun delegationSpecifierAdditionalResolve(
        resolveSession: ResolveSession, cjElement: CjElement,
        typeStatement: CjTypeStatement, file: CjFile,
        bindingTraceFilter: BindingTraceFilter
    ): BindingTrace {
        val trace = createDelegatingTrace(cjElement, bindingTraceFilter)
        val descriptor = resolveSession.resolveToDescriptor(typeStatement) as LazyClassDescriptorBase

        // 激活超类型的解析
        ForceResolveUtil.forceResolveAllContents(descriptor.typeConstructor.supertypes)

        val bodyResolver = createBodyResolver(resolveSession, trace, file, StatementFilter.NONE)
        bodyResolver.resolveSuperTypeEntryList(
            DataFlowInfo.EMPTY,
            typeStatement,
            descriptor,
            descriptor.unsubstitutedPrimaryConstructor,
            descriptor.scopeForConstructorHeaderResolution,
            descriptor.scopeForMemberDeclarationResolution,
            resolveSession.inferenceSession
        )

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
        resolveSession: ResolveSession, cjElement: CjElement,
        bindingTraceFilter: BindingTraceFilter
    ): BindingTrace {
        val trace = createDelegatingTrace(cjElement, bindingTraceFilter)

        if (cjElement is CjSimpleNameExpression) {
            val header = cjElement.findParentOfType<CjPackageDirective>(true)!!

            if (Name.isValidIdentifier(cjElement.getReferencedName())) {
//                if (trace.bindingContext[BindingContext.REFERENCE_TARGET, cjElement] == null) {
//                    val fqName = header.getFqName(cjElement)
//                    val packageDescriptor = resolveSession.moduleDescriptor.getPackage(fqName)
//                    trace.record(BindingContext.REFERENCE_TARGET, cjElement, packageDescriptor)
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

    override fun resolveMainFunctionBody(function: CjMainFunction): BindingContext {
        return getElementsAdditionalResolve(function, null, FULL)
    }

    override fun resolveFunctionBody(function: CjNamedFunction): BindingContext {
        return getElementsAdditionalResolve(function, null, FULL)
    }

    companion object {
        @set:TestOnly
        var forceFullAnalysisModeInTests: Boolean = false
    }


    private class BodyResolveContextForLazy(
        private val topDownAnalysisMode: TopDownAnalysisMode,
        private val declaringScopes: Function1<CjDeclaration, LexicalScope?>
    ) : BodiesResolveContext {

        override val files: Collection<CjFile> = setOf()
        override val endSecondaryConstructors: MutableMap<CjEndSecondaryConstructor, ClassConstructorDescriptor> =
            hashMapOf()
        override val primaryConstructors: MutableMap<CjPrimaryConstructor, ClassConstructorDescriptor> = hashMapOf()
        override val secondaryConstructors: MutableMap<CjSecondaryConstructor, ClassConstructorDescriptor> = hashMapOf()
        override val declaredClasses: MutableMap<CjTypeStatement, ClassDescriptorWithResolutionScopes> = hashMapOf()
        override val properties: MutableMap<CjProperty, PropertyDescriptor> = hashMapOf()
        override val variables: MutableMap<CjVariable, VariableDescriptor> = hashMapOf()
        override val functions: MutableMap<CjNamedFunction, SimpleFunctionDescriptor> = hashMapOf()
        override val mainFunctions: MutableMap<CjMainFunction, SimpleFunctionDescriptor> = hashMapOf()
        override val typeAliases: MutableMap<CjTypeAlias, TypeAliasDescriptor> = hashMapOf()


        override fun getDeclaringScope(declaration: CjDeclaration): LexicalScope? = declaringScopes(declaration)


        override fun getOuterDataFlowInfo(): DataFlowInfo = DataFlowInfo.EMPTY

        override fun getTopDownAnalysisMode() = topDownAnalysisMode

        override fun getLocalContext(): ExpressionTypingContext? = null
    }

}
