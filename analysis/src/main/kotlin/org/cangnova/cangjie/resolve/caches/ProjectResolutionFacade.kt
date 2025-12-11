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

package org.cangnova.cangjie.resolve.caches

import com.intellij.openapi.diagnostic.ControlFlowException
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import org.cangnova.cangjie.context.GlobalContextImpl
import org.cangnova.cangjie.context.withProject
import org.cangnova.cangjie.descriptors.ModuleDescriptor
import org.cangnova.cangjie.descriptors.AnalysisContext
import org.cangnova.cangjie.diagnostics.DiagnosticSink
import org.cangnova.cangjie.psi.CjElement
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.resolve.AnalysisResult
import org.cangnova.cangjie.resolve.CangJieModificationTrackerService
import org.cangnova.cangjie.resolve.CompositeBindingContext
import org.cangnova.cangjie.resolve.EmptyResolverForProject
import org.cangnova.cangjie.resolve.ResolverForModule
import org.cangnova.cangjie.resolve.ResolverForProject
import org.cangnova.cangjie.storage.CancellableSimpleLock
import org.cangnova.cangjie.storage.guarded
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.ReentrantLock


class ProjectResolutionFacade(
    private val debugString: String,
    private val resolverDebugName: String,
    val project: Project,
    val globalContext: GlobalContextImpl,
//    val settings: PlatformAnalysisSettings,
    val reuseDataFrom: ProjectResolutionFacade?,
    val moduleFilter: (AnalysisContext) -> Boolean,
    dependencies: List<Any>,
    private val invalidateOnOOCB: Boolean,
    val syntheticFiles: Collection<CjFile> = listOf(),
    val allModules: Collection<AnalysisContext>? = null//空意味着从想法模型为模块创建解析器
) {

    // 创建并缓存一个值，该值用于存储项目解析器提供者
    // 此缓存值将在项目解析过程中被使用，以提高解析效率
    private val cachedValue = CachedValuesManager.getManager(project).createCachedValue(
        {
            // 计算并获取模块解析器提供者
            val resolverProvider = computeModuleResolverProvider()

            // 根据invalidateOnOOCB标志决定是否添加超出代码块的依赖
            // 如果需要，将项目依赖解析器和超出代码块的跟踪器合并
            val allDependencies = if (invalidateOnOOCB) {
                resolverForProjectDependencies + CangJieCodeBlockModificationListener.getInstance(project).cangjieOutOfCodeBlockTracker
            } else {
                resolverForProjectDependencies
            }

            // 创建并返回缓存值结果，包含解析器提供者和所有依赖
            CachedValueProvider.Result.create(resolverProvider, allDependencies)
        },
        /* trackValue = */ false
    )
    private val analysisResultsLock = ReentrantLock()
    private val resolverForProjectDependencies = dependencies + globalContext.exceptionTracker
    private val cachedResolverForProject: ResolverForProject<AnalysisContext>
        get() = globalContext.storageManager.compute { cachedValue.value }

    private val analysisResultsSimpleLock = CancellableSimpleLock(
        analysisResultsLock,
        checkCancelled = {
            ProgressManager.checkCanceled()
        },
        interruptedExceptionHandler = { throw ProcessCanceledException(it) })

    /**
     * 计算模块解析器提供者
     *
     * 此函数负责构建一个解析器实例，用于解析项目中的模块信息它通过聚合所有模块信息，
     * 过滤和解析这些模块，并考虑合成文件和模块依赖关系来完成这项任务
     *
     * @return ResolverForProject<AnalysisContext> 实例，用于解析模块信息
     */
    private fun computeModuleResolverProvider(): ResolverForProject<AnalysisContext> {
        // 初始化代理解析器，如果没有重用的数据，则使用空解析器
        val delegateResolverForProject: ResolverForProject<AnalysisContext> =
            reuseDataFrom?.cachedResolverForProject ?: EmptyResolverForProject()

        // 获取所有模块信息，如果allModules为空，则从Idea模型中获取
        val allModuleInfos = (allModules
            ?: getModuleInfosFromIdeaModel(project/*, (settings as? PlatformAnalysisSettingsImpl)?.platform*/))
            .toMutableSet()

        // 将合成文件按模块信息分组，以便后续处理
        val syntheticFilesByModule = syntheticFiles.groupBy { it.moduleInfo }
        // 获取合成文件对应的模块信息
        val syntheticFilesModules = syntheticFilesByModule.keys
        // 将合成文件对应的模块信息添加到所有模块信息中
        allModuleInfos.addAll(syntheticFilesModules)

        // 根据模块过滤条件过滤解析的模块
        val resolvedModules = allModuleInfos.filter(moduleFilter)
        // 解析模块及其依赖关系
        val resolvedModulesWithDependencies = resolvedModules /*+
            listOfNotNull(ScriptDependenciesInfo.ForProject.createIfRequired(project, resolvedModules))*/

        // 返回模块解析器实例
        return IdeaResolverForProject(
            resolverDebugName,
            globalContext.withProject(project),
            resolvedModulesWithDependencies,
            syntheticFilesByModule,
            delegateResolverForProject,
            /*      if (invalidateOnOOCB)*/
            CangJieModificationTrackerService.getInstance(project).outOfBlockModificationTracker /*else JavaLibraryModificationTracker.getInstance(
            project
        ),*/
//        settings
        )
    }


    internal fun getResolverForProject(): ResolverForProject<AnalysisContext> = cachedResolverForProject
    internal fun resolverForModuleInfo(context: AnalysisContext) = cachedResolverForProject.resolverForModule(moduleInfo)


    private val analysisResults = CachedValuesManager.getManager(project).createCachedValue(
        {
            val resolverForProject = cachedResolverForProject

            val results = object : SLRUCache<CjFile, PerFileAnalysisCache>(2, 3) {
                private val lock = ReentrantLock()

                override fun createValue(file: CjFile): PerFileAnalysisCache {
                    return PerFileAnalysisCache(
                        file,
                        resolverForProject.resolverForModule(file.moduleInfo).componentProvider
                    )

                }

                override fun getIfCached(key: CjFile): PerFileAnalysisCache? {
                    if (lock.tryLock()) {
                        try {
                            return super.getIfCached(key)
                        } finally {
                            lock.unlock()
                        }
                    }
                    return null
                }

                override fun get(key: CjFile): PerFileAnalysisCache {
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

    internal fun findModuleDescriptor(ideaModuleInfo: AnalysisContext): ModuleDescriptor {
        return cachedResolverForProject.descriptorForModule(ideaModuleInfo)
    }

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
//        return AnalysisResult.success(bindingContext, moduleDescriptor)
        return AnalysisResult.success(bindingContext, findModuleDescriptor(elements.first().moduleInfo))

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
        return AnalysisResult.success(bindingContext, findModuleDescriptor(element.moduleInfo))

//        return AnalysisResult.success(bindingContext, moduleDescriptor)
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

    internal fun resolverForElement(element: PsiElement): ResolverForModule {


        val moduleInfos = mutableSetOf<AnalysisContext>()


        val elementModuleInfos = ModuleInfoProvider.getInstance(element.project).collect(
            element,
            config =/* config ?:*/ ModuleInfoProvider.Configuration.Default,
        )

        for (result in elementModuleInfos) {
            val moduleInfo = result.getOrNull()
            if (moduleInfo != null) {
                val resolver = cachedResolverForProject.tryGetResolverForModule(moduleInfo)
                if (resolver != null) {
                    return resolver
                } else {
                    moduleInfos += moduleInfo
                }
            }

            val error = result.exceptionOrNull()
            if (error != null) {
                LOG.warn("Could not find correct module information", error)
            }
        }

        val containingFile = element.containingFile as? CjFile
        return cachedResolverForProject.tryGetResolverForModule(NotUnderContentRootModuleInfo(project, containingFile))
            ?: cachedResolverForProject.diagnoseUnknownModuleInfo(moduleInfos.toList())
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
