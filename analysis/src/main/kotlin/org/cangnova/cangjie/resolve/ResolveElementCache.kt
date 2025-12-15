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

package org.cangnova.cangjie.resolve

import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectRootModificationTracker
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.registry.Registry
import com.intellij.psi.util.CachedValue
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import com.intellij.psi.util.findParentOfType
import com.intellij.util.containers.CollectionFactory
import org.cangnova.cangjie.cache.trackers.CangJieCodeBlockModificationListener

import org.cangnova.cangjie.container.get
import org.cangnova.cangjie.context.SimpleGlobalContext
import org.cangnova.cangjie.context.withModule
import org.cangnova.cangjie.context.withProject
import org.cangnova.cangjie.descriptors.*
import org.cangnova.cangjie.descriptors.macro.MacroDescriptor
import org.cangnova.cangjie.frontend.createContainerForBodyResolve
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.psi.*
import org.cangnova.cangjie.psi.psiUtil.findElementOfAdditionalResolve
import org.cangnova.cangjie.psi.psiUtil.forEachDescendantOfType
import org.cangnova.cangjie.psi.psiUtil.getElementTextWithContext
import org.cangnova.cangjie.psi.psiUtil.getNonStrictParentOfType
import org.cangnova.cangjie.resolve.binding.BindingContext
import org.cangnova.cangjie.resolve.binding.BindingTrace
import org.cangnova.cangjie.resolve.binding.BindingTraceFilter
import org.cangnova.cangjie.resolve.binding.BindingTraceForBodyResolve
import org.cangnova.cangjie.resolve.binding.BodiesResolveContext
import org.cangnova.cangjie.resolve.caches.CodeFragmentAnalyzer
import org.cangnova.cangjie.resolve.caches.SLRUCache
import org.cangnova.cangjie.resolve.caches.analyzeControlFlow
import org.cangnova.cangjie.resolve.calls.smartcasts.DataFlowInfo
import org.cangnova.cangjie.resolve.calls.util.languageVersionSettings
import org.cangnova.cangjie.resolve.controlFlow.ControlFlowInformationProviderImpl
import org.cangnova.cangjie.resolve.lazy.*
import org.cangnova.cangjie.resolve.lazy.BodyResolveMode.*
import org.cangnova.cangjie.resolve.lazy.descriptors.LazyClassDescriptorBase
import org.cangnova.cangjie.resolve.scopes.LexicalScope
import org.cangnova.cangjie.resolve.AnalysisContextCapability
import org.cangnova.cangjie.types.expressions.ExpressionTypingContext
import org.cangnova.cangjie.utils.isUnitTestMode
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
    CjTypePattern::class,
    CjPackageDirective::class,


    )

class ResolveElementCache(
    private val resolveSession: ResolveSession,
    private val project: Project,

    private val codeFragmentAnalyzer: CodeFragmentAnalyzer
) : BodyResolveCache {
    private val cacheDependencies = listOfNotNull(
        resolveSession.exceptionTracker,
        ProjectRootModificationTracker.getInstance(project),
        // 对于源码上下文，添加代码块修改追踪器
        resolveSession.moduleDescriptor.getCapability(AnalysisContextCapability)?.let { context ->
            if (context.isSourceContext) {
                CangJieCodeBlockModificationListener.getInstance(project).cangjieOutOfCodeBlockTracker
            } else null
        }
    ).toTypedArray()
    val traceSize get() = resolveSession.trace.size

    /**
     * 表示一个缓存的部分解析结果，用于存储绑定上下文、文件信息和解析模式。
     *
     * @property bindingContext 当前缓存的绑定上下文。
     * @property mode 当前缓存的解析模式。
     * @constructor 初始化缓存对象，同时记录文件的修改戳。
     */
    private class CachedPartialResolve(
        val bindingContext: BindingContext,
        file: CjFile,
        val mode: BodyResolveMode
    ) {
        // 文件的修改戳，用于判断缓存是否过期。
        private val modificationStamp: Long = modificationStamp(file)

        /**
         * 检查缓存是否是最新的。
         *
         * @param file 要检查的文件。
         * @param newMode 新的解析模式。
         * @return 如果缓存是最新的，则返回 true；否则返回 false。
         */
        fun isUpToDate(file: CjFile, newMode: BodyResolveMode): Boolean {
            return modificationStamp == modificationStamp(file) && mode.doesNotLessThan(newMode)
        }

        /**
         * 获取文件的修改戳。
         *
         * 对于非物理文件，如果文件被修改，MODIFICATION_COUNT 不会增加，因此需要在任何文件修改时重置数据。
         *
         * @param file 要获取修改戳的文件。
         * @return 文件的修改戳。
         */
        private fun modificationStamp(file: CjFile): Long {

            return if (!file.isPhysical)
                file.modificationStamp // 对于非物理文件，直接使用文件的修改戳。
            else
                file.inBlockModificationCount // 对于物理文件，使用块级修改计数。
        }

        /**
         * 返回对象的字符串表示。
         *
         * @return 缓存的部分解析结果的字符串形式，包括解析模式和修改戳。
         */
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
            {
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

    //在“代码块外”的更改后清空整个缓存，每个条目都会使用自己的修改戳进行检查。
    private val fullResolveCache: CachedValue<MutableMap<CjElement, CachedFullResolve>> =
        CachedValuesManager.getManager(project).createCachedValue(
            {
                CachedValueProvider.Result.create(
                    CollectionFactory.createConcurrentWeakKeySoftValueMap(),
                    cacheDependencies
                )
            },
            false
        )

    private fun findElementOfAdditionalResolve(element: CjElement, bodyResolveMode: BodyResolveMode): CjElement? {
        if (element is CjAnnotation && bodyResolveMode == PARTIAL_NO_ADDITIONAL)
            return element

        return element.findElementOfAdditionalResolve()
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

        ensureFileAnnotationsResolved(element.getContainingCjFile())

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

                !EXCLUDED_TYPES.any { kclass ->
                    kclass == it::class

                }
//                it !is CjAnonymousInitializer && it !is CjDestructuringDeclaration && it !is CjDestructuringDeclarationEntry
            }?.let { resolveSession.resolveToDescriptor(it) }
            resolveSession.bindingContext
        }

        return bindingContext

    }

    private val forcedFullResolveOnHighlighting =
        Registry.`is`("cangjie.resolve.force.full.resolve.on.highlighting", true)

    /**
     * 执行元素的附加解析。
     *
     * @param resolveElement 需要解析的元素。
     * @param contextElement 上下文元素，用于部分解析。
     * @param contextElements 多个上下文元素集合，用于部分解析。
     * @param bodyResolveMode 解析模式，决定解析的深度和范围。
     * @return 返回解析后的绑定上下文 [BindingContext]。
     */
    private fun getElementsAdditionalResolve(
        resolveElement: CjElement,
        contextElement: CjElement?,
        contextElements: Collection<CjElement>? = null,
        bodyResolveMode: BodyResolveMode
    ): BindingContext {

        // 如果没有上下文元素且解析模式不是 FULL，则断言失败
        if (contextElements == null && contextElement == null) {
            assert(bodyResolveMode == FULL)
        }

        // 强制执行完整分析以避免对当前选中的文件进行冗余分析
        if (bodyResolveMode != FULL &&
            bodyResolveMode != PARTIAL_FOR_COMPLETION &&
            (!isUnitTestMode   || forceFullAnalysisModeInTests) &&
            forcedFullResolveOnHighlighting && DaemonCodeAnalyzerStatusService.getInstance(project).daemonRunning
        ) {
            val virtualFile = resolveElement.containingFile.virtualFile
            // 仅适用于实际（物理）文件
            if (virtualFile != null && FileEditorManager.getInstance(resolveElement.project)?.selectedFiles?.any { it == virtualFile } == true) {
                return getElementsAdditionalResolve(resolveElement, contextElement, contextElements, FULL)
            }
        }

        val fullResolveMap = fullResolveCache.value

        when (bodyResolveMode) {
            FULL -> {
                // 执行完整解析并缓存结果
                val bindingContext = performElementAdditionalResolve(resolveElement, null, FULL).first
                fullResolveMap[resolveElement] = CachedFullResolve(bindingContext, resolveElement)
                return bindingContext
            }

            else -> {
                // 如果解析元素不是声明，则返回完整解析结果
                if (resolveElement !is CjDeclaration) {
                    return getElementsAdditionalResolve(
                        resolveElement,
                        contextElement = null,
                        contextElements = null,
                        bodyResolveMode = FULL
                    )
                }

                val file = resolveElement.getContainingCjFile()
                // 获取需要解析的语句列表
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

                // 检查缓存结果是否已是最新的
                if (cachedResults.all { it != null && it.isUpToDate(file, bodyResolveMode) }) {
                    // 部分解析结果已缓存
                    return CompositeBindingContext.create(cachedResults.map { it!!.bindingContext }.distinct())
                }

                // 执行部分解析
                val (bindingContext, statementFilter) = performElementAdditionalResolve(
                    resolveElement,
                    contextElements ?: listOfNotNull(contextElement),
                    bodyResolveMode
                )

                // 如果没有语句过滤器且解析模式提供了控制流分析和诊断，则将部分解析结果视为完整解析
                if (statementFilter == StatementFilter.NONE &&
                    bodyResolveMode.doControlFlowAnalysis && !bodyResolveMode.bindingTraceFilter.ignoreDiagnostics
                ) {
                    fullResolveMap[resolveElement] = CachedFullResolve(bindingContext, resolveElement)
                    return bindingContext
                }

                val partialBodyResolveCacheValue = partialBodyResolveCache.value
                val expressionsMap = synchronized(partialBodyResolveCacheValue) {
                    partialBodyResolveCacheValue[file]
                }

                val resolveToCache = CachedPartialResolve(bindingContext, file, bodyResolveMode)

                // 更新缓存
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

                // 使用整个声明键来获取不在任何块内的解析结果（例如默认参数值）
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

            is CjMacroDeclaration -> functionAdditionalResolve(
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

            is CjCodeFragment -> codeFragmentAdditionalResolve(resolveElement, bodyResolveMode)

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

        // 注意：bodyResolver.resolveProperty 是私有方法，这里需要另一种方式
        // 通过 resolveSession 解析属性体
        // bodyResolver.resolveProperty(bodyResolveContext, property, descriptor)

        forceResolveAnnotationsInside(property)

        val lvs = resolveSession.languageVersionSettings
        for (accessor in property.accessors) {
            ControlFlowInformationProviderImpl(
                accessor, trace, lvs, /*resolveSession.platformDiagnosticSuppressor*/
            ).checkDeclaration()
        }

        return trace
    }

    /**
     * 对类型约束进行额外的解析处理
     * 此函数旨在针对类型约束（CjTypeConstraint）进行更深层次的解析，以确保所有相关类型参数被完全解析
     *
     * @param analyzer 代码分析器，用于解析类型约束及其相关类型参数
     * @param typeConstraint 类型约束对象，表示需要进行额外解析的类型约束条件
     * @return 返回解析过程中的绑定追踪对象，包含解析过程中的所有绑定信息
     */
    private fun typeConstraintAdditionalResolve(
        analyzer: CangJieCodeAnalyzer,
        typeConstraint: CjTypeConstraint
    ): BindingTrace {
        // 获取类型约束所属的声明对象，用于后续的详细解析
        val declaration = typeConstraint.findParentOfType<CjDeclaration>(true)!!
        // 解析声明对象，获取其描述符，以便访问类型参数信息
        val descriptor = analyzer.resolveToDescriptor(declaration) as ClassifierDescriptorWithTypeParameters

        // 遍历声明的类型参数描述符，确保每个类型参数都被强制解析
        for (parameterDescriptor in descriptor.declaredTypeParameters) {
            ForceResolveUtil.forceResolveAllContents(parameterDescriptor)
        }

        // 返回解析过程中产生的绑定追踪对象，以便进一步处理或记录
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

    /**
     * 强制解析给定元素内的注解
     *
     * 此函数的目的是确保在给定的代码元素内，所有注解的解析都已完成并可用
     * 它根据元素是否在编译文件中，采取不同的策略来遍历和解析注解
     *
     * @param element 要解析注解的代码元素，类型为[CjAnnotated]
     */
    private fun forceResolveAnnotationsInside(element: CjAnnotated) {
        // 定义一个对注解条目执行强制解析的操作
        val action: (CjAnnotation) -> Unit = { entry ->
            // 尝试从解析会话的绑定上下文中获取注解信息，并执行强制解析
            resolveSession.bindingContext[BindingContext.ANNOTATION, entry]?.let {
                ForceResolveUtil.forceResolveAllContents(it)
            }
        }

        // 根据元素是否在编译文件中，选择不同的处理方式
        if (element.getContainingCjFile().isCompiled) {
            // 如果元素在编译文件中，直接遍历其所有注解条目并执行解析操作
            element.annotationEntries.forEach(action)
        } else {
            // 如果元素不在编译文件中，使用更复杂的遍历方式来解析注解
            // 这种方式可以深入到元素的后代节点中，但不会进入[CjBlockExpression]类型节点
            element.forEachDescendantOfType<CjAnnotation>(
                canGoInside = { it !is CjBlockExpression },
                action = action
            )
        }
    }

    /**
     * 创建一个用于解析函数体的解析器
     *
     * @param resolveSession 解析会话，包含解析过程中需要的上下文信息
     * @param trace 绑定跟踪对象，用于记录解析过程中的绑定信息
     * @param file 正在解析的Cj文件对象
     * @param statementFilter 语句过滤器，用于过滤需要解析的语句
     * @return 返回一个BodyResolver对象，用于解析函数体
     *
     * 此函数负责初始化和配置函数体解析器它需要解析会话、绑定跟踪对象、
     * Cj文件对象和语句过滤器作为输入参数通过这些参数，函数创建了一个合适的
     * BodyResolver实例，用于后续的函数体解析工作
     */
    private fun createBodyResolver(
        resolveSession: ResolveSession,
        trace: BindingTrace,
        file: CjFile,
        statementFilter: StatementFilter
    ): BodyResolver {
        // 创建全局上下文，包含存储管理器和异常跟踪器
        val globalContext = SimpleGlobalContext(resolveSession.storageManager, resolveSession.exceptionTracker)
        // 获取当前解析会话的模块描述符
        val module = resolveSession.moduleDescriptor
        // 创建并配置函数体解析器容器
        return createContainerForBodyResolve(
            globalContext.withProject(file.project).withModule(module),
            trace,
            // targetPlatform,
            statementFilter,
            PlatformDependentAnalyzerServicesImpl,
            // targetPlatform.findAnalyzerServices(file.project),
            resolveSession.languageVersionSettings,
            // IdeaModuleStructureOracle(),
            // IdeSealedClassInheritorsProvider,
            ControlFlowInformationProviderImpl.Factory,
            IdeaAbsentDescriptorHandler(resolveSession.declarationProviderFactory)
        ).get()
    }


    /**
     * 执行函数的额外解析逻辑。
     *
     * 该函数负责在给定的解析会话中，对指定的函数进行额外的解析处理，主要包括解析函数体和解析注解等。
     *
     * @param resolveSession 解析会话，用于跟踪解析过程的状态。
     * @param namedFunction 需要进行额外解析的函数。
     * @param file 函数所属的文件，用于获取文件级别的上下文信息。
     * @param statementFilter 语句过滤器，用于过滤需要解析的语句。
     * @param bindingTraceFilter 绑定追踪过滤器，用于过滤绑定追踪信息。
     * @return 返回更新后的绑定追踪对象，包含解析过程中收集的绑定信息。
     */
    private fun functionAdditionalResolve(
        resolveSession: ResolveSession,
        namedFunction: CjFunction,
        file: CjFile,
        statementFilter: StatementFilter,
        bindingTraceFilter: BindingTraceFilter
    ): BindingTrace {
        // 创建一个委托追踪对象，用于过滤和处理绑定信息。
        val trace = createDelegatingTrace(namedFunction, bindingTraceFilter)

        // 获取函数声明的解析作用域。
        val scope = resolveSession.declarationScopeProvider.getResolutionScopeForDeclaration(namedFunction)
        // 解析函数声明，获取函数描述符。
        val functionDescriptor = resolveSession.resolveToDescriptor(namedFunction) as FunctionDescriptor
        // 强制解析函数描述符的所有内容，确保完全解析。
        ForceResolveUtil.forceResolveAllContents(functionDescriptor)

        // 创建函数体解析器。
        val bodyResolver = createBodyResolver(resolveSession, trace, file, statementFilter)
        // 解析函数体，收集绑定信息。
        bodyResolver.resolveFunctionBody(DataFlowInfo.EMPTY, trace, namedFunction, functionDescriptor, scope, null)

        // 强制解析函数内部的注解。
        forceResolveAnnotationsInside(namedFunction)

        // 返回更新后的绑定追踪对象。
        return trace
    }

    /**
     * 在委托属性解析过程中，为指定的类型声明执行额外的解析步骤
     * 此函数主要用于增强类型声明的解析，确保所有相关描述符和超类型被正确解析
     *
     * @param resolveSession 解析会话，用于执行解析操作
     * @param cjElement 当前正在解析的元素
     * @param typeStatement 需要额外解析的类型声明
     * @param file 当前解析的文件
     * @param bindingTraceFilter 用于过滤绑定跟踪的过滤器
     * @return 返回一个委托后的绑定跟踪对象
     */
    private fun delegationSpecifierAdditionalResolve(
        resolveSession: ResolveSession, cjElement: CjElement,
        typeStatement: CjTypeStatement, file: CjFile,
        bindingTraceFilter: BindingTraceFilter
    ): BindingTrace {
        // 创建一个委托跟踪，用于记录解析过程中的绑定信息
        val trace = createDelegatingTrace(cjElement, bindingTraceFilter)
        // 解析类型声明到描述符
        val descriptor = resolveSession.resolveToDescriptor(typeStatement) as LazyClassDescriptorBase

        // 激活超类型的解析
        ForceResolveUtil.forceResolveAllContents(descriptor.typeConstructor.supertypes)

        // 创建一个体解析器，用于解析类型声明的超类型条目列表
        val bodyResolver = createBodyResolver(resolveSession, trace, file, StatementFilter.NONE)
        // 解析超类型条目列表，确保所有超类型被正确解析
        bodyResolver.resolveSuperTypeEntryList(
            DataFlowInfo.EMPTY,
            typeStatement,
            descriptor,
            descriptor.unsubstitutedPrimaryConstructor,
            descriptor.scopeForConstructorHeaderResolution,
            descriptor.scopeForMemberDeclarationResolution,
            resolveSession.inferenceSession
        )

        // 返回委托后的绑定跟踪对象
        return trace
    }

    /*
   注意，目前我们必须使用自定义 trace 重新创建容器，以禁止在顶级 trace（来自 DI 容器的 trace）中解析主体。
    在顶级 trace 中解析主体可能会导致内存泄漏和不正确的解析，
    因为顶级 trace 不会在代码块内的修改时失效（而主体解析必然会失效）
    。 */
    private fun createDelegatingTrace(resolveElement: CjElement, filter: BindingTraceFilter): BindingTrace {
        return resolveSession.storageManager.createSafeTrace(
            BindingTraceForBodyResolve(
                resolveSession.bindingContext,
                AnalyzingUtils.formDebugNameForBindingTrace("trace to resolve element", resolveElement),
                filter
            )
        )
    }

    /**
     * 在给定的解析会话中，为指定的元素提供额外的解析信息记录
     * 此函数主要用于处理包引用的解析，特别是当元素是包指令中的简单名称表达式时
     *
     * @param resolveSession 解析会话，用于获取模块描述符等信息
     * @param cjElement 需要进行解析的CJ元素
     * @param bindingTraceFilter 过滤器，用于处理绑定跟踪信息
     * @return 返回一个委托跟踪对象，用于记录解析过程中的绑定信息
     */
    private fun packageRefAdditionalResolve(
        resolveSession: ResolveSession, cjElement: CjElement,
        bindingTraceFilter: BindingTraceFilter
    ): BindingTrace {
        // 创建一个委托跟踪对象，用于记录与cjElement相关的绑定信息
        val trace = createDelegatingTrace(cjElement, bindingTraceFilter)

        // 如果cjElement是一个简单的名称表达式，则尝试解析它所引用的包
        if (cjElement is CjSimpleNameExpression) {
            // 获取包含cjElement的包指令
            val header = cjElement.findParentOfType<CjPackageDirective>(true)!!

            // 检查cjElement的引用名称是否是一个有效的标识符
            if (Name.isValidIdentifier(cjElement.referencedName)) {
                // 如果当前元素的引用目标尚未被解析，则进行解析
                if (trace.bindingContext[BindingContext.REFERENCE_TARGET, cjElement] == null) {
                    // 获取当前元素所引用的全限定名
                    val fqName = header.getFqName(cjElement)
                    // 获取对应的包描述符
                    val packageDescriptor = resolveSession.moduleDescriptor.getPackage(fqName)
                    // 记录引用目标与包描述符的关联
                    trace.record(BindingContext.REFERENCE_TARGET, cjElement, packageDescriptor)
                }
            }
        }

        // 返回委托跟踪对象
        return trace
    }

    /**
     * 获取元素的额外解析信息，该函数是解析过程中获取特定元素附加信息的工具方法
     *
     * @param resolveElement 需要进行额外解析的元素
     * @param contextElements 可选的上下文元素集合，提供额外的解析上下文
     * @param bodyResolveMode 解析模式，决定了解析的范围和深度
     *
     * @return 返回一个更新后的绑定上下文，包含了额外解析信息
     *
     * 此函数通过调用重载函数[getElementsAdditionalResolve]，利用提供的参数来获取解析信息
     */
    private fun getElementsAdditionalResolve(
        resolveElement: CjElement,
        contextElements: Collection<CjElement>?,
        bodyResolveMode: BodyResolveMode
    ): BindingContext = getElementsAdditionalResolve(resolveElement, null, contextElements, bodyResolveMode)

    /**
     * 解析主函数体
     *
     * 此函数负责对主函数体进行解析，以获取更深层次的绑定上下文它超越了基本的解析，
     * 提供了一种方式来处理和分析主函数内的元素
     *
     * @param function 主要的函数对象，类型为CjMainFunction，代表需要解析的主函数
     * @return BindingContext 返回一个绑定上下文对象，该对象包含了解析后的信息和绑定数据
     */
    override fun resolveMainFunctionBody(function: CjMainFunction): BindingContext {
        return getElementsAdditionalResolve(function, null, FULL)
    }

    /**
     * 解析宏主体的函数
     *
     * 此函数的目的是解析宏定义中的主体部分，以便获取更详细的绑定上下文信息
     * 它通过调用[getElementsAdditionalResolve]函数来实现，对指定的宏进行额外的解析处理
     *
     * @param macro 要解析的宏声明对象，不能为空
     * @return 返回一个更新后的绑定上下文对象，包含了宏主体解析后的信息
     */
    override fun resolveMacroBody(macro: CjMacroDeclaration): BindingContext {
        return getElementsAdditionalResolve(macro, null, FULL)
    }

    /**
     * 解析函数体语义
     *
     * 此函数负责对给定的函数体进行语义解析，它是一个覆盖的方法，意味着它在子类中可能有不同的实现
     * 主要目的是通过解析函数体，获取到绑定上下文，以便于后续的代码生成和类型检查
     *
     * @param function 待解析的函数，不能为空，它是解析过程的主要输入
     * @return BindingContext 解析后的绑定上下文，包含了函数体内各种元素的语义信息
     */
    override fun resolveFunctionBody(function: CjNamedFunction): BindingContext {
        return getElementsAdditionalResolve(function, null, FULL)
    }

    companion object {
        @set:TestOnly
        var forceFullAnalysisModeInTests: Boolean = false
    }

    /**
     * BodyResolveContextForLazy类是为惰性解析过程中的函数体解析上下文。
     * 它存储了在解析过程中需要的各种描述符映射，用于跟踪不同类型的声明。
     * 此类继承自BodiesResolveContext，但所有函数和属性都是只读的，反映了其在惰性解析中的用途。
     *
     * @param topDownAnalysisMode 顶层分析模式，决定了分析的策略。
     * @param declaringScopes 一个函数，用于获取给定声明的词法作用域。
     */
    private class BodyResolveContextForLazy(
        override val topDownAnalysisMode: TopDownAnalysisMode,
        private val declaringScopes: Function1<CjDeclaration, LexicalScope?>
    ) : BodiesResolveContext {

        // 覆盖files属性，表示当前上下文中涉及的文件集合，这里设置为空集合。
        override val files: Collection<CjFile> = setOf()

        // 存储结束的次构造函数与其对应的类构造函数描述符之间的映射。
        override val endSecondaryConstructors: MutableMap<CjEndSecondaryConstructor, ClassConstructorDescriptor> =
            hashMapOf()

        // 存储主构造函数与其对应的类构造函数描述符之间的映射。
        override val primaryConstructors: MutableMap<CjPrimaryConstructor, ClassConstructorDescriptor> = hashMapOf()

        // 存储次构造函数与其对应的类构造函数描述符之间的映射。
        override val secondaryConstructors: MutableMap<CjSecondaryConstructor, ClassConstructorDescriptor> = hashMapOf()

        // 存储声明的类与其对应的类描述符之间的映射。
        override val declaredClasses: MutableMap<CjTypeStatement, ClassDescriptorWithResolutionScopes> = hashMapOf()

        // 存储属性与其对应的属性描述符之间的映射。
        override val properties: MutableMap<CjProperty, PropertyDescriptor> = hashMapOf()

        // 存储变量与其对应的变量描述符之间的映射。
        override val variables: MutableMap<CjVariable, VariableDescriptor> = hashMapOf()

        // 存储通过模式匹配的变量与其对应的变量描述符列表之间的映射。
        override val variablesByPattern: MutableMap<CjVariable, List<VariableDescriptor>> = hashMapOf()

        // 存储函数与其对应的函数描述符之间的映射。
        override val functions: MutableMap<CjNamedFunction, SimpleFunctionDescriptor> = hashMapOf()

        // 存储主函数与其对应的函数描述符之间的映射。
        override val mainFunctions: MutableMap<CjMainFunction, SimpleFunctionDescriptor> = hashMapOf()

        // 存储类型别名与其对应的类型别名描述符之间的映射。
        override val typeAliases: MutableMap<CjTypeAlias, TypeAliasDescriptor> = hashMapOf()

        // 存储宏声明与其对应的宏描述符之间的映射。
        override val macros: MutableMap<CjMacroDeclaration, MacroDescriptor> = hashMapOf()

        // 本地表达式类型上下文，对于惰性解析始终返回null
        override val localContext: ExpressionTypingContext? = null

        // 外部数据流信息，对于惰性解析始终返回空数据流信息
        override val outerDataFlowInfo: DataFlowInfo = DataFlowInfo.EMPTY

        /**
         * 获取声明的作用域。
         *
         * @param declaration 要获取作用域的声明。
         * @return 声明的作用域，如果没有则返回null。
         */
        override fun getDeclaringScope(declaration: CjDeclaration): LexicalScope? = declaringScopes(declaration)
    }
}
