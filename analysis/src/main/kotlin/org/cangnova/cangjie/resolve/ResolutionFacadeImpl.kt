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

import com.intellij.openapi.progress.impl.CancellationCheck.Companion.runWithCancellationCheck
import org.cangnova.cangjie.container.get
import org.cangnova.cangjie.container.getService
import org.cangnova.cangjie.descriptors.DeclarationDescriptor
import org.cangnova.cangjie.diagnostics.DiagnosticSink
import org.cangnova.cangjie.descriptors.ModuleDescriptor
import org.cangnova.cangjie.psi.CjDeclaration
import org.cangnova.cangjie.psi.CjElement
import org.cangnova.cangjie.psi.CjPatternVariable
import org.cangnova.cangjie.psi.CjPsiUtil
import org.cangnova.cangjie.resolve.caches.ProjectResolutionFacade
import org.cangnova.cangjie.resolve.caches.ResolutionFacadeModuleDescriptorProvider
import org.cangnova.cangjie.resolve.lazy.AbsentDescriptorHandler
import org.cangnova.cangjie.resolve.lazy.BodyResolveMode
import org.cangnova.cangjie.resolve.lazy.ResolveSession
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.registry.Registry
import com.intellij.psi.PsiElement
import org.cangnova.cangjie.FrontendInternals
import org.cangnova.cangjie.macro.analysis.MacroExpandedAnalysisBridge
import org.cangnova.cangjie.moduleinfo.IdeaModuleInfo
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.resolve.binding.BindingContext

class ModuleResolutionFacadeImpl(
    private val projectFacade: ProjectResolutionFacade,
    private val context: IdeaModuleInfo
) : ResolutionFacade, ResolutionFacadeModuleDescriptorProvider {
    override val moduleDescriptor: ModuleDescriptor
        get() = findModuleDescriptor(context)
    override val project: Project
        get() = projectFacade.project

    @FrontendInternals
    override fun <T : Any> getFrontendService(serviceClass: Class<T>): T = getFrontendService(context, serviceClass)

    private fun <T : Any> getFrontendService(ideaModuleInfo: IdeaModuleInfo, serviceClass: Class<T>): T {
        return projectFacade.resolverForModuleInfo(ideaModuleInfo).componentProvider.getService(serviceClass)
    }

    override fun <T : Any> getIdeService(serviceClass: Class<T>): T {
        return projectFacade.resolverForModuleInfo(context).componentProvider.create(serviceClass)
    }

    override fun findModuleDescriptor(ideaModuleInfo: IdeaModuleInfo) = projectFacade.findModuleDescriptor(ideaModuleInfo)
    override fun getResolverForProject(): ResolverForProject<IdeaModuleInfo> {
        return projectFacade.getResolverForProject()
    }

    override fun analyzeWithAllCompilerChecks(
        element: CjElement,
        callback: DiagnosticSink.DiagnosticsCallback?
    ): AnalysisResult {
        ResolveInDispatchThreadManager.assertNoResolveInDispatchThread()
        val result = runWithCancellationCheck {
            projectFacade.getAnalysisResultsForElement(element, callback)
        }
        return mergeWithExpandedAnalysis(element, result)
    }

    override fun analyzeWithAllCompilerChecks(
        elements: Collection<CjElement>,
        callback: DiagnosticSink.DiagnosticsCallback?
    ): AnalysisResult {
        ResolveInDispatchThreadManager.assertNoResolveInDispatchThread()

        val result = runWithCancellationCheck {
            projectFacade.getAnalysisResultsForElements(elements, callback)
        }
        val firstElement = elements.firstOrNull() ?: return result
        return mergeWithExpandedAnalysis(firstElement, result)
    }

    /**
     * 将展开文件的分析结果合并到源文件的分析结果中
     *
     * 使用源文件的 projectFacade 分析展开文件，确保 Resolver 包含
     * ModuleProductionSourceInfo，避免 "does not know how to resolveName" 错误。
     */
    private fun mergeWithExpandedAnalysis(element: CjElement, sourceResult: AnalysisResult): AnalysisResult {
        if (sourceResult.isError()) return sourceResult

        val bridge = MacroExpandedAnalysisBridge.getInstance(project)
        if (!bridge.isEnabled()) return sourceResult

        val sourceFile = element.containingFile as? CjFile ?: return sourceResult
        val expandedResult = analyzeExpandedFile(bridge, sourceFile) ?: return sourceResult
        return bridge.mergeAnalysisResults(sourceFile, sourceResult, expandedResult)
    }

    private fun mergeBindingContext(element: CjElement, sourceContext: BindingContext): BindingContext {
        val bridge = MacroExpandedAnalysisBridge.getInstance(project)
        if (!bridge.isEnabled()) return sourceContext

        val sourceFile = element.containingFile as? CjFile ?: return sourceContext
        // 优先 peek 缓存，缓存未命中时回退到完整分析
        val expandedResult = fetchExpandedFileAnalysis(bridge, sourceFile)
            ?: analyzeExpandedFile(bridge, sourceFile)
            ?: return sourceContext
        return bridge.mergeBindingContext(sourceFile, sourceContext, expandedResult)
    }

    /**
     * 使用源文件的 projectFacade 分析展开文件
     *
     * 关键点：
     * 1. 必须复用源文件的 projectFacade（通过 reuseDataFrom）而非走 CangJieCacheService 路由，
     *    因为展开文件不在项目源码根下，独立路由会创建不包含 ModuleProductionSourceInfo 的 Resolver。
     * 2. 必须将展开文件作为 syntheticFile 注入子 facade，否则展开文件中宏生成的新声明
     *    （如 @abc 生成的 class）不在 Stub 索引中，ResolveSession 找不到其描述符。
     * 3. 子 facade 缓存在 MacroExpandedAnalysisBridge 中，后续 peek 和分析复用同一 facade。
     */
    private fun analyzeExpandedFile(bridge: MacroExpandedAnalysisBridge, sourceFile: CjFile): AnalysisResult? {
        val expandedFile = bridge.getExpandedPsiFile(sourceFile) ?: return null
        val sourceFilePath = sourceFile.virtualFile?.path ?: return null
        return try {
            val expandedFacade = bridge.getOrCreateExpandedFacade(
                projectFacade, expandedFile, sourceFilePath
            )
            runWithCancellationCheck {
                expandedFacade.getAnalysisResultsForElement(expandedFile, null)
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Peek 展开文件的已缓存分析结果（不触发新分析）
     *
     * 供 analyze() 的 mergeBindingContext 路径使用。
     * 使用与 analyzeExpandedFile 相同的缓存 facade，
     * 如果 facade 尚不存在（analyzeWithAllCompilerChecks 未执行过），返回 null 跳过合并。
     */
    private fun fetchExpandedFileAnalysis(bridge: MacroExpandedAnalysisBridge, sourceFile: CjFile): AnalysisResult? {
        val expandedFile = bridge.getExpandedPsiFile(sourceFile) ?: return null
        val sourceFilePath = sourceFile.virtualFile?.path ?: return null
        return try {
            val expandedFacade = bridge.getExpandedFacade(sourceFilePath) ?: return null
            expandedFacade.fetchAnalysisResultsForElement(expandedFile)
        } catch (e: Exception) {
            null
        }
    }

    override fun resolveToDescriptor(
        declaration: CjDeclaration,
        bodyResolveMode: BodyResolveMode
    ): DeclarationDescriptor =
        runWithCancellationCheck {
            if (CjPsiUtil.isLocal(declaration)) {
                val bindingContext = analyze(declaration, bodyResolveMode)

                    bindingContext[BindingContext.DECLARATION_TO_DESCRIPTOR, declaration]
                        ?: getFrontendService(context, AbsentDescriptorHandler::class.java).diagnoseDescriptorNotFound(
                            declaration
                        )

            } else {
                ResolveInDispatchThreadManager.assertNoResolveInDispatchThread()

                val resolveSession =
                    projectFacade.resolverForElement(declaration).componentProvider.get<ResolveSession>()
                resolveSession.resolveToDescriptor(declaration)
            }
        }

    override fun analyze(elements: Collection<CjElement>, bodyResolveMode: BodyResolveMode): BindingContext {
        ResolveInDispatchThreadManager.assertNoResolveInDispatchThread()

        if (elements.isEmpty()) return BindingContext.EMPTY

        if (usePerFileAnalysisCache) {
            elements.singleOrNull()?.let { element ->
                fetchWithAllCompilerChecks(element)?.takeUnless { it.isError() }?.let {
                    return mergeBindingContext(element, it.bindingContext)
                }
            }
        }

        @OptIn(FrontendInternals::class)
        val resolveElementCache = getFrontendService(elements.first(), ResolveElementCache::class.java)
        val result = runWithCancellationCheck {
            resolveElementCache.resolveToElements(elements, bodyResolveMode)
        }
        return mergeBindingContext(elements.first(), result)
    }


    override fun fetchWithAllCompilerChecks(element: CjElement): AnalysisResult? {
        ResolveInDispatchThreadManager.assertNoResolveInDispatchThread()

        return runWithCancellationCheck {
            projectFacade.fetchAnalysisResultsForElement(element)
        }
    }


    companion object {
        private val usePerFileAnalysisCache = Registry.`is`("cangjie.resolveName.cache.uses.perfile.cache", true)
    }

    //    @FrontendInternals
//    override fun <T : Any> getFrontendService(element: PsiElement, serviceClass: Class<T>): T {
//        return projectFacade.resolverForElement(element).componentProvider.getService(serviceClass)
//    }
    override fun analyze(element: CjElement, bodyResolveMode: BodyResolveMode): BindingContext {

        ResolveInDispatchThreadManager.assertNoResolveInDispatchThread()

        if (usePerFileAnalysisCache) {
            fetchWithAllCompilerChecks(element)?.takeUnless { it.isError() }?.let {
                return mergeBindingContext(element, it.bindingContext)
            }
        }

        @OptIn(FrontendInternals::class)
        val resolveElementCache = getFrontendService(element, ResolveElementCache::class.java)
        val result = runWithCancellationCheck {
            resolveElementCache.resolveToElement(element, bodyResolveMode)
        }
        return mergeBindingContext(element, result)
    }


    @FrontendInternals
    override fun <T : Any> getFrontendService(element: PsiElement, serviceClass: Class<T>): T {

        return projectFacade.resolverForElement(element).componentProvider.getService(serviceClass)
    }
}


fun ResolutionFacade.findModuleDescriptor(ideaModuleInfo: IdeaModuleInfo): ModuleDescriptor {
    return (this as ResolutionFacadeModuleDescriptorProvider).findModuleDescriptor(ideaModuleInfo)
}


