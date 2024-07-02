package com.huawei.cangjie.resolve.caches

import com.huawei.cangjie.analyzer.AnalysisResult
import com.huawei.cangjie.analyzer.createModuleDescriptor
import com.huawei.cangjie.container.ComponentProvider
import com.huawei.cangjie.context.GlobalContextImpl
import com.huawei.cangjie.context.withModule
import com.huawei.cangjie.context.withProject
import com.huawei.cangjie.descriptors.DiagnosticSink
import com.huawei.cangjie.frontend.createContainerForLazyResolve
import com.huawei.cangjie.idea.cache.trackers.CangJieCodeBlockModificationListener
import com.huawei.cangjie.psi.CjElement
import com.huawei.cangjie.psi.CjFile
import com.huawei.cangjie.resolve.CodeAnalyzerInitializer
import com.huawei.cangjie.resolve.CompositeBindingContext
import com.huawei.cangjie.resolve.lazy.IdeaAbsentDescriptorHandler
import com.huawei.cangjie.resolve.lazy.declarations.DeclarationProviderFactoryService
import com.huawei.cangjie.storage.CancellableSimpleLock
import com.huawei.cangjie.storage.guarded
import com.intellij.openapi.diagnostic.ControlFlowException
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import com.intellij.util.containers.SLRUCache
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.ReentrantLock


class ProjectResolutionFacade(
    private val debugString: String,
    private val resolverDebugName: String,
    val project: Project,
    val globalContext: GlobalContextImpl,
//    val settings: PlatformAnalysisSettings,
//    val reuseDataFrom: ProjectResolutionFacade?,
//    val moduleFilter: (IdeaModuleInfo) -> Boolean,
    dependencies: List<Any>,
    private val invalidateOnOOCB: Boolean,
    val syntheticFiles: Collection<CjFile> = listOf(),
//    val allModules: Collection<IdeaModuleInfo>? = null // null means create resolvers for modules from idea model
) {
    private val analysisResultsLock = ReentrantLock()
    private val resolverForProjectDependencies = dependencies + globalContext.exceptionTracker

    private val analysisResultsSimpleLock = CancellableSimpleLock(analysisResultsLock,
        checkCancelled = {
            ProgressManager.checkCanceled()
        },
        interruptedExceptionHandler = { throw ProcessCanceledException(it) })

//    private val cachedValue = CachedValuesManager.getManager(project).createCachedValue(
//        {
//            val resolverProvider = computeModuleResolverProvider()
//            val allDependencies = if (invalidateOnOOCB) {
//                resolverForProjectDependencies + KotlinCodeBlockModificationListener.getInstance(project).kotlinOutOfCodeBlockTracker
//            } else {
//                resolverForProjectDependencies
//            }
//            CachedValueProvider.Result.create(resolverProvider, allDependencies)
//        },
//        /* trackValue = */ false
//    )

//    private val cachedResolverForProject: ResolverForProject<IdeaModuleInfo>
//        get() = globalContext.storageManager.compute { cachedValue.value }
//

    val moduleDescriptor = createModuleDescriptor(globalContext.withProject(project), project)
    var componentProvider: ComponentProvider? = null
    private val analysisResults = CachedValuesManager.getManager(project).createCachedValue(
        {

//            val resolverForProject = cachedResolverForProject
            val results = object : SLRUCache<CjFile, PerFileAnalysisCache>(2, 3) {
                private val lock = ReentrantLock()

                override fun createValue(file: CjFile): PerFileAnalysisCache {
//                    TODO()

                    val trace = CodeAnalyzerInitializer.getInstance(project).createTrace()


                    val declarationProviderFactory = DeclarationProviderFactoryService.createDeclarationProviderFactory(
                        project, globalContext.storageManager, syntheticFiles,
//                        moduleContentScope,
                        GlobalSearchScope.fileScope(file)


                    )
                    componentProvider = createContainerForLazyResolve(
                        globalContext.withProject(project)
                            .withModule(moduleDescriptor),
                        trace, declarationProviderFactory, IdeaAbsentDescriptorHandler::class.java
                    )
                    return PerFileAnalysisCache(
                        file,
                        componentProvider!!
//                        TODO()
//

//                        createContainer("cangjie", PlatformDependentAnalyzerServicesImpl){

//                            useInstance(trace)
//
//                            configureStandardResolveComponents()
//                        }
//                        resolverForProject.resolverForModule(file.moduleInfo).componentProvider
                    )
                }

                override fun getIfCached(key: CjFile?): PerFileAnalysisCache? {
                    if (lock.tryLock()) {
                        try {
                            return super.getIfCached(key)
                        } finally {
                            lock.unlock()
                        }
                    }
                    return null
                }

                override fun get(key: CjFile?): PerFileAnalysisCache {
                    lock.lock()
                    try {
                        val cache = super.get(key)
                        if (cache.isValid) {
                            return cache
                        }
                        remove(key)
                        return super.get(key)
                    } finally {
                        lock.unlock()
                    }
                }

            }


            val allDependencies = resolverForProjectDependencies +
                    CangJieCodeBlockModificationListener.getInstance(project).cangjieOutOfCodeBlockTracker
            CachedValueProvider.Result.create(results, allDependencies)
        }, false
    )

    internal fun getAnalysisResultsForElements(
        elements: Collection<CjElement>,
        callback: DiagnosticSink.DiagnosticsCallback? = null
    ): AnalysisResult {
        assert(elements.isNotEmpty()) { "elements collection should not be empty" }

        val cache = analysisResultsSimpleLock.guarded { analysisResults.value!! }
        val results = elements.map { analysisResultForElement(it, cache, callback) }
        val bindingContext = CompositeBindingContext.create(results.map { it.bindingContext })
        results.firstOrNull { it.isError() }?.let {
            return AnalysisResult.internalError(bindingContext, it.error)
        }

        //TODO: (module refactoring) several elements are passed here in debugger
        return AnalysisResult.success(bindingContext, moduleDescriptor)
    }

    internal fun getAnalysisResultsForElement(
        element: CjElement,
        callback: DiagnosticSink.DiagnosticsCallback? = null
    ): AnalysisResult {
        val cache = analysisResultsSimpleLock.guarded {
            analysisResults.value!!
        }
        val result = analysisResultForElement(element, cache, callback)
        val bindingContext = result.bindingContext
        result.takeIf { it.isError() }?.let {
            return AnalysisResult.internalError(bindingContext, it.error)
        }

        //TODO: (module refactoring) several elements are passed here in debugger
        return AnalysisResult.success(bindingContext, moduleDescriptor)
    }

    private fun analysisResultForElement(
        element: CjElement,
        cache: SLRUCache<CjFile, PerFileAnalysisCache>,
        callback: DiagnosticSink.DiagnosticsCallback?
    ): AnalysisResult {
        val containingCjFile = element.getContainingCjFile()
        val perFileCache = cache[containingCjFile]
        return try {
            perFileCache.getAnalysisResults(element, callback)
        } catch (e: Throwable) {
            if (e is ControlFlowException) {
                throw e
            }
            val actualCache = analysisResultsSimpleLock.guarded {
                analysisResults.upToDateOrNull?.get()
            }
            if (cache !== actualCache) {
                throw IllegalStateException(
                    "Cache has been invalidated during performing analysis for $containingCjFile",
                    e
                )
            }
            throw e
        }
    }

    internal fun fetchAnalysisResultsForElement(element: CjElement): AnalysisResult? {
        val cache: SLRUCache<CjFile, PerFileAnalysisCache>? =
            analysisResultsLock.tryGuarded {
                analysisResults.upToDateOrNull?.get()
            }
        val perFileCache = cache?.getIfCached(element.getContainingCjFile())
        return perFileCache?.fetchAnalysisResults(element)
    }



}

const val CHECK_CANCELLATION_PERIOD_MS: Long = 50
inline fun <T> ReentrantLock.tryGuarded(crossinline computable: () -> T): T? =
    if (tryLock(CHECK_CANCELLATION_PERIOD_MS, TimeUnit.MILLISECONDS)) {
        try {
            computable()
        } finally {
            unlock()
        }
    } else {
        null
    }
