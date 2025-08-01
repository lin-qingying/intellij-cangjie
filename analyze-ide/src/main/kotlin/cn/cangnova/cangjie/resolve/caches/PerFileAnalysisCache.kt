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

package cn.cangnova.cangjie.resolve.caches

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
import cn.cangnova.cangjie.resolve.AnalysisResult
import cn.cangnova.cangjie.resolve.DelegateAnalysisResult
import cn.cangnova.cangjie.container.ComponentProvider
import cn.cangnova.cangjie.container.get
import cn.cangnova.cangjie.context.GlobalContext
import cn.cangnova.cangjie.context.ModuleContext
import cn.cangnova.cangjie.context.withModule
import cn.cangnova.cangjie.context.withProject
import cn.cangnova.cangjie.descriptors.BindingTrace
import cn.cangnova.cangjie.descriptors.DeclarationDescriptorWithSource
import cn.cangnova.cangjie.descriptors.InvalidModuleException
import cn.cangnova.cangjie.descriptors.ModuleDescriptor
import cn.cangnova.cangjie.descriptors.PositioningStrategies.DECLARATION_WITH_BODY
import cn.cangnova.cangjie.diagnostics.*
import cn.cangnova.cangjie.frontend.createContainerForLazyBodyResolve
import cn.cangnova.cangjie.ide.cache.trackers.clearInBlockModifications
import cn.cangnova.cangjie.ide.cache.trackers.inBlockModifications
import cn.cangnova.cangjie.ide.cache.trackers.removeInBlockModifications
import cn.cangnova.cangjie.ide.stubindex.resolve.PluginDeclarationProviderFactory
import cn.cangnova.cangjie.psi.*
import cn.cangnova.cangjie.psi.psiUtil.parentsWithSelf
import cn.cangnova.cangjie.resolve.*
import cn.cangnova.cangjie.resolve.controlFlow.ControlFlowInformationProviderImpl
import cn.cangnova.cangjie.resolve.lazy.BodyResolveMode
import cn.cangnova.cangjie.resolve.lazy.IdeaAbsentDescriptorHandler
import cn.cangnova.cangjie.resolve.lazy.ResolveSession
import cn.cangnova.cangjie.resolve.source.getPsi
import cn.cangnova.cangjie.storage.CancellableSimpleLock
import cn.cangnova.cangjie.storage.guarded
import cn.cangnova.cangjie.types.CangJieType
import cn.cangnova.cangjie.utils.CodeFragmentUtils
import cn.cangnova.cangjie.utils.checkWithAttachment
import cn.cangnova.cangjie.utils.safeAs
import cn.cangnova.cangjie.utils.slicedMap.ReadOnlySlice
import cn.cangnova.cangjie.utils.slicedMap.WritableSlice
import java.util.concurrent.locks.ReentrantLock

/**
 * 将当前异常转换为InvalidModuleException并抛出
 *
 * 此函数用于将当前异常实例转换为InvalidModuleException类型，并执行抛出操作
 * 如果转换成功，则执行给定的操作并抛出新的异常；如果转换失败，则什么都不做
 *
 * @param action 一个高阶函数，接受一个InvalidModuleException参数，并返回一个Throwable实例
 *               此函数允许调用者在抛出异常前对异常进行处理或转换，默认实现是直接返回传入的异常
 *
 * 注意：此函数使用inline修饰，以减少额外的栈帧开销，提高性能
 */
private inline fun Throwable.throwAsInvalidModuleException(crossinline action: (InvalidModuleException) -> Throwable = { it }) {
    asInvalidModuleException()?.let {
        throw action(it)
    }
}


/**
 * 将当前异常转换为InvalidModuleException实例，如果可能的话。
 *
 * 此函数旨在处理与模块验证相关的异常，将其包装或转换为InvalidModuleException类型。
 * 它特别关注于处理InvalidModuleException和AssertionError类型的异常，
 * 以及递归地处理异常链以查找或创建InvalidModuleException实例。
 *
 * @return 当前异常作为InvalidModuleException实例，如果适用的话；否则返回null。
 */
private fun Throwable.asInvalidModuleException(): InvalidModuleException? {
    return when (this) {
        // 如果当前异常已经是InvalidModuleException类型，则直接返回。
        is InvalidModuleException -> this
        // 如果当前异常是AssertionError类型，并且消息表明存在依赖配置错误，则将其转换为InvalidModuleException。
        is AssertionError ->

            if (message?.contains("contained in his own dependencies, this is probably a misconfiguration") == true)
                InvalidModuleException(message!!)
            else null

        // 对于其他类型的异常，检查异常链的下一个异常，确保没有循环引用，并尝试将其转换为InvalidModuleException。
        else -> cause?.takeIf { it != this }?.asInvalidModuleException()
    }
}

/**
 * 每个文件的分析缓存类，用于在分析过程中缓存和管理文件的分析结果。
 *
 * @param file 当前分析的文件
 * @param componentProvider 提供组件实例的提供者
 */
internal class PerFileAnalysisCache(val file: CjFile, componentProvider: ComponentProvider) {
    // 全局上下文
    private val globalContext = componentProvider.get<GlobalContext>()

    // 当前文件的分析结果
    private var fileResult: AnalysisResult? = null

    // 模块描述符
    private val moduleDescriptor = componentProvider.get<ModuleDescriptor>()

    // 方法体解析缓存
    private val bodyResolveCache = componentProvider.get<BodyResolveCache>()

    // 代码片段分析器
    private val codeFragmentAnalyzer = componentProvider.get<CodeFragmentAnalyzer>()

    // 解析会话
    private val resolveSession = componentProvider.get<ResolveSession>()

    // 线程安全锁
    private val lock = ReentrantLock()

    // 缓存，用于存储元素的分析结果
    private val cache = HashMap<PsiElement, AnalysisResult>()

    // 插件声明提供工厂
    private val pluginDeclarationProviderFactory = componentProvider.get<PluginDeclarationProviderFactory>()

    // 可取消的简单锁，用于防止取消的情况下操作失败
    private val guardLock = CancellableSimpleLock(
        lock,
        checkCancelled = { ProgressIndicatorProvider.checkCanceled() },
        interruptedExceptionHandler = { throw ProcessCanceledException(it) }
    )

    // 检查缓存是否有效
    internal val isValid: Boolean get() = true

    /**
     * 获取指定元素的分析结果，如果存在缓存，则直接返回缓存的结果。
     *
     * @param element 需要获取分析结果的代码元素
     * @return 分析结果，如果未找到结果则返回 null
     */
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

    /**
     * 查找并返回给定可分析元素的分析结果。
     *
     * @param analyzableElement 需要查找分析结果的元素。
     * @return 如果找到分析结果则返回 [AnalysisResult]，否则返回 null。
     */
    private fun lookUp(analyzableElement: CjElement): AnalysisResult? {
        // 初始化当前元素的后代集合和需要移除的元素集合
        val descendantsOfCurrent = arrayListOf<PsiElement>()
        val toRemove = hashSetOf<PsiElement>()

        var result: AnalysisResult? = null

        // 遍历当前元素及其所有父元素
        for (current in analyzableElement.parentsWithSelf) {
            val cached = cache[current]
            if (cached != null) {
                // 如果在缓存中找到了分析结果
                result = cached
                // 将当前元素的所有后代加入到需要移除的集合中
                toRemove.addAll(descendantsOfCurrent)
                // 清空当前元素的后代集合
                descendantsOfCurrent.clear()
            }

            // 将当前元素加入到后代集合中
            descendantsOfCurrent.add(current)
        }

        // 从缓存中移除所有需要移除的元素
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
            // 忽略评估器中用于编译的代码片段，因为缓存可能导致数据一致性问题
            // 但是，来自评估器的不用于编译的代码片段（例如，用于高亮显示）应该缓存以保持评估器的当前性能。
            if (analyzableParent.isUsedForCompilationInEvaluator()) return@guarded performAnalyze(element, callback)

            // 第一步：如果适用，执行增量分析
            getIncrementalAnalysisResult(callback)?.let {

                if (it is DelegateAnalysisResult) {
                    cache[analyzableParent] = it.result
                    return@guarded it.result
                }
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

    /**
     * 判断是否为评估器中用于编译的代码片段。
     */
    private fun CjElement.isUsedForCompilationInEvaluator(): Boolean =
        containingFile is CjCodeFragment && containingFile.getCopyableUserData(CodeFragmentUtils.USED_FOR_COMPILATION_IN_IR_EVALUATOR) ?: false

    /**
     * 更新文件的缓存结果。
     */
    private fun updateFileResultFromCache() {
        // 如果文件结果不在缓存中，则从缓存中移动文件结果
        if (fileResult == null && cache.containsKey(file)) {
            fileResult = cache[file]

            // 清除整个缓存中的现有结果：
            // 如果增量分析适用，它将为文件生成单个值
            // 否则这些结果可能是过期的
            cache.clear()
        }

    }


    /**
     * 获取增量分析结果。
     *
     * @param callback 诊断回调，用于处理分析过程中产生的诊断信息。
     * @return 分析结果，如果发生错误或没有增量修改则可能返回 null。
     */
    private fun getIncrementalAnalysisResult(callback: DiagnosticSink.DiagnosticsCallback?): AnalysisResult? {
        // 从缓存中更新文件结果
        updateFileResultFromCache()

        // 获取文件中的增量修改部分
        val inBlockModifications = file.inBlockModifications

        // 如果存在增量修改部分
        if (inBlockModifications.isNotEmpty()) {
            try {
                fileResult = fileResult?.let { result ->
                    var analysisResult = result

                    // 如果当前结果有错误，则强制进行全量分析
                    if (analysisResult.isError()) return@let null

                    // 遍历每个增量修改部分
                    for (inBlockModification in inBlockModifications) {

//                        对于需要推断的方法，进行全量分析
                        if (inBlockModification is CjFunctionImpl && inBlockModification.isInferReturnType) {
                            return DelegateAnalysisResult(performAnalyze(inBlockModification, callback))
//                            return null
                        }

                        val resultCtx = analysisResult.bindingContext

                        // 检查绑定上下文是否支持增量分析
                        val stackedCtx =
                            if (resultCtx is StackedCompositeBindingContextTrace.StackedCompositeBindingContext) resultCtx else null

                        // 如果不支持增量分析，则返回 null
                        if (stackedCtx?.isIncrementalAnalysisApplicable() == false) return@let null

                        // 创建新的跟踪上下文
                        val trace: StackedCompositeBindingContextTrace =
                            if (stackedCtx != null && stackedCtx.element() == inBlockModification) {
                                val trace = stackedCtx.bindingTrace()
                                trace.clear()
                                trace
                            } else {
                                // 反映堆叠绑定上下文的深度
                                val depth = (stackedCtx?.depth() ?: 0) + 1

                                StackedCompositeBindingContextTrace(
                                    depth,
                                    element = inBlockModification,
                                    resolveContext = resolveSession.bindingContext,
                                    parentContext = resultCtx
                                )
                            }

                        // 调用回调处理父诊断信息
                        callback?.let { trace.parentDiagnosticsApartElement.forEach(it::callback) }

                        // 分析增量修改部分
                        val newResult = analyze(inBlockModification, trace, callback)
                        analysisResult = wrapResult(result, newResult, trace)
                    }

                    // 移除已处理的增量修改部分
                    file.removeInBlockModifications(inBlockModifications)

                    analysisResult
                }
            } catch (e: Throwable) {
                // 处理异常情况
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

        // 如果文件结果为空，清除增量修改部分
        if (fileResult == null) {
            file.clearInBlockModifications()
        }
        return fileResult
    }

    /**
     * 清理文件的结果缓存。
     */
    private fun clearFileResultCache() {
        file.clearInBlockModifications()
        fileResult = null
    }

    /**
     * 包装分析结果。
     */
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

    /**
     * 校验元素是否属于当前文件。
     */
    private fun check(element: CjElement) {
        checkWithAttachment(element.containingFile == file, {
            "Expected $file, but was ${element.containingFile} for ${if (element.isValid) "valid" else "invalid"} $element "
        }) {
            it.withPsiAttachment("element.cj", element)
            it.withPsiAttachment("file.cj", element.containingFile)
            it.withPsiAttachment("original.cj", file)
        }
    }

    /**
     * 执行元素分析。
     */
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

    /**
     * 分析元素并返回结果。
     */
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

    /**
     * 处理异常。
     */
    private fun handleException(e: Throwable) {
        e.throwAsInvalidModuleException()
        if (e !is ControlFlowException) {
            clearFileResultCache()
        }
        throw e
    }
}

object CangJieResolveDataProvider {

    fun findAnalyzableParent(element: CjElement): CjElement? {
        if (element is CjFile) return element

        val topmostElement = element.findTopmostParentInFile {
            it is CjNamedFunction ||
                    it is CjAnonymousInitializer ||
                    it is CjProperty ||
                    it is CjImportDirectiveItem ||
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
 * 请注意：`trace` 回退到 [resolveContext]（在解析过程中使用），该上下文中不包含该 [element] 的早期解析痕迹。
 *
 * 当 trace 转化为 [BindingContext] 时，它会回退到 [parentContext]:
 * 预期所有特定于 [element]（及其子元素）的切片都存储在此绑定上下文中，
 * 对于其他元素，它会回退到 [parentContext]。
 */
private class StackedCompositeBindingContextTrace(
    val depth: Int, // 原始 cjFile bindingContext 上的堆栈深度
    val element: CjElement, // 绑定上下文追踪的目标元素
    val resolveContext: BindingContext, // 解析上下文
    val parentContext: BindingContext // 父级上下文，用于回退
) : DelegatingBindingTrace(
    resolveContext,
    "Stacked trace for resolution of $element",
    allowSliceRewrite = true
) {
    /**
     * 实际上 StackedCompositeBindingContext 包含最新的和部分过时的上下文（parentContext）。
     *
     * 与 [DelegatingBindingTrace#bindingContext] 不同：
     * - 如果结果在当前上下文中不存在，它会回退到 [parentContext] 而不是 [resolveContext]。
     * - 诊断信息会从当前上下文和 [parentContext] 聚合。
     *
     * 注意：最新的结果存储在当前上下文（[DelegatingBindingTrace#map]）中。
     * 注意：它不会删除过时的结果，只是隐藏，因此会有一些额外的内存占用。
     */
    val stackedContext = StackedCompositeBindingContext()

    /**
     * 来自 [parentContext] 的所有诊断，排除属于 [element] 或其子元素的诊断。
     */
    val parentDiagnosticsApartElement: Collection<Diagnostic> =
        (resolveContext.diagnostics.all() + parentContext.diagnostics.all()).filterApartElement()

    /**
     * 来自 [parentContext] 的所有诊断（不受抑制），排除属于 [element] 或其子元素的诊断。
     */
    val parentDiagnosticsNoSuppressionApartElement: Collection<Diagnostic> =
        (resolveContext.diagnostics.noSuppression() + parentContext.diagnostics.noSuppression()).filterApartElement()

    /**
     * 过滤诊断信息，排除属于 [element] 或其子元素的内容。
     */
    private fun Collection<Diagnostic>.filterApartElement() =
        toSet().let { s ->
            s.filter { it.psiElement == element && selfDiagnosticToHold(it) } +
                    s.filter { it.psiElement.parentsWithSelf.none { e -> e == element } }
        }

    /**
     * 绑定上下文的内部实现，用于在上下文中存储和访问诊断和绑定信息。
     */
    inner class StackedCompositeBindingContext : BindingContext {
        var cachedDiagnostics: Diagnostics? = null // 缓存的诊断信息

        /**
         * 获取绑定追踪实例。
         */
        fun bindingTrace(): StackedCompositeBindingContextTrace = this@StackedCompositeBindingContextTrace

        /**
         * 获取当前追踪的目标元素。
         */
        fun element(): CjElement = this@StackedCompositeBindingContextTrace.element

        /**
         * 获取绑定堆栈的深度。
         */
        fun depth(): Int = this@StackedCompositeBindingContextTrace.depth

        /**
         * 判断是否可以应用增量分析。
         * 为了防止绑定上下文堆栈过深，当当前深度小于16时，增量分析适用。
         *
         * @return 如果当前深度小于16，则返回true，否则返回false。
         */
        fun isIncrementalAnalysisApplicable(): Boolean = this@StackedCompositeBindingContextTrace.depth < 16

        /**
         * 检查接收者是否是一个被重新分析的 PsiElement，因此应该在重新分析的上下文中有一个结果。
         * 当当前上下文中没有该元素的信息时，我们不应该在父上下文中查找这些元素。
         * 由于 PsiElement 的变更，这可能会导致错误的信息。
         */
        private fun <K : Any?> K.containedInReanalyzedElement(): Boolean {
            return when (element) {
                is CjDeclarationWithBody -> {
                    // 在重新分析函数体内的 Psi 元素应该只存在于重新分析上下文中。
                    val body = element.bodyExpression ?: return false
                    (this as? PsiElement)?.parentsWithSelf?.contains(body) == true
                }

                is CjTypeStatement -> {
                    // 匿名初始化器和次级构造函数内的 Psi 元素应仅在重新分析上下文中有信息。
                    (this as? PsiElement)?.parents(withSelf = false)?.any {
                        /*it in element.getAnonymousInitializers() ||*/ it in element.secondaryConstructors
                    } == true
                }

                else -> false
            }
        }

        /**
         * 获取诊断信息，若缓存不存在则合并生成。
         */
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

        /**
         * 获取绑定切片中的值。
         */
        override fun <K : Any?, V : Any?> get(slice: ReadOnlySlice<K, V>, key: K): V? {
            selfGet(slice, key)?.let { return it }
            if (!key.containedInReanalyzedElement()) {
                return parentContext.get(slice, key)?.takeIf {
                    (it as? DeclarationDescriptorWithSource)?.source?.getPsi()?.isValid != false
                }
            }
            return null
        }

        /**
         * 获取表达式的类型信息。
         */
        override fun getType(expression: CjExpression): CangJieType? {
            val typeInfo = get(BindingContext.EXPRESSION_TYPE_INFO, expression)
            return typeInfo?.type
        }

        /**
         * 获取切片的所有键。
         */
        override fun <K, V> getKeys(slice: WritableSlice<K, V>): Collection<K> {
            val keys = map.getKeys(slice)
            val fromParent = parentContext.getKeys(slice).filter {
                !it.containedInReanalyzedElement()
            }
            if (keys.isEmpty()) return fromParent
            if (fromParent.isEmpty()) return keys

            return keys + fromParent
        }

        /**
         * 获取切片的内容。
         */
        override fun <K : Any?, V : Any?> getSliceContents(slice: ReadOnlySlice<K, V>): ImmutableMap<K, V> {
            val parentSliceContents = parentContext.getSliceContents(slice).filter {
                !it.key.containedInReanalyzedElement()
            }
            val mapSliceContents = map.getSliceContents(slice)
            return ImmutableMap.copyOf(parentSliceContents + mapSliceContents)
        }

        /**
         * 将自身数据添加到目标追踪中。
         */
        override fun addOwnDataTo(trace: BindingTrace, commitDiagnostics: Boolean) =
            throw UnsupportedOperationException()
    }

    /**
     * 从当前上下文或父上下文中获取绑定数据。
     */
    override fun <K : Any?, V : Any?> get(slice: ReadOnlySlice<K, V>, key: K): V? =
        if (slice == BindingContext.ANNOTATION) {
            selfGet(slice, key) ?: parentContext.get(slice, key)
        } else {
            super.get(slice, key)
        }

    /**
     * 清除当前追踪数据。
     */
    override fun clear() {
        super.clear()
        stackedContext.cachedDiagnostics = null
    }

    companion object {
        /**
         * 检查诊断是否需要保留。
         */
        private fun selfDiagnosticToHold(d: Diagnostic): Boolean {
            val positioningStrategy = d.factory.safeAs<DiagnosticFactoryWithPsiElement<*, *>>()?.positioningStrategy
            return when (positioningStrategy) {
                DECLARATION_WITH_BODY -> false
                else -> true
            }
        }
    }
}

/**
 * [MergedDiagnostics] 类合并了多个诊断集合，并提供了对这些诊断的查询方法。
 * 它实现了 [Diagnostics] 接口，允许通过 PSI 元素获取诊断信息。
 *
 * @param diagnostics 所有诊断的集合，可能包括被抑制的诊断。
 * @param noSuppressionDiagnostics 未被抑制的诊断集合，即需要特别注意的诊断。
 * @param modificationTracker 用于跟踪自上次检查以来，源代码是否已被修改的跟踪器。
 */
private class MergedDiagnostics(
    val diagnostics: Collection<Diagnostic>,
    val noSuppressionDiagnostics: Collection<Diagnostic>,
    override val modificationTracker: ModificationTracker
) : Diagnostics {
    /**
     * [elementsCache] 用于缓存诊断元素，以提高查询效率。
     * 它是一个诊断元素缓存，根据给定的 PSI 元素返回相关的诊断信息。
     */
    private val elementsCache = DiagnosticsElementsCache(this) { true }

    /**
     * 返回所有诊断的集合。
     *
     * @return 所有诊断的集合，包括被抑制的诊断。
     */
    override fun all() = diagnostics

    /**
     * 根据给定的 PSI 元素返回相关的可变诊断集合。
     *
     * @param psiElement 要查询诊断信息的 PSI 元素。
     * @return 与给定 PSI 元素相关的诊断集合。
     */
    override fun forElement(psiElement: PsiElement): MutableCollection<Diagnostic> =
        elementsCache.getDiagnostics(psiElement)

    /**
     * 返回一个没有被抑制的诊断的 [MergedDiagnostics] 实例。
     * 如果当前实例中的未被抑制的诊断集合为空，则返回当前实例；
     * 否则，创建并返回一个新的 [MergedDiagnostics] 实例，其中只包含未被抑制的诊断。
     *
     * @return 一个没有被抑制的诊断的 [MergedDiagnostics] 实例。
     */
    override fun noSuppression() = if (noSuppressionDiagnostics.isEmpty()) {
        this
    } else {
        MergedDiagnostics(noSuppressionDiagnostics, emptyList(), modificationTracker)
    }
}
